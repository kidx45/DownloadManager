package download.manager.dao;

import download.manager.model.Download;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DownloadDAO {

    private Connection connection;

    public DownloadDAO() {
        this.connection = DBConnection.getConnection();
    }

    // ─── INSERT ───────────────────────────────────────────────
    // Call this when user adds a new download URL
    public int addDownload(Download download) {
        if (connection == null) {
            System.out.println("✗ Cannot add download because the database connection is unavailable.");
            return -1;
        }

        String sql = "INSERT INTO downloads (url, file_name, file_size, status, progress, chunks, save_path) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, download.getUrl());
            stmt.setString(2, download.getFileName());
            stmt.setLong(3, download.getFileSize());
            stmt.setString(4, download.getStatus());
            stmt.setDouble(5, download.getProgress());
            stmt.setInt(6, 8);
            stmt.setString(7, download.getSavePath());
            stmt.executeUpdate();

            // Get the auto-generated ID back from MySQL
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                int generatedId = keys.getInt(1);
                System.out.println("✓ Download added to DB with ID: " + generatedId);
                return generatedId;
            }
        } catch (SQLException e) {
            System.out.println("✗ Failed to add download: " + e.getMessage());
        }
        return -1;
    }

    // ─── UPDATE STATUS ────────────────────────────────────────
    // Call this when status changes: PENDING → DOWNLOADING → COMPLETED / FAILED
    public void updateStatus(int id, String status) {
        if (connection == null) {
            System.out.println("✗ Cannot update status because the database connection is unavailable.");
            return;
        }

        String sql = "UPDATE downloads SET status = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, id);
            stmt.executeUpdate();
            System.out.println("✓ Download " + id + " status updated to: " + status);
        } catch (SQLException e) {
            System.out.println("✗ Failed to update status: " + e.getMessage());
        }
    }

    // ─── UPDATE PROGRESS ─────────────────────────────────────
    // Call this periodically from threads to track % done
    public void updateProgress(int id, double progress) {
        if (connection == null) {
            return;
        }

        String sql = "UPDATE downloads SET progress = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDouble(1, progress);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("✗ Failed to update progress: " + e.getMessage());
        }
    }

    // ─── GET ALL ──────────────────────────────────────────────
    // Returns all downloads — useful for showing download history
    public List<Download> getAllDownloads() {
        List<Download> downloads = new ArrayList<>();
        if (connection == null) {
            System.out.println("✗ Cannot load download history because the database connection is unavailable.");
            return downloads;
        }

        String sql = "SELECT * FROM downloads ORDER BY created_at DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Download d = new Download(
                        rs.getString("url"),
                        rs.getString("file_name"),
                        rs.getLong("file_size"),
                        rs.getString("save_path")
                );
                d.setId(rs.getInt("id"));
                d.setStatus(rs.getString("status"));
                d.setProgress(rs.getDouble("progress"));
                d.setCreatedAt(rs.getTimestamp("created_at"));
                downloads.add(d);
            }
        } catch (SQLException e) {
            System.out.println("✗ Failed to get downloads: " + e.getMessage());
        }
        return downloads;
    }

    // ─── GET BY ID ────────────────────────────────────────────
    public Download getDownloadById(int id) {
        if (connection == null) {
            System.out.println("✗ Cannot load download because the database connection is unavailable.");
            return null;
        }

        String sql = "SELECT * FROM downloads WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Download d = new Download(
                        rs.getString("url"),
                        rs.getString("file_name"),
                        rs.getLong("file_size"),
                        rs.getString("save_path")
                );
                d.setId(rs.getInt("id"));
                d.setStatus(rs.getString("status"));
                d.setProgress(rs.getDouble("progress"));
                d.setCreatedAt(rs.getTimestamp("created_at"));
                return d;
            }
        } catch (SQLException e) {
            System.out.println("✗ Failed to get download: " + e.getMessage());
        }
        return null;
    }

    // ─── DELETE ───────────────────────────────────────────────
    public void deleteDownload(int id) {
        if (connection == null) {
            System.out.println("✗ Cannot delete download because the database connection is unavailable.");
            return;
        }

        String sql = "DELETE FROM downloads WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            System.out.println("✓ Download " + id + " deleted.");
        } catch (SQLException e) {
            System.out.println("✗ Failed to delete download: " + e.getMessage());
        }
    }
}