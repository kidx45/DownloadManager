package download.manager;

import download.manager.dao.DBConnection;
import download.manager.ui.DownloadManagerUI;

public class Main {

    public static void main(String[] args) {
        DownloadManagerUI.showUi();

        Runtime.getRuntime().addShutdownHook(new Thread(DBConnection::closeConnection));
    }
}