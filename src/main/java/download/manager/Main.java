package download.manager;

import download.manager.core.DownloadInfo;
import download.manager.dao.DBConnection;
import download.manager.dao.DownloadDAO;
import download.manager.model.Download;

import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║       Java Download Manager      ║");
        System.out.println("╚══════════════════════════════════╝");

        DownloadDAO dao = new DownloadDAO();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\nWhat would you like to do?");
            System.out.println("  1 → Start a new download");
            System.out.println("  2 → View download history");
            System.out.println("  3 → Exit");
            System.out.print("Choice: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    System.out.print("Enter URL: ");
                    String url = scanner.nextLine().trim();
                    if (url.isEmpty()) {
                        System.out.println("✗ URL cannot be empty.");
                    } else {
                        new DownloadInfo(url, dao);
                    }
                }

                case "2" -> {
                    List<Download> downloads = dao.getAllDownloads();
                    if (downloads.isEmpty()) {
                        System.out.println("No downloads yet.");
                    } else {
                        System.out.println("\n─── Download History ───────────────────");
                        for (Download d : downloads) {
                            System.out.println(d);
                        }
                        System.out.println("────────────────────────────────────────");
                    }
                }

                case "3" -> {
                    DBConnection.closeConnection();
                    System.out.println("Goodbye!");
                    scanner.close();
                    return;
                }

                default -> System.out.println("✗ Invalid choice, try 1, 2 or 3.");
            }
        }
    }
}