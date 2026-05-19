package download.manager;

import download.manager.dao.DBConnection;
import download.manager.ui.DownloadManagerFX;
import javafx.application.Application;

public class Main {

    public static void main(String[] args) {
        Application.launch(DownloadManagerFX.class, args);
    }
}