module com.example.mat {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires javafx.swing;


    opens com.example.mat to javafx.fxml;
    exports com.example.mat;
}