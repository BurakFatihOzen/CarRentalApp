package model;

// Bu sınıfın 'model' paketinde olduğunu belirtir.
public class Vehicle {

    // ==========================================
    // DEĞİŞKENLER (FIELDS)
    // ==========================================
    // Bu değişkenler veritabanındaki 'vehicle' tablosunun sütunlarıdır.
    // 'private' yaparak bu verilerin doğrudan değiştirilmesini engelliyoruz (Kapsülleme).

    private int id;         // Veritabanındaki 'vehicle_id' (Benzersiz kimlik)
    private String plate;   // Plaka (Örn: 06 BRK 058) - Araçları ayırt etmek için kritik.
    private String brand;   // Marka (Örn: BMW)
    private String model;   // Model (Örn: 320i)

    // Fiyatlar genellikle kuruşlu olabileceği için 'double' veri tipi kullanılır.
    // SQL'deki 'NUMERIC(10,2)' tipinin Java karşılığıdır.
    private double price;   // Günlük Kiralama Ücreti

    // Aracın o anki durumu: "AVAILABLE" (Müsait), "RENTED" (Kirada), "MAINTENANCE" (Bakımda) vs.
    private String status;

    // ==========================================
    // YAPICI METOT (CONSTRUCTOR)
    // ==========================================
    // Veritabanından (SQL) gelen bir satır veriyi (Row), Java nesnesine dönüştürür.
    // Örn: veritabanından "1, 06AB12, BMW..." geldiğinde bu metot çalışır ve bir 'Vehicle' nesnesi oluşturur.
    public Vehicle(int id, String plate, String brand, String model, double price, String status) {
        this.id = id;           // Gelen ID'yi bu nesneye kaydet
        this.plate = plate;     // Gelen plakayı kaydet
        this.brand = brand;     // ...
        this.model = model;     // ...
        this.price = price;     // ...
        this.status = status;   // ...
    }

    // ==========================================
    // GETTER METOTLARI (OKUYUCULAR)
    // ==========================================
    // Arayüzdeki (GUI) tablolar, verileri okumak için bu metotları çağırır.
    // Örneğin araç tablosunu doldururken "Marka sütununa ne yazayım?" diye sorulduğunda
    // 'getBrand()' metodu cevap verir.

    public int getId() {
        return id;
    }

    public String getPlate() {
        return plate;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public double getPrice() {
        return price;
    }

    public String getStatus() {
        return status;
    }
}