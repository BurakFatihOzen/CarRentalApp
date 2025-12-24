package ui;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * RentACarApp Sınıfı (Ana Başlatıcı)
 * ----------------------------------
 * Bu sınıf, uygulamanın GİRİŞ NOKTASI (Entry Point)'dır.
 * JavaFX uygulamaları standart Java programlarından farklı olarak
 * `Application` sınıfından miras alır (extends) ve yaşam döngüsünü bu sınıf yönetir.
 */
public class RentACarApp extends Application {

    /**
     * start() Metodu:
     * ---------------
     * JavaFX çalışma zamanı (Runtime) hazır olduğunda otomatik olarak bu metodu çağırır.
     * Burası UI (Arayüz) işlemlerinin başladığı yerdir.
     *
     * @param primaryStage : İşletim sistemi tarafından oluşturulan
     * Uygulamanın ana penceresi (Window/Stage).
     */
    @Override
    public void start(Stage primaryStage) {
        // Uygulamanın mantığı:
        // Program açılır açılmaz kullanıcıyı karşılayacak ekran "LoginView" olmalıdır.

        // LoginView sınıfından bir örnek (instance) oluşturuyoruz.
        LoginView loginView = new LoginView();

        // LoginView sınıfındaki 'show' metoduna ana pencereyi (primaryStage) gönderiyoruz.
        // Böylece LoginView, kendi tasarımını bu pencerenin içine yerleştirecek.
        loginView.show(primaryStage);
    }

    /**
     * main() Metodu:
     * --------------
     * Standart Java uygulamalarının başlangıç noktasıdır.
     * JavaFX'te main metodu genellikle tek bir satırdan oluşur: launch(args).
     */
    public static void main(String[] args) {
        // launch(args):
        // 1. JavaFX altyapısını başlatır.
        // 2. RentACarApp sınıfından bir nesne oluşturur.
        // 3. 'start' metodunu çağırarak uygulamayı ayağa kaldırır.
        launch(args);
    }
}
