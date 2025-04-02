package com.example.mat;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class HelloApplication extends Application {

    private Canvas canvas;
    private GraphicsContext gc;
    private Color currentColor = Color.BLACK;
    private double strokeWidth = 2;
    private double lastX = -1, lastY = -1; // To store the last mouse position for continuous drawing

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Interactive Digital Whiteboard");

        // Create a Canvas for drawing
        canvas = new Canvas(800, 600);
        gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Create a Color Picker
        ColorPicker colorPicker = new ColorPicker(currentColor);
        colorPicker.setStyle("-fx-font-size: 16px; -fx-background-color: #f8f9fa; -fx-border-color: #dfe6e9; -fx-border-width: 2px;");
        colorPicker.setOnAction(event -> currentColor = colorPicker.getValue());

        // Create a Slider for Stroke Width
        Slider strokeSlider = new Slider(1, 10, strokeWidth);
        strokeSlider.setShowTickLabels(true);
        strokeSlider.setShowTickMarks(true);
        strokeSlider.setBlockIncrement(1);
        strokeSlider.setStyle("-fx-font-size: 14px; -fx-background-color: #dfe6e9;");
        strokeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            strokeWidth = newValue.doubleValue();
        });

        // Create Buttons with updated design
        Button clearButton = createStyledButton("Clear");
        clearButton.setOnAction(e -> clearCanvas());

        Button textButton = createStyledButton("Add Text");
        textButton.setOnAction(e -> addTextToCanvas());

        Button imageButton = createStyledButton("Add Image");
        imageButton.setOnAction(e -> addImageToCanvas(primaryStage));

        Button saveButton = createStyledButton("Save");
        saveButton.setOnAction(e -> saveCanvas(primaryStage));

        // HBox Layout for Buttons with new styling
        HBox tools = new HBox(20, colorPicker, strokeSlider, clearButton, textButton, imageButton, saveButton);
        tools.setStyle("-fx-padding: 20px; -fx-background-color: #2d3436; -fx-spacing: 15px; -fx-border-radius: 10px; -fx-border-color: #b2bec3; -fx-border-width: 3px;");
        tools.setAlignment(Pos.CENTER);
        tools.setMaxWidth(Double.MAX_VALUE);
        tools.setPrefHeight(90);

        // VBox Layout for the entire window, with canvas at the top and tools below
        VBox root = new VBox(15, canvas, tools);
        root.setStyle("-fx-background-color: #f1f2f6; -fx-padding: 15px;");
        root.setAlignment(Pos.CENTER);

        // Mouse Drawing Event
        canvas.setOnMousePressed(e -> {
            gc.setStroke(currentColor);
            gc.setLineWidth(strokeWidth);
            lastX = e.getX();
            lastY = e.getY();
        });
        canvas.setOnMouseDragged(e -> {
            double currentX = e.getX();
            double currentY = e.getY();

            // Drawing continuous line
            if (lastX != -1 && lastY != -1) {
                gc.strokeLine(lastX, lastY, currentX, currentY); // Draw line from last position to current position
            }

            lastX = currentX; // Update last position
            lastY = currentY;
        });

        // Scene setup with adjusted dimensions
        Scene scene = new Scene(root, 1024, 768);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Create a styled button with modern, flat design
    private Button createStyledButton(String text) {
        Button button = new Button(text);
        button.setStyle("-fx-font-size: 16px; -fx-background-color: #0984e3; -fx-text-fill: white; -fx-padding: 12px 25px; -fx-border-radius: 20px; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 10, 0, 0, 3);");
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #74b9ff; -fx-font-size: 16px; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.2), 10, 0, 0, 3);"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #0984e3; -fx-font-size: 16px; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 10, 0, 0, 3);"));
        button.setOnMousePressed(e -> button.setStyle("-fx-background-color: #0984e3; -fx-font-size: 16px;"));
        return button;
    }

    // Clear the canvas
    private void clearCanvas() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        lastX = -1; // Reset the last positions
        lastY = -1;
    }

    // Add Text to Canvas
    private void addTextToCanvas() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Enter Text");
        dialog.setHeaderText("Add your text to the whiteboard");
        dialog.showAndWait().ifPresent(text -> {
            gc.setFill(currentColor);
            gc.setFont(Font.font("Arial", 24));
            gc.fillText(text, canvas.getWidth() / 2 - text.length() * 6, canvas.getHeight() / 2);
        });
    }

    // Add Image to Canvas (Centered & Scaled)
    private void addImageToCanvas(Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            Image image = new Image(file.toURI().toString());

            // Calculate scaling to fit inside canvas while maintaining aspect ratio
            double maxWidth = canvas.getWidth() * 0.7;
            double maxHeight = canvas.getHeight() * 0.7;
            double aspectRatio = image.getWidth() / image.getHeight();

            double width = maxWidth;
            double height = maxWidth / aspectRatio;

            if (height > maxHeight) {
                height = maxHeight;
                width = maxHeight * aspectRatio;
            }

            // Center the image
            double x = (canvas.getWidth() - width) / 2;
            double y = (canvas.getHeight() - height) / 2;

            gc.drawImage(image, x, y, width, height);
        }
    }

    // Save Canvas to File
    private void saveCanvas(Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Files", "*.png"));
        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(canvas.snapshot(null, null), null), "png", file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
