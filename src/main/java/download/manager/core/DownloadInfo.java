package download.manager.core;

import download.manager.dao.DownloadDAO;
import download.manager.model.Download;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DownloadInfo {

    private static final int NUM_THREADS = 8;

    private final String downloadUrl;
    private final DownloadDAO dao;
    private String fileName;
    private long fileSize;
    private URL nonStringUrl;
    private int downloadId;

    public DownloadInfo(String downloadUrl, DownloadDAO dao) {
        this.downloadUrl = downloadUrl;
        this.dao = dao;
        start();
    }

    private void start() {
        try {
            // ─── STEP 1: Parse URL and extract file name ──────────────
            nonStringUrl = new URL(downloadUrl);
            fileName = downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1);
            System.out.println("File name: " + fileName);

            // ─── STEP 2: Get file size ─────────────────────────────────
            HttpURLConnection connection = (HttpURLConnection) nonStringUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            connection.setRequestProperty("Referer", "https://www.google.com");
            connection.setRequestProperty("Accept", "application/octet-stream,*/*");
            connection.connect();

            String contentLength = connection.getHeaderField("Content-Length");
            if (contentLength != null) {
                fileSize = Long.parseLong(contentLength);
            } else {
                fileSize = connection.getContentLengthLong();
            }
            connection.disconnect();

            if (fileSize <= 0) {
                System.out.println("✗ Could not determine file size.");
                return;
            }

            System.out.printf("File size: %.2f MB%n", fileSize / (1024.0 * 1024.0));

            // ─── STEP 3: Insert into DB with PENDING status ────────────
            Download download = new Download(downloadUrl, fileName, fileSize, fileName);
            downloadId = dao.addDownload(download);
            if (downloadId == -1) {
                System.out.println("✗ Failed to save download to DB.");
                return;
            }

            // ─── STEP 4: Create the output file ───────────────────────
            RandomAccessFile outputFile = new RandomAccessFile(fileName, "rw");
            outputFile.setLength(fileSize); // pre-allocate exact file size

            // ─── STEP 5: Calculate chunk sizes ────────────────────────
            long chunkSize = fileSize / NUM_THREADS;
            long remainingBytes = fileSize % NUM_THREADS;

            System.out.printf("Splitting into %d chunks of %.2f MB each%n",
                    NUM_THREADS, chunkSize / (1024.0 * 1024.0));

            // ─── STEP 6: Update DB status to DOWNLOADING ──────────────
            dao.updateStatus(downloadId, "DOWNLOADING");

            // ─── STEP 7: Shared progress counter across all threads ────
            AtomicLong totalBytesDownloaded = new AtomicLong(0);

            // ─── STEP 8: Launch thread pool ───────────────────────────
            ExecutorService threadPool = Executors.newFixedThreadPool(NUM_THREADS);

            long startByte = 0;
            for (int i = 0; i < NUM_THREADS; i++) {
                long endByte;

                // Give remaining bytes to the last thread
                if (i == NUM_THREADS - 1) {
                    endByte = startByte + chunkSize + remainingBytes - 1;
                } else {
                    endByte = startByte + chunkSize - 1;
                }

                threadPool.submit(new Downloader(
                        nonStringUrl,
                        startByte,
                        endByte,
                        i + 1,
                        outputFile,
                        dao,
                        downloadId,
                        totalBytesDownloaded,
                        fileSize
                ));

                startByte = endByte + 1;
            }

            // ─── STEP 9: Wait for all threads to finish ───────────────
            threadPool.shutdown();
            boolean finished = threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

            // ─── STEP 10: Update final status in DB ───────────────────
            if (finished) {
                dao.updateProgress(downloadId, 100.0);
                dao.updateStatus(downloadId, "COMPLETED");
                System.out.println("✓ Download COMPLETED: " + fileName);
            } else {
                dao.updateStatus(downloadId, "FAILED");
                System.out.println("✗ Download FAILED: " + fileName);
            }

            outputFile.close();

        } catch (MalformedURLException e) {
            System.out.println("✗ Invalid URL: " + e.getMessage());
            if (downloadId != -1) dao.updateStatus(downloadId, "FAILED");
        } catch (IOException e) {
            Logger.getLogger(DownloadInfo.class.getName()).log(Level.SEVERE, null, e);
            if (downloadId != -1) dao.updateStatus(downloadId, "FAILED");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.getLogger(DownloadInfo.class.getName()).log(Level.SEVERE, null, e);
            if (downloadId != -1) dao.updateStatus(downloadId, "FAILED");
        }
    }
}