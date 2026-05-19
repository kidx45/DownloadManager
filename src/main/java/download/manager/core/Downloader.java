package download.manager.core;

import download.manager.dao.DownloadDAO;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Downloader implements Runnable {

    private final URL downloadURL;
    private final long startByte;
    private final long endByte;
    private final int threadNum;
    private final RandomAccessFile outputFile;
    private final DownloadDAO dao;
    private final int downloadId;
    private final AtomicLong totalBytesDownloaded;
    private final long totalFileSize;
    private final DownloadInfo downloadInfo;

    public Downloader(URL downloadURL, long startByte, long endByte, int threadNum,
                      RandomAccessFile outputFile, DownloadDAO dao, int downloadId,
                      AtomicLong totalBytesDownloaded, long totalFileSize,
                      DownloadInfo downloadInfo) {
        this.downloadURL = downloadURL;
        this.startByte = startByte;
        this.endByte = endByte;
        this.threadNum = threadNum;
        this.outputFile = outputFile;
        this.dao = dao;
        this.downloadId = downloadId;
        this.totalBytesDownloaded = totalBytesDownloaded;
        this.totalFileSize = totalFileSize;
        this.downloadInfo = downloadInfo;
    }

    @Override
    public void run() {
        download();
    }

    private void download() {
        HttpURLConnection httpURLConnection = null;
        try {
            System.out.printf("Thread %d starting: bytes %d → %d%n", threadNum, startByte, endByte);

            httpURLConnection = (HttpURLConnection) downloadURL.openConnection();
            httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            httpURLConnection.setRequestProperty("Referer", "https://www.google.com");
            httpURLConnection.setRequestProperty("Accept", "application/octet-stream,*/*");
            httpURLConnection.setRequestProperty("Range", "bytes=" + startByte + "-" + endByte);

            int responseCode = httpURLConnection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_PARTIAL && responseCode != HttpURLConnection.HTTP_OK) {
                System.out.println("✗ Thread " + threadNum + ": Server doesn't support range requests! Code: " + responseCode);
                return;
            }

            InputStream stream = httpURLConnection.getInputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            long currentPosition = startByte;

            while ((bytesRead = stream.read(buffer)) != -1) {

                // ─── PAUSE CHECK ──────────────────────────────────────
                synchronized (downloadInfo.getPauseLock()) {
                    while (downloadInfo.isPaused()) {
                        try {
                            dao.updateStatus(downloadId, "PAUSED");
                            System.out.println("Thread " + threadNum + " paused...");
                            downloadInfo.getPauseLock().wait();
                            dao.updateStatus(downloadId, "DOWNLOADING");
                            System.out.println("Thread " + threadNum + " resumed!");
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
                // ──────────────────────────────────────────────────────

                synchronized (outputFile) {
                    outputFile.seek(currentPosition);
                    outputFile.write(buffer, 0, bytesRead);
                }
                currentPosition += bytesRead;

                long downloaded = totalBytesDownloaded.addAndGet(bytesRead);
                double progress = (downloaded * 100.0) / totalFileSize;

                if ((int) progress % 1 == 0) {
                    dao.updateProgress(downloadId, Math.min(progress, 100.0));
                }
            }

            System.out.printf("✓ Thread %d finished!%n", threadNum);

        } catch (IOException ex) {
            Logger.getLogger(Downloader.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
    }
}