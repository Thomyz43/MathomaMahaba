module com.example.mat {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.swing;
    requires javafx.media;
    requires java.desktop;

    opens com.example.mat to javafx.fxml;
    exports com.example.mat;
}
