package ui;

import service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.io.File;

public class LoginView {

    private AuthService authService = new AuthService();

    public void show(Stage stage) {

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #f5f6fa;");

        HBox loginCard = new HBox();
        loginCard.setMaxSize(800, 480);
        loginCard.setStyle("-fx-background-color: white; " +
                "-fx-background-radius: 20; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 10);");

        // 1. SOL PANEL (GÖRSEL ALANI)
        VBox leftPane = new VBox(25);
        leftPane.setAlignment(Pos.CENTER);
        leftPane.setPrefWidth(400);
        leftPane.setStyle("-fx-background-color: #1e272e; -fx-background-radius: 20 0 0 20;");

        // YEREL DOSYA YÜKLEME KISMI
        try {
            // Bilgisayarındaki tam dosya yolu
            String localPath = "C:/Users/brkfa/OneDrive/Masaüstü/7c7dcdcb6342652018521182fa04cc8b-Photoroom.png";
            File file = new File(localPath);

            if (file.exists()) {
                Image carImage = new Image(file.toURI().toString());
                ImageView carView = new ImageView(carImage);
                carView.setFitWidth(320);
                carView.setPreserveRatio(true);
                leftPane.getChildren().add(carView);
            } else {
                System.out.println("Dosya bulunamadı: " + localPath);
            }
        } catch (Exception e) {
            System.out.println("Görsel yükleme hatası: " + e.getMessage());
        }

        Label lblBrand = new Label("SEAL RENT A CAR");
        lblBrand.setTextFill(Color.WHITE);
        lblBrand.setFont(Font.font("System", FontWeight.BOLD, 26));

        leftPane.getChildren().addAll(lblBrand);

        // 2. SAĞ PANEL (FORM ALANI)
        VBox rightPane = new VBox(20);
        rightPane.setPadding(new Insets(50));
        rightPane.setAlignment(Pos.CENTER_LEFT);
        rightPane.setPrefWidth(400);

        Label lblTitle = new Label("Giriş");
        lblTitle.setFont(Font.font("System", FontWeight.BOLD, 28));

        TextField txtUser = new TextField();
        txtUser.setPromptText("Kullanıcı Adı");
        txtUser.setPrefHeight(45);
        txtUser.setStyle("-fx-background-radius: 10; -fx-border-color: #dcdde1; -fx-border-radius: 10;");

        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Şifre");
        txtPass.setPrefHeight(45);
        txtPass.setStyle("-fx-background-radius: 10; -fx-border-color: #dcdde1; -fx-border-radius: 10;");

        Button btnLogin = new Button("GİRİŞ YAP");
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setPrefHeight(50);
        btnLogin.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10;");
        btnLogin.setDefaultButton(true);

        Label lblStatus = new Label();

        rightPane.getChildren().addAll(lblTitle, new Separator(), txtUser, txtPass, btnLogin, lblStatus);
        loginCard.getChildren().addAll(leftPane, rightPane);
        root.getChildren().add(loginCard);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Car Rental System - Login");
        stage.setMaximized(true);
        stage.show();

        // Giriş Aksiyonu
        btnLogin.setOnAction(e -> {
            try {
                if (authService.login(txtUser.getText(), txtPass.getText())) {
                    stage.close();
                    new MainView().show();
                } else {
                    lblStatus.setText("Hatalı kullanıcı adı veya şifre!");
                    lblStatus.setTextFill(Color.RED);
                }
            } catch (Exception ex) {
                lblStatus.setText("Sistem Hatası!");
            }
        });
    }
}