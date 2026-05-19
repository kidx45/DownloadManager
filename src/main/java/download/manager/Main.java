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
            System.out.println("  1 → Start a new download");
            System.out.println("  2 → View download history");
            System.out.println("  3 → Pause / Resume a download");
            System.out.println("  4 → Exit");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    System.out.print("Enter URL: ");
                    String url = scanner.nextLine().trim();
                    if (url.isEmpty()) {
                        System.out.println("✗ URL cannot be empty.");
                    } else {
                        // Run download in background thread so menu stays active
                        Thread downloadThread = new Thread(() -> {
                            new DownloadInfo(url, dao);
                        }, "download-thread");
                        downloadThread.setDaemon(true);
                        downloadThread.start();
                        System.out.println("Download started in background! Use option 3 to pause/resume.");
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
                    if (DownloadInfo.activeDownloads.isEmpty()) {
                        System.out.println("No active downloads to pause/resume.");
                    } else {
                        System.out.println("Active downloads:");
                        DownloadInfo.activeDownloads.forEach((id, info) ->
                            System.out.println("  ID: " + id + " → " + (info.isPaused() ? "PAUSED" : "DOWNLOADING"))
                        );
                        System.out.print("Enter download ID to pause/resume: ");
                        String idInput = scanner.nextLine().trim();
                        try {
                            int id = Integer.parseInt(idInput);
                            DownloadInfo info = DownloadInfo.activeDownloads.get(id);
                            if (info != null) {
                                info.togglePause();
                                System.out.println(info.isPaused() ? "⏸ Paused!" : "▶ Resumed!");
                            } else {
                                System.out.println("✗ No active download with that ID.");
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("✗ Invalid ID.");
                        }
                    }
                }

                case "4" -> {
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