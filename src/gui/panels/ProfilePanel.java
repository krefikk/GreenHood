package gui.panels;

import app.AppUtils;
import app.Localization;
import data.ProfileData;
import gui.GuiHelper;
import gui.MainFrame;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class ProfilePanel extends JPanel {

	private static final long serialVersionUID = 1L;
	public MainFrame mainFrame;
    private JPanel infoContainer;
    private JButton btnEdit;

    public ProfilePanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(240, 240, 240));
        headerPanel.setBorder(new EmptyBorder(15, 30, 15, 30));

        JLabel lblTitle = new JLabel(Localization.get("profileinfo"));
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));

        btnEdit = new JButton(Localization.get("edit"));
        btnEdit.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnEdit.setFocusable(false);
        btnEdit.addActionListener(_ -> mainFrame.switchPanel("EDIT_PROFILE"));

        headerPanel.add(lblTitle, BorderLayout.WEST);
        headerPanel.add(btnEdit, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Middle part to contain profile information
        infoContainer = new JPanel(new GridBagLayout());
        infoContainer.setBackground(Color.WHITE);
        
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.add(infoContainer, BorderLayout.NORTH);
        
        JScrollPane scrollPane = new JScrollPane(wrapper);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // Footer
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(new EmptyBorder(15, 30, 15, 30));

        // Back button
        JButton btnBack = new JButton(Localization.get("returntomainmenu"));
        btnBack.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnBack.addActionListener(_ -> mainFrame.switchPanel("DASHBOARD"));
        
        // Delete button
        JButton btnDeleteAccount = new JButton(Localization.get("deleteacc"));
        btnDeleteAccount.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDeleteAccount.setBackground(new Color(220, 53, 69)); // Red
        btnDeleteAccount.setForeground(Color.WHITE);
        btnDeleteAccount.setFocusable(false);
        
        btnDeleteAccount.addActionListener(_ -> performAccountDeletion());

        bottomPanel.add(btnBack, BorderLayout.WEST);
        bottomPanel.add(btnDeleteAccount, BorderLayout.EAST);
        
        add(bottomPanel, BorderLayout.SOUTH);

        // On profile panel opens
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                loadProfileData();
            }
        });
    }

    private void loadProfileData() {
        infoContainer.removeAll();
        String role = mainFrame.getCurrentRole();
        String username = mainFrame.getCurrentUsername();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20); // Row/column gaps
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        
        // Check if guest
        if ("GUEST".equals(role)) {
            JLabel lblGuest = new JLabel(Localization.get("guestprofile"));
            lblGuest.setFont(new Font("Arial", Font.BOLD, 16));
            lblGuest.setForeground(Color.GRAY);
            infoContainer.add(lblGuest);
            btnEdit.setEnabled(false);
            infoContainer.revalidate();
            infoContainer.repaint();
            return;
        }

        btnEdit.setEnabled(true); // USER or COMPANY can edit

        AppUtils.runAsync(this, () -> {
            // Get data
            ProfileData.NeighborProfile nProfile = null;
            ProfileData.CompanyProfile cProfile = null;

            if ("USER".equals(role)) {
                nProfile = ProfileData.getNeighborProfile(username);
            } else if ("COMPANY".equals(role)) {
                cProfile = ProfileData.getCompanyProfile(username);
            }

            final ProfileData.NeighborProfile finalNProfile = nProfile;
            final ProfileData.CompanyProfile finalCProfile = cProfile;

            // UI
            SwingUtilities.invokeLater(() -> {
                GridBagConstraints gbcs = new GridBagConstraints();
                gbcs.insets = new Insets(10, 20, 10, 20); 
                gbcs.fill = GridBagConstraints.HORIZONTAL;
                gbcs.anchor = GridBagConstraints.WEST;

                if (finalNProfile != null) {
                    int row = 0;
                    addDetailRow(Localization.get("userid") + ":", String.valueOf(finalNProfile.id), gbcs, row++);
                    addDetailRow(Localization.get("tc") + ":", finalNProfile.tckn, gbcs, row++);
                    addDetailRow(Localization.get("fullname") + ":", finalNProfile.fname + (finalNProfile.mname == null ? " " : " " + finalNProfile.mname + " ") + finalNProfile.lname, gbcs, row++);
                    addDetailRow(Localization.get("email") + ":", finalNProfile.email, gbcs, row++);
                    addDetailRow(Localization.get("phonenumber") + ":", finalNProfile.phone, gbcs, row++);
                    addDetailRow(Localization.get("address") + ":", finalNProfile.address, gbcs, row++);
                    addDetailRow(Localization.get("age") + ":", finalNProfile.bdate + " (" + Localization.get("yearsold", finalNProfile.age) + ")", gbcs, row++);
                    
                    String sexText = Localization.get("unspecified");
                    if (finalNProfile.sex != null) {
                        switch (finalNProfile.sex.trim().toUpperCase()) {
                            case "M": sexText = Localization.get("male"); break;
                            case "F": sexText = Localization.get("female"); break;
                            case "N": sexText = Localization.get("unspecified"); break;
                        }
                    }
                    addDetailRow(Localization.get("gender") + ":", sexText, gbcs, row++);
                } 
                else if (finalCProfile != null) {
                    int row = 0;
                    addDetailRow(Localization.get("companyid") + ":", String.valueOf(finalCProfile.id), gbcs, row++);
                    addDetailRow(Localization.get("taxno") + ":", finalCProfile.taxNumber, gbcs, row++);
                    addDetailRow(Localization.get("companyname") + ":", finalCProfile.cname, gbcs, row++);
                    
                    String wasteTypesHtml = "<html><body style='width: 600px'>" + finalCProfile.supportedTypes + "</body></html>";
                    addDetailRow(Localization.get("recyclabledisposaltypes") + ":", wasteTypesHtml, gbcs, row++);
                    
                    addDetailRow(Localization.get("phonenumber") + ":", finalCProfile.phone, gbcs, row++);
                    addDetailRow(Localization.get("fax") + ":", finalCProfile.fax, gbcs, row++);
                    addDetailRow(Localization.get("address") + ":", finalCProfile.address, gbcs, row++);
                    addDetailRow(Localization.get("governmentservice") + ":", finalCProfile.isGov ? Localization.get("yes") : Localization.get("no"), gbcs, row++);
                }

                infoContainer.revalidate();
                infoContainer.repaint();
            });
        });
    }
    
    // Delete account
    private void performAccountDeletion() {
        // Get confirmation from user
    	boolean choice = GuiHelper.confirm(this, Localization.get("confirm"), Localization.get("confirmdeleteacc"), Localization.get("yes"), Localization.get("no"));
        if (choice != true) {
            return; // Canceled
        }

        // Get current information before logout
        String username = mainFrame.getCurrentUsername();
        String role = mainFrame.getCurrentRole();
        
        AppUtils.runAsync(this, () -> {
            boolean success = false;

            if ("USER".equals(role)) {
                success = ProfileData.deleteNeighbor(username);
            } else if ("COMPANY".equals(role)) {
                success = ProfileData.deleteCompany(username);
            }

            if (success) {
                SwingUtilities.invokeLater(() -> {
                    GuiHelper.showMessage(this, Localization.get("successfulldeleteacc"));
                    mainFrame.logout();
                });
            } else {
                throw new AppUtils.DialogException(Localization.get("faileddeleteacc"));
            }
        });
    }

    // Structure Methods
    private void addDetailRow(String title, String value, GridBagConstraints gbc, int row) {
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 14));
        lblTitle.setForeground(new Color(80, 80, 80));

        JLabel lblValue = new JLabel(value == null ? "-" : value);
        lblValue.setFont(new Font("Arial", Font.PLAIN, 15));

        gbc.gridx = 0; gbc.gridy = row;
        gbc.weightx = 0.3; // Left -> 30%
        infoContainer.add(lblTitle, gbc);

        gbc.gridx = 1; gbc.gridy = row;
        gbc.weightx = 0.7; // Right -> 70%
        infoContainer.add(lblValue, gbc);
    }
}