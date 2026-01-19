package gui.panels;

import app.AppUtils;
import app.Localization;
import data.DisposalData;
import data.ProfileData;
import data.DisposalData.*;
import data.ProfileData.*;
import data.ReservationData;
import gui.GuiHelper;
import gui.GuiListHelper;
import gui.MainFrame;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.Date;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CompanyDashboardPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    public MainFrame mainFrame;
    private String companyTaxNumber;
    private List<String> allowedTypes; 
    
    private final int ITEM_HEIGHT = 95;
    private final int GAP = 4;

    // UI Elements
    private JPanel pnlAvailableList; 
    private JPanel pnlReservedList;  
    private JPanel pnlRecycledList;  
    private JPanel pnlStatsGrid;     
    
    // Statistics Filtering UI
    private JButton btnStatsMonth, btnStatsYear, btnStatsAll;
    private int currentStatsRange = 0; // 0: All, 1: Year, 2: Month
    
    // General Filtering
    private JPanel pnlActiveFilterContainer; 
    private JLabel lblActiveFilterText;      
    private DisposalFilter currentFilter = null;

    // Formats
    private static final DecimalFormat df = new DecimalFormat("#0.00");

    public CompanyDashboardPanel(MainFrame mainFrame, String taxNumber) {
        this.mainFrame = mainFrame;
        this.companyTaxNumber = taxNumber;
        
        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(240, 242, 245));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // Header
        JPanel topPanel = new JPanel(new BorderLayout(15, 0));
        topPanel.setOpaque(false);
        
        JButton btnBack = new JButton(Localization.get("returntomainmenu"));
        btnBack.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnBack.setFocusPainted(false);
        btnBack.addActionListener(_ -> mainFrame.switchPanel("DASHBOARD"));
        
        JLabel lblTitle = new JLabel(Localization.get("corpdisposalmng"));
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(44, 62, 80));
        lblTitle.setBorder(new EmptyBorder(0, 10, 0, 0));
        
        topPanel.add(btnBack, BorderLayout.WEST);
        topPanel.add(lblTitle, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // Content
        JPanel contentPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        contentPanel.setOpaque(false);

        contentPanel.add(createLeftPanel());   
        contentPanel.add(createCenterPanel()); 
        contentPanel.add(createRightPanel());  

        add(contentPanel, BorderLayout.CENTER);
        
        initData();
    }
    
    private void initData() {
        AppUtils.runAsync(this, () -> {
            this.allowedTypes = ProfileData.getCompanyAllowedDisposalTypes(companyTaxNumber);
            refreshDataInternal();
        });
    }

    // Left menu
    private JPanel createLeftPanel() {
        JPanel container = new JPanel(new BorderLayout(0, 5));
        container.setOpaque(false);
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(0,0,5,0));
        
        JLabel lblHeader = new JLabel(Localization.get("disposalpool"));
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblHeader.setForeground(new Color(52, 73, 94));
        
        JButton btnFilter = new JButton(Localization.get("dofilter"));
        btnFilter.setIcon(UIManager.getIcon("Tree.openIcon")); 
        btnFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnFilter.setFocusPainted(false);
        btnFilter.addActionListener(_ -> openFilterDialog()); 
        
        headerPanel.add(lblHeader, BorderLayout.WEST);
        headerPanel.add(btnFilter, BorderLayout.EAST);
        
        // Filter bar
        pnlActiveFilterContainer = new JPanel(new BorderLayout());
        pnlActiveFilterContainer.setOpaque(false);
        pnlActiveFilterContainer.setVisible(false); 
        pnlActiveFilterContainer.setBorder(new EmptyBorder(0, 0, 5, 0));

        JPanel chipPanel = new JPanel(new BorderLayout(5, 0));
        chipPanel.setBackground(new Color(220, 230, 241)); 
        chipPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(100, 149, 237), 1, true),
                new EmptyBorder(5, 10, 5, 5)
        ));

        lblActiveFilterText = new JLabel(Localization.get("filter") + ": ...");
        lblActiveFilterText.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        
        JButton btnClearFilter = new JButton("x");
        btnClearFilter.setMargin(new Insets(0, 4, 0, 4));
        btnClearFilter.setFont(new Font("Consolas", Font.BOLD, 12));
        btnClearFilter.setForeground(Color.RED);
        btnClearFilter.setBorder(null);
        btnClearFilter.setContentAreaFilled(false);
        btnClearFilter.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClearFilter.addActionListener(_ -> clearFilter()); 

        chipPanel.add(lblActiveFilterText, BorderLayout.CENTER);
        chipPanel.add(btnClearFilter, BorderLayout.EAST);
        pnlActiveFilterContainer.add(chipPanel, BorderLayout.CENTER);

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.setOpaque(false);
        topContainer.add(headerPanel, BorderLayout.NORTH);
        topContainer.add(pnlActiveFilterContainer, BorderLayout.CENTER);

        container.add(topContainer, BorderLayout.NORTH);

        // List
        pnlAvailableList = GuiListHelper.createListPanel();
        container.add(GuiListHelper.createScrollPane(pnlAvailableList), BorderLayout.CENTER);
        
        return container;
    }

    // Middle menu
    private JPanel createCenterPanel() {
        JPanel container = new JPanel(new BorderLayout(0, 10));
        container.setOpaque(false);
        
        // Header wrapper
        JPanel headerWrapper = new JPanel(new BorderLayout(0, 5));
        headerWrapper.setOpaque(false);

        JLabel lblHeader = new JLabel(Localization.get("stats"));
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblHeader.setForeground(new Color(52, 73, 94));
        headerWrapper.add(lblHeader, BorderLayout.NORTH);

        // Statistics filtering
        JPanel pnlDateFilters = new JPanel(new GridLayout(1, 3, 5, 0));
        pnlDateFilters.setOpaque(false);

        btnStatsMonth = createStatButton(Localization.get("lastmonth"));
        btnStatsYear = createStatButton(Localization.get("lastyear"));
        btnStatsAll = createStatButton(Localization.get("alltime"));

        // Action listeners
        btnStatsMonth.addActionListener(_ -> setStatsRange(2, btnStatsMonth));
        btnStatsYear.addActionListener(_ -> setStatsRange(1, btnStatsYear));
        btnStatsAll.addActionListener(_ -> setStatsRange(0, btnStatsAll));

        // Default active
        updateStatButtons(btnStatsAll);

        pnlDateFilters.add(btnStatsMonth);
        pnlDateFilters.add(btnStatsYear);
        pnlDateFilters.add(btnStatsAll);
        
        headerWrapper.add(pnlDateFilters, BorderLayout.CENTER);
        container.add(headerWrapper, BorderLayout.NORTH);
        
        // Statistics Grid
        pnlStatsGrid = new JPanel(new GridLayout(5, 1, 0, 10));
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
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        return btn;
    }
    
    // Statistics Logic
    private void setStatsRange(int range, JButton activeBtn) {
        this.currentStatsRange = range;
        updateStatButtons(activeBtn);
        refreshData(); 
    }
    
    private void updateStatButtons(JButton activeBtn) {
        // Reset all
        resetStatBtn(btnStatsMonth);
        resetStatBtn(btnStatsYear);
        resetStatBtn(btnStatsAll);
        
        // Set active
        activeBtn.setBackground(new Color(52, 152, 219));
        activeBtn.setForeground(Color.WHITE);
        activeBtn.setBorder(new LineBorder(new Color(41, 128, 185), 1));
    }
    
    private void resetStatBtn(JButton btn) {
        btn.setBackground(Color.WHITE);
        btn.setForeground(Color.DARK_GRAY);
        btn.setBorder(new LineBorder(Color.LIGHT_GRAY, 1));
        btn.setOpaque(true);
        btn.setBorderPainted(false);
    }

    // Right menu
    private JPanel createRightPanel() {
        JPanel container = new JPanel(new BorderLayout(0, 10));
        container.setOpaque(false);
        
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        pnlReservedList = GuiListHelper.createListPanel();
        pnlRecycledList = GuiListHelper.createListPanel();

        tabbedPane.addTab(Localization.get("reservedones"), GuiListHelper.createScrollPane(pnlReservedList));
        tabbedPane.addTab(Localization.get("recycledones"), GuiListHelper.createScrollPane(pnlRecycledList));
        
        container.add(tabbedPane, BorderLayout.CENTER);
        return container;
    }
    
    // Data refreshing
    private void refreshData() {
        AppUtils.runAsync(this, () -> refreshDataInternal());
    }
    
    private void refreshDataInternal() throws Exception {
        List<DisposalRecord> filteredList = DisposalData.getAvailableDisposalsFiltered(currentFilter, companyTaxNumber);
        
        CompanyStats stats = ProfileData.getCompanyStats(companyTaxNumber, currentStatsRange);
        List<DisposalRecord> reserved = ProfileData.getCompanyReservedDisposals(companyTaxNumber);
        List<DisposalRecord> recycled = ProfileData.getCompanyRecycledDisposals(companyTaxNumber);

        // UI
        SwingUtilities.invokeLater(() -> {
            updateAvailablePanel(filteredList);
            updateStatsPanel(stats);
            updateReservedPanel(reserved);
            updateRecycledPanel(recycled);
        });
    }
    
    private void updateAvailablePanel(List<DisposalRecord> list) {
        pnlAvailableList.removeAll();
        
        if (list.isEmpty()) {
            String msg = (currentFilter != null) ? Localization.get("nodisposals") : Localization.get("nodisposalrecord");
            GuiListHelper.addEmptyMessage(pnlAvailableList, msg);
        } else {
            for (DisposalRecord rec : list) {
                pnlAvailableList.add(createAvailableItem(rec));
                pnlAvailableList.add(Box.createVerticalStrut(GAP));
            }
        }
        GuiListHelper.refreshPanel(pnlAvailableList);
    }

    private void updateStatsPanel(CompanyStats stats) {
        pnlStatsGrid.removeAll();
        addStatCard(Localization.get("totalreservation"), String.valueOf(stats.reservedCount), GuiListHelper.COLOR_RESERVED);
        addStatCard(Localization.get("reservedweightvolume"), df.format(stats.reservedWeight) + "kg / " + df.format(stats.reservedVolume) + "m³", Color.GRAY);
        addStatCard(Localization.get("totalrecycles"), String.valueOf(stats.recycledCount), GuiListHelper.COLOR_RECYCLED);
        addStatCard(Localization.get("recycledweightvolume"), df.format(stats.recycledWeight) + "kg / " + df.format(stats.recycledVolume) + "m³", Color.GRAY);
        addStatCard(Localization.get("contscore"), df.format(stats.totalScore), GuiListHelper.COLOR_AVAILABLE);
        pnlStatsGrid.revalidate(); 
        pnlStatsGrid.repaint();
    }

    private void updateReservedPanel(List<DisposalRecord> list) {
        pnlReservedList.removeAll();
        if (list.isEmpty()) GuiListHelper.addEmptyMessage(pnlReservedList, Localization.get("noreservations"));
        else {
            for (DisposalRecord rec : list) {
                pnlReservedList.add(createReservedItem(rec));
                pnlReservedList.add(Box.createVerticalStrut(GAP));
            }
        }
        GuiListHelper.refreshPanel(pnlReservedList);
    }

    private void updateRecycledPanel(List<DisposalRecord> list) {
        pnlRecycledList.removeAll();
        if (list.isEmpty()) GuiListHelper.addEmptyMessage(pnlRecycledList, Localization.get("norecycles"));
        else {
            for (DisposalRecord rec : list) {
                pnlRecycledList.add(createRecycledItem(rec));
                pnlRecycledList.add(Box.createVerticalStrut(GAP));
            }
        }
        GuiListHelper.refreshPanel(pnlRecycledList);
    }
    
    // Helper methods
    private JPanel createAvailableItem(DisposalRecord rec) {
        Color borderColor = getColorForType(rec.disposalTypeName);
        
        JPanel panel = GuiListHelper.createBaseCard(borderColor, ITEM_HEIGHT);
        
        String translatedType = rec.getName();
        String htmlContent = String.format(
            "<html>" +
            "<div style='font-family:Segoe UI; font-size:11px; color:#333333;'>" +
            "<b style='font-size:13px; color:#000000;'>%s</b><br>" +
            Localization.get("disposaldate") + ": <span style='color:#555555;'>%s</span><br>" +
            "<b>%s kg</b> / <b>%s m³</b><br>" +
            "<span style='color:#2c3e50;'>" + Localization.get("scorevalue") + ": <b>%s</b></span>" +
            "</div></html>",
            translatedType, rec.discardDate, rec.weight, rec.volume, df.format(rec.score)
        );
        panel.add(new JLabel(htmlContent), BorderLayout.CENTER);

        JButton btnReserve = new JButton(Localization.get("reserve"));
        btnReserve.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnReserve.setPreferredSize(new Dimension(100, 30));
        btnReserve.setFocusPainted(false);
        btnReserve.setVisible(false);

        boolean isAllowed = allowedTypes != null && allowedTypes.contains(rec.disposalTypeName);

        if (isAllowed) {
            btnReserve.setBackground(GuiListHelper.COLOR_RESERVED);
            btnReserve.setForeground(Color.WHITE);
            
            btnReserve.addActionListener(_ -> {
                AppUtils.runAsync(this, () -> {
                    boolean success = DisposalData.reserveDiscardedDisposal(companyTaxNumber, rec.ddno);
                    if (!success) {
                    	throw new AppUtils.DialogException(Localization.get("failed"));
                    }
                    SwingUtilities.invokeLater(() -> {
                    	refreshData();
                    	if (success)
                    		GuiHelper.showMessage(Localization.get("disposalreserved"));
                    });
                });
            });

            panel.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { 
                    panel.setBackground(new Color(255, 240, 230));
                    btnReserve.setVisible(true); 
                }
                public void mouseExited(MouseEvent e) { 
                    if(!panel.contains(e.getPoint())) { 
                        panel.setBackground(GuiListHelper.CARD_BG); 
                        btnReserve.setVisible(false); 
                    }
                }
            });
        } else {
            btnReserve.setText(Localization.get("outofscope"));
            btnReserve.setBackground(new Color(230, 230, 230)); 
            btnReserve.setForeground(Color.GRAY);
            btnReserve.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            
            panel.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { btnReserve.setVisible(true); }
                public void mouseExited(MouseEvent e) { 
                    if(!panel.contains(e.getPoint())) btnReserve.setVisible(false); 
                }
            });
            
            btnReserve.addActionListener(_ -> GuiHelper.showMessage(Localization.get("outofscopetext")));
        }

        JPanel btnWrapper = new JPanel(new GridBagLayout());
        btnWrapper.setOpaque(false);
        btnWrapper.add(btnReserve);
        
        panel.add(btnWrapper, BorderLayout.EAST);
        
        return panel;
    }

    private JPanel createReservedItem(DisposalRecord rec) {
        JPanel panel = GuiListHelper.createBaseCard(GuiListHelper.COLOR_RESERVED, ITEM_HEIGHT);
        String translatedType = rec.getName();
        
        String htmlContent = String.format(
            "<html>" +
            "<div style='font-family:Segoe UI; font-size:11px; color:#333333;'>" +
            "<b style='font-size:13px; color:#000000;'>%s</b><br>" +
            Localization.get("reservationdate") + ": <span style='color:#555555;'>%s</span><br>" +
            "<b>%s kg</b> / <b>%s m³</b><br>" +
            "<span style='color:#d35400;'><i>" + Localization.get("pendingaction") + "</i></span>" +
            "</div></html>",
            translatedType, rec.resDate, rec.weight, rec.volume
        );
        
        panel.add(new JLabel(htmlContent), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new GridLayout(2, 1, 0, 5)); 
        btnPanel.setOpaque(false);

        Dimension btnSize = new Dimension(110, 22);
        Font btnFont = new Font("Segoe UI", Font.BOLD, 10);

        JButton btnConfirm = new JButton(Localization.get("recycle"));
        btnConfirm.setBackground(GuiListHelper.COLOR_RECYCLED);
        btnConfirm.setForeground(Color.WHITE);
        btnConfirm.setFocusPainted(false);
        btnConfirm.setFont(btnFont);
        btnConfirm.setPreferredSize(btnSize);
        btnConfirm.setBorder(BorderFactory.createEmptyBorder()); 

        btnConfirm.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btnConfirm.setBackground(new Color(39, 174, 96)); }
            public void mouseExited(MouseEvent e) { btnConfirm.setBackground(GuiListHelper.COLOR_RECYCLED); }
        });

        JButton btnCancel = new JButton(Localization.get("cancel"));
        Color redText = new Color(231, 76, 60);
        btnCancel.setBackground(Color.WHITE);
        btnCancel.setForeground(redText);
        btnCancel.setBorder(BorderFactory.createLineBorder(redText)); 
        btnCancel.setFocusPainted(false);
        btnCancel.setFont(btnFont);
        btnCancel.setPreferredSize(btnSize);

        btnCancel.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btnCancel.setBackground(redText); btnCancel.setForeground(Color.WHITE); }
            public void mouseExited(MouseEvent e) { btnCancel.setBackground(Color.WHITE); btnCancel.setForeground(redText); }
        });
        
        btnCancel.addActionListener(_ -> {
            AppUtils.runAsync(this, () -> {
                ReservationData.cancelReservation(rec.ddno);
                SwingUtilities.invokeLater(() -> refreshData());
            });
        });

        btnConfirm.addActionListener(_ -> {
            AppUtils.runAsync(this, () -> {
                DisposalData.completeRecycling(rec.ddno);
                SwingUtilities.invokeLater(() -> refreshData());
            });
        });

        btnPanel.add(btnConfirm);
        btnPanel.add(btnCancel);

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.add(btnPanel);
        wrapper.setVisible(false);

        panel.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { 
                panel.setBackground(new Color(255, 248, 240)); 
                wrapper.setVisible(true); 
            }
            public void mouseExited(MouseEvent e) { 
                if(!panel.contains(e.getPoint())) { 
                    panel.setBackground(GuiListHelper.CARD_BG); 
                    wrapper.setVisible(false); 
                }
            }
        });

        panel.add(wrapper, BorderLayout.EAST);
        return panel;
    }

    private JPanel createRecycledItem(DisposalRecord rec) {
        JPanel panel = GuiListHelper.createBaseCard(GuiListHelper.COLOR_RECYCLED, ITEM_HEIGHT);
        String translatedType = rec.getName();
        
        String htmlContent = String.format(
            "<html>" +
            "<div style='font-family:Segoe UI; font-size:11px; color:#333333;'>" +
            "<b style='font-size:13px; color:#000000;'>%s</b><br>" +
            Localization.get("recycledate") + ": <span style='color:#555555;'>%s</span><br>" +
            "<b>%s kg</b> / <b>%s m³</b><br>" +
            "<span style='color:#27ae60;'><b>" + Localization.get("recycled") + "</b></span>" +
            "</div></html>",
            translatedType, rec.recDate, rec.weight, rec.volume
        );
        
        panel.add(new JLabel(htmlContent), BorderLayout.CENTER);
        
        JLabel lblDone = new JLabel("<html><span style='color:green; font-size:24px'>✓</span></html>");
        panel.add(lblDone, BorderLayout.EAST);
        
        panel.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { panel.setBackground(new Color(245, 255, 245)); }
            public void mouseExited(MouseEvent e) { panel.setBackground(GuiListHelper.CARD_BG); }
        });
        
        return panel;
    }
    
    // Helper methods
    private void addStatCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 5, 0, 0, color),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        JLabel lblTitle = new JLabel(title);
        lblTitle.setForeground(Color.GRAY);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        
        JLabel lblValue = new JLabel(value);
        lblValue.setForeground(Color.DARK_GRAY);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 16));
        
        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);
        pnlStatsGrid.add(card);
    }

    private Color getColorForType(String type) {
        if (type == null) return GuiListHelper.COLOR_DEFAULT;
        
        // not localizable text
        switch (type) {
            case "Paper": return GuiListHelper.COLOR_AVAILABLE;
            case "Plastic": return new Color(241, 196, 15);
            case "Glass": return GuiListHelper.COLOR_RECYCLED;
            case "Metal": return new Color(155, 89, 182);
            case "Organic": return new Color(211, 84, 0);
            case "Electronic": return new Color(52, 73, 94);
            default: return GuiListHelper.COLOR_DEFAULT;
        }
    }
    
    private void openFilterDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), Localization.get("disposalfilter"), true);
        dialog.setLayout(new BorderLayout(10, 10));
        
        JPanel pnlMain = new JPanel(new BorderLayout(10, 10));
        pnlMain.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel pnlTypesContainer = new JPanel(new BorderLayout());
        pnlTypesContainer.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), Localization.get("recyclabledisposaltypes"),
                0, 0, new Font("Segoe UI", Font.BOLD, 12)));

        JPanel pnlCheckBoxes = new JPanel(new GridLayout(0, 2, 10, 10));
        pnlCheckBoxes.setBackground(Color.WHITE);
        pnlCheckBoxes.setBorder(new EmptyBorder(10, 10, 10, 10));

        String[] types = {"Paper", "Plastic", "Glass", "Metal", "Battery", "Organic", "Textile", "Medical", "Electronic", "Wood"};
        List<JCheckBox> checkBoxes = new ArrayList<>();

        for (String rawType : types) {
            // Create localization key
            String localizedLabel = Localization.get("disposaltype" + rawType.toLowerCase());

            JCheckBox cb = new JCheckBox(localizedLabel);
            cb.setActionCommand(rawType); 
            cb.setBackground(Color.WHITE);
            cb.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            cb.setFocusPainted(false);
            checkBoxes.add(cb);
            pnlCheckBoxes.add(cb);
        }

        JScrollPane scrollTypes = new JScrollPane(pnlCheckBoxes);
        scrollTypes.setBorder(null);
        scrollTypes.setPreferredSize(new Dimension(400, 200));
        pnlTypesContainer.add(scrollTypes, BorderLayout.CENTER);
        pnlMain.add(pnlTypesContainer, BorderLayout.CENTER);

        JPanel pnlBottomContainer = new JPanel(new BorderLayout(0, 15));

        // GridBagLayout
        JPanel pnlInputs = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Header line
        gbc.gridx = 0; gbc.gridy = 0;
        pnlInputs.add(new JLabel(""), gbc);

        gbc.gridx = 1;
        JLabel lblMin = new JLabel(Localization.get("min"), SwingConstants.CENTER);
        lblMin.setFont(new Font("Segoe UI", Font.BOLD, 11));
        pnlInputs.add(lblMin, gbc);

        gbc.gridx = 2;
        JLabel lblMax = new JLabel(Localization.get("max"), SwingConstants.CENTER);
        lblMax.setFont(new Font("Segoe UI", Font.BOLD, 11));
        pnlInputs.add(lblMax, gbc);

        // Min/Max weights
        JTextField txtMinWeight = new JTextField(8);
        JTextField txtMaxWeight = new JTextField(8);
        
        gbc.gridx = 0; gbc.gridy = 1;
        pnlInputs.add(new JLabel(Localization.get("weight") + " (kg):"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.5; 
        pnlInputs.add(txtMinWeight, gbc);
        
        gbc.gridx = 2; gbc.weightx = 0.5; 
        pnlInputs.add(txtMaxWeight, gbc);

        // Min/Max volumes
        JTextField txtMinVol = new JTextField(8);
        JTextField txtMaxVol = new JTextField(8);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        pnlInputs.add(new JLabel(Localization.get("volume") + " (m³):"), gbc);

        gbc.gridx = 1; gbc.weightx = 0.5;
        pnlInputs.add(txtMinVol, gbc);

        gbc.gridx = 2; gbc.weightx = 0.5;
        pnlInputs.add(txtMaxVol, gbc);

        // Min/Max dates
        JTextField txtStartDate = new JTextField(8);
        JTextField txtEndDate = new JTextField(8);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        pnlInputs.add(new JLabel(Localization.get("date") + ":"), gbc);

        gbc.gridx = 1; gbc.weightx = 0.5;
        pnlInputs.add(txtStartDate, gbc);

        gbc.gridx = 2; gbc.weightx = 0.5;
        pnlInputs.add(txtEndDate, gbc);

        // Check boxes
        JCheckBox chkOnlyAllowed = new JCheckBox(Localization.get("showonlyabletorecycle"));
        chkOnlyAllowed.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 3; // 3 columns
        pnlInputs.add(chkOnlyAllowed, gbc);

        pnlBottomContainer.add(pnlInputs, BorderLayout.CENTER);

        // Buttons
        JPanel pnlBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCancel = new JButton(Localization.get("cancel"));
        JButton btnApply = new JButton(Localization.get("apply"));

        btnApply.addActionListener(_ -> {
            List<String> selectedTypes = new ArrayList<>();
            
            for (JCheckBox cb : checkBoxes) {
                if (cb.isSelected()) {
                    selectedTypes.add(cb.getActionCommand());
                }
            }

            DisposalFilter newFilter = new DisposalFilter(
            		selectedTypes, 
            		parseDoubleSafe(txtMinWeight.getText()), 
            		parseDoubleSafe(txtMaxWeight.getText()),
            		parseDoubleSafe(txtMinVol.getText()),
            		parseDoubleSafe(txtMaxVol.getText()),
            		parseDateSafe(txtStartDate.getText()),
            		parseDateSafe(txtEndDate.getText()),
            		chkOnlyAllowed.isSelected()
            );
            
            applyFilter(newFilter);
            dialog.dispose();
        });
        btnCancel.addActionListener(_ -> dialog.dispose());

        pnlBtn.add(btnCancel); pnlBtn.add(btnApply);
        pnlBottomContainer.add(pnlBtn, BorderLayout.SOUTH);

        pnlMain.add(pnlBottomContainer, BorderLayout.SOUTH);

        dialog.add(pnlMain);
        
        // If there is an active filter, change options to visualize that filter
        if (this.currentFilter != null) {
            if (currentFilter.types != null) {
                for (JCheckBox cb : checkBoxes) {
                    if (currentFilter.types.contains(cb.getActionCommand())) {
                        cb.setSelected(true);
                    }
                }
            }

            if (currentFilter.minWeight != null) txtMinWeight.setText(String.valueOf(currentFilter.minWeight));
            if (currentFilter.maxWeight != null) txtMaxWeight.setText(String.valueOf(currentFilter.maxWeight));
            if (currentFilter.minVolume != null) txtMinVol.setText(String.valueOf(currentFilter.minVolume));
            if (currentFilter.maxVolume != null) txtMaxVol.setText(String.valueOf(currentFilter.maxVolume));

            if (currentFilter.startDate != null) txtStartDate.setText(currentFilter.startDate.toString());
            if (currentFilter.endDate != null) txtEndDate.setText(currentFilter.endDate.toString());

            chkOnlyAllowed.setSelected(currentFilter.onlyAllowed);
        }
        
        dialog.pack(); 
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void applyFilter(DisposalFilter filter) {
        AppUtils.runAsync(this, () -> {
            this.currentFilter = filter;
            SwingUtilities.invokeLater(() -> {
                String text = "<html><b>" + Localization.get("filter") + ":</b> " + Localization.get("active") + "</html>"; 
                lblActiveFilterText.setText(text);
                pnlActiveFilterContainer.setVisible(true);
            });
            refreshDataInternal();
        });
    }
    
    private void clearFilter() {
        AppUtils.runAsync(this, () -> {
            this.currentFilter = null;
            SwingUtilities.invokeLater(() -> {
                pnlActiveFilterContainer.setVisible(false);
            });
            refreshDataInternal();
        });
    }
    
    private Double parseDoubleSafe(String val) { try { return val.isEmpty() ? null : Double.parseDouble(val); } catch (Exception e) { return null; } }
    private Date parseDateSafe(String val) { try { return val.isEmpty() ? null : Date.valueOf(val); } catch (Exception e) { return null; } }
}