package service;

// Bu sınıf 'service' paketindedir. Yardımcı hizmet sınıfı olarak geçer.
public class AuthContext {

    // ==========================================
    // GLOBAL ERİŞİM DEĞİŞKENİ (STATIC)
    // ==========================================
    // 'static' kelimesi burada HAYATİ önem taşır.
    // Static olduğu için, bu değişkenden uygulama boyunca SADECE BİR TANE vardır.
    // Uygulamanın herhangi bir yerinden (Login ekranı, Ana Menü, Araç Listesi vb.)
    // bu değişkene ulaşılabilir ve değeri okunabilir.

    // Senaryo:
    // 1. Kullanıcı Login ekranında giriş yapar.
    // 2. Veritabanından rolü ("ADMIN" veya "STAFF") doğrulanır.
    // 3. Biz bu değişkene o rolü yazarız: AuthContext.currentRole = "ADMIN";
    // 4. Artık Ana Menü açıldığında bu değişkene bakar:
    //    - Eğer "ADMIN" yazıyorsa "Personel Sil" butonunu gösterir.
    //    - Eğer "STAFF" yazıyorsa o butonu gizler.

    public static String currentRole;
    // Alabileceği değerler genellikle: "ADMIN", "STAFF" veya null (giriş yapılmadı).

}