package download.manager.ui;

import download.manager.core.DownloadInfo;
import download.manager.dao.DownloadDAO;
import download.manager.model.Download;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

public class DownloadManagerUI extends JFrame {

    private final DownloadDAO dao = new DownloadDAO();
    private final JTextField urlField = new JTextField();
    private final JButton startButton = new JButton("Start Download");
    private final JButton refreshButton = new JButton("Refresh History");
    private final JProgressBar progressBar = new JProgressBar();
    private final JLabel statusLabel = new JLabel("Ready");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "File", "Status", "Progress", "URL"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable historyTable = new JTable(tableModel);

    public DownloadManagerUI() {
        super("Java Download Manager");
        buildUi();
        loadHistory();
    }

    public static void showUi() {
        SwingUtilities.invokeLater(() -> new DownloadManagerUI().setVisible(true));
    }

    private void buildUi() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        root.setBackground(new Color(245, 247, 250));
        setContentPane(root);

        JLabel title = new JLabel("Download Manager");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));

        JLabel subtitle = new JLabel("Paste a direct file URL, start the download, and track progress in the table below.");
        subtitle.setForeground(new Color(85, 92, 102));

        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setOpaque(false);
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        JPanel controlPanel = new JPanel(new BorderLayout(12, 12));
        controlPanel.setOpaque(false);
        controlPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 232)),
                new EmptyBorder(14, 14, 14, 14)
        ));

        JPanel inputRow = new JPanel(new BorderLayout(10, 10));
        inputRow.setOpaque(false);
        JLabel urlLabel = new JLabel("File URL");
        urlField.setToolTipText("Enter a direct file URL, such as a .zip, .mp4, or .pdf link");
        inputRow.add(urlLabel, BorderLayout.NORTH);
        inputRow.add(urlField, BorderLayout.CENTER);
        controlPanel.add(inputRow, BorderLayout.CENTER);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionRow.setOpaque(false);
        startButton.addActionListener(e -> startDownload());
        refreshButton.addActionListener(e -> loadHistory());
        actionRow.add(startButton);
        actionRow.add(refreshButton);
        actionRow.add(statusLabel);
        controlPanel.add(actionRow, BorderLayout.SOUTH);
        root.add(controlPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setOpaque(false);

        progressBar.setStringPainted(true);
        progressBar.setString("Idle");
        bottomPanel.add(progressBar, BorderLayout.NORTH);

        historyTable.setRowHeight(26);
        historyTable.setFillsViewportHeight(true);
        historyTable.setAutoCreateRowSorter(true);
        JScrollPane tableScroll = new JScrollPane(historyTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Download History"));
        bottomPanel.add(tableScroll, BorderLayout.CENTER);

        root.add(bottomPanel, BorderLayout.SOUTH);

        pack();
    }

    private void startDownload() {
        String url = urlField.getText().trim();
        if (url.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a download URL.", "Missing URL", JOptionPane.WARNING_MESSAGE);
            return;
        }

        startButton.setEnabled(false);
        refreshButton.setEnabled(false);
        progressBar.setIndeterminate(true);
        progressBar.setString("Downloading...");
        statusLabel.setText("Starting download...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                new DownloadInfo(url, dao);
                return null;
            }

            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                progressBar.setValue(100);
                progressBar.setString("Done");
                statusLabel.setText("Download finished. Refreshing history...");
                loadHistory();
                startButton.setEnabled(true);
                refreshButton.setEnabled(true);
            }
        };

        worker.execute();
    }

    private void loadHistory() {
        List<Download> downloads = dao.getAllDownloads();
        tableModel.setRowCount(0);

        for (Download download : downloads) {
            tableModel.addRow(new Object[]{
                    download.getId(),
                    download.getFileName(),
                    download.getStatus(),
                    String.format("%.1f%%", download.getProgress()),
                    download.getUrl()
            });
        }

        if (downloads.isEmpty()) {
            statusLabel.setText("No downloads yet.");
        } else {
            statusLabel.setText("Loaded " + downloads.size() + " download(s).");
        }
    }
}