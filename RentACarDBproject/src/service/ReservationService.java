package service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Reservation;
import model.Customer;
import java.sql.*;

public class ReservationService {

    public static ObservableList<Reservation> getReservationsForUI() {
        ObservableList<Reservation> list = FXCollections.observableArrayList();
        String sql = "SELECT r.reservation_id, r.vehicle_id, r.reservation_status, r.start_date, v.brand, v.model, c.full_name " +
                "FROM reservation r JOIN vehicle v ON r.vehicle_id = v.vehicle_id JOIN customer c ON r.customer_id = c.customer_id " +
                "ORDER BY r.reservation_id DESC";
        try (Connection conn = Db.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Reservation(rs.getInt("reservation_id"), rs.getInt("vehicle_id"), rs.getString("reservation_status"),
                        rs.getDate("start_date"), rs.getString("brand"), rs.getString("model"), rs.getString("full_name")));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // Onaylama aşamasında bakım kontrolü yapar
    public static void approveReservation(int reservationId) throws Exception {
        AuthService.requireLogin();
        try (Connection conn = Db.getConnection()) {
            // 1. Önce aracın durumunu kontrol et
            String sqlCheck = "SELECT v.vehicle_status, v.plate FROM reservation r " +
                    "JOIN vehicle v ON r.vehicle_id = v.vehicle_id WHERE r.reservation_id = ?";
            PreparedStatement psCheck = conn.prepareStatement(sqlCheck);
            psCheck.setInt(1, reservationId);
            ResultSet rs = psCheck.executeQuery();

            if (rs.next()) {
                String status = rs.getString("vehicle_status");
                // Eğer araç AVAILABLE (Müsait) değilse Java tarafında hata fırlatıyoruz
                if (!"AVAILABLE".equalsIgnoreCase(status)) {
                    throw new Exception("HATA: " + rs.getString("plate") + " plakalı araç şu an müsait değil! (Durum: " + status + ")");
                }
            }

            // 2. Eğer müsaitse güncelleme işlemini yap
            PreparedStatement psUpdate = conn.prepareStatement("UPDATE reservation SET reservation_status='APPROVED' WHERE reservation_id=?");
            psUpdate.setInt(1, reservationId);
            psUpdate.executeUpdate();
        }
    }

    // Kiralama başlatır (pickup_branch_id hatasını çözer)
    public static void startRental(int reservationId) throws Exception {
        AuthService.requireLogin();
        try (Connection conn = Db.getConnection()) {
            // 1. ARACIN GÜNCEL DURUMUNU SORGULA
            String checkSql = "SELECT v.vehicle_status, v.plate FROM reservation r " +
                    "JOIN vehicle v ON r.vehicle_id = v.vehicle_id WHERE r.reservation_id = ?";
            PreparedStatement psCheck = conn.prepareStatement(checkSql);
            psCheck.setInt(1, reservationId);
            ResultSet rs = psCheck.executeQuery();

            if (rs.next()) {
                String status = rs.getString("vehicle_status");
                // Eğer araç zaten kiradaysa, Java tarafında hata fırlat
                if ("RENTED".equalsIgnoreCase(status)) {
                    throw new Exception("HATA: " + rs.getString("plate") + " plakalı araç şu an zaten kirada!");
                }
            }

            // 2. EĞER ARAÇ KİRADA DEĞİLSE İŞLEME DEVAM ET (Mevcut kodun)
            String findBranch = "SELECT v.branch_id FROM reservation r JOIN vehicle v ON r.vehicle_id = v.vehicle_id WHERE r.reservation_id = ?";
            PreparedStatement psFind = conn.prepareStatement(findBranch);
            psFind.setInt(1, reservationId);
            ResultSet rsBranch = psFind.executeQuery();

            if (rsBranch.next()) {
                int bId = rsBranch.getInt("branch_id");
                String sql = "INSERT INTO rental (reservation_id, pickup_branch_id, dropoff_branch_id, rental_date, payment_status) VALUES (?, ?, ?, CURRENT_DATE, 'UNPAID')";
                PreparedStatement psInsert = conn.prepareStatement(sql);
                psInsert.setInt(1, reservationId);
                psInsert.setInt(2, bId);
                psInsert.setInt(3, bId);
                psInsert.executeUpdate();
            }
        }
    }

    public static void finishRental(int reservationId) throws Exception {
        AuthService.requireLogin();
        try (Connection conn = Db.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("UPDATE rental SET return_date = CURRENT_DATE, payment_status = 'PAID' WHERE reservation_id = ?");
            ps.setInt(1, reservationId);
            ps.executeUpdate();
        }
    }

    // AddReservationView içindeki "cannot find symbol" hatasını çözer
    public static int createCustomerAndGetId(String fullName, String phone, String licenseNo) throws Exception {
        AuthService.requireLogin();
        try (Connection conn = Db.getConnection()) {
            PreparedStatement check = conn.prepareStatement("SELECT customer_id FROM customer WHERE license_no = ?");
            check.setString(1, licenseNo);
            ResultSet rsCheck = check.executeQuery();
            if (rsCheck.next()) return rsCheck.getInt("customer_id");

            PreparedStatement ps = conn.prepareStatement("INSERT INTO customer (full_name, phone, license_no) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, fullName);
            ps.setString(2, phone);
            ps.setString(3, licenseNo);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    // AddReservationView içindeki tarih parametresi hatasını çözer
    public static void addReservation(int customerId, int vehicleId, java.sql.Date start, java.sql.Date end, double price) throws Exception {
        AuthService.requireLogin();
        String sql = "INSERT INTO reservation (customer_id, vehicle_id, start_date, end_date, total_price, reservation_status) VALUES (?, ?, ?, ?, ?, 'PENDING')";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.setInt(2, vehicleId);
            ps.setDate(3, start);
            ps.setDate(4, end);
            ps.setDouble(5, price);
            ps.executeUpdate();
        }
    }

    public static void cancelReservation(int reservationId) throws Exception {
        AuthService.requireLogin();
        try (Connection conn = Db.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("UPDATE reservation SET reservation_status='CANCELLED' WHERE reservation_id=?");
            ps.setInt(1, reservationId);
            ps.executeUpdate();
        }
    }

    public static void deleteReservation(int id) throws Exception {
        AuthService.requireLogin();
        try (Connection conn = Db.getConnection()) {
            PreparedStatement ps1 = conn.prepareStatement("DELETE FROM rental WHERE reservation_id=?");
            ps1.setInt(1, id); ps1.executeUpdate();
            PreparedStatement ps2 = conn.prepareStatement("DELETE FROM reservation WHERE reservation_id=?");
            ps2.setInt(1, id); ps2.executeUpdate();
        }
    }

    public static ObservableList<Reservation> searchReservations(String query) {
        ObservableList<Reservation> list = FXCollections.observableArrayList();
        String sql = "SELECT r.reservation_id, r.vehicle_id, r.reservation_status, r.start_date, v.brand, v.model, c.full_name FROM reservation r JOIN vehicle v ON r.vehicle_id = v.vehicle_id JOIN customer c ON r.customer_id = c.customer_id WHERE r.reservation_status ILIKE ? OR c.full_name ILIKE ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + query + "%");
            ps.setString(2, "%" + query + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Reservation(rs.getInt("reservation_id"), rs.getInt("vehicle_id"), rs.getString("reservation_status"),
                        rs.getDate("start_date"), rs.getString("brand"), rs.getString("model"), rs.getString("full_name")));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }
}