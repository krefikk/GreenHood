package gui.panels;

import app.AppUtils;
import app.Localization;
import data.DashboardData;
import data.DisposalData;
import data.ProfileData;
import data.DashboardData.LeaderboardEntry;
import data.DisposalData.*;
import data.ProfileData.*;
import gui.GuiHelper;
import gui.GuiListHelper;
import gui.MainFrame;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.List;

public class UserDashboardPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    public MainFrame mainFrame;
    private String userTCKN;
    
    // UI Elements
    private JPanel pnlHistoryList; 
    private JPanel pnlStatsGrid;
    private JPanel pnlLeaderboardList;
    private JLabel lblLeaderboardHeader;
    
    // Statistics Filtering UI
    private JButton btnStatsMonth, btnStatsYear, btnStatsAll;
    private int currentStatsRange = 0; // 0: All, 1: Year, 2: Month
    
    // Decimal format
    private static final DecimalFormat df = new DecimalFormat("#0.00");

    public UserDashboardPanel(MainFrame mainFrame, String tckn) {
        this.mainFrame = mainFrame;
        this.userTCKN = tckn;
        
        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(240, 242, 245));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // Header
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        
        JButton btnBack = new JButton(Localization.get("returntomainmenu"));
        btnBack.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnBack.setFocusPainted(false);
        btnBack.addActionListener(_ -> mainFrame.switchPanel("DASHBOARD"));
        
        JLabel lblTitle = new JLabel(Localization.get("persdisposalmng"));
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(new Color(44, 62, 80));
        lblTitle.setBorder(new EmptyBorder(0, 10, 0, 0));
        
        topPanel.add(btnBack, BorderLayout.WEST);
        topPanel.add(lblTitle, BorderLayout.CENTER);
        
        add(topPanel, BorderLayout.NORTH);

        // Main content layout
        JPanel contentPanel = new JPanel(new GridLayout(1, 3, 20, 0)); 
        contentPanel.setOpaque(false);

        // Left menu
        contentPanel.add(createLeftPanel());
        // Middle menu
        contentPanel.add(createCenterPanel());
        // Right menu
        contentPanel.add(createRightPanel());
        add(contentPanel, BorderLayout.CENTER);
        
        // Load data
        refreshData();
    }

    // Left Menu
    private JPanel createLeftPanel() {
        JPanel container = new JPanel(new BorderLayout(0, 10));
        container.setOpaque(false);
        
        // Title
        JLabel lblHeader = new JLabel(Localization.get("disposalhstry"));
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblHeader.setForeground(new Color(52, 73, 94));
        container.add(lblHeader, BorderLayout.NORTH);
        
        // List area
        pnlHistoryList = new JPanel();
        pnlHistoryList.setLayout(new BoxLayout(pnlHistoryList, BoxLayout.Y_AXIS));
        pnlHistoryList.setBackground(Color.WHITE);
        
        JScrollPane scroll = new JScrollPane(pnlHistoryList);
        scroll.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        container.add(scroll, BorderLayout.CENTER);
        
        // Add button
        JButton btnAdd = new JButton(Localization.get("addnewdisposal"));
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnAdd.setBackground(new Color(39, 174, 96));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFocusPainted(false);
        btnAdd.setPreferredSize(new Dimension(100, 40));
        
        btnAdd.addActionListener(_ -> {
        	AppUtils.runAsync(this, () -> {
                List<DisposalType> types = DisposalData.getAllDisposalTypesWithCoefs();
                SwingUtilities.invokeLater(() -> showAddDisposalDialog(types));
            });
        });
        
        container.add(btnAdd, BorderLayout.SOUTH);
        return container;
    }

    // Middle Menu
    private JPanel createCenterPanel() {
        JPanel container = new JPanel(new BorderLayout(0, 10));
        container.setOpaque(false);
        
        // Header Wrapper
        JPanel headerWrapper = new JPanel(new BorderLayout(0, 5));
        headerWrapper.setOpaque(false);
        
        JLabel lblHeader = new JLabel(Localization.get("stats"));
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblHeader.setForeground(new Color(52, 73, 94));
        headerWrapper.add(lblHeader, BorderLayout.NORTH);
        
        // Statistics filter buttons
        JPanel pnlDateFilters = new JPanel(new GridLayout(1, 3, 5, 0));
        pnlDateFilters.setOpaque(false);

        btnStatsMonth = createStatButton(Localization.get("lastmonth"));
        btnStatsYear = createStatButton(Localization.get("lastyear"));
        btnStatsAll = createStatButton(Localization.get("alltime"));

        // Action Listeners
        btnStatsMonth.addActionListener(_ -> setStatsRange(2, btnStatsMonth));
        btnStatsYear.addActionListener(_ -> setStatsRange(1, btnStatsYear));
        btnStatsAll.addActionListener(_ -> setStatsRange(0, btnStatsAll));

        // Default Active
        updateStatButtons(btnStatsAll);

        pnlDateFilters.add(btnStatsMonth);
        pnlDateFilters.add(btnStatsYear);
        pnlDateFilters.add(btnStatsAll);
        
        headerWrapper.add(pnlDateFilters, BorderLayout.CENTER);
        container.add(headerWrapper, BorderLayout.NORTH);
        
        // Statistics grid
        pnlStatsGrid = new JPanel(new GridLayout(3, 2, 10, 10));
        pnlStatsGrid.setOpaque(false);
        
        container.add(pnlStatsGrid, BorderLayout.CENTER);
        return container;
    }
    
    // Statistics Button Helper
    private JButton createStatButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(Color.GRAY, 1));
        btn.setBackground(Color.WHITE);
        btn.setForeground(Color.DARK_GRAY);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
    
    // Statistics Logic
    private void setStatsRange(int range, JButton activeBtn) {
        this.currentStatsRange = range;
        updateStatButtons(activeBtn);
        refreshData(); 
    }
    
    private void updateStatButtons(JButton activeBtn) {
        resetStatBtn(btnStatsMonth);
        resetStatBtn(btnStatsYear);
        resetStatBtn(btnStatsAll);
        
        activeBtn.setBackground(new Color(52, 152, 219)); // Blue
        activeBtn.setForeground(Color.WHITE);
        activeBtn.setBorder(new LineBorder(new Color(41, 128, 185), 1));
    }
    
    private void resetStatBtn(JButton btn) {
        btn.setBackground(Color.WHITE);
        btn.setForeground(Color.DARK_GRAY);
        btn.setBorder(new LineBorder(Color.LIGHT_GRAY, 1));
    }

    // Right menu with dynamic title
    private JPanel createRightPanel() {
        JPanel container = new JPanel(new BorderLayout(0, 10));
        container.setOpaque(false);

        lblLeaderboardHeader = new JLabel(Localization.get("nleaderboard"));
        lblLeaderboardHeader.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblLeaderboardHeader.setForeground(new Color(52, 73, 94));
        container.add(lblLeaderboardHeader, BorderLayout.NORTH);
        
        pnlLeaderboardList = new JPanel();
        pnlLeaderboardList.setLayout(new BoxLayout(pnlLeaderboardList, BoxLayout.Y_AXIS));
        pnlLeaderboardList.setBackground(Color.WHITE);
        
        JScrollPane scroll = new JScrollPane(pnlLeaderboardList);
        scroll.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        container.add(scroll, BorderLayout.CENTER);
        
        return container;
    }
    
    private void showAddDisposalDialog(List<DisposalType> types) {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        
        JComboBox<DisposalType> cmbTypes = new JComboBox<>(types.toArray(new DisposalType[0]));
        JTextField txtWeight = new JTextField();
        JTextField txtVolume = new JTextField();
        
        panel.add(new JLabel(Localization.get("disposaltype") + ":"));
        panel.add(cmbTypes);
        panel.add(new JLabel(Localization.get("weight") + " (kg):"));
        panel.add(txtWeight);
        panel.add(new JLabel(Localization.get("volume") + " (m³):"));
        panel.add(txtVolume);
        
        Object[] options = { Localization.get("ok"), Localization.get("cancel") };
        int result = JOptionPane.showOptionDialog(
            this, 
            panel, 
            Localization.get("addnewdisposal"), 
            JOptionPane.OK_CANCEL_OPTION, 
            JOptionPane.PLAIN_MESSAGE,
            null,
            options,
            options[0]
        );

        if (result == 0) {
            try {
                DisposalType selectedType = (DisposalType) cmbTypes.getSelectedItem();
                String wStr = txtWeight.getText().trim();
                String vStr = txtVolume.getText().trim();
                
                if (selectedType == null || wStr.isEmpty() || vStr.isEmpty()) {
                    GuiHelper.showMessage(Localization.get("emptyfields"));
                    return;
                }
                
                double weight = Double.parseDouble(wStr);
                double volume = Double.parseDouble(vStr);
                
                if (weight <= 0 || volume <= 0) {
                    GuiHelper.showMessage(Localization.get("errorlowweightvolume"));
                    return;
                }

                boolean confirm = GuiHelper.confirm(
                    this, 
                    Localization.get("confirm"), 
                    Localization.get("confirmadddisposal"), 
                    Localization.get("yes"), 
                    Localization.get("no")
                );

                if (confirm) {
                	final double finalWeight = weight;
                    final double finalVolume = volume;
                    
                    AppUtils.runAsync(this, () -> {
                        double tCost = DisposalData.calculateDisposalTransportationCost(selectedType.id, finalWeight, finalVolume);
                        double ddScore = DisposalData.calculateDisposalScore(selectedType.id, finalWeight, finalVolume);
                        
                        boolean success = DisposalData.addDisposalRecord(
                            this.userTCKN, selectedType.id, finalWeight, finalVolume, tCost, ddScore
                        );
                        
                        if (!success)
                        	throw new AppUtils.DialogException(Localization.get("faileddisposal"));
                        
                        SwingUtilities.invokeLater(() -> {
                            refreshData();
                            if (success)
                            	GuiHelper.showMessage(Localization.get("successfulldisposal"));
                        });
                    });
                }
                
            } catch (NumberFormatException ex) {
                GuiHelper.showMessage(Localization.get("invalidweightvolume"));
            }
        }
    }

    // Data loading
    private void refreshData() {
        AppUtils.runAsync(this, () -> {
            // Get data
            String neighborhoodName = ProfileData.getUserNeighborhoodName(userTCKN);
            
            int daysLimit = 0;
            if (currentStatsRange == 1) daysLimit = 365;
            else if (currentStatsRange == 2) daysLimit = 30;
            
            List<DisposalRecord> history = ProfileData.getFilteredDisposalHistory(userTCKN, daysLimit);
            UserStats stats = ProfileData.getUserStats(userTCKN, currentStatsRange);
            List<LeaderboardEntry> leaders = DashboardData.getNeighborhoodLeaderboard(userTCKN, 25);

            // UI
            SwingUtilities.invokeLater(() -> {
                lblLeaderboardHeader.setText(Localization.get("nghleaderboard", neighborhoodName));

                // History list
                pnlHistoryList.removeAll();
                if (history.isEmpty()) {
                    JLabel lblEmpty = new JLabel(Localization.get("nodisposalrecord"), SwingConstants.CENTER);
                    lblEmpty.setBorder(new EmptyBorder(20,0,0,0));
                    pnlHistoryList.add(lblEmpty);
                } else {
                    for (DisposalRecord rec : history) {
                        pnlHistoryList.add(createHistoryItem(rec));
                        pnlHistoryList.add(Box.createVerticalStrut(5));
                    }
                }
                
                // Statistics
                pnlStatsGrid.removeAll();
                addStatCard(Localization.get("totaldisposal"), String.valueOf(stats.totalCount), new Color(52, 152, 219));
                addStatCard(Localization.get("totalreservationreceived"), String.valueOf(stats.reservedCount), new Color(149, 165, 166));
                addStatCard(Localization.get("totalrecycles"), String.valueOf(stats.recycledCount), new Color(46, 204, 113));
                addStatCard(Localization.get("totaldiscardedweight"), df.format(stats.totalWeight) + " kg", new Color(155, 89, 182));
                addStatCard(Localization.get("totaldiscardedvolume"), df.format(stats.totalVolume) + " m³", new Color(230, 126, 34));
                addStatCard(Localization.get("contscore"), df.format(stats.totalScore), new Color(241, 196, 15));
                pnlStatsGrid.revalidate();
                pnlStatsGrid.repaint();
                
                // Leader board
                pnlLeaderboardList.removeAll();
                int rank = 1;
                for (LeaderboardEntry item : leaders) {
                    boolean isMe = item.uniqueNo.equals(this.userTCKN);
                    JPanel card = GuiListHelper.createStyledLeaderboardCard(rank, item.name, item.score, isMe, 60);
                    pnlLeaderboardList.add(card);
                    pnlLeaderboardList.add(Box.createVerticalStrut(5));
                    rank++;
                }

                pnlLeaderboardList.revalidate();
                pnlLeaderboardList.repaint();
                pnlHistoryList.revalidate();
                pnlHistoryList.repaint();
            });
        });
    }

    // Helper methods
    private JPanel createHistoryItem(DisposalRecord rec) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 5, 0, 0, getColorForType(rec.disposalTypeName)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 125));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT); 

        String dateStr = rec.discardDate.toString();
        String statusText = rec.isRecycled ? "<span style='color:#27ae60'><b>" + Localization.get("recycled") + "</b></span>" : 
                            (rec.isReserved ? "<span style='color:#e67e22'><b>" + Localization.get("reserved") + "</b></span>" : "<span style='color:#7f8c8d'>" + Localization.get("available") + "</span>");
        
        String htmlContent = "<html><body style='width: 200px'>" +
                "<div style='font-family:Segoe UI; font-size:10px;'>" +
                "<b>" + Localization.get("disposaltype") + ":</b> <span>" + rec.disposalTypeName + "</span><br>" +
                "<b>" + Localization.get("disposaldate") + ":</b> " + dateStr + "<br>" +
                "<b>" + Localization.get("weight") + ":</b> " + rec.weight + " kg / <b>" + Localization.get("volume") + ":</b> " + rec.volume + " m³<br>" +
                "<b>" + Localization.get("status") + ":</b> " + statusText + "<br>" +
                "<span><b>" + Localization.get("score") + ": " + df.format(rec.score) + "</b></span>" +
                "</div></body></html>";
        
        JLabel lblInfo = new JLabel(htmlContent);
        panel.add(lblInfo, BorderLayout.CENTER);

        if (!rec.isReserved && !rec.isRecycled) {
            JButton btnDel = new JButton("X");
            btnDel.setForeground(new Color(231, 76, 60));
            btnDel.setBorder(null);
            btnDel.setContentAreaFilled(false);
            btnDel.setFont(new Font("Segoe UI", Font.BOLD, 18));
            btnDel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnDel.setVisible(false);

            btnDel.addActionListener(_ -> {
            	boolean choice = GuiHelper.confirm(this, Localization.get("confirm"), Localization.get("confirmdeletedisposal"), Localization.get("yes"), Localization.get("no"));
            	if (choice == true) {
            		AppUtils.runAsync(this, () -> {
                        boolean success = DisposalData.deleteDisposal(rec.ddno);
                        SwingUtilities.invokeLater(() -> refreshData());
                        if (!success) {
                        	throw new AppUtils.DialogException(Localization.get("faileddelete"));
                        }
                    });
                }
            });

            panel.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { 
                    panel.setBackground(new Color(248, 249, 250));
                    btnDel.setVisible(true); 
                }
                public void mouseExited(MouseEvent e) { 
                    if (!panel.contains(e.getPoint())) {
                        panel.setBackground(Color.WHITE); 
                        btnDel.setVisible(false);
                    }
                }
            });
            panel.add(btnDel, BorderLayout.EAST);
        } else {
             panel.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { panel.setBackground(new Color(250, 250, 250)); }
                public void mouseExited(MouseEvent e) { panel.setBackground(Color.WHITE); }
            });
        }

        return panel;
    }

    private void addStatCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230,230,230)),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        JLabel lblTitle = new JLabel(title);
        lblTitle.setForeground(Color.GRAY);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        JLabel lblValue = new JLabel(value);
        lblValue.setForeground(color);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 20));
        
        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);
        
        pnlStatsGrid.add(card);
    }
    
    private Color getColorForType(String type) {
        if (type == null) return Color.GRAY;
        switch (type) {
            case "Paper": return new Color(52, 152, 219);     // Blue
            case "Plastic": return new Color(241, 196, 15);   // Yellow
            case "Glass": return new Color(46, 204, 113);     // Green
            case "Metal": return new Color(155, 89, 182);     // Purple
            case "Organic": return new Color(211, 84, 0);     // Orange
            case "Electronic": return new Color(52, 73, 94);  // Dark Blue
            case "Wood": return new Color(121, 85, 72);       // Brown
            case "Textile": return new Color(236, 64, 122);   // Pink
            case "Medical": return new Color(192, 57, 43);    // Red
            case "Battery": return new Color(127, 140, 141);  // Gray
            
            default: return Color.LIGHT_GRAY;
        }
    }
}