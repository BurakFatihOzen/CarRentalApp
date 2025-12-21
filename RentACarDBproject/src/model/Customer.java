package model;

// Bu sınıfın 'model' paketinin bir parçası olduğunu belirtir.
// Model paketleri genellikle veritabanı tablolarının Java'daki karşılığıdır.
public class Customer {

    // ==========================================
    // DEĞİŞKENLER (FIELDS)
    // ==========================================
    // 'private' demek: Bu verilere başka sınıflar doğrudan elleyemez, sadece bu sınıf içinden erişilir.
    // Bu, veri güvenliği (Encapsulation) için önemlidir.

    private int id;             // Veritabanındaki 'customer_id' (Benzersiz numara)
    private String fullName;    // Müşterinin adı soyadı
    private String phone;       // Telefon numarası
    private String licenseNo;   // Ehliyet numarası (Kiralama için kritik bilgi)

    // ==========================================
    // KURUCU METOT 1 (FULL CONSTRUCTOR)
    // ==========================================
    // 'new Customer(...)' dendiğinde çalışan ana metottur.
    // Veritabanından gelen TÜM bilgileri nesneye doldurmak için kullanılır.
    public Customer(int id, String fullName, String phone, String licenseNo) {
        this.id = id;               // Gelen id'yi, bu nesnenin id'sine eşitle
        this.fullName = fullName;   // Gelen ismi, bu nesnenin ismine eşitle
        this.phone = phone;         // ...
        this.licenseNo = licenseNo; // ...
    }

    // ==========================================
    // KURUCU METOT 2 (BASİT / OVERLOAD CONSTRUCTOR)
    // ==========================================
    // Bazen tüm detaylara ihtiyacımız olmaz. Örneğin açılır listede (ComboBox)
    // sadece isim ve ID göstermek yeterlidir. Telefon ve ehliyeti boş geçiyoruz.
    // Buna "Constructor Overloading" (Aşırı Yükleme) denir.
    public Customer(int id, String fullName) {
        this.id = id;
        this.fullName = fullName;
        this.phone = "";        // Boş bırakıyoruz (Null hatası almamak için boş metin atadık)
        this.licenseNo = "";    // Boş bırakıyoruz
    }

    // ==========================================
    // GETTER METOTLARI (OKUYUCULAR)
    // ==========================================
    // Değişkenler 'private' olduğu için, dışarıdan (örneğin arayüzden)
    // bu verilere ulaşmak isteyenler bu metotları kullanır.

    public int getId() {
        return id; // ID bilgisini dışarı gönder
    }

    public String getFullName() {
        return fullName; // İsim bilgisini dışarı gönder
    }

    public String getPhone() {
        return phone; // Telefonu dışarı gönder
    }

    public String getLicenseNo() {
        return licenseNo; // Ehliyeti dışarı gönder
    }

    // ==========================================
    // TOSTRING METODU (METİNSEL TEMSİL)
    // ==========================================
    // Java'da bir nesneyi ekrana yazdırmak istediğinizde (örneğin ComboBox içinde),
    // Java varsayılan olarak saçma bir kod yazar (örn: model.Customer@15db9742).
    // Bu metodu ekleyerek Java'ya diyoruz ki: "Bu nesneyi yazı olarak göstermen gerekirse
    // şu formatta göster: Ad Soyad (ID: 1)"
    @Override
    public String toString() {
        return fullName + " (ID: " + id + ")";
    }
}