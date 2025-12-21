package ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Customer;
import service.CustomerService;
import java.util.Optional;

/**
 * CustomerView Sınıfı
 * -------------------
 * Bu sınıf, Müşteri (Customer) verilerinin listelendiği, arandığı ve
 * yönetildiği (Ekle/Sil/Güncelle) arayüz katmanıdır.
 */
public class CustomerView {

    /**
     * Müşteri yönetim penceresini oluşturur ve gösterir.
     */
    public void show() {
        Stage stage = new Stage();

        // --- ANA DÜZEN (Layout) ---
        // VBox: Bileşenleri dikey (yukarıdan aşağıya) dizer.
        VBox root = new VBox(10); // Elemanlar arası 10px boşluk
        root.setPadding(new Insets(10)); // Pencere kenarlarından 10px iç boşluk

        // ==========================================
        // 1. ARAMA ALANI
        // ==========================================
        TextField txtSearch = new TextField();
        txtSearch.setPromptText("İsim veya Ehliyet No ile arayın...");
        // Arama kutusunun yatayda büyüyerek boşluğu doldurması için:
        HBox.setHgrow(txtSearch, Priority.ALWAYS);

        Button btnSearch = new Button("Ara");
        Button btnRefresh = new Button("Yenile");

        // HBox: Arama elemanlarını yan yana dizer.
        HBox searchBox = new HBox(10, txtSearch, btnSearch, btnRefresh);

        // ==========================================
        // 2. TABLO (TableView) YAPILANDIRMASI
        // ==========================================
        TableView<Customer> table = new TableView<>();

        // Sütunların tablo genişliğine sığması için politika (Scrollbar çıkmasını engeller)
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // -- Sütun Tanımları --
        // PropertyValueFactory("variableName"):
        // Customer sınıfındaki "getVariableName()" metodunu çağırır.
        // İsimler, model sınıfındaki değişken adlarıyla birebir aynı olmalıdır!

        TableColumn<Customer, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setMaxWidth(50); // ID sütunu dar olsun

        TableColumn<Customer, String> colName = new TableColumn<>("Ad Soyad");
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));

        TableColumn<Customer, String> colPhone = new TableColumn<>("Telefon");
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));

        TableColumn<Customer, String> colLicense = new TableColumn<>("Ehliyet No");
        colLicense.setCellValueFactory(new PropertyValueFactory<>("licenseNo"));

        // Sütunları tabloya ekle
        table.getColumns().addAll(colId, colName, colPhone, colLicense);

        // Başlangıç verilerini servisten çek ve yükle
        table.setItems(CustomerService.getAllCustomers());

        // ==========================================
        // 3. AKSİYON BUTONLARI
        // ==========================================
        Button btnAdd = new Button("Yeni Müşteri Ekle");
        Button btnUpdate = new Button("Seçiliyi Güncelle");
        Button btnDelete = new Button("Seçiliyi Sil");

        HBox actionBox = new HBox(10, btnAdd, btnUpdate, btnDelete);
        actionBox.setPadding(new Insets(10, 0, 0, 0)); // Üstten biraz boşluk

        // --- OLAY DİNLEYİCİLERİ (Event Handlers) ---

        // ARAMA BUTONU
        btnSearch.setOnAction(e -> {
            String query = txtSearch.getText();
            table.setItems(CustomerService.searchCustomers(query));
        });

        // YENİLE BUTONU
        btnRefresh.setOnAction(e -> {
            txtSearch.clear(); // Arama metnini temizle
            table.setItems(CustomerService.getAllCustomers()); // Tüm listeyi getir
        });

        // EKLEME BUTONU
        // Formu açarken 'null' gönderiyoruz, çünkü düzenlenecek bir müşteri yok, yenisi oluşturulacak.
        btnAdd.setOnAction(e -> showCustomerForm(null, table));

        // GÜNCELLEME BUTONU
        btnUpdate.setOnAction(e -> {
            Customer selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // Seçili müşteriyi forma gönder
                showCustomerForm(selected, table);
            } else {
                showAlert(Alert.AlertType.WARNING, "Seçim Yapılmadı", "Lütfen güncellenecek müşteriyi seçin.");
            }
        });

        // SİLME BUTONU
        btnDelete.setOnAction(e -> {
            Customer selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // Kritik İşlem Onayı
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Müşteri Sil");
                alert.setHeaderText(selected.getFullName() + " silinecek.");
                alert.setContentText("DİKKAT: Bu işlem geri alınamaz!\nEmin misiniz?");

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    try {
                        CustomerService.deleteCustomer(selected.getId());
                        table.setItems(CustomerService.getAllCustomers()); // Tabloyu güncelle
                        showAlert(Alert.AlertType.INFORMATION, "Başarılı", "Müşteri silindi.");
                    } catch (Exception ex) {
                        showAlert(Alert.AlertType.ERROR, "Hata", "Silme işlemi başarısız: " + ex.getMessage());
                    }
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "Seçim Yapılmadı", "Lütfen silinecek müşteriyi seçin.");
            }
        });

        // Elemanları ana panele ekle
        VBox.setVgrow(table, Priority.ALWAYS); // Tablo dikey boşluğu doldursun
        root.getChildren().addAll(searchBox, table, actionBox);

        stage.setScene(new Scene(root, 700, 500));
        stage.setTitle("Müşteri Yönetimi");
        stage.show();
    }

    /**
     * --- ORTAK FORM PENCERESİ (EKLEME ve GÜNCELLEME) ---
     * Hem "Yeni Ekle" hem de "Güncelle" işlemleri için aynı formu kullanırız.
     * Kod tekrarını önler ve bakımı kolaylaştırır.
     *
     * @param customer Eğer null ise "Yeni Ekleme" modunda açılır.
     * Eğer dolu ise "Güncelleme" modunda açılır ve kutular dolar.
     * @param table    İşlem bitince verileri yenilemek için tablo referansı.
     */
    private void showCustomerForm(Customer customer, TableView<Customer> table) {
        Stage formStage = new Stage();

        // MODALITY: Bu pencere açıkken kullanıcının arka plandaki ana pencereye
        // tıklamasını engeller. Kullanıcıyı formu bitirmeye zorlar.
        formStage.initModality(Modality.APPLICATION_MODAL);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(10);
        grid.setVgap(15);

        // Form Alanları
        TextField txtName = new TextField();
        TextField txtPhone = new TextField();
        TextField txtLicense = new TextField();

        // Mod Kontrolü (Ekleme mi Güncelleme mi?)
        if (customer != null) {
            // Güncelleme Modu: Var olan bilgileri kutulara doldur
            txtName.setText(customer.getFullName());
            txtPhone.setText(customer.getPhone());
            txtLicense.setText(customer.getLicenseNo());
            formStage.setTitle("Müşteri Güncelle: " + customer.getFullName());
        } else {
            // Ekleme Modu
            formStage.setTitle("Yeni Müşteri Ekle");
        }

        grid.add(new Label("Ad Soyad:"), 0, 0);   grid.add(txtName, 1, 0);
        grid.add(new Label("Telefon:"), 0, 1);    grid.add(txtPhone, 1, 1);
        grid.add(new Label("Ehliyet No:"), 0, 2); grid.add(txtLicense, 1, 2);

        Button btnSave = new Button("Kaydet");
        btnSave.setMaxWidth(Double.MAX_VALUE); // Butonu genişlet
        grid.add(btnSave, 1, 3);

        // KAYDET BUTONU AKSİYONU
        btnSave.setOnAction(e -> {
            // 1. Validasyon (Boş alan kontrolü)
            if (txtName.getText().trim().isEmpty() ||
                    txtPhone.getText().trim().isEmpty() ||
                    txtLicense.getText().trim().isEmpty()) {

                showAlert(Alert.AlertType.WARNING, "Eksik Bilgi", "Lütfen tüm alanları doldurunuz.");
                return;
            }

            try {
                if (customer == null) {
                    // YENİ KAYIT (INSERT)
                    CustomerService.addCustomer(
                            txtName.getText().trim(),
                            txtPhone.getText().trim(),
                            txtLicense.getText().trim()
                    );
                } else {
                    // GÜNCELLEME (UPDATE)
                    CustomerService.updateCustomer(
                            customer.getId(),
                            txtName.getText().trim(),
                            txtPhone.getText().trim(),
                            txtLicense.getText().trim()
                    );
                }

                // İşlem başarılıysa tabloyu yenile ve pencereyi kapat
                table.setItems(CustomerService.getAllCustomers());
                formStage.close();

            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "İşlem Hatası", "Kaydedilemedi: " + ex.getMessage());
            }
        });

        formStage.setScene(new Scene(grid, 350, 250));
        formStage.show();
    }

    // Kod tekrarını azaltmak için yardımcı metot
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}