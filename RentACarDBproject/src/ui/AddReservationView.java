package ui;

import service.ReservationService;
import service.VehicleService;
import model.Vehicle;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * AddReservationView Sınıfı
 * -------------------------
 * Bu sınıf, yeni bir rezervasyon oluşturmak için açılan pencereyi (Stage) tasarlar.
 * İki temel işlevi tek ekranda birleştirir:
 * 1. Hızlı Müşteri Kaydı (Ad, Tel, Ehliyet).
 * 2. Rezervasyon Detayları (Araç seçimi, Tarih aralığı, Fiyat).
 */
public class AddReservationView {

    // Sınıf seviyesinde tanımladık çünkü hesaplama metodunda (calculatePrice) bunlara erişmemiz gerekecek.
    private ComboBox<Vehicle> cmbVehicle;
    private DatePicker dpStart;
    private DatePicker dpEnd;
    private TextField txtPrice;

    /**
     * Pencereyi oluşturur ve ekrana getirir.
     */
    public void show() {
        Stage stage = new Stage(); // Yeni bir pencere oluştur

        // --- LAYOUT (DÜZEN) AYARLARI ---
        // GridPane: Excel tablosu gibi satır ve sütunlardan oluşan esnek bir düzen.
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20)); // Pencere kenarlarından 20 piksel boşluk
        grid.setHgap(10); // Sütunlar arası boşluk (Horizontal Gap)
        grid.setVgap(10); // Satırlar arası boşluk (Vertical Gap)

        // ==========================================
        // 1. BÖLÜM: MÜŞTERİ BİLGİLERİ (MANUEL GİRİŞ)
        // ==========================================
        // Kullanıcı veritabanında yoksa bile buradan hızlıca ekleyebileceğiz.

        Label lblName = new Label("Ad Soyad:");
        TextField txtName = new TextField();
        txtName.setPromptText("Örn: Ali Yılmaz"); // Kullanıcıya ipucu yazısı

        Label lblPhone = new Label("Telefon:");
        TextField txtPhone = new TextField();
        txtPhone.setPromptText("Örn: 0555 123 45 67");

        Label lblLicense = new Label("Ehliyet No:");
        TextField txtLicense = new TextField();
        txtLicense.setPromptText("Örn: 123456");

        // ==========================================
        // 2. BÖLÜM: ARAÇ SEÇİMİ VE ÖZELLEŞTİRME
        // ==========================================
        Label lblVeh = new Label("Araç Seç:");

        // ComboBox'ı oluşturuyoruz.
        cmbVehicle = new ComboBox<>();

        // Veritabanından gelen araçları (ObservableList) ComboBox'a yüklüyoruz.
        // NOT: VehicleService.getAllVehiclesForUI() metodu daha önce yazdığımız güvenli metot olmalıdır.
        cmbVehicle.setItems(VehicleService.getAllVehiclesForUI());
        cmbVehicle.setPromptText("Listeden bir araç seçiniz...");

        /* --- HÜCRE ÖZELLEŞTİRME (CELL FACTORY) ---
           ComboBox normalde nesnenin 'toString()' metodunu gösterir.
           Ancak biz 'Vehicle' nesnesinin içinden Marka, Model ve Plaka'yı alıp
           özel bir formatta göstermek istiyoruz.
        */

        // 1. Açılır listedeki görünüm (Dropdown List):
        cmbVehicle.setCellFactory(param -> new ListCell<Vehicle>() {
            @Override
            protected void updateItem(Vehicle item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // Örn: Ford Focus - 34 ABC 123
                    setText(item.getBrand() + " " + item.getModel() + " - " + item.getPlate());
                }
            }
        });

        // 2. Seçildikten sonra kutunun içinde görünen metin (Button Cell):
        cmbVehicle.setButtonCell(new ListCell<Vehicle>() {
            @Override
            protected void updateItem(Vehicle item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // Seçilince sadece Plaka ve Marka görünsün.
                    setText(item.getPlate() + " (" + item.getBrand() + ")");
                }
            }
        });

        // ==========================================
        // 3. BÖLÜM: TARİH VE FİYAT
        // ==========================================
        dpStart = new DatePicker(); // Başlangıç Tarihi
        dpEnd = new DatePicker();   // Bitiş Tarihi

        // Geçmiş tarihlerin seçilmesini engellemek için (İsteğe bağlı UX geliştirmesi)
        // dpStart.setDayCellFactory(...); // Burası daha ileri seviye, şimdilik basit tutuyoruz.

        txtPrice = new TextField();
        txtPrice.setEditable(false); // Kullanıcı fiyatı elle değiştiremesin, sistem hesaplasın.
        txtPrice.setPromptText("Tarih seçince hesaplanacak...");
        // CSS ile fiyat alanını gri yapıp 'disabled' hissiyatı verebiliriz ama okunabilir olsun diye bırakıyoruz.
        txtPrice.setStyle("-fx-background-color: #f0f0f0;");

        // ==========================================
        // EKRAN YERLEŞİMİ (GRID ADD)
        // grid.add(Node, Sütun İndeksi, Satır İndeksi);
        // ==========================================

        // Satır 0
        grid.add(lblName, 0, 0);
        grid.add(txtName, 1, 0);

        // Satır 1
        grid.add(lblPhone, 0, 1);
        grid.add(txtPhone, 1, 1);

        // Satır 2
        grid.add(lblLicense, 0, 2);
        grid.add(txtLicense, 1, 2);

        // Satır 3 (Ayırıcı Çizgi)
        // Separator, görsel olarak müşteri ile araç bilgilerini ayırır.
        // 0. sütundan başla, 2 sütun genişliğinde ol (colspan = 2)
        grid.add(new Separator(), 0, 3, 2, 1);

        // Satır 4
        grid.add(lblVeh, 0, 4);
        grid.add(cmbVehicle, 1, 4);

        // Satır 5
        grid.add(new Label("Başlangıç:"), 0, 5);
        grid.add(dpStart, 1, 5);

        // Satır 6
        grid.add(new Label("Bitiş:"), 0, 6);
        grid.add(dpEnd, 1, 6);

        // Satır 7
        grid.add(new Label("Toplam Tutar:"), 0, 7);
        grid.add(txtPrice, 1, 7);

        // Satır 8 (Kaydet Butonu)
        Button btnSave = new Button("Kaydet ve Müşteriyi Oluştur");
        btnSave.setMaxWidth(Double.MAX_VALUE); // Butonun hücreyi doldurması için
        grid.add(btnSave, 1, 8);


        // ==========================================
        // OLAY DİNLEYİCİLERİ (EVENT LISTENERS)
        // ==========================================

        // Araç değişirse, Başlangıç değişirse veya Bitiş değişirse -> Fiyatı yeniden hesapla.
        cmbVehicle.setOnAction(e -> calculatePrice());
        dpStart.setOnAction(e -> calculatePrice());
        dpEnd.setOnAction(e -> calculatePrice());

        // KAYDET BUTONU AKSİYONU
        btnSave.setOnAction(e -> {
            try {
                // 1. Validasyon (Boş alan kontrolü)
                if (txtName.getText().isEmpty() || txtPhone.getText().isEmpty() || txtLicense.getText().isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "Eksik Bilgi", "Lütfen Müşteri bilgilerini eksiksiz girin!");
                    return; // İşlemi durdur
                }
                if (cmbVehicle.getValue() == null) {
                    showAlert(Alert.AlertType.WARNING, "Araç Seçilmedi", "Lütfen bir araç seçin!");
                    return;
                }
                if (dpStart.getValue() == null || dpEnd.getValue() == null) {
                    showAlert(Alert.AlertType.WARNING, "Tarih Hatası", "Lütfen başlangıç ve bitiş tarihlerini girin.");
                    return;
                }

                // Fiyat alanı boşsa, tarih mantığında hata vardır (Bitiş < Başlangıç gibi).
                if (txtPrice.getText().isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "Hesaplama Hatası", "Tarihleri kontrol edin, fiyat hesaplanamadı.");
                    return;
                }

                // 2. İş Mantığı: Önce Müşteriyi Oluştur
                // Bu metot ReservationService'de olmalı ve oluşturduğu müşterinin ID'sini dönmeli.
                int customerId = ReservationService.createCustomerAndGetId(
                        txtName.getText().trim(),
                        txtPhone.getText().trim(),
                        txtLicense.getText().trim()
                );

                // 3. İş Mantığı: Rezervasyonu Kaydet
                Vehicle selectedVeh = cmbVehicle.getValue();
                double price = Double.parseDouble(txtPrice.getText());

                // LocalDate -> java.sql.Date dönüşümü (Veritabanı için)
                Date start = Date.valueOf(dpStart.getValue());
                Date end = Date.valueOf(dpEnd.getValue());

                ReservationService.addReservation(customerId, selectedVeh.getId(), start, end, price);

                // 4. Başarılı sonuç
                showAlert(Alert.AlertType.INFORMATION, "Başarılı", "Müşteri ve Rezervasyon başarıyla kaydedildi!");
                stage.close(); // Pencereyi kapat

            } catch (Exception ex) {
                // Beklenmedik bir hata olursa (Veritabanı hatası vb.)
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Sistem Hatası", "Hata: " + ex.getMessage());
            }
        });

        // Pencereyi son haline getir ve göster
        stage.setScene(new Scene(grid, 450, 550));
        stage.setTitle("Yeni Rezervasyon Girişi");
        stage.show();
    }

    /**
     * Dinamik Fiyat Hesaplama Metodu
     * Araç, Başlangıç ve Bitiş tarihi seçili ise gün farkını bulup fiyatı hesaplar.
     */
    private void calculatePrice() {
        // Üç alan da dolu mu?
        if (cmbVehicle.getValue() != null && dpStart.getValue() != null && dpEnd.getValue() != null) {

            LocalDate start = dpStart.getValue();
            LocalDate end = dpEnd.getValue();

            // Tarih kontrolü: Bitiş, Başlangıçtan önce olamaz.
            if (end.isBefore(start)) {
                txtPrice.setText(""); // Hatalı durumda fiyatı temizle
                return;
            }

            // Bugünün tarihinden önce mi kontrolü (Opsiyonel)
            if (start.isBefore(LocalDate.now())) {
                // Geçmişe rezervasyon yapılamaz uyarısı verilebilir, şimdilik fiyatı siliyoruz.
                txtPrice.setText("");
                return;
            }

            // ChronoUnit.DAYS.between(start, end) iki tarih arasındaki TAM gün sayısını verir.
            long days = ChronoUnit.DAYS.between(start, end);

            // Eğer aynı gün alıp bırakıyorsa (0 gün) en az 1 günlük ücret alalım.
            if (days == 0) days = 1;

            // Hesaplama: Gün Sayısı * Aracın Günlük Fiyatı
            double total = days * cmbVehicle.getValue().getPrice();

            // Sonucu metin kutusuna yaz
            txtPrice.setText(String.valueOf(total));
        } else {
            // Herhangi biri eksikse fiyat kutusunu temizle
            txtPrice.setText("");
        }
    }

    // Kod tekrarını azaltmak için Alert (Uyarı) gösterme metodu
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null); // Başlık kısmını sade tutuyoruz
        alert.setContentText(content);
        alert.showAndWait();
    }
}