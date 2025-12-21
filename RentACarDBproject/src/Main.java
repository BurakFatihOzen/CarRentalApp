import service.AuthService;
import service.VehicleService;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        System.out.print("Kullanıcı Adı: ");
        String username = scanner.nextLine();

        System.out.print("Şifre: ");
        String password = scanner.nextLine();

        try {
            boolean ok = AuthService.login(username, password);

            if (!ok) {
                System.out.println("Giriş başarısız");
                return;
            }

            System.out.println("Giriş başarılı | Rol: " + AuthService.getRole());

            // Listeleme ve arama (herkes yapabilir)
            System.out.println("\n--- Araç Listesi ---");
            // Yeni liste alma yöntemi (JavaFX listesi döndürür)
            for (model.Vehicle v : VehicleService.getAllVehiclesForUI()) {
                System.out.println(v.getBrand() + " " + v.getModel() + " - " + v.getPlate() + " (" + v.getStatus() + ")");
            }

            // Silme denemesi (SADECE ADMIN yapabilir)
            System.out.println("\n--- Araç Ekleme Denemesi ---");
            VehicleService.addVehicle(8,"Citroen","C-Elysée","06ALT301",1050,"AVAILABLE");

        } catch (Exception e) {
            System.out.println("Hata: " + e.getMessage());
            e.printStackTrace();
        }

        scanner.close();
    }
}


