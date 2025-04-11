package com.example.mat;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
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
    private boolean isEraseMode = false;
    private boolean isDrawingMode = true;

    private MediaPlayer mediaPlayer;
    private Image movableImage;
    private double imageX, imageY;
    private boolean imageBeingDragged = false;

    private boolean isDarkMode = false;
    private HBox toolBarRef;

    public static void main(String[] args) {
        System.setProperty("jdk.module.illegalAccess.silent", "true");
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Interactive Digital Whiteboard");

        setupCanvas();

        ColorPicker colorPicker = createColorPicker();
        Slider strokeSlider = createStrokeSlider();
        HBox buttonBar = createToolBar(primaryStage, colorPicker, strokeSlider);

        VBox root = new VBox(15, canvas, buttonBar);
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root, 1024, 768);
        primaryStage.setScene(scene);
        primaryStage.show();

        applyTheme();
    }

    private void setupCanvas() {
        canvas = new Canvas(800, 600);
        gc = canvas.getGraphicsContext2D();
        clearCanvas();

        canvas.setOnMousePressed(e -> {
            if (isEraseMode) {
                gc.clearRect(e.getX(), e.getY(), strokeWidth, strokeWidth);
            } else if (isDrawingMode) {
                gc.beginPath();
                gc.moveTo(e.getX(), e.getY());
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (isEraseMode) {
                gc.clearRect(e.getX(), e.getY(), strokeWidth, strokeWidth);
            } else if (isDrawingMode) {
                gc.setStroke(currentColor);
                gc.setLineWidth(strokeWidth);
                gc.lineTo(e.getX(), e.getY());
                gc.stroke();
            }

            if (imageBeingDragged && movableImage != null) {
                imageX = e.getX() - movableImage.getWidth() / 2;
                imageY = e.getY() - movableImage.getHeight() / 2;
                drawCanvasImage();
            }
        });

        canvas.setOnMouseReleased(e -> gc.closePath());
        canvas.setOnMouseClicked(e -> {
            if (movableImage != null &&
                    e.getX() >= imageX && e.getX() <= imageX + movableImage.getWidth() &&
                    e.getY() >= imageY && e.getY() <= imageY + movableImage.getHeight()) {
                imageBeingDragged = true;
            }
        });
    }

    private ColorPicker createColorPicker() {
        ColorPicker colorPicker = new ColorPicker(currentColor);
        colorPicker.setStyle("-fx-font-size: 16px;");
        colorPicker.setOnAction(event -> {
            if (!isEraseMode && isDrawingMode) currentColor = colorPicker.getValue();
        });
        return colorPicker;
    }

    private Slider createStrokeSlider() {
        Slider slider = new Slider(1, 10, strokeWidth);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setBlockIncrement(1);
        slider.valueProperty().addListener((obs, oldVal, newVal) -> strokeWidth = newVal.doubleValue());
        return slider;
    }

    private HBox createToolBar(Stage stage, ColorPicker colorPicker, Slider strokeSlider) {
        Button clearBtn = createStyledButton("Clear", e -> clearCanvas());
        Button textBtn = createStyledButton("Add Text", e -> addTextToCanvas());
        Button imageBtn = createStyledButton("Add Image", e -> addImageToCanvas(stage));
        Button videoBtn = createStyledButton("Add Video", e -> playVideo(stage));
        Button musicBtn = createStyledButton("Add Music", e -> playMusic(stage));
        Button saveBtn = createStyledButton("Save", e -> showSaveAsDialog(stage));
        Button eraseBtn = createStyledButton("Erase", e -> isEraseMode = !isEraseMode);
        Button backBtn = createStyledButton("Back", e -> {
            stopMedia();
            clearCanvas();
            stage.setScene(createCanvasScene(stage));
        });

        Button moveUpBtn = createStyledButton("Move Up", e -> moveImage(0, -10));
        Button moveDownBtn = createStyledButton("Move Down", e -> moveImage(0, 10));
        Button moveLeftBtn = createStyledButton("Move Left", e -> moveImage(-10, 0));
        Button moveRightBtn = createStyledButton("Move Right", e -> moveImage(10, 0));

        Button drawBtn = createStyledButton("Draw", e -> {
            isDrawingMode = true;
            isEraseMode = false;
        });

        ToggleButton darkModeToggle = new ToggleButton("Dark Mode");
        darkModeToggle.setStyle("-fx-font-size: 14px; -fx-background-color: #95a5a6; -fx-text-fill: white;");
        darkModeToggle.setOnAction(e -> {
            isDarkMode = darkModeToggle.isSelected();
            applyTheme();
        });

        toolBarRef = new HBox(15, colorPicker, strokeSlider, clearBtn, textBtn, imageBtn, videoBtn, musicBtn, saveBtn, eraseBtn, backBtn,
                moveUpBtn, moveDownBtn, moveLeftBtn, moveRightBtn, drawBtn, darkModeToggle);
        toolBarRef.setAlignment(Pos.CENTER);
        return toolBarRef;
    }

    private Button createStyledButton(String label, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button button = new Button(label);
        button.setOnAction(handler);
        return button;
    }

    private void clearCanvas() {
        gc.setFill(isDarkMode ? Color.web("#2c3e50") : Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    private void addTextToCanvas() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Text");
        dialog.setHeaderText("Enter your text:");
        dialog.showAndWait().ifPresent(text -> {
            gc.setFill(currentColor);
            gc.setFont(Font.font("Arial", 24));
            gc.fillText(text, canvas.getWidth() / 2 - (text.length() * 6), canvas.getHeight() / 2);
        });
    }

    private void addImageToCanvas(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            movableImage = new Image(file.toURI().toString());
            imageX = (canvas.getWidth() - movableImage.getWidth()) / 2;
            imageY = (canvas.getHeight() - movableImage.getHeight()) / 2;
            drawCanvasImage();
        }
    }

    private void drawCanvasImage() {
        clearCanvas();
        if (movableImage != null) {
            gc.drawImage(movableImage, imageX, imageY);
        }
    }

    private void moveImage(double deltaX, double deltaY) {
        if (movableImage != null) {
            imageX += deltaX;
            imageY += deltaY;
            drawCanvasImage();
        }
    }

    private void playVideo(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.mkv", "*.avi"));
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            MediaView mediaView = new MediaView(mediaPlayer);
            mediaView.setFitWidth(canvas.getWidth());
            mediaView.setFitHeight(canvas.getHeight());

            Button playPauseBtn = new Button("Play");
            playPauseBtn.setOnAction(event -> {
                if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    mediaPlayer.pause();
                    playPauseBtn.setText("Play");
                } else {
                    mediaPlayer.play();
                    playPauseBtn.setText("Pause");
                }
            });

            VBox videoRoot = new VBox(mediaView, playPauseBtn);
            videoRoot.setAlignment(Pos.CENTER);
            videoRoot.setStyle("-fx-background-color: #f1f2f6;");
            mediaPlayer.play();
            stage.setScene(new Scene(videoRoot, 1024, 768));
        }
    }

    private void playMusic(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav"));
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);

            Button playPauseBtn = new Button("Play");
            playPauseBtn.setOnAction(event -> {
                if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    mediaPlayer.pause();
                    playPauseBtn.setText("Play");
                } else {
                    mediaPlayer.play();
                    playPauseBtn.setText("Pause");
                }
            });

            VBox musicRoot = new VBox(playPauseBtn);
            musicRoot.setAlignment(Pos.CENTER);
            musicRoot.setStyle("-fx-background-color: #f1f2f6;");
            mediaPlayer.play();
            stage.setScene(new Scene(musicRoot, 1024, 768));
        }
    }

    private void showSaveAsDialog(Stage stage) {
        ChoiceDialog<String> choiceDialog = new ChoiceDialog<>("Picture", "Picture", "Video");
        choiceDialog.setTitle("Save As");
        choiceDialog.setHeaderText("Choose Save Format");
        choiceDialog.setContentText("Select one:");

        choiceDialog.showAndWait().ifPresent(choice -> {
            if ("Picture".equals(choice)) {
                saveCanvasAsImage(stage);
            } else if ("Video".equals(choice)) {
                saveCanvasAsVideo(stage);
            }
        });
    }

    private void saveCanvasAsImage(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG Image", "*.png"),
                new FileChooser.ExtensionFilter("JPEG Image", "*.jpg")
        );
        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            WritableImage snapshot = canvas.snapshot(null, null);
            String ext = getFileExtension(file);
            try {
                if ("png".equalsIgnoreCase(ext)) {
                    ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "PNG", file);
                } else if ("jpg".equalsIgnoreCase(ext)) {
                    ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "JPEG", file);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveCanvasAsVideo(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Video (Simulated)");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("GIF File", "*.gif")
        );
        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Save As Video");
            alert.setHeaderText("Simulated Save");
            alert.setContentText("Saving as video (GIF) is currently a placeholder.\nFile would be: " + file.getName());
            alert.showAndWait();
        }
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return (dot > 0) ? name.substring(dot + 1).toLowerCase() : "";
    }

    private Scene createCanvasScene(Stage stage) {
        VBox root = new VBox(15, canvas);
        root.setAlignment(Pos.CENTER);
        return new Scene(root, 1024, 768);
    }

    private void stopMedia() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer = null;
        }
    }

    private void applyTheme() {
        String background = isDarkMode ? "#2c3e50" : "#fefefe";
        String toolbarColor = isDarkMode ? "#34495e" : "#16a085";
        String buttonColor = isDarkMode ? "#e67e22" : "#e67e22";
        String textColor = "white";

        if (canvas.getParent() instanceof VBox rootBox) {
            rootBox.setStyle("-fx-background-color: " + background + "; -fx-padding: 15px;");
        }

        if (toolBarRef != null) {
            toolBarRef.setStyle("-fx-padding: 20px; -fx-background-color: " + toolbarColor + "; -fx-border-color: #dfe6e9;");
            for (javafx.scene.Node node : toolBarRef.getChildren()) {
                if (node instanceof Button btn) {
                    btn.setStyle("-fx-font-size: 14px; -fx-background-color: " + buttonColor + "; -fx-text-fill: " + textColor + ";");
                }
                if (node instanceof ToggleButton toggleBtn) {
                    toggleBtn.setStyle("-fx-font-size: 14px; -fx-background-color: #95a5a6; -fx-text-fill: white;");
                }
            }
        }

        clearCanvas();
    }
}