package service;

// Java'nın veritabanı işlemlerini yöneten standart kütüphaneleri (JDBC).
// 'Connection', 'DriverManager' ve 'SQLException' sınıflarını kullanabilmek için çağırıyoruz.
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Db {

    // ==========================================
    // BAĞLANTI AYARLARI (CONSTANTS)
    // ==========================================
    // Bu bilgiler sabittir ve veritabanına nasıl ulaşılacağını tarif eder.
    // 'private static final': Bu değişkenler sadece bu sınıf içindir, değiştirilemez ve heryerden aynıdır.

    // 1. URL (ADRES): Veritabanının evi neresi?
    // "jdbc:postgresql://" -> Biz PostgreSQL kullanıyoruz.
    // "localhost"          -> Veritabanı bu bilgisayarın içinde (Uzak sunucuda değil).
    // "5432"               -> PostgreSQL'in varsayılan kapı numarası (Port).
    // "car_rental_db"      -> Bağlanmak istediğimiz veritabanının adı. (SQL dosyasında oluşturduğumuz ad).
    private static final String URL = "jdbc:postgresql://localhost:5432/car_rental_db";

    // 2. KULLANICI ADI
    // PostgreSQL kurulumunda belirlenen kullanıcı adı. Varsayılan genelde 'postgres'tir.
    private static final String USER = "postgres";   // Sende farklıysa burayı değiştirmen gerekebilir.

    // 3. ŞİFRE
    // PostgreSQL kurulumunda belirlediğin şifre.
    // !!! DİKKAT !!! -> Kendi bilgisayarındaki şifre neyse buraya onu yazmalısın.
    private static final String PASS = "123456";

    // ==========================================
    // BAĞLANTI METODU (CONNECTION FACTORY)
    // ==========================================
    // Diğer sınıflar (CustomerService, AuthService vb.) veritabanına bağlanmak istediklerinde
    // bu metodu çağırırlar: Db.getConnection();

    public static Connection getConnection() throws SQLException {
        // DriverManager, Java'nın veritabanı sürücüsünü yöneten sınıfıdır.
        // Adresi (URL), Kullanıcıyı (USER) ve Şifreyi (PASS) alıp bize açık bir hat (Connection) verir.
        // Eğer şifre yanlışsa veya veritabanı kapalıysa burada hata (SQLException) patlar.
        return DriverManager.getConnection(URL, USER, PASS);
    }
}