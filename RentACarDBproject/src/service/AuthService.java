package service;

// Java'nın veritabanı (SQL) işlemleri için gerekli kütüphanesi.
// Connection, PreparedStatement, ResultSet gibi sınıfları içerir.
import java.sql.*;

public class AuthService {

    // ==========================================
    // OTURUM DEĞİŞKENİ (SESSION)
    // ==========================================
    // Sisteme giriş yapan kişinin rolünü (ADMIN veya STAFF) burada tutarız.
    // 'static' olduğu için uygulama kapanana kadar bu bilgi hafızada kalır.
    // Başlangıçta 'null'dur, yani kimse giriş yapmamıştır.
    private static String currentRole = null;

    // ==========================================
    // LOGIN METODU (GİRİŞ İŞLEMİ)
    // ==========================================
    // Kullanıcı adı ve şifreyi alır, veritabanına sorar.
    // Doğruysa 'true' döner ve rolü hafızaya alır. Yanlışsa 'false' döner.
    public static boolean login(String username, String password) throws Exception {

        // 1. Veritabanı bağlantısını al (Db sınıfımızdan)
        Connection conn = Db.getConnection();

        // 2. SQL Sorgusunu Hazırla
        // "users" tablosunda bu kullanıcı adı ve şifreye sahip biri var mı?
        // Varsa onun 'role' (yetki) bilgisini getir.
        PreparedStatement ps = conn.prepareStatement(
                "SELECT role FROM users WHERE username=? AND password=?"
        );

        // 3. Soru işaretlerinin (?) yerine gerçek verileri koy
        // Bu yöntem (PreparedStatement) "SQL Injection" denilen saldırıyı önler.
        ps.setString(1, username);
        ps.setString(2, password);

        // 4. Sorguyu çalıştır ve sonucu al
        ResultSet rs = ps.executeQuery();

        // 5. Sonuç kontrolü
        // rs.next() -> "Veritabanından en az bir satır kayıt döndü mü?" demektir.
        if (rs.next()) {
            // EVET, kayıt bulundu. Yani kullanıcı adı ve şifre doğru.

            // Veritabanındaki 'role' sütununu alıp hafızaya (currentRole) yazıyoruz.
            currentRole = rs.getString("role");

            conn.close(); // Bağlantıyı kapat (İşimiz bitti)
            return true;  // Giriş başarılı!
        }

        // HAYIR, kayıt bulunamadı. Şifre veya kullanıcı adı yanlış.
        conn.close();
        return false; // Giriş başarısız!
    }

    // ==========================================
    // YETKİ KONTROL METOTLARI (GUARDS)
    // ==========================================
    // Bu metotlar, hassas işlemlerden önce çağrılır.
    // Eğer yetki yoksa programı hata (Exception) vererek durdururlar.

    // Sadece ADMIN'in yapabileceği işler için kontrol (Örn: Personel silmek)
    public static void requireAdmin() {
        if (!"ADMIN".equals(currentRole)) {
            // Eğer rol 'ADMIN' değilse, hata fırlat ve işlemi durdur.
            throw new RuntimeException("Bu işlem için ADMIN yetkisi gerekir");
        }
    }

    // Sadece STAFF'ın yapabileceği işler için kontrol
    // (Proje mantığına göre Staff işlemlerini Admin de yapabiliyorsa burası güncellenebilir)
    public static void requireStaff() {
        if (!"STAFF".equals(currentRole)) {
            throw new RuntimeException("Bu işlem için STAFF yetkisi gerekir");
        }
    }

    // Herhangi birinin giriş yapmış olması yeterli
    // (Sisteme giriş yapmadan işlem yapılmasını engeller)
    public static void requireLogin() {
        if (currentRole == null) {
            throw new RuntimeException("Önce giriş yapmalısın");
        }
    }

    // Şu anki rolü öğrenmek isteyen diğer sınıflar için (Getter)
    public static String getRole() {
        return currentRole;
    }
}