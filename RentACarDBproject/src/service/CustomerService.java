package service;

// JavaFX'in özel listesi. Bu listeye veri eklenince ekrandaki tablo (TableView)
// kendini otomatik günceller. Normal 'ArrayList' bunu yapamaz.
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Customer;
import java.sql.*;

public class CustomerService {

    // ==========================================
    // 1. TÜM MÜŞTERİLERİ GETİR (READ)
    // ==========================================
    // Amaç: Veritabanındaki tüm müşteri satırlarını çekip, Java nesnelerine çevirmek
    // ve ekrandaki tabloya doldurmak.
    public static ObservableList<Customer> getAllCustomers() {
        // 1. Boş bir "Gözlemlenebilir Liste" oluşturuyoruz.
        ObservableList<Customer> list = FXCollections.observableArrayList();

        // 2. SQL Sorgusu: Müşterileri ID'si en büyük olan (en son eklenen) en üstte olacak şekilde getir.
        String sql = "SELECT * FROM customer ORDER BY customer_id DESC";

        // 3. Veritabanı Bağlantısı (Try-with-resources yapısı)
        // Parantez içindeki (conn, st, rs) nesneleri iş bitince otomatik kapanır.
        try (Connection conn = Db.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) { // executeQuery: Sadece veri okumak için kullanılır.

            // 4. Satır satır okuma döngüsü
            // rs.next() her çalıştığında bir sonraki satıra geçer. Satır varsa 'true' döner.
            while (rs.next()) {
                // Veritabanından gelen satırı (rs), Java nesnesine (Customer) dönüştürüyoruz.
                // rs.getInt("col_name") -> O sütundaki veriyi al.
                list.add(new Customer(
                        rs.getInt("customer_id"),
                        rs.getString("full_name"),
                        rs.getString("phone"),
                        rs.getString("license_no")
                ));
            }
        } catch (Exception e) {
            // Bir hata olursa (bağlantı kopması vs.) konsola kırmızı hata mesajı bas.
            e.printStackTrace();
        }

        // 5. Dolu listeyi arayüze geri gönder.
        return list;
    }

    // ==========================================
    // 2. ARAMA YAP (SEARCH / FILTER)
    // ==========================================
    // Amaç: Kullanıcının arama kutusuna yazdığı metni veritabanında aramak.
    public static ObservableList<Customer> searchCustomers(String query) {
        ObservableList<Customer> list = FXCollections.observableArrayList();

        // SQL Sorgusu:
        // ILIKE: PostgreSQL'e özel bir komuttur. Büyük/Küçük harf duyarsız arama yapar.
        // '%...%' : Başı veya sonu ne olursa olsun, içinde bu kelime geçiyorsa bul demektir.
        // Hem isme (full_name) hem de ehliyet no'ya (license_no) bakar.
        String sql = "SELECT * FROM customer WHERE full_name ILIKE '%" + query + "%' OR license_no ILIKE '%" + query + "%'";

        try (Connection conn = Db.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new Customer(
                        rs.getInt("customer_id"),
                        rs.getString("full_name"),
                        rs.getString("phone"),
                        rs.getString("license_no")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // ==========================================
    // 3. GÜNCELLEME (UPDATE)
    // ==========================================
    // Amaç: Mevcut bir müşterinin bilgilerini değiştirmek.
    public static void updateCustomer(int id, String name, String phone, String license) throws Exception {
        // Güvenlik Kontrolü: Giriş yapmamış kimse bu işlemi yapamaz.
        AuthService.requireLogin();

        // SQL Sorgusu:
        // Soru işaretleri (?) yer tutucudur (Placeholder).
        // Verileri doğrudan string'e yapıştırmak yerine '?' kullanmak daha güvenlidir.
        String sql = "UPDATE customer SET full_name=?, phone=?, license_no=? WHERE customer_id=?";

        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Soru işaretlerini sırasıyla dolduruyoruz:
            ps.setString(1, name);    // 1. soru işareti: İsim
            ps.setString(2, phone);   // 2. soru işareti: Telefon
            ps.setString(3, license); // 3. soru işareti: Ehliyet
            ps.setInt(4, id);         // 4. soru işareti: Hangi ID güncellenecek? (WHERE kısmı)

            // Veritabanında değişikliği uygula (SELECT dışındaki işlemler için executeUpdate kullanılır)
            ps.executeUpdate();
        }
    }

    // ==========================================
    // 4. SİLME (DELETE)
    // ==========================================
    // Amaç: Müşteriyi sistemden kaldırmak.
    public static void deleteCustomer(int id) throws Exception {
        AuthService.requireLogin();

        // NOT: Veritabanını kurarken "ON DELETE CASCADE" eklemiştik.
        // Bu sayede müşteri silinirse, ona ait tüm rezervasyonlar da otomatik silinir.
        // Ekstra kod yazmamıza gerek kalmaz.
        String sql = "DELETE FROM customer WHERE customer_id=?";

        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ==========================================
    // 5. YENİ EKLEME (CREATE)
    // ==========================================
    // Amaç: Sisteme yeni müşteri kaydetmek.
    public static void addCustomer(String name, String phone, String license) throws Exception {
        AuthService.requireLogin();

        // INSERT komutu ile yeni satır ekliyoruz.
        String sql = "INSERT INTO customer (full_name, phone, license_no) VALUES (?, ?, ?)";

        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, license);

            ps.executeUpdate(); // Kaydı gerçekleştir.
        }
    }
}