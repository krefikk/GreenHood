package gui.panels;

import app.AppUtils;
import app.Localization;
import data.DashboardData;
import data.DisposalData;
import data.DisposalData.*;
import data.ReservationData;
import gui.GuiHelper;
import gui.GuiListHelper;
import gui.MainFrame;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.text.SimpleDateFormat;

public class DashboardPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    public MainFrame mainFrame;
    
    // Design
    private final int FEED_HEIGHT = 100;
    private final int LEADER_HEIGHT = 75;
    private final int GAP = 4;
    
    // Data Control
    private int feedLimit = 100; // Default = 100
    
    // UI Elements
    private JLabel lblUserInfo;
    private JButton btnProfile;
    private JButton btnShow;
    private JSpinner spinnerLimit;
    
    // List components
    private JPanel neighborListPanel; 
    private JPanel companyListPanel;
    private JPanel disposalListPanel;
    private JPanel reservationListPanel;
    private JPanel recycledListPanel;
    
    // Middle part
    private JPanel centerPanel; 
    
    // Formats
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public DashboardPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(15, 15)); 
        setBackground(new Color(240, 242, 245)); 
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // Header
        createHeader();

        // Left menu
        createLeftSidebar();

        // Right menu
        createRightSidebar();

        // Middle menu
        centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        add(centerPanel, BorderLayout.CENTER);

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                refreshDashboard();
            }
        });
    }

    // Live preview menu
    private void createLeftSidebar() {
        JPanel leftSidebar = new JPanel(new BorderLayout(0, 5));
        leftSidebar.setOpaque(false);
        leftSidebar.setPreferredSize(new Dimension(400, 0));

        // Title
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

        JLabel lblFeedTitle = new JLabel(Localization.get("livestream"), SwingConstants.CENTER);
        lblFeedTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblFeedTitle.setForeground(new Color(52, 73, 94));
        
        // Dash board
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        controlPanel.setOpaque(false);
        
        // Last Label
        JLabel lblLast = new JLabel(Localization.get("feedprefix")); 
        lblLast.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        // Input spinner
        SpinnerNumberModel model = new SpinnerNumberModel(100, 1, 10000, 10);
        spinnerLimit = new JSpinner(model);
        spinnerLimit.setPreferredSize(new Dimension(60, 24));
        
        // Items label
        JLabel lblItems = new JLabel(Localization.get("feedsuffix")); 
        lblItems.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Show button
        btnShow = new JButton(Localization.get("showbtn")); 
        btnShow.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnShow.setBackground(new Color(52, 152, 219));
        btnShow.setForeground(Color.WHITE);
        btnShow.setFocusPainted(false);
        btnShow.setMargin(new Insets(2, 10, 2, 10));
        btnShow.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btnShow.addActionListener(_ -> {
            try {
            	spinnerLimit.commitEdit();
                int val = (Integer) spinnerLimit.getValue();
                this.feedLimit = val;
                refreshFeedsOnly();
            } catch (Exception ex) {
            	// If user enters a ridiculous value
            	spinnerLimit.setValue(100);
                this.feedLimit = 100;
            }
        });

        controlPanel.add(lblLast);
        controlPanel.add(spinnerLimit);
        controlPanel.add(lblItems);
        controlPanel.add(btnShow);

        headerPanel.add(lblFeedTitle, BorderLayout.NORTH);
        headerPanel.add(controlPanel, BorderLayout.CENTER);

        leftSidebar.add(headerPanel, BorderLayout.NORTH);
        
        // Tabs
        JTabbedPane leftTabs = new JTabbedPane();
        leftTabs.setFont(new Font("Segoe UI", Font.BOLD, 12));
        leftTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        disposalListPanel = GuiListHelper.createListPanel();
        reservationListPanel = GuiListHelper.createListPanel();
        recycledListPanel = GuiListHelper.createListPanel();

        leftTabs.addTab(Localization.get("recentdisposals"), GuiListHelper.createScrollPane(disposalListPanel));
        leftTabs.addTab(Localization.get("recentreservations"), GuiListHelper.createScrollPane(reservationListPanel));
        leftTabs.addTab(Localization.get("recentrecycles"), GuiListHelper.createScrollPane(recycledListPanel));
        
        leftSidebar.add(leftTabs, BorderLayout.CENTER);
        add(leftSidebar, BorderLayout.WEST);
    }

    private void createRightSidebar() {
        JPanel rightSidebar = new JPanel(new BorderLayout());
        rightSidebar.setOpaque(false);
        rightSidebar.setPreferredSize(new Dimension(380, 0));
        
        JLabel lblLeaderboardTitle = new JLabel(Localization.get("leaderboards"), SwingConstants.CENTER);
        lblLeaderboardTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblLeaderboardTitle.setForeground(new Color(52, 73, 94));
        lblLeaderboardTitle.setBorder(new EmptyBorder(0, 0, 10, 0));
        rightSidebar.add(lblLeaderboardTitle, BorderLayout.NORTH);

        JTabbedPane rightTabs = new JTabbedPane();
        rightTabs.setFont(new Font("Segoe UI", Font.BOLD, 12));
        rightTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        
        neighborListPanel = GuiListHelper.createListPanel();
        companyListPanel = GuiListHelper.createListPanel();

        rightTabs.addTab(Localization.get("neighbors"), GuiListHelper.createScrollPane(neighborListPanel));
        rightTabs.addTab(Localization.get("companies"), GuiListHelper.createScrollPane(companyListPanel));
        
        rightSidebar.add(rightTabs, BorderLayout.CENTER);
        add(rightSidebar, BorderLayout.EAST);
    }

    private void createHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setPreferredSize(new Dimension(1280, 50));

        JPanel leftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftHeader.setOpaque(false);

        JButton btnLogout = new JButton(Localization.get("logout"));
        btnLogout.setOpaque(true);
        btnLogout.setBorderPainted(false);
        btnLogout.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setBackground(new Color(231, 76, 60)); 
        btnLogout.setFocusPainted(false);
        btnLogout.setBorder(new EmptyBorder(5, 15, 5, 15));
        btnLogout.addActionListener(_ -> {
            boolean confirmed = GuiHelper.confirm(this, Localization.get("logout") + "?", Localization.get("confirmlogout"), Localization.get("yes"), Localization.get("no"));
            if(confirmed == true) mainFrame.logout();
        });

        JLabel lblTitle = new JLabel(Localization.get("dashboard"));
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(44, 62, 80));
        
        leftHeader.add(btnLogout);
        leftHeader.add(lblTitle);

        JPanel profileArea = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        profileArea.setOpaque(false);
        
        lblUserInfo = new JLabel(Localization.get("loading"));
        lblUserInfo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblUserInfo.setForeground(new Color(52, 73, 94));
        lblUserInfo.setBorder(new EmptyBorder(0, 0, 0, 10)); 
        
        btnProfile = new JButton(Localization.get("myprofile"));
        btnProfile.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnProfile.setBackground(Color.WHITE);
        btnProfile.setFocusPainted(false);
        btnProfile.addActionListener(_ -> mainFrame.switchPanel("PROFILE_PANEL"));
        
        profileArea.add(lblUserInfo);
        profileArea.add(btnProfile);

        headerPanel.add(leftHeader, BorderLayout.WEST);
        headerPanel.add(profileArea, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);
    }

    private void refreshDashboard() {
        String role = mainFrame.getCurrentRole();
        String username = mainFrame.getCurrentUsername(); 

        if ("GUEST".equals(role)) {
            lblUserInfo.setText(Localization.get("guest"));
            btnProfile.setEnabled(false);
        } else {
            lblUserInfo.setText(Localization.get("welcome", username));
            btnProfile.setEnabled(true);
        }

        // Middle panel button
        centerPanel.removeAll();
        JButton actionButton = new JButton();
        actionButton.setFont(new Font("Segoe UI", Font.BOLD, 18));
        actionButton.setPreferredSize(new Dimension(300, 60));
        actionButton.setFocusable(false);
        actionButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if ("USER".equals(role)) {
            actionButton.setText(Localization.get("userdashboard"));
            actionButton.setBackground(new Color(46, 204, 113));
            actionButton.setForeground(Color.WHITE);
            actionButton.addActionListener(_ -> mainFrame.showUserDashboard());
        } else if ("COMPANY".equals(role)) {
            actionButton.setText(Localization.get("companydashboard"));
            actionButton.setBackground(new Color(52, 152, 219));
            actionButton.setForeground(Color.WHITE);
            actionButton.addActionListener(_ -> mainFrame.showCompanyDashboard());
        } else {
            actionButton.setText(Localization.get("noaccess"));
            actionButton.setEnabled(false);
        }
        centerPanel.add(actionButton);
        centerPanel.revalidate();
        centerPanel.repaint();

        // Get all data
        refreshAllData(username, role);
    }
    
    // Refreshes all data in the page
    private void refreshAllData(String myUsername, String myRole) {
        AppUtils.runAsync(this, () -> {  
            // Database connections
            List<DisposalRecord> disposals = DisposalData.getLastDisposals(feedLimit);
            List<DisposalRecord> reservations = ReservationData.getLastReservations(feedLimit);
            List<DisposalRecord> recycled = DisposalData.getLastRecycledItems(feedLimit);
            
            List<DashboardData.LeaderboardEntry> topNeighbors = DashboardData.getTopNeighbors(25);
            List<DashboardData.LeaderboardEntry> topCompanies = DashboardData.getTopCompanies(25);

            // UI
            SwingUtilities.invokeLater(() -> {
                updateDisposalPanel(disposals);
                updateReservationPanel(reservations);
                updateRecycledPanel(recycled);
                updateNeighborPanel(topNeighbors, myUsername, myRole);
                updateCompanyPanel(topCompanies, myUsername, myRole);
            });
        });
    }
    
    // Only refreshes left menu
    private void refreshFeedsOnly() {
    	// Lock the show button
        if (btnShow != null) btnShow.setEnabled(false);
        
        // Get all three lists
        AppUtils.runAsync(this, 
            () -> {
                // Database connections
                List<DisposalRecord> disposals = DisposalData.getLastDisposals(feedLimit);
                List<DisposalRecord> reservations = ReservationData.getLastReservations(feedLimit);
                List<DisposalRecord> recycled = DisposalData.getLastRecycledItems(feedLimit);
                
                // UI
                SwingUtilities.invokeLater(() -> {
                    updateDisposalPanel(disposals);
                    updateReservationPanel(reservations);
                    updateRecycledPanel(recycled);
                });
            },
            () -> {
                SwingUtilities.invokeLater(() -> {
                    if(btnShow != null) btnShow.setEnabled(true); // Unlock the show button
                });
            }
        );
    }

    // Recently discarded list
    private void updateDisposalPanel(List<DisposalRecord> list) {
        disposalListPanel.removeAll();
        for (DisposalRecord d : list) {
            Color statusColor;
            String statusText, statusStyle;

            if (d.isRecycled) {
                statusColor = GuiListHelper.COLOR_RECYCLED; 
                statusText = Localization.get("recycled") + ": " + d.companyName;
                statusStyle = "#27ae60"; 
            } else if (d.isReserved) {
                statusColor = GuiListHelper.COLOR_RESERVED; 
                statusText = Localization.get("reserved") + ": " + d.companyName;
                statusStyle = "#d35400"; 
            } else {
                statusColor = GuiListHelper.COLOR_AVAILABLE; 
                statusText = Localization.get("available");
                statusStyle = "#2980b9"; 
            }
            
            String typeName = d.getName();
            String dateStr = (d.discardDate != null) ? sdf.format(d.discardDate) : "-";

            String html = String.format(
                "<html><div style='font-family:Segoe UI; font-size:11px; color:#333333;'>" +
                "<b style='font-size:13px; color:#000000;'>%s</b><br>" + 
                Localization.get("disposaldate") + ": <span style='color:#555555;'>%s</span><br>" +
                "<b>%.2f kg</b> / <b>%.2f m³</b><br>" + 
                "<span style='color:%s;'><b>%s</b></span></div></html>",
                typeName, dateStr, d.weight, d.volume, statusStyle, statusText
            );
            
            disposalListPanel.add(GuiListHelper.createFeedCard(html, statusColor, FEED_HEIGHT));
            disposalListPanel.add(Box.createVerticalStrut(GAP));
        }
        if (list.isEmpty()) GuiListHelper.addEmptyMessage(disposalListPanel, Localization.get("nodisposals"));
        GuiListHelper.refreshPanel(disposalListPanel);
    }

    // Recent reservations
    private void updateReservationPanel(List<DisposalRecord> list) {
        reservationListPanel.removeAll();
        for (DisposalRecord r : list) {
            String typeName = r.getName();
            String dateStr = (r.resDate != null) ? sdf.format(r.resDate) : "-";
            
            String html = String.format(
                "<html><div style='font-family:Segoe UI; font-size:11px; color:#333333;'>" +
                "<b style='font-size:13px; color:#000000;'>%s</b><br>" + 
                Localization.get("reservationdate") + ": <span style='color:#555555;'>%s</span><br>" +
                "<b>%.2f kg</b> / <b>%.2f m³</b><br>" + 
                Localization.get("reserver") + ": <span style='color:#d35400;'><b>%s</b></span></div></html>",
                typeName, dateStr, r.weight, r.volume, r.companyName
            );
            
            reservationListPanel.add(GuiListHelper.createFeedCard(html, GuiListHelper.COLOR_RESERVED, FEED_HEIGHT));
            reservationListPanel.add(Box.createVerticalStrut(GAP));
        }
        if (list.isEmpty()) GuiListHelper.addEmptyMessage(reservationListPanel, Localization.get("noreservations"));
        GuiListHelper.refreshPanel(reservationListPanel);
    }

    // Recent recycles
    private void updateRecycledPanel(List<DisposalRecord> list) {
        recycledListPanel.removeAll();
        for (DisposalRecord item : list) {
            String dateStr = (item.recDate != null) ? sdf.format(item.recDate) : "-";
            String typeName = item.getName();

            String html = String.format(
                "<html><div style='font-family:Segoe UI; font-size:11px; color:#333333;'>" +
                "<b style='font-size:13px; color:#000000;'>%s</b><br>" + 
                "<b>%.2f kg</b> / <b>%.2f m³</b><br>" + 
                Localization.get("recycledate") + ": <span style='color:#555555;'>%s</span><br>" + 
                Localization.get("gainedscore") + ": <b>%.2f</b><br>" + 
                Localization.get("recycler") + ": <span style='color:#27ae60;'><b>%s</b></span></div></html>",
                typeName, item.weight, item.volume, dateStr, item.score, item.companyName
            );
            
            recycledListPanel.add(GuiListHelper.createFeedCard(html, GuiListHelper.COLOR_RECYCLED, FEED_HEIGHT));
            recycledListPanel.add(Box.createVerticalStrut(GAP));
        }
        if (list.isEmpty()) GuiListHelper.addEmptyMessage(recycledListPanel, Localization.get("norecycles"));
        GuiListHelper.refreshPanel(recycledListPanel);
    }

    // Leader boards
    private void updateNeighborPanel(List<DashboardData.LeaderboardEntry> leaders, String myUsername, String myRole) {
        neighborListPanel.removeAll();
        int rank = 1;
        for (DashboardData.LeaderboardEntry entry : leaders) {
            boolean isMe = "USER".equals(myRole) && entry.uniqueNo.equals(myUsername);
            JPanel card = GuiListHelper.createStyledLeaderboardCard(rank, entry.name, entry.score, isMe, LEADER_HEIGHT);
            neighborListPanel.add(card);
            neighborListPanel.add(Box.createVerticalStrut(GAP));
            rank++;
        }
        if (leaders.isEmpty()) GuiListHelper.addEmptyMessage(neighborListPanel, Localization.get("nodata"));
        GuiListHelper.refreshPanel(neighborListPanel);
    }

    private void updateCompanyPanel(List<DashboardData.LeaderboardEntry> leaders, String myUsername, String myRole) {
        companyListPanel.removeAll();
        int rank = 1;
        for (DashboardData.LeaderboardEntry entry : leaders) {
            boolean isMe = "COMPANY".equals(myRole) && entry.name.equals(myUsername);
            JPanel card = GuiListHelper.createStyledLeaderboardCard(rank, entry.name, entry.score, isMe, LEADER_HEIGHT);
            companyListPanel.add(card);
            companyListPanel.add(Box.createVerticalStrut(GAP));
            rank++;
        }
        if (leaders.isEmpty()) GuiListHelper.addEmptyMessage(companyListPanel, Localization.get("nodata"));
        GuiListHelper.refreshPanel(companyListPanel);
    }
}