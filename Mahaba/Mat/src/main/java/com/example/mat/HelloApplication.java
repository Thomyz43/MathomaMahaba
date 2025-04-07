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
import javafx.scene.media.*;
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
    private boolean isEraseMode = false; // Flag for erase mode

    private MediaPlayer mediaPlayer;
    private Image movableImage;
    private double imageX, imageY;
    private boolean isImageDragged = false;
    private double lastImageX, lastImageY;

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
        colorPicker.setOnAction(event -> {
            if (!isEraseMode) {
                currentColor = colorPicker.getValue(); // Update color if not in erase mode
            }
        });

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

        Button videoButton = createStyledButton("Add Video");
        videoButton.setOnAction(e -> addVideoToCanvas(primaryStage));

        Button musicButton = createStyledButton("Add Music");
        musicButton.setOnAction(e -> addMusicToCanvas(primaryStage));

        Button saveButton = createStyledButton("Save");
        saveButton.setOnAction(e -> saveCanvas(primaryStage));

        // Create Erase Button
        Button eraseButton = createStyledButton("Erase");
        eraseButton.setOnAction(e -> toggleEraseMode());

        // Create a Back to Canvas Button
        Button backToCanvasButton = createStyledButton("Back to Canvas");
        backToCanvasButton.setOnAction(e -> {
            // Clear canvas and stop any media before switching
            clearCanvas(); // Clears the canvas
            stopMedia();   // Stops any playing audio or video

            // Revert back to the canvas scene with buttons and original layout
            primaryStage.setScene(createCanvasScene(primaryStage)); // Switches back to canvas scene
        });

        // HBox Layout for Buttons with new styling
        HBox tools = new HBox(20, colorPicker, strokeSlider, clearButton, textButton, imageButton, videoButton, musicButton, saveButton, eraseButton, backToCanvasButton);
        tools.setStyle("-fx-padding: 20px; -fx-background-color: #2d3436; -fx-spacing: 15px; -fx-border-radius: 10px; -fx-border-color: #b2bec3; -fx-border-width: 3px;");
        tools.setAlignment(Pos.CENTER);
        tools.setMaxWidth(Double.MAX_VALUE);
        tools.setPrefHeight(90);

        // VBox Layout for the entire window, with canvas at the top and tools below
        VBox root = new VBox(15, canvas, tools);
        root.setStyle("-fx-background-color: #f1f2f6; -fx-padding: 15px;");
        root.setAlignment(Pos.CENTER);

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

    // Toggle Erase Mode
    private void toggleEraseMode() {
        isEraseMode = !isEraseMode;
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

    // Add Image to Canvas (Movable)
    private void addImageToCanvas(Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            movableImage = new Image(file.toURI().toString());
            imageX = (canvas.getWidth() - movableImage.getWidth()) / 2;
            imageY = (canvas.getHeight() - movableImage.getHeight()) / 2;
            drawCanvas();
        }
    }

    // Draw the Canvas with all elements (image, etc.)
    private void drawCanvas() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (movableImage != null) {
            gc.drawImage(movableImage, imageX, imageY);
        }
    }

    // Add Video to Canvas (Play/Pause)
    private void addVideoToCanvas(Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.mkv", "*.avi"));
        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setAutoPlay(true);

            MediaView mediaView = new MediaView(mediaPlayer);
            mediaView.setFitWidth(canvas.getWidth());
            mediaView.setFitHeight(canvas.getHeight());

            // Layout for video
            VBox videoRoot = new VBox(15, mediaView);
            videoRoot.setStyle("-fx-background-color: #f1f2f6; -fx-padding: 15px;");
            videoRoot.setAlignment(Pos.CENTER);

            Scene videoScene = new Scene(videoRoot, 1024, 768);
            primaryStage.setScene(videoScene);
            primaryStage.show();
        }
    }

    // Add Music to Canvas
    private void addMusicToCanvas(Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav"));
        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.play();
        }
    }

    // Save Canvas to File
    private void saveCanvas(Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg"));
        File file = fileChooser.showSaveDialog(primaryStage);

        if (file != null) {
            String extension = getFileExtension(file);
            switch (extension) {
                case "png":
                case "jpg":
                    saveCanvasAsImage(file, extension);
                    break;
                default:
                    System.out.println("Invalid file type.");
            }
        }
    }

    private void saveCanvasAsImage(File file, String extension) {
        try {
            Image image = canvas.snapshot(null, null);
            if ("png".equalsIgnoreCase(extension)) {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "PNG", file);
            } else if ("jpg".equalsIgnoreCase(extension)) {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "JPG", file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int i = name.lastIndexOf('.');
        return (i > 0) ? name.substring(i + 1).toLowerCase() : "";
    }

    // Create a canvas scene for easy switching
    private Scene createCanvasScene(Stage primaryStage) {
        VBox root = new VBox(15, canvas);
        root.setStyle("-fx-background-color: #f1f2f6; -fx-padding: 15px;");
        root.setAlignment(Pos.CENTER);
        return new Scene(root, 1024, 768);
    }

    // Method to stop any media (video/music)
    private void stopMedia() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer = null;
        }
    }
}
