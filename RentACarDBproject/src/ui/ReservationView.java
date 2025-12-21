package ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Reservation;
import service.ReservationService;
import java.sql.Date;
import java.util.Optional;

/**
 * ReservationView Sınıfı
 * ----------------------
 * Bu sınıf, sistemdeki rezervasyonların listelendiği ve durumlarının yönetildiği ekrandır.
 * Operatör veya Admin; rezervasyonları onaylayabilir, iptal edebilir veya silebilir.
 */
public class ReservationView {

    private TableView<Reservation> table; // Tabloyu sınıf seviyesinde tanımladık (Helper metotlar erişsin diye)

    public void show() {
        Stage stage = new Stage();

        // --- ANA DÜZEN ---
        VBox root = new VBox(10); // Elemanlar arası 10px dikey boşluk
        root.setPadding(new Insets(10)); // Pencere kenar boşluğu

        // ==========================================
        // 1. ARAMA VE FİLTRELEME ALANI
        // ==========================================
        TextField txtSearch = new TextField();
        txtSearch.setPromptText("Müşteri Adı veya Durum (WAITING/APPROVED) ara...");
        // Arama kutusu yatayda büyüsün (Responsive)
        HBox.setHgrow(txtSearch, Priority.ALWAYS);

        Button btnSearch = new Button("Ara");
        Button btnRefresh = new Button("Listeyi Yenile");

        HBox searchBox = new HBox(10, txtSearch, btnSearch, btnRefresh);

        // ==========================================
        // 2. TABLO (TableView) YAPILANDIRMASI
        // ==========================================
        table = new TableView<>();
        // Sütunlar ekran genişliğine otomatik yayılsın (Scrollbar çıkmasın)
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        /* * SÜTUN TANIMLARI
         * PropertyValueFactory("degiskenAdi") -> Reservation sınıfındaki 'getDegiskenAdi()' metodunu çağırır.
         */

        TableColumn<Reservation, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setMaxWidth(50); // ID sütunu dar olsun

        TableColumn<Reservation, String> colCust = new TableColumn<>("Müşteri");
        colCust.setCellValueFactory(new PropertyValueFactory<>("customerName"));

        TableColumn<Reservation, String> colBrand = new TableColumn<>("Marka");
        colBrand.setCellValueFactory(new PropertyValueFactory<>("brand"));

        TableColumn<Reservation, String> colModel = new TableColumn<>("Model");
        colModel.setCellValueFactory(new PropertyValueFactory<>("model"));

        TableColumn<Reservation, Integer> colVehId = new TableColumn<>("Araç ID");
        colVehId.setCellValueFactory(new PropertyValueFactory<>("vehicleId"));
        colVehId.setMaxWidth(60);

        TableColumn<Reservation, String> colStatus = new TableColumn<>("Durum");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<Reservation, Date> colDate = new TableColumn<>("Başlangıç");
        colDate.setCellValueFactory(new PropertyValueFactory<>("startDate"));

        // Sütunları tabloya ekle
        table.getColumns().addAll(colId, colCust, colBrand, colModel, colVehId, colStatus, colDate);

        // İlk verileri yükle
        refreshTable();

        // ==========================================
        // 3. AKSİYON BUTONLARI
        // ==========================================
        Button btnAdd = new Button("Yeni Rezervasyon");

        Button btnApprove = new Button("Seçiliyi Onayla");
        // Onay butonu için yeşilimsi stil (CSS)
        btnApprove.setStyle("-fx-base: #b6ffb6;");

        Button btnCancel = new Button("Seçiliyi İptal Et");
        // İptal butonu için turuncumsu stil
        btnCancel.setStyle("-fx-base: #ffcccb;");

        Button btnDelete = new Button("Kayıttan Sil");
        // Silme butonu için kırmızı stil
        btnDelete.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

        HBox actionBox = new HBox(10, btnAdd, btnApprove, btnCancel, btnDelete);
        actionBox.setPadding(new Insets(10, 0, 0, 0));

        // ==========================================
        // 4. İŞLEVLER (EVENT HANDLERS)
        // ==========================================

        // --- ARAMA ---
        btnSearch.setOnAction(e -> table.setItems(ReservationService.searchReservations(txtSearch.getText())));

        // --- YENİLE ---
        btnRefresh.setOnAction(e -> {
            txtSearch.clear();
            refreshTable();
        });

        // --- EKLEME ---
        btnAdd.setOnAction(e -> new AddReservationView().show());

        // --- ONAYLAMA (APPROVE) ---
        btnApprove.setOnAction(e -> {
            Reservation sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                try {
                    // Servis katmanını çağırarak veritabanında durumu güncelle
                    ReservationService.approveReservation(sel.getId());
                    refreshTable(); // Tabloyu güncelle
                    showAlert(Alert.AlertType.INFORMATION, "Başarılı", "Rezervasyon onaylandı ve araç durumu kiralandı olarak güncellendi.");
                } catch (Exception ex) {
                    // Servisten gelen özel hata mesajlarını işle
                    String msg = ex.getMessage();
                    if (msg.contains("APPROVED")) {
                        showAlert(Alert.AlertType.ERROR, "Onay Hatası", "Bu araç için zaten onaylı bir rezervasyon var!");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Hata", msg);
                    }
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "Seçim Yok", "Lütfen onaylanacak rezervasyonu seçin.");
            }
        });

        // --- İPTAL ETME (CANCEL) ---
        // Bu işlem kaydı silmez, sadece durumunu 'CANCELLED' yapar.
        btnCancel.setOnAction(e -> {
            Reservation sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                // Kullanıcıya soralım: Emin misin?
                if (confirmAction("Rezervasyon İptali", "Seçili rezervasyon iptal edilecek. Emin misiniz?")) {
                    try {
                        ReservationService.cancelReservation(sel.getId());
                        refreshTable();
                        showAlert(Alert.AlertType.INFORMATION, "Bilgi", "Rezervasyon iptal edildi.");
                    } catch (Exception ex) {
                        showAlert(Alert.AlertType.ERROR, "Hata", ex.getMessage());
                    }
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "Seçim Yok", "Lütfen iptal edilecek satırı seçin.");
            }
        });

        // --- SİLME (DELETE) ---
        // Bu işlem kaydı veritabanından tamamen siler.
        btnDelete.setOnAction(e -> {
            Reservation sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                // Kritik işlem: Kesinlikle onay sorulmalı.
                if (confirmAction("Kalıcı Silme", "DİKKAT: Bu kayıt veritabanından tamamen silinecek!\nDevam etmek istiyor musunuz?")) {
                    try {
                        ReservationService.deleteReservation(sel.getId());
                        refreshTable();
                        showAlert(Alert.AlertType.INFORMATION, "Silindi", "Kayıt başarıyla silindi.");
                    } catch (Exception ex) {
                        showAlert(Alert.AlertType.ERROR, "Hata", "Silme işlemi başarısız: " + ex.getMessage());
                    }
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "Seçim Yok", "Lütfen silinecek satırı seçin.");
            }
        });

        // Tablonun dikeyde tüm boşluğu doldurmasını sağla
        VBox.setVgrow(table, Priority.ALWAYS);
        root.getChildren().addAll(searchBox, table, actionBox);

        stage.setScene(new Scene(root, 800, 500));
        stage.setTitle("Rezervasyon Yönetimi");
        stage.show();
    }

    // --- YARDIMCI METOTLAR ---

    // Tabloyu yenilemek için (Kod tekrarını önler)
    private void refreshTable() {
        table.setItems(ReservationService.getReservationsForUI());
    }

    // Basit uyarı kutusu göstermek için
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Evet/Hayır sorusu sormak için
    private boolean confirmAction(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        Optional<ButtonType> result = alert.showAndWait();
        // Eğer kullanıcı 'OK' butonuna basarsa true döner
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}