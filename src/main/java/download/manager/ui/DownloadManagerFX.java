package download.manager.ui;

import download.manager.core.DownloadInfo;
import download.manager.dao.DownloadDAO;
import download.manager.dao.DBConnection;
import download.manager.model.Download;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

public class DownloadManagerFX extends Application {

    private final DownloadDAO dao = new DownloadDAO();
    private final TextField urlField = new TextField();
    private final Button startButton = new Button("Start Download");
    private final Button refreshButton = new Button("Refresh History");
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label statusLabel = new Label("Ready");
    private final TableView<Download> tableView = new TableView<>();
    private final ObservableList<Download> downloads = FXCollections.observableArrayList();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Download Manager (JavaFX)");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));

        VBox topBox = new VBox(8);
        HBox inputRow = new HBox(8);
        urlField.setPromptText("Enter direct file URL (e.g. https://.../file.zip)");
        HBox.setHgrow(urlField, Priority.ALWAYS);
        startButton.setOnAction(e -> startDownload());
        refreshButton.setOnAction(e -> loadHistory());
        inputRow.getChildren().addAll(urlField, startButton, refreshButton);
        topBox.getChildren().addAll(new Label("Download Manager"), inputRow);
        root.setTop(topBox);

        progressBar.setPrefWidth(400);
        progressBar.setProgress(0);
        VBox bottomBox = new VBox(8, progressBar, statusLabel);
        bottomBox.setPadding(new Insets(8, 0, 0, 0));
        root.setBottom(bottomBox);

        TableColumn<Download, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> javafx.beans.binding.Bindings.createIntegerBinding(data.getValue()::getId));

        TableColumn<Download, String> fileCol = new TableColumn<>("File");
        fileCol.setCellValueFactory(
                data -> javafx.beans.binding.Bindings.createStringBinding(data.getValue()::getFileName));

        TableColumn<Download, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(
                data -> javafx.beans.binding.Bindings.createStringBinding(data.getValue()::getStatus));

        TableColumn<Download, String> progCol = new TableColumn<>("Progress");
        progCol.setCellValueFactory(data -> javafx.beans.binding.Bindings
                .createStringBinding(() -> String.format("%.1f%%", data.getValue().getProgress())));

        TableColumn<Download, String> urlCol = new TableColumn<>("URL");
        urlCol.setCellValueFactory(data -> javafx.beans.binding.Bindings.createStringBinding(data.getValue()::getUrl));

        tableView.getColumns().addAll(idCol, fileCol, statusCol, progCol, urlCol);
        tableView.setItems(downloads);
        root.setCenter(tableView);

        primaryStage.setScene(new Scene(root, 900, 600));
        primaryStage.show();

        loadHistory();
    }

    private void startDownload() {
        String url = urlField.getText().trim();
        if (url.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Please enter a URL.", ButtonType.OK);
            a.showAndWait();
            return;
        }

        startButton.setDisable(true);
        refreshButton.setDisable(true);
        progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        statusLabel.setText("Starting...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                new DownloadInfo(url, dao);
                return null;
            }

            @Override
            protected void succeeded() {
                progressBar.setProgress(1.0);
                statusLabel.setText("Download finished");
                startButton.setDisable(false);
                refreshButton.setDisable(false);
                loadHistory();
            }

            @Override
            protected void failed() {
                statusLabel.setText("Download failed");
                startButton.setDisable(false);
                refreshButton.setDisable(false);
                progressBar.setProgress(0);
                loadHistory();
            }
        };

        Thread t = new Thread(task, "download-task");
        t.setDaemon(true);
        t.start();
    }

    private void loadHistory() {
        downloads.clear();
        List<Download> list = dao.getAllDownloads();
        if (list != null && !list.isEmpty()) {
            downloads.addAll(list);
            statusLabel.setText("Loaded " + list.size() + " downloads");
        } else {
            statusLabel.setText("No downloads or DB unreachable");
        }
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        DBConnection.closeConnection();
        Platform.exit();
    }
}
