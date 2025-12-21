package ui;

import service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * LoginView Sınıfı
 * ----------------
 * Uygulamanın açılış ekranıdır. Kullanıcıdan kimlik bilgilerini alır
 * ve AuthService aracılığıyla doğrulama yapar.
 */
public class LoginView {

    // Servis katmanını çağırıyoruz.
    // (Not: Gerçek projelerde bu genellikle Dependency Injection ile yapılır,
    // ancak burada doğrudan new ile oluşturuyoruz.)
    private AuthService authService = new AuthService();

    /**
     * Giriş ekranını oluşturur ve gösterir.
     * @param stage Uygulamanın ana sahnesi (PrimaryStage)
     */
    public void show(Stage stage) {

        // --- 1. ARAYÜZ BİLEŞENLERİ (UI COMPONENTS) ---

        // Kullanıcı Adı Alanı
        Label lblUser = new Label("Kullanıcı Adı:");
        TextField txtUser = new TextField();
        txtUser.setPromptText("kullanici_adi"); // Silik ipucu metni

        // Şifre Alanı (Yazılanlar gizli görünsün diye PasswordField kullanılır)
        Label lblPass = new Label("Şifre:");
        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("******");

        // Giriş Butonu
        Button btnLogin = new Button("Giriş Yap");
        // Butonun genişliğini kutuya yayması için:
        btnLogin.setMaxWidth(Double.MAX_VALUE);

        // ÖNEMLİ UX (Kullanıcı Deneyimi):
        // Kullanıcı şifreyi yazıp 'Enter' tuşuna basarsa bu buton tetiklenir.
        btnLogin.setDefaultButton(true);

        // Durum Mesajı (Hata veya Başarı durumunda altta çıkan yazı)
        Label lblStatus = new Label();

        // --- 2. AKSİYON (EVENT HANDLING) ---

        btnLogin.setOnAction(e -> {
            String username = txtUser.getText().trim(); // Boşlukları temizle
            String password = txtPass.getText();

            // A) Basit Validasyon (Boş mu?)
            if (username.isEmpty() || password.isEmpty()) {
                lblStatus.setText("Lütfen tüm alanları doldurunuz.");
                lblStatus.setStyle("-fx-text-fill: red;"); // Kırmızı renk
                return; // İşlemi durdur
            }

            // B) Servis Çağrısı (Login İşlemi)
            try {
                // AuthService sınıfındaki login metodunu çağırıyoruz.
                // Bu metot veritabanına gidip kontrol edecek.
                boolean isSuccess = authService.login(username, password);

                if (isSuccess) {
                    // --- GİRİŞ BAŞARILI ---
                    lblStatus.setText("Giriş Başarılı! Yönlendiriliyor...");
                    lblStatus.setStyle("-fx-text-fill: green;"); // Yeşil renk

                    // 1. Mevcut Giriş Penceresini Tamamen Kapat
                    // 'hide()' yerine 'close()' kullanmak belleği temizler.
                    stage.close();

                    // 2. Ana Menüyü (MainView) Aç
                    // MainView sınıfınızın var olduğunu varsayıyoruz.
                    new MainView().show();

                } else {
                    // --- GİRİŞ BAŞARISIZ ---
                    lblStatus.setText("Kullanıcı adı veya şifre hatalı!");
                    lblStatus.setStyle("-fx-text-fill: red;");
                    // Hatalı girişten sonra şifre alanını temizlemek iyi bir pratiktir.
                    txtPass.clear();
                }

            } catch (Exception ex) {
                // --- SİSTEM HATASI ---
                // Veritabanı bağlantısı kopmuş olabilir vb.
                lblStatus.setText("Bağlantı Hatası: " + ex.getMessage());
                lblStatus.setStyle("-fx-text-fill: red;");
                ex.printStackTrace(); // Geliştirici görsün diye konsola yaz
            }
        });

        // --- 3. DÜZEN (LAYOUT) ---

        VBox layout = new VBox(15); // Elemanlar arası 15 piksel boşluk
        layout.setPadding(new Insets(40)); // Pencere kenarlarından 40 piksel içeride
        layout.setAlignment(Pos.CENTER); // Her şeyi ortala (Dikey ve Yatay)

        // Bileşenleri sırasıyla ekle
        layout.getChildren().addAll(
                new Label("ARAÇ KİRALAMA SİSTEMİ"), // Başlık
                new Separator(), // Çizgi
                lblUser, txtUser,
                lblPass, txtPass,
                btnLogin,
                lblStatus
        );

        // --- 4. SAHNE AYARLARI ---

        Scene scene = new Scene(layout, 350, 400); // Pencere boyutu
        stage.setScene(scene);
        stage.setTitle("Sisteme Giriş");
        // Pencere boyutlandırılamasın (Login ekranları genelde sabittir)
        stage.setResizable(false);
        stage.show();
    }
}