package ui;

import service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * MainView Sınıfı (Ana Menü)
 * --------------------------
 * Kullanıcı giriş yaptıktan sonra karşılaştığı ana panodur (Dashboard).
 * Bu sınıf, kullanıcının yetkisine göre (Admin/Staff) başlık gösterir
 * ve uygulamanın diğer modüllerine (Araç, Rezervasyon, Müşteri) yönlendirme yapar.
 */
public class MainView {

    /**
     * Ana menü penceresini oluşturur ve gösterir.
     */
    public void show() {
        Stage stage = new Stage();

        // --- ANA DÜZEN (LAYOUT) ---
        // BorderPane: Ekranı 5 bölgeye ayırır (Üst, Alt, Sağ, Sol, Orta).
        // Ana menüler için idealdir çünkü üstte başlık, ortada butonlar, altta çıkış butonu yapabiliriz.
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20)); // Pencere kenarlarından boşluk

        // ==========================================
        // 1. ÜST KISIM (HEADER) - BİLGİ PANELİ
        // ==========================================

        // AuthService'den anlık giriş yapan kullanıcının rolünü alıyoruz.
        String role = AuthService.getRole();
        if (role == null) role = "MİSAFİR"; // Hata durumunda varsayılan

        // Başlık Yazısı
        Label titleLabel = new Label("Rent A Car Yönetim Paneli");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #333;");

        // Rol Bilgisi Yazısı
        Label roleLabel = new Label("Aktif Kullanıcı Yetkisi: " + role);
        roleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

        // Başlık ve Rolü alt alta dizmek için VBox kullanıyoruz (Header Box)
        VBox headerBox = new VBox(5, titleLabel, roleLabel);
        headerBox.setAlignment(Pos.CENTER); // Ortala
        headerBox.setPadding(new Insets(0, 0, 20, 0)); // Alt tarafa biraz boşluk bırak

        // BorderPane'in en tepesine (TOP) bu kutuyu yerleştir.
        root.setTop(headerBox);

        // ==========================================
        // 2. ORTA KISIM (CENTER) - MENÜ BUTONLARI
        // ==========================================

        Button btnCars = new Button("Araç Yönetimi");
        Button btnReservations = new Button("Rezervasyon Yönetimi");
        Button btnCustomers = new Button("Müşteri Yönetimi");

        // Butonların hepsini aynı genişliğe ayarla (Görsel bütünlük için)
        double buttonWidth = 250;
        btnCars.setMinWidth(buttonWidth);
        btnReservations.setMinWidth(buttonWidth);
        btnCustomers.setMinWidth(buttonWidth);

        // Butonları biraz süsleyelim (İsteğe bağlı CSS)
        String btnStyle = "-fx-font-size: 14px; -fx-padding: 10px;";
        btnCars.setStyle(btnStyle);
        btnReservations.setStyle(btnStyle);
        btnCustomers.setStyle(btnStyle);

        // --- BUTON AKSİYONLARI ---

        // 1. Araç Yönetimi Penceresini Aç
        btnCars.setOnAction(e -> {
            // CarListView sınıfından bir nesne oluşturup gösteriyoruz.
            new CarListView().show();
        });

        // 2. Rezervasyon Yönetimi Penceresini Aç
        btnReservations.setOnAction(e -> {
            new ReservationView().show();
        });

        // 3. Müşteri Yönetimi Penceresini Aç
        btnCustomers.setOnAction(e -> {
            new CustomerView().show();
        });

        // Butonları dikey bir kutuya (VBox) koy
        VBox menuBox = new VBox(15); // Butonlar arası 15px boşluk
        menuBox.getChildren().addAll(btnCars, btnReservations, btnCustomers);
        menuBox.setAlignment(Pos.CENTER); // Kutuyu ortala

        // BorderPane'in ortasına (CENTER) yerleştir
        root.setCenter(menuBox);

        // ==========================================
        // 3. ALT KISIM (BOTTOM) - ÇIKIŞ YAP
        // ==========================================

        Button btnLogout = new Button("Güvenli Çıkış");
        btnLogout.setMinWidth(150);
        // Çıkış butonu kırmızımsı olsun ki dikkat çeksin
        btnLogout.setStyle("-fx-background-color: #ffcccc; -fx-text-fill: darkred; -fx-border-color: darkred; -fx-border-radius: 5; -fx-background-radius: 5;");

        btnLogout.setOnAction(e -> {
            // 1. Kullanıcı oturumunu temizle (Opsiyonel: AuthService.logout() metodu yazılabilir)
            System.out.println("Kullanıcı çıkış yaptı.");

            // 2. Ana menüyü kapat
            stage.close();

            // 3. Tekrar Login ekranını aç
            new LoginView().show(new Stage());
        });

        // Çıkış butonunu barındıran kutu
        VBox bottomBox = new VBox(btnLogout);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setPadding(new Insets(20, 0, 0, 0)); // Üstten boşluk

        // BorderPane'in altına (BOTTOM) yerleştir
        root.setBottom(bottomBox);

        // ==========================================
        // SAHNE AYARLARI
        // ==========================================
        Scene scene = new Scene(root, 600, 500); // Pencere boyutu
        stage.setScene(scene);
        stage.setTitle("Rent A Car - Ana Menü");
        stage.centerOnScreen(); // Pencereyi ekranın ortasında aç
        stage.show();
    }
}