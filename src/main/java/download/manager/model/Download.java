package download.manager.model;

import java.sql.Timestamp;

public class Download {
    private int id;
    private String url;
    private String fileName;
    private long fileSize;
    private String status;
    private double progress;
    private int chunks;
    private String savePath;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Constructor for creating a new download
    public Download(String url, String fileName, long fileSize, String savePath) {
        this.url = url;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.status = "PENDING";
        this.progress = 0.0;
        this.chunks = 8;
        this.savePath = savePath;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getProgress() { return progress; }
    public void setProgress(double progress) { this.progress = progress; }

    public String getSavePath() { return savePath; }
    public void setSavePath(String savePath) { this.savePath = savePath; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return String.format("[%d] %s | %s | %.1f%% | %s", id, fileName, status, progress, url);
    }
}