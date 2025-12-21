package ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Vehicle;
import service.AuthService;
import service.VehicleService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * CarListView Sınıfı
 * ------------------
 * Bu sınıf, uygulamadaki tüm araçların listelendiği ana yönetim ekranıdır.
 * - Araçları Tablo (TableView) içinde gösterir.
 * - Arama (Filtreleme) yapılmasını sağlar.
 * - Admin yetkisi varsa Ekleme, Silme ve Durum Güncelleme butonlarını gösterir.
 */
public class CarListView {

    private TableView<Vehicle> table; // Diğer metotlardan erişilebilmesi için sınıf seviyesinde tanımladık.

    public void show() {
        Stage stage = new Stage();

        // --- ANA DÜZEN (Layout) ---
        // VBox: Elemanları yukarıdan aşağıya dizer.
        VBox root = new VBox(10); // Elemanlar arası 10px boşluk
        root.setPadding(new Insets(10)); // Kenarlardan 10px boşluk

        // ==========================================
        // 1. ARAMA ÇUBUĞU (Üst Kısım)
        // ==========================================
        TextField txtSearch = new TextField();
        txtSearch.setPromptText("Marka, Model veya Plaka ara...");
        // Arama kutusu yatayda büyüsün (Responsive tasarım)
        HBox.setHgrow(txtSearch, Priority.ALWAYS);

        Button btnSearch = new Button("Ara");
        Button btnRefresh = new Button("Listeyi Yenile");

        // HBox: Elemanları yan yana dizer (Search Box + Butonlar)
        HBox searchBox = new HBox(10, txtSearch, btnSearch, btnRefresh);

        // ==========================================
        // 2. TABLO (TableView) AYARLARI
        // ==========================================
        table = new TableView<>();

        // Sütunların tablo genişliğine otomatik yayılmasını sağlar.
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        /*
         * TableColumn ve PropertyValueFactory Mantığı:
         * --------------------------------------------
         * "new PropertyValueFactory<>("brand")" kodu şu anlama gelir:
         * Vehicle sınıfına git, "getBrand()" metodunu bul ve dönen değeri bu hücreye yaz.
         * DİKKAT: Parantez içindeki isim ("brand"), Vehicle sınıfındaki değişken adıyla aynı olmalıdır!
         */

        TableColumn<Vehicle, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setMaxWidth(50); // ID sütunu çok geniş olmasın

        TableColumn<Vehicle, String> colBrand = new TableColumn<>("Marka");
        colBrand.setCellValueFactory(new PropertyValueFactory<>("brand"));

        TableColumn<Vehicle, String> colModel = new TableColumn<>("Model");
        colModel.setCellValueFactory(new PropertyValueFactory<>("model"));

        TableColumn<Vehicle, String> colPlate = new TableColumn<>("Plaka");
        colPlate.setCellValueFactory(new PropertyValueFactory<>("plate"));

        TableColumn<Vehicle, Double> colPrice = new TableColumn<>("Günlük Ücret");
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));

        TableColumn<Vehicle, String> colStatus = new TableColumn<>("Durum");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Sütunları tabloya ekle
        table.getColumns().addAll(colId, colBrand, colModel, colPlate, colPrice, colStatus);

        // İlk açılışta verileri yükle
        refreshTable();

        // ==========================================
        // 3. YÖNETİM BUTONLARI (Sadece Admin)
        // ==========================================
        Button btnAdd = new Button("Yeni Araç Ekle");
        Button btnDelete = new Button("Seçili Aracı Sil");
        Button btnUpdate = new Button("Durum Değiştir...");

        HBox actionBox = new HBox(10, btnAdd, btnDelete, btnUpdate);
        actionBox.setPadding(new Insets(10, 0, 0, 0)); // Üstten biraz boşluk bırak

        // YETKİ KONTROLÜ:
        // Eğer giriş yapan kullanıcı ADMIN değilse, bu buton kutusunu gizle.
        if (!"ADMIN".equalsIgnoreCase(AuthService.getRole())) {
            actionBox.setVisible(false); // Görünürlüğü kapat
            actionBox.setManaged(false); // Layout hesabından çıkar (Boşluk kalmasın)
        }

        // ==========================================
        // 4. AKSİYONLAR (Event Handlers)
        // ==========================================

        // --- ARAMA İŞLEMİ ---
        btnSearch.setOnAction(e -> {
            String searchText = txtSearch.getText();
            // Service katmanındaki güvenli arama metodunu çağır
            table.setItems(VehicleService.searchVehicles(searchText));
        });

        // --- YENİLEME İŞLEMİ ---
        btnRefresh.setOnAction(e -> {
            txtSearch.clear(); // Arama kutusunu temizle
            refreshTable();    // Tüm veriyi tekrar çek
        });

        // --- EKLEME İŞLEMİ ---
        btnAdd.setOnAction(e -> {
            // Yeni ekleme penceresini aç
            new AddVehicleView().show();
            // Not: AddVehicleView kapandığında tablonun otomatik güncellenmesi için
            // daha ileri teknikler (Callback veya ShowAndWait) gerekebilir.
            // Şimdilik kullanıcı manuel "Yenile" butonuna basacaktır.
        });

        // --- SİLME İŞLEMİ (Onaylı) ---
        btnDelete.setOnAction(e -> {
            Vehicle selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // Silmeden önce kullanıcıya sor (Confirmation Alert)
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Silme Onayı");
                confirm.setHeaderText("Araç Silinecek: " + selected.getPlate());
                confirm.setContentText("Bu işlemi onaylıyor musunuz?");

                // Kullanıcı OK tuşuna basarsa silme işlemini yap
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    try {
                        VehicleService.deleteVehicle(selected.getId());
                        refreshTable(); // Tabloyu güncelle
                        new Alert(Alert.AlertType.INFORMATION, "Araç sistemden silindi.").show();
                    } catch (Exception ex) {
                        new Alert(Alert.AlertType.ERROR, "Hata: " + ex.getMessage()).show();
                    }
                }
            } else {
                // Hiçbir satır seçilmemişse uyar
                new Alert(Alert.AlertType.WARNING, "Lütfen silmek istediğiniz aracı tablodan seçiniz.").show();
            }
        });

        // --- DURUM GÜNCELLEME (AVAILABLE / RENTED / MAINTENANCE) ---
        btnUpdate.setOnAction(e -> {
            Vehicle selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // Seçenekler Listesi
                List<String> choices = Arrays.asList("AVAILABLE", "MAINTENANCE", "RENTED");

                // ChoiceDialog: Kullanıcıya listeden seçim yaptıran hazır pencere
                ChoiceDialog<String> dialog = new ChoiceDialog<>(selected.getStatus(), choices);
                dialog.setTitle("Durum Güncelle");
                dialog.setHeaderText(selected.getBrand() + " " + selected.getModel() + " (" + selected.getPlate() + ")");
                dialog.setContentText("Yeni Durumu Seçiniz:");

                // Diyaloğu göster ve sonucu bekle
                Optional<String> result = dialog.showAndWait();

                // Eğer kullanıcı bir seçim yapıp OK dediyse:
                result.ifPresent(newStatus -> {
                    try {
                        VehicleService.updateVehicleStatus(selected.getId(), newStatus);
                        refreshTable(); // Tabloyu anında güncelle
                    } catch (Exception ex) {
                        new Alert(Alert.AlertType.ERROR, "Güncelleme Hatası: " + ex.getMessage()).show();
                    }
                });
            } else {
                new Alert(Alert.AlertType.WARNING, "Lütfen durumunu değiştirmek istediğiniz aracı seçiniz.").show();
            }
        });

        // Elemanları ana kök panele ekle
        // VBox.setVgrow(table, Priority.ALWAYS) -> Tablo dikeyde kalan tüm boşluğu doldursun
        VBox.setVgrow(table, Priority.ALWAYS);
        root.getChildren().addAll(searchBox, table, actionBox);

        Scene scene = new Scene(root, 800, 600); // Pencere boyutu biraz büyütüldü
        stage.setScene(scene);
        stage.setTitle("Araç Filo Yönetimi");
        stage.show();
    }

    /**
     * Tablodaki verileri veritabanından yeniden çeker.
     * Kod tekrarını önlemek için ayrı metoda alındı.
     */
    private void refreshTable() {
        table.setItems(VehicleService.getAllVehiclesForUI());
    }
}