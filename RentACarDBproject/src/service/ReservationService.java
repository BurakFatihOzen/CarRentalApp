package service;

// JavaFX listeleri ve SQL kütüphaneleri
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Reservation;
import model.Customer;
import java.sql.*;

public class ReservationService {

    // ==========================================
    // 1. REZERVASYONLARI LİSTELEME (UI İÇİN)
    // ==========================================
    // Amaç: Ana ekrandaki tabloya rezervasyonları doldurmak.
    // KRİTİK NOKTA: Veritabanında 'reservation' tablosunda sadece ID'ler (vehicle_id, customer_id) var.
    // Ancak kullanıcı ekranda "5" veya "3" görmek istemez. "BMW 320i" veya "Ahmet Yılmaz" görmek ister.
    // Bu yüzden SQL'de 'JOIN' işlemi yaparak diğer tablolardan isimleri çekiyoruz.
    public static ObservableList<Reservation> getReservationsForUI() {
        ObservableList<Reservation> list = FXCollections.observableArrayList();

        // SQL: Reservation (r) tablosunu, Vehicle (v) ve Customer (c) ile birleştir.
        String sql = "SELECT r.reservation_id, r.vehicle_id, r.reservation_status, r.start_date, " +
                "v.brand, v.model, c.full_name " +
                "FROM reservation r " +
                "JOIN vehicle v ON r.vehicle_id = v.vehicle_id " +
                "JOIN customer c ON r.customer_id = c.customer_id " +
                "ORDER BY r.reservation_id DESC"; // En son eklenen en üstte görünsün

        try (Connection conn = Db.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                // Veritabanından gelen hem ID'leri hem de İsimleri model sınıfına paketliyoruz.
                list.add(new Reservation(
                        rs.getInt("reservation_id"),
                        rs.getInt("vehicle_id"),
                        rs.getString("reservation_status"),
                        rs.getDate("start_date"),
                        rs.getString("brand"),     // JOIN'den geldi
                        rs.getString("model"),     // JOIN'den geldi
                        rs.getString("full_name")  // JOIN'den geldi
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // ==========================================
    // 2. MÜŞTERİ LİSTESİ (COMBOBOX İÇİN)
    // ==========================================
    // Amaç: Yeni rezervasyon eklerken "Müşteri Seçiniz" kutusunu (Dropdown) doldurmak.
    // Burada müşterinin tüm detaylarına (Telefon, Ehliyet) gerek yok, sadece Adı ve ID'si yeterli.
    public static ObservableList<Customer> getCustomersForCombo() {
        ObservableList<Customer> list = FXCollections.observableArrayList();
        String sql = "SELECT customer_id, full_name FROM customer ORDER BY full_name";

        try (Connection conn = Db.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                // Customer sınıfının 2 parametreli (Basit) yapıcısını kullanıyoruz.
                list.add(new Customer(rs.getInt("customer_id"), rs.getString("full_name")));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // ==========================================
    // 3. ARAMA VE FİLTRELEME
    // ==========================================
    // Amaç: Kullanıcı arama kutusuna bir şey yazdığında, bunu hem rezervasyon durumunda
    // hem de müşteri isminde aramak.
    public static ObservableList<Reservation> searchReservations(String query) {
        ObservableList<Reservation> list = FXCollections.observableArrayList();

        // ILIKE: Büyük/Küçük harf duyarsız arama.
        // Hem statüye (APPROVED, PENDING) hem de müşteri adına bakıyoruz.
        String sql = "SELECT r.reservation_id, r.vehicle_id, r.reservation_status, r.start_date, " +
                "v.brand, v.model, c.full_name " +
                "FROM reservation r " +
                "JOIN vehicle v ON r.vehicle_id = v.vehicle_id " +
                "JOIN customer c ON r.customer_id = c.customer_id " +
                "WHERE r.reservation_status ILIKE '%" + query + "%' OR c.full_name ILIKE '%" + query + "%'";

        try (Connection conn = Db.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new Reservation(
                        rs.getInt("reservation_id"), rs.getInt("vehicle_id"), rs.getString("reservation_status"),
                        rs.getDate("start_date"), rs.getString("brand"), rs.getString("model"), rs.getString("full_name")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // ==========================================
    // 4. SİLME İŞLEMİ (DELETE)
    // ==========================================
    public static void deleteReservation(int id) throws Exception {
        AuthService.requireLogin(); // Güvenlik
        Connection conn = Db.getConnection();

        try {
            // ADIM 1: Önce bu rezervasyona bağlı 'Rental' (Kiralama) kaydı varsa onu sil.
            // (Database'de CASCADE ayarlı olsa bile, kod tarafında bunu açıkça yapmak güvenli bir alışkanlıktır)
            PreparedStatement psRental = conn.prepareStatement("DELETE FROM rental WHERE reservation_id=?");
            psRental.setInt(1, id);
            psRental.executeUpdate();

            // ADIM 2: Rezervasyonun kendisini sil.
            PreparedStatement psRes = conn.prepareStatement("DELETE FROM reservation WHERE reservation_id=?");
            psRes.setInt(1, id);
            psRes.executeUpdate();

        } finally {
            conn.close(); // Bağlantıyı kapatmayı unutma
        }
    }

    // ==========================================
    // 5. ONAYLAMA (APPROVE) VE KONTROL
    // ==========================================
    // Amaç: Rezervasyonu onaylamak ama önce aracın durumu uygun mu diye bakmak.
    public static void approveReservation(int reservationId) throws Exception {
        AuthService.requireLogin();
        Connection conn = Db.getConnection();

        try {
            // KONTROL: Önce bu rezervasyondaki aracın durumuna bakıyoruz.
            String checkSql = "SELECT v.vehicle_status, v.plate FROM reservation r JOIN vehicle v ON r.vehicle_id = v.vehicle_id WHERE r.reservation_id = ?";
            PreparedStatement psCheck = conn.prepareStatement(checkSql);
            psCheck.setInt(1, reservationId);
            ResultSet rs = psCheck.executeQuery();

            if (rs.next()) {
                // Eğer araç 'MAINTENANCE' (Bakım) modundaysa, onaylamaya izin verme!
                if ("MAINTENANCE".equalsIgnoreCase(rs.getString("vehicle_status"))) {
                    throw new Exception("Bu araç (" + rs.getString("plate") + ") şu an BAKIMDA olduğu için onaylanamaz!");
                }
            } else {
                throw new Exception("Rezervasyon bulunamadı.");
            }

            // Araç uygunsa durumu 'APPROVED' yap.
            // NOT: Database'de bir TRIGGER yazdıysan, bu işlem otomatik olarak Vehicle tablosunu da günceller.
            PreparedStatement psUpdate = conn.prepareStatement("UPDATE reservation SET reservation_status='APPROVED' WHERE reservation_id=?");
            psUpdate.setInt(1, reservationId);
            psUpdate.executeUpdate();

        } finally { conn.close(); }
    }

    // ==========================================
    // 6. İPTAL ETME (CANCEL)
    // ==========================================
    public static void cancelReservation(int reservationId) throws Exception {
        AuthService.requireLogin();
        Connection conn = Db.getConnection();
        try {
            // Durumu 'CANCELLED' yapıyoruz. Kaydı silmiyoruz, geçmiş veri kalsın istiyoruz.
            PreparedStatement psUpdate = conn.prepareStatement("UPDATE reservation SET reservation_status='CANCELLED' WHERE reservation_id=?");
            psUpdate.setInt(1, reservationId);
            psUpdate.executeUpdate();
        } finally {
            conn.close();
        }
    }

    // ==========================================
    // 7. AKILLI MÜŞTERİ YÖNETİMİ
    // ==========================================
    // Amaç: Hızlı rezervasyon ekranında ehliyet numarasını girince;
    // - Eğer müşteri zaten kayıtlıysa, onun ID'sini bul getir.
    // - Eğer müşteri yoksa, onu hemen "Hızlı Kayıt" olarak veritabanına ekle ve YENİ ID'sini getir.
    public static int createCustomerAndGetId(String fullName, String phone, String licenseNo) throws Exception {
        AuthService.requireLogin();
        Connection conn = Db.getConnection();
        int newCustomerId = -1; // Hata durumu için varsayılan

        try {
            // 1. KONTROL: Bu ehliyet numarası veritabanında var mı?
            PreparedStatement check = conn.prepareStatement("SELECT customer_id FROM customer WHERE license_no = ?");
            check.setString(1, licenseNo);
            ResultSet rsCheck = check.executeQuery();

            if (rsCheck.next()) {
                // VARMIŞ! Mevcut ID'yi al.
                newCustomerId = rsCheck.getInt("customer_id");
            } else {
                // YOKMUŞ! Yeni müşteri oluştur.
                String sql = "INSERT INTO customer (full_name, phone, license_no) VALUES (?, ?, ?)";

                // RETURN_GENERATED_KEYS: "Eklediğin satırın otomatik oluşan ID'sini bana geri ver" demektir.
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, fullName);
                ps.setString(2, phone);
                ps.setString(3, licenseNo);
                ps.executeUpdate();

                // Yeni oluşan ID'yi al
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) newCustomerId = rs.getInt(1);
            }
        } finally { conn.close(); }

        return newCustomerId; // Bulunan veya yeni oluşturulan ID'yi döndür.
    }

    // ==========================================
    // 8. YENİ REZERVASYON EKLEME
    // ==========================================
    public static void addReservation(int customerId, int vehicleId, Date start, Date end, double price) throws Exception {
        AuthService.requireLogin();

        // Yeni rezervasyonlar varsayılan olarak 'PENDING' (Beklemede) statüsünde açılır.
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
}