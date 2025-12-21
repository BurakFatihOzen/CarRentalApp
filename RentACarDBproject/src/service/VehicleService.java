package service;

// JavaFX arayüz bileşenlerinin (örneğin TableView) otomatik güncellenmesi için gerekli listeler.
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
// Veritabanından çekilen verileri nesne olarak tutacak model sınıfımız.
import model.Vehicle;
// Veritabanı bağlantısı (JDBC) işlemleri için gerekli kütüphaneler (Connection, PreparedStatement, vb.).
import java.sql.*;

/**
 * VehicleService Sınıfı
 * --------------------
 * Bu sınıf, Araç (Vehicle) verileri ile ilgili tüm veritabanı işlemlerini yönetir.
 * "Data Access Object" (DAO) mantığına benzer bir rol üstlenir.
 * UI (Arayüz) ile Veritabanı arasında bir köprü görevi görür.
 */
public class VehicleService {

    /**
     * --- UI İÇİN TÜM ARAÇLARI GETİR ---
     * Veritabanındaki tüm araçları çeker ve JavaFX TableView'da gösterilmeye hazır
     * bir liste (ObservableList) olarak döndürür.
     *
     * @return ObservableList<Vehicle> -> Tüm araçların listesi.
     */
    public static ObservableList<Vehicle> getAllVehiclesForUI() {
        // UI'ın dinleyebileceği boş bir liste oluşturuyoruz.
        ObservableList<Vehicle> list = FXCollections.observableArrayList();

        // Veritabanı sorgusu: Tüm araçları ID sırasına göre getir.
        String sql = "SELECT * FROM vehicle ORDER BY vehicle_id";

        // "Try-with-resources" yapısı:
        // Connection, Statement ve ResultSet işlemleri bittiğinde otomatik olarak kapatılır (close).
        // Bu, bellek sızıntılarını ve açık kalan bağlantı sorunlarını önler.
        try (Connection conn = Db.getConnection();          // Veritabanı bağlantısını al
             Statement st = conn.createStatement();         // Sorgu çalıştırmak için ifade oluştur
             ResultSet rs = st.executeQuery(sql)) {         // Sorguyu çalıştır ve sonuçları al

            // Veritabanından dönen her bir satır (row) için döngü çalışır.
            while (rs.next()) {
                // O satırdaki veriyi bir Vehicle nesnesine dönüştürüp listeye ekliyoruz.
                // Kod tekrarını önlemek için 'mapResultSetToVehicle' yardımcı metodunu kullanıyoruz.
                list.add(mapResultSetToVehicle(rs));
            }

        } catch (SQLException e) {
            // Hata oluşursa konsola yazdır (Gerçek projelerde loglama yapılmalıdır).
            e.printStackTrace();
        }
        // Dolu listeyi arayüze (Controller'a) geri döndür.
        return list;
    }

    /**
     * --- UI İÇİN ARAMA YAPMA (Güvenli Versiyon) ---
     * Kullanıcının girdiği metne göre (Marka, Model veya Plaka) arama yapar.
     *
     * ÖNEMLİ: SQL Injection saldırılarına karşı 'PreparedStatement' kullanılır.
     * Kullanıcı girdisi doğrudan sorguya yapıştırılmaz, parametre olarak verilir.
     *
     * @param searchText Kullanıcının aradığı kelime.
     * @return Arama kriterine uyan araçların listesi.
     */
    public static ObservableList<Vehicle> searchVehicles(String searchText) {
        ObservableList<Vehicle> list = FXCollections.observableArrayList();

        // '?' karakterleri yer tutucudur (placeholder).
        // ILIKE: PostgreSQL'de büyük/küçük harf duyarsız arama yapar.
        String sql = "SELECT * FROM vehicle WHERE brand ILIKE ? OR model ILIKE ? OR plate ILIKE ?";

        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) { // Sorguyu hazırla

            // Aranacak metni SQL'in LIKE formatına (%aranan%) çeviriyoruz.
            String searchPattern = "%" + searchText + "%";

            // Soru işaretlerinin yerine güvenli bir şekilde veriyi koyuyoruz.
            ps.setString(1, searchPattern); // 1. soru işareti (brand)
            ps.setString(2, searchPattern); // 2. soru işareti (model)
            ps.setString(3, searchPattern); // 3. soru işareti (plate)

            // Sorguyu çalıştır ve sonuçları al.
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToVehicle(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * --- EKLEME (Sadece Admin Yetkisiyle) ---
     * Yeni bir aracı veritabanına ekler.
     * İşlemden önce kullanıcının Admin olup olmadığı kontrol edilir.
     */
    public static void addVehicle(int branchId, String brand, String model, String plate, double price, String status) throws Exception {
        // 1. Güvenlik Kontrolü: İşlemi yapan admin mi?
        AuthService.requireAdmin();

        // 2. Basit Veri Doğrulama (Validation)
        if (price < 0) throw new IllegalArgumentException("Fiyat negatif olamaz.");

        // Veri ekleme sorgusu (INSERT). Parametreler için yine '?' kullanıyoruz.
        String sql = "INSERT INTO vehicle (branch_id, brand, model, plate, daily_price, vehicle_status) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Parametreleri sırasıyla yerleştiriyoruz.
            ps.setInt(1, branchId);
            ps.setString(2, brand);
            ps.setString(3, model);
            ps.setString(4, plate);
            ps.setDouble(5, price);
            ps.setString(6, status);

            // executeUpdate(): Veri değiştiren (INSERT, UPDATE, DELETE) sorgular için kullanılır.
            ps.executeUpdate();
        }
    }

    /**
     * --- SİLME (Sadece Admin Yetkisiyle) ---
     * Verilen ID'ye sahip aracı veritabanından siler.
     */
    public static void deleteVehicle(int id) throws Exception {
        AuthService.requireAdmin(); // Yetki kontrolü

        String sql = "DELETE FROM vehicle WHERE vehicle_id=?";

        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id); // Silinecek ID'yi ata
            ps.executeUpdate(); // Silme işlemini uygula
        }
    }

    /**
     * --- GÜNCELLEME (Status Update - Sadece Admin) ---
     * Aracın durumunu (Müsait, Kirada, Bakımda vb.) günceller.
     */
    public static void updateVehicleStatus(int id, String newStatus) throws Exception {
        AuthService.requireAdmin(); // Yetki kontrolü

        String sql = "UPDATE vehicle SET vehicle_status=? WHERE vehicle_id=?";

        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newStatus); // Yeni durumu ata
            ps.setInt(2, id);           // Hangi araç olduğunu belirt
            ps.executeUpdate();         // Güncellemeyi uygula
        }
    }

    /**
     * --- YARDIMCI METOT (Helper Method) ---
     * Veritabanından gelen ham satırı (ResultSet), Java nesnesine (Vehicle) dönüştürür.
     *
     * NEDEN GEREKLİ?
     * "getAllVehiclesForUI" ve "searchVehicles" metotlarında aynı dönüştürme işlemi yapılıyor.
     * Kod tekrarını önlemek (DRY Prensibi) ve Vehicle sınıfında bir değişiklik olduğunda
     * sadece burayı değiştirmek için bu işlemi ayırdık.
     */
    private static Vehicle mapResultSetToVehicle(ResultSet rs) throws SQLException {
        return new Vehicle(
                rs.getInt("vehicle_id"),       // DB sütun adı: vehicle_id
                rs.getString("plate"),         // DB sütun adı: plate
                rs.getString("brand"),         // DB sütun adı: brand
                rs.getString("model"),         // DB sütun adı: model
                rs.getDouble("daily_price"),   // DB sütun adı: daily_price
                rs.getString("vehicle_status") // DB sütun adı: vehicle_status
        );
    }
}