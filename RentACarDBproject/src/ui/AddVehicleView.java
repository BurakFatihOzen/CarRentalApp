package ui;

import service.VehicleService;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

/**
 * AddVehicleView Sınıfı
 * ---------------------
 * Bu sınıf, sisteme yeni bir araç eklemek için kullanılan formu oluşturur.
 * JavaFX arayüz bileşenlerini kullanarak kullanıcıdan araç bilgilerini alır
 * ve Service katmanına iletir.
 */
public class AddVehicleView {

    /**
     * Ekleme penceresini oluşturur ve gösterir.
     */
    public void show() {
        // Yeni bir pencere (Stage) oluşturuluyor.
        Stage stage = new Stage();

        // --- LAYOUT (DÜZEN) AYARLARI ---
        // GridPane: Form elemanlarını satır ve sütun mantığıyla düzenlemek için idealdir.
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20)); // Pencere kenar boşlukları
        grid.setHgap(10); // Yatay boşluk (Bileşenler arası)
        grid.setVgap(15); // Dikey boşluk (Satırlar arası)

        // --- FORM ELEMANLARI (Girdiler) ---

        // 1. Şube ID (Gerçek uygulamada burası şube listesinden seçilen bir ComboBox olmalıdır)
        Label lblBranch = new Label("Şube ID:");
        TextField txtBranchId = new TextField("1"); // Varsayılan olarak 1
        txtBranchId.setPromptText("Sayısal değer giriniz");

        // 2. Marka
        Label lblBrand = new Label("Marka:");
        TextField txtBrand = new TextField();
        txtBrand.setPromptText("Örn: Toyota");

        // 3. Model
        Label lblModel = new Label("Model:");
        TextField txtModel = new TextField();
        txtModel.setPromptText("Örn: Corolla");

        // 4. Plaka
        Label lblPlate = new Label("Plaka:");
        TextField txtPlate = new TextField();
        txtPlate.setPromptText("Örn: 34 ABC 123");

        // 5. Günlük Ücret
        Label lblPrice = new Label("Günlük Ücret:");
        TextField txtPrice = new TextField();
        txtPrice.setPromptText("Örn: 1500.0");

        // 6. Durum (ComboBox)
        Label lblStatus = new Label("Durum:");
        ComboBox<String> cmbStatus = new ComboBox<>();
        // Veritabanındaki ENUM değerleriyle birebir aynı olmalı
        cmbStatus.getItems().addAll("AVAILABLE", "MAINTENANCE", "RENTED");
        // Kullanıcı seçmeyi unutursa diye varsayılan olarak ilki seçilsin
        cmbStatus.getSelectionModel().select("AVAILABLE");

        // 7. Kaydet Butonu
        Button btnSave = new Button("Kaydet");
        // Butonun genişliğini bulunduğu hücreye yayması için
        btnSave.setMaxWidth(Double.MAX_VALUE);

        // --- ELEMANLARIN GRID'E EKLENMESİ ---
        // (Sütun, Satır) mantığıyla eklenir.
        grid.add(lblBranch, 0, 0);   grid.add(txtBranchId, 1, 0);
        grid.add(lblBrand, 0, 1);    grid.add(txtBrand, 1, 1);
        grid.add(lblModel, 0, 2);    grid.add(txtModel, 1, 2);
        grid.add(lblPlate, 0, 3);    grid.add(txtPlate, 1, 3);
        grid.add(lblPrice, 0, 4);    grid.add(txtPrice, 1, 4);
        grid.add(lblStatus, 0, 5);   grid.add(cmbStatus, 1, 5);
        grid.add(btnSave, 1, 6);     // Buton 1. sütun, 6. satıra

        // --- BUTON AKSİYONU (KAYDETME MANTIĞI) ---
        btnSave.setOnAction(e -> {
            try {
                // 1. ADIM: Boş Alan Kontrolü (Validation)
                if (txtBrand.getText().trim().isEmpty() ||
                        txtModel.getText().trim().isEmpty() ||
                        txtPlate.getText().trim().isEmpty() ||
                        txtPrice.getText().trim().isEmpty()) {

                    showAlert(Alert.AlertType.WARNING, "Eksik Bilgi", "Lütfen tüm alanları doldurunuz.");
                    return; // İşlemi durdur
                }

                // 2. ADIM: Sayısal Dönüşüm Kontrolü
                // Kullanıcı "Fiyat" kısmına "bin lira" yazarsa uygulama çöker.
                // Bunu önlemek için try-catch içinde parse işlemi yapıyoruz.
                int branchId;
                double price;
                try {
                    branchId = Integer.parseInt(txtBranchId.getText().trim());
                    price = Double.parseDouble(txtPrice.getText().trim());
                } catch (NumberFormatException nfe) {
                    showAlert(Alert.AlertType.ERROR, "Format Hatası", "Şube ID ve Fiyat alanlarına sadece sayı giriniz.");
                    return; // İşlemi durdur
                }

                // 3. ADIM: Servis Çağrısı (Veritabanı İşlemi)
                VehicleService.addVehicle(
                        branchId,
                        txtBrand.getText().trim(),  // Başındaki/sonundaki boşlukları temizle
                        txtModel.getText().trim(),
                        txtPlate.getText().trim().toUpperCase(), // Plakayı büyük harfe çevir
                        price,
                        cmbStatus.getValue()
                );

                // 4. ADIM: Başarı Mesajı ve Kapatma
                showAlert(Alert.AlertType.INFORMATION, "Başarılı", "Yeni araç sisteme başarıyla eklendi!");
                stage.close(); // İşlem bitince pencereyi kapat

            } catch (Exception ex) {
                // Veritabanı veya bilinmeyen diğer hatalar için
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Sistem Hatası", "Kayıt sırasında bir hata oluştu: " + ex.getMessage());
            }
        });

        // Sahneyi (Scene) oluştur ve pencereye (Stage) ata
        Scene scene = new Scene(grid, 400, 350); // Genişlik: 400, Yükseklik: 350
        stage.setScene(scene);
        stage.setTitle("Yeni Araç Ekle");

        // Pencereyi "Modal" yap (Kullanıcı bu pencereyi kapatmadan ana ekrana dönemesin)
        // stage.initModality(Modality.APPLICATION_MODAL); // İsteğe bağlı

        stage.show();
    }

    // Kod tekrarını önlemek için yardımcı metot
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}