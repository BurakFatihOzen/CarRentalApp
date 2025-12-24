package ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Reservation;
import service.ReservationService;

public class ReservationView {

    private TableView<Reservation> table;

    public void show() {
        Stage stage = new Stage();
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        stage.setTitle("Rezervasyon ve Kiralama Yönetimi");

        // --- Arama Çubuğu ---
        TextField txtSearch = new TextField();
        txtSearch.setPromptText("Müşteri adı veya durum ara...");
        Button btnSearch = new Button("Ara");
        Button btnReset = new Button("Listeyi Yenile");
        HBox searchBox = new HBox(10, txtSearch, btnSearch, btnReset);

        // --- Tablo Yapılandırması ---
        table = new TableView<>();

        TableColumn<Reservation, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Reservation, String> colCustomer = new TableColumn<>("Müşteri");
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));

        TableColumn<Reservation, String> colBrand = new TableColumn<>("Marka");
        colBrand.setCellValueFactory(new PropertyValueFactory<>("brand"));

        TableColumn<Reservation, String> colModel = new TableColumn<>("Model");
        colModel.setCellValueFactory(new PropertyValueFactory<>("model"));

        TableColumn<Reservation, String> colStatus = new TableColumn<>("Durum");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<Reservation, String> colDate = new TableColumn<>("Başlangıç");
        colDate.setCellValueFactory(new PropertyValueFactory<>("startDate"));

        table.getColumns().addAll(colId, colCustomer, colBrand, colModel, colStatus, colDate);
        refreshTable();

        // --- BUTONLAR (Yeni Fonksiyonlar Eklendi) ---
        Button btnNew = new Button("Yeni Rezervasyon");

        Button btnApprove = new Button("Seçiliyi Onayla");
        btnApprove.setStyle("-fx-background-color: #c8e6c9;"); // Yeşilimsi

        // Tetikleyici 2'yi (trgRentalInsert) çalıştıracak buton
        Button btnStartRental = new Button("Kiralama Başlat (Aracı Teslim Et)");
        btnStartRental.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

        // Tetikleyici 3'ü (trgReturnDate) çalıştıracak buton
        Button btnFinishRental = new Button("Kiralama Bitir (Aracı Geri Al)");
        btnFinishRental.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");

        Button btnCancel = new Button("Seçiliyi İptal Et");
        Button btnDelete = new Button("Kayıttan Sil");
        btnDelete.setStyle("-fx-text-fill: red;");

        // Alt Panel Dizilimi
        HBox actionBox = new HBox(10, btnNew, btnApprove, btnStartRental, btnFinishRental, btnCancel, btnDelete);
        actionBox.setPadding(new Insets(10, 0, 0, 0));

        // --- BUTON OLAYLARI ---

        btnApprove.setOnAction(e -> {
            Reservation sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                try {
                    ReservationService.approveReservation(sel.getId());
                    refreshTable();
                    new Alert(Alert.AlertType.INFORMATION, "Rezervasyon Onaylandı! Araç: RESERVED").show();
                } catch (Exception ex) { showEx(ex); }
            }
        });

        btnStartRental.setOnAction(e -> {
            Reservation sel = table.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getStatus().equals("APPROVED")) {
                try {
                    ReservationService.startRental(sel.getId());
                    refreshTable();
                    new Alert(Alert.AlertType.INFORMATION, "Kiralama Başladı! Araç: RENTED\nRezervasyon: COMPLETED").show();
                } catch (Exception ex) { showEx(ex); }
            } else {
                new Alert(Alert.AlertType.WARNING, "Sadece ONAYLI (APPROVED) kayıtlar kiralamaya dönüştürülebilir!").show();
            }
        });

        btnFinishRental.setOnAction(e -> {
            Reservation sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                try {
                    ReservationService.finishRental(sel.getId());
                    refreshTable();
                    new Alert(Alert.AlertType.INFORMATION, "Araç Teslim Alındı! Araç: AVAILABLE").show();
                } catch (Exception ex) { showEx(ex); }
            }
        });

        btnNew.setOnAction(e -> {
            new AddReservationView().show();
            refreshTable();
        });

        btnCancel.setOnAction(e -> {
            Reservation sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                try {
                    ReservationService.cancelReservation(sel.getId());
                    refreshTable();
                } catch (Exception ex) { showEx(ex); }
            }
        });

        btnDelete.setOnAction(e -> {
            Reservation sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                try {
                    ReservationService.deleteReservation(sel.getId());
                    refreshTable();
                } catch (Exception ex) { showEx(ex); }
            }
        });

        btnSearch.setOnAction(e -> table.setItems(ReservationService.searchReservations(txtSearch.getText())));
        btnReset.setOnAction(e -> refreshTable());

        root.getChildren().addAll(searchBox, table, actionBox);
        stage.setScene(new Scene(root, 950, 500));
        stage.show();
    }

    private void refreshTable() {
        table.setItems(ReservationService.getReservationsForUI());
    }

    private void showEx(Exception ex) {
        new Alert(Alert.AlertType.ERROR, "Hata: " + ex.getMessage()).show();
    }
}