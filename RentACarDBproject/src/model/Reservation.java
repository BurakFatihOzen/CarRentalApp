package model;

// Bu kütüphane, veritabanındaki (SQL) tarih formatını Java'da kullanabilmek için gereklidir.
// Java'da 'java.util.Date' ve 'java.sql.Date' vardır. Veritabanı işlerinde 'sql' olan kullanılır.
import java.sql.Date;

public class Reservation {

    // ==========================================
    // TEMEL DEĞİŞKENLER (VERİTABANI SÜTUNLARI)
    // ==========================================
    // Bu değişkenler, veritabanındaki 'reservation' tablosunun birebir karşılığıdır.

    private int id;           // Rezervasyonun benzersiz numarası (PK)
    private int vehicleId;    // Hangi aracın rezerve edildiği (FK)
    private String status;    // Durum bilgisi: "PENDING", "APPROVED", "CANCELLED"
    private Date startDate;   // Rezervasyonun başlangıç tarihi

    // ==========================================
    // EKSTRA TABLO VERİLERİ (JOIN ALANLARI)
    // ==========================================
    // !!! KRİTİK NOKTA !!!
    // Veritabanındaki 'reservation' tablosunda aslında 'brand', 'model' veya 'customerName'
    // diye sütunlar YOKTUR. Sadece ID'ler vardır.
    // Ancak kullanıcı arayüzünde (tabloda) ID yerine "BMW 320i" veya "Ahmet Yılmaz" görmek isteriz.
    // Bu yüzden SQL sorgusunda JOIN yaparak çektiğimiz bu ek bilgileri de bu sınıfta tutuyoruz.

    private String brand;         // Vehicle tablosundan gelen Marka (Örn: BMW)
    private String model;         // Vehicle tablosundan gelen Model (Örn: 320i)
    private String customerName;  // Customer tablosundan gelen Müşteri Adı (Örn: Burak Özen)

    // ==========================================
    // YAPICI METOT (CONSTRUCTOR)
    // ==========================================
    // Veritabanından gelen karmaşık sorgu sonucunu (ResultSet) tek bir pakete dönüştürür.
    // Hem ana tablo verilerini hem de JOIN ile gelen ek verileri alır.
    public Reservation(int id, int vehicleId, String status, Date startDate, String brand, String model, String customerName) {
        // Gelen verileri bu nesnenin hafızasına kaydet:
        this.id = id;
        this.vehicleId = vehicleId;
        this.status = status;
        this.startDate = startDate;

        // Ekstra bilgileri de kaydet:
        this.brand = brand;
        this.model = model;
        this.customerName = customerName;
    }

    // ==========================================
    // GETTER METOTLARI (VERİ OKUMA)
    // ==========================================
    // Arayüz (UI) tarafında tabloyu doldururken bu metotları kullanacağız.
    // Örneğin: tablonun "Marka" sütununa 'getBrand()' sonucunu yazacağız.

    public int getId() {
        return id;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public String getStatus() {
        return status;
    }

    public Date getStartDate() {
        return startDate;
    }

    // Bu veriler 'vehicle' tablosundan geldi ama bu nesne üzerinden erişiyoruz.
    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    // Bu veri 'customer' tablosundan geldi.
    // Tabloda "Kimi Rezerve Etti?" sütununda göstereceğiz.
    public String getCustomerName() {
        return customerName;
    }
}