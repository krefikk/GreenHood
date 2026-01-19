package gui.panels;

import app.Localization;
import data.ProfileData;
import database.AddressManager;
import gui.MainFrame;
import gui.GuiHelper;
import gui.GuiHelper.ComboItem;
import database.AuthManager;
import app.AppUtils;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

public class EditProfilePanel extends JPanel {

    private static final long serialVersionUID = 1L;
    public MainFrame mainFrame;
    private JPanel formPanel;
    
    // User
    private int currentId;
    
    // Neighbor
    private JTextField txtFname, txtMname, txtLname;
    private JTextField txtBdate; // YYYY-MM-DD
    private JTextField txtEmail;
    
    // Company
    private JTextField txtCname;
    private JTextField txtFax;
    
    // Common
    private JTextField txtPhone;
    
    // Address
    private JComboBox<ComboItem> cmbProvince;
    private JComboBox<ComboItem> cmbDistrict;
    private JComboBox<ComboItem> cmbNeighborhood;
    private JComboBox<ComboItem> cmbStreet;
    private JTextField txtBuildingNo, txtFloorNo, txtDoorNo;
    
    // Flags
    private boolean isUpdating = false;

    public EditProfilePanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());

        // Header
        JLabel lblTitle = new JLabel(Localization.get("editprofile"), SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setBorder(new EmptyBorder(20, 0, 20, 0));
        add(lblTitle, BorderLayout.NORTH);

        // Scrollable form
        formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        
        // Wrapper for aligning form panel
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.add(formPanel, BorderLayout.NORTH);
        
        JScrollPane scrollPane = new JScrollPane(wrapper);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Change password button
        JButton btnChangePass = new JButton(Localization.get("changepass"));
        btnChangePass.setBackground(new Color(243, 156, 18));
        btnChangePass.setForeground(Color.WHITE);
        
        JButton btnCancel = new JButton(Localization.get("back"));
        btnCancel.setBackground(new Color(231, 76, 60));
        btnCancel.setForeground(Color.WHITE);
        
        JButton btnSave = new JButton(Localization.get("save"));
        btnSave.setBackground(new Color(46, 204, 113));
        btnSave.setForeground(Color.WHITE);
        
        // Action Listeners
        btnCancel.addActionListener(_ -> mainFrame.switchPanel("PROFILE_PANEL"));
        btnSave.addActionListener(_ -> saveChanges());
        
        // Change password window
        btnChangePass.addActionListener(_ -> showChangePasswordDialog());

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnChangePass);
        buttonPanel.add(btnSave);
        
        add(buttonPanel, BorderLayout.SOUTH);

        // Load data on panel open
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                loadFormData();
            }
        });
    }

    private void loadFormData() {
        formPanel.removeAll();
        formPanel.repaint();
        
        String role = mainFrame.getCurrentRole();
        String username = mainFrame.getCurrentUsername(); 

        AppUtils.runAsync(this, () -> {
            // Get profile data
            ProfileData.NeighborProfile nProfile = null;
            ProfileData.CompanyProfile cProfile = null;
            int addressID = 0;
            
            if ("USER".equals(role)) {
                nProfile = ProfileData.getNeighborProfile(username);
                if (nProfile != null) addressID = nProfile.addressID;
            } else {
                cProfile = ProfileData.getCompanyProfile(username);
                if (cProfile != null) addressID = cProfile.addressID;
            }
            
            // Get address lists
            AddressManager.AddressDetails addrDetails = null;
            List<ComboItem> provinces = AddressManager.getProvinces();
            List<ComboItem> districts = new ArrayList<>();
            List<ComboItem> neighborhoods = new ArrayList<>();
            List<ComboItem> streets = new ArrayList<>();
            
            if (addressID > 0) {
                addrDetails = AddressManager.getAddressDetails(addressID);
                if (addrDetails != null) {
                    // Get districts, neighborhoods and streets of selected province
                    districts = AddressManager.getDistricts(addrDetails.provinceID);
                    neighborhoods = AddressManager.getNeighborhoods(addrDetails.districtID);
                    streets = AddressManager.getStreets(addrDetails.neighborhoodID);
                }
            }
            
            // Make variables unchangeable for Lambda
            final ProfileData.NeighborProfile finalNProfile = nProfile;
            final ProfileData.CompanyProfile finalCProfile = cProfile;
            final AddressManager.AddressDetails finalAddrDetails = addrDetails;
            final List<ComboItem> finalProvinces = provinces;
            final List<ComboItem> finalDistricts = districts;
            final List<ComboItem> finalNeighborhoods = neighborhoods;
            final List<ComboItem> finalStreets = streets;
            
            // UI
            SwingUtilities.invokeLater(() -> {
                buildFormUI(role, finalNProfile, finalCProfile, finalAddrDetails, 
                            finalProvinces, finalDistricts, finalNeighborhoods, finalStreets);
            });
        });
    }
    
    private void buildFormUI(String role, 
            ProfileData.NeighborProfile nProfile, 
            ProfileData.CompanyProfile cProfile,
            AddressManager.AddressDetails addrDetails,
            List<ComboItem> provinces, List<ComboItem> districts, 
            List<ComboItem> neighborhoods, List<ComboItem> streets) {

    	isUpdating = true; // Stop listeners temporarily
    	try {
    		GridBagConstraints gbc = new GridBagConstraints();
    		gbc.insets = new Insets(5, 10, 5, 10);
    		gbc.fill = GridBagConstraints.HORIZONTAL;
    		int row = 0;

    		addSectionHeader(Localization.get("generalinfo"), gbc, row++);

    		// Fill all the profile information
    		if ("USER".equals(role) && nProfile != null) {
    			currentId = nProfile.id;
    			txtFname = addFormRow(Localization.get("fname") + ":", nProfile.fname, gbc, row++);
    			txtMname = addFormRow(Localization.get("mname") + ":", (nProfile.mname == null ? "" : nProfile.mname), gbc, row++);
    			txtLname = addFormRow(Localization.get("lname") + ":", nProfile.lname, gbc, row++);
    			txtBdate = addFormRow(Localization.get("birthdate") + ":", String.valueOf(nProfile.bdate), gbc, row++);
    			txtEmail = addFormRow(Localization.get("email") + ":", nProfile.email, gbc, row++);
    			txtPhone = addFormRow(Localization.get("phonenumber") + ":", nProfile.phone, gbc, row++);
    		} else if ("COMPANY".equals(role) && cProfile != null) {
    			currentId = cProfile.id;
    			txtCname = addFormRow(Localization.get("companyname") + ":", cProfile.cname, gbc, row++);
    			txtPhone = addFormRow(Localization.get("phonenumber") + ":", cProfile.phone, gbc, row++);
    			txtFax = addFormRow(Localization.get("fax") + ":", cProfile.fax, gbc, row++);
    		}

    		// Create address UI
    		gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
    		formPanel.add(Box.createVerticalStrut(20), gbc);
    		addSectionHeader(Localization.get("addressinfo"), gbc, row++);
    		gbc.gridwidth = 1;

    		cmbProvince = new JComboBox<>();
    		cmbDistrict = new JComboBox<>();
    		cmbNeighborhood = new JComboBox<>();
    		cmbStreet = new JComboBox<>();

    		txtBuildingNo = new JTextField(10);
    		txtFloorNo = new JTextField(10);
    		txtDoorNo = new JTextField(10);

    		// Fill all address information
    		cmbProvince.addItem(new ComboItem(0, Localization.get("choose")));
    		for (ComboItem item : provinces) cmbProvince.addItem(item);

    		cmbDistrict.addItem(new ComboItem(0, Localization.get("choose")));
    		for (ComboItem item : districts) cmbDistrict.addItem(item);

    		cmbNeighborhood.addItem(new ComboItem(0, Localization.get("choose")));
    		for (ComboItem item : neighborhoods) cmbNeighborhood.addItem(item);

    		cmbStreet.addItem(new ComboItem(0, Localization.get("choose")));
    		for (ComboItem item : streets) cmbStreet.addItem(item);

    		if (addrDetails != null) {
    			setSelectedValue(cmbProvince, addrDetails.provinceID);
    			setSelectedValue(cmbDistrict, addrDetails.districtID);
    			setSelectedValue(cmbNeighborhood, addrDetails.neighborhoodID);
    			setSelectedValue(cmbStreet, addrDetails.streetID);

    			txtBuildingNo.setText(String.valueOf(addrDetails.buildingNo));
    			txtFloorNo.setText(String.valueOf(addrDetails.floorNo));
    			txtDoorNo.setText(String.valueOf(addrDetails.doorNo));
    		}

    		// UI placement
    		addAddressRow(Localization.get("province") + ":", cmbProvince, gbc, row++);
    		addAddressRow(Localization.get("district") + ":", cmbDistrict, gbc, row++);
    		addAddressRow(Localization.get("neighborhood") + ":", cmbNeighborhood, gbc, row++);
    		addAddressRow(Localization.get("street") + ":", cmbStreet, gbc, row++);
    		addAddressRow(Localization.get("buildingno") + ":", txtBuildingNo, gbc, row++);
    		addAddressRow(Localization.get("floorno") + ":", txtFloorNo, gbc, row++);
    		addAddressRow(Localization.get("doorno") + ":", txtDoorNo, gbc, row++);

    		// Apply listeners
    		setupAddressListeners();

    	} finally {
    		isUpdating = false; // Start listeners
    		formPanel.revalidate();
    		formPanel.repaint();
    	}
    }
    
    // UI Helper Methods
    private void addSectionHeader(String title, GridBagConstraints gbc, int row) {
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lbl.setForeground(new Color(44, 62, 80));
        lbl.setBorder(new CompoundBorder(
                new EmptyBorder(10, 0, 5, 0),
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY)
        ));
        
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        formPanel.add(lbl, gbc);
        gbc.gridwidth = 1; // Reset
    }

    private JTextField addFormRow(String label, String value, GridBagConstraints gbc, int row) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Arial", Font.BOLD, 12));
        
        JTextField txt = new JTextField(value, 20);
        
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        formPanel.add(lbl, gbc);
        
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 0.7;
        formPanel.add(txt, gbc);
        
        return txt;
    }
    
    private void addAddressRow(String label, JComponent comp, GridBagConstraints gbc, int row) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Arial", Font.BOLD, 12));
        
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        formPanel.add(lbl, gbc);
        
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 0.7;
        formPanel.add(comp, gbc);
    }

    private void saveChanges() {
        String role = mainFrame.getCurrentRole();
        
        // Address validation
        ComboItem selectedProvince = (ComboItem) cmbProvince.getSelectedItem();
        ComboItem selectedDistrict = (ComboItem) cmbDistrict.getSelectedItem();
        ComboItem selectedNeighborhood = (ComboItem) cmbNeighborhood.getSelectedItem();
        ComboItem selectedStreet = (ComboItem) cmbStreet.getSelectedItem();
        
        if (selectedStreet == null || selectedStreet.getID() == 0) {
            JOptionPane.showMessageDialog(this, Localization.get("erroraddresschoose"));
            return;
        }
        
        if (txtBuildingNo.getText().isEmpty() || txtFloorNo.getText().isEmpty() || txtDoorNo.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, Localization.get("emptybuildinginfo"));
            return;
        }
        
        final String bNo = txtBuildingNo.getText();
        final String fNo = txtFloorNo.getText();
        final String dNo = txtDoorNo.getText();
        final int sId = selectedStreet.getID();
        final int nId = selectedNeighborhood.getID();
        final int disId = selectedDistrict.getID();
        final int pId = selectedProvince.getID();

        AppUtils.runAsync(this, () -> {
            boolean success = false;

            if ("USER".equals(role)) {
                String fname = txtFname.getText();
                String mname = txtMname.getText();
                String lname = txtLname.getText();
                Date bdate;
                try {
                     bdate = Date.valueOf(txtBdate.getText());
                } catch (IllegalArgumentException ex) {
                    throw new AppUtils.DialogException(Localization.get("formatdate"));
                }
                String email = txtEmail.getText();
                String phone = txtPhone.getText();
                
                success = ProfileData.updateNeighbor(
                    currentId, fname, mname, lname, bdate, email, phone,
                    pId, disId, nId, sId, bNo, fNo, dNo
                );
                
            } else if ("COMPANY".equals(role)) {
                String cname = txtCname.getText();
                String phone = txtPhone.getText();
                String fax = txtFax.getText();

                success = ProfileData.updateCompany(
                    currentId, cname, phone, fax,
                    pId, disId, nId, sId, bNo, fNo, dNo
                );
            }

            if (success) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, Localization.get("successfullupdate"));
                    mainFrame.switchPanel("PROFILE_PANEL");
                });
            } else {
                throw new AppUtils.DialogException(Localization.get("failedupdate"));
            }
        });
    }
    
    private void showChangePasswordDialog() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        
        JPasswordField txtOldPass = new JPasswordField();
        JPasswordField txtNewPass = new JPasswordField();
        JPasswordField txtConfirmPass = new JPasswordField();
        
        panel.add(new JLabel(Localization.get("currpass") + ":"));
        panel.add(txtOldPass);
        
        panel.add(new JLabel(Localization.get("newpass") + ":"));
        panel.add(txtNewPass);
        
        panel.add(new JLabel(Localization.get("newpass") + " (" + Localization.get("again") + ") :"));
        panel.add(txtConfirmPass);
        
        Object[] options = { Localization.get("apply"), Localization.get("cancel") };
        int result = JOptionPane.showOptionDialog(
            this, 
            panel, 
            Localization.get("changepass"), 
            JOptionPane.OK_CANCEL_OPTION, 
            JOptionPane.PLAIN_MESSAGE,
            null,
            options,
            options[0]
        );
        
        if (result == 0) {
            String oldPass = new String(txtOldPass.getPassword());
            String newPass = new String(txtNewPass.getPassword());
            String confirmPass = new String(txtConfirmPass.getPassword());
            
            // Validations
            if (oldPass.isEmpty() || newPass.isEmpty()) {
                GuiHelper.showMessage(Localization.get("emptyfields"));
                return;
            }
            
            if (!newPass.equals(confirmPass)) {
                GuiHelper.showMessage(Localization.get("uncompatiblepass"));
                return;
            }
            
            if (oldPass.equals(newPass)) {
                GuiHelper.showMessage(Localization.get("samepass"));
                return;
            }

            // Changing password asynchronously
            AppUtils.runAsync(this, () -> {
                AuthManager.changePassword(
                    mainFrame.getCurrentRole(), currentId, oldPass, newPass
                );
                
                SwingUtilities.invokeLater(() -> {
                	GuiHelper.showMessage(Localization.get("successfullupdatepass"));
                });
            });
        }
    }
    
    // To choose a value from a dropdown menu
    private void setSelectedValue(JComboBox<ComboItem> comboBox, int value) {
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            ComboItem item = comboBox.getItemAt(i);
            if (item.getID() == value) {
                comboBox.setSelectedIndex(i);
                return;
            }
        }
    }
    
    private void setupAddressListeners() {
        cmbProvince.addItemListener(e -> {
            if (isUpdating) return;
            if (e.getStateChange() == ItemEvent.SELECTED) {
                ComboItem item = (ComboItem) cmbProvince.getSelectedItem();
                if (item != null) loadDistrictsAsync(item.getID());
            }
        });

        cmbDistrict.addItemListener(e -> {
            if (isUpdating) return;
            if (e.getStateChange() == ItemEvent.SELECTED) {
                ComboItem item = (ComboItem) cmbDistrict.getSelectedItem();
                if (item != null) loadNeighborhoodsAsync(item.getID());
            }
        });
        
        cmbNeighborhood.addItemListener(e -> {
            if (isUpdating) return;
            if (e.getStateChange() == ItemEvent.SELECTED) {
                ComboItem item = (ComboItem) cmbNeighborhood.getSelectedItem();
                if (item != null) loadStreetsAsync(item.getID());
            }
        });
    }

    // Asynchronous loaders
    private void loadDistrictsAsync(int provinceID) {
        AppUtils.runAsync(this, () -> {
            List<ComboItem> list = AddressManager.getDistricts(provinceID);
            SwingUtilities.invokeLater(() -> {
                isUpdating = true;
                cmbDistrict.removeAllItems();
                cmbNeighborhood.removeAllItems();
                cmbStreet.removeAllItems();
                cmbDistrict.addItem(new ComboItem(0, Localization.get("choose")));
                for (ComboItem i : list) cmbDistrict.addItem(i);
                isUpdating = false;
            });
        });
    }

    private void loadNeighborhoodsAsync(int districtID) {
        AppUtils.runAsync(this, () -> {
            List<ComboItem> list = AddressManager.getNeighborhoods(districtID);
            SwingUtilities.invokeLater(() -> {
                isUpdating = true;
                cmbNeighborhood.removeAllItems();
                cmbStreet.removeAllItems();
                cmbNeighborhood.addItem(new ComboItem(0, Localization.get("choose")));
                for (ComboItem i : list) cmbNeighborhood.addItem(i);
                isUpdating = false;
            });
        });
    }

    private void loadStreetsAsync(int neighborhoodID) {
        AppUtils.runAsync(this, () -> {
            List<ComboItem> list = AddressManager.getStreets(neighborhoodID);
            SwingUtilities.invokeLater(() -> {
                isUpdating = true;
                cmbStreet.removeAllItems();
                cmbStreet.addItem(new ComboItem(0, Localization.get("choose")));
                for (ComboItem i : list) cmbStreet.addItem(i);
                isUpdating = false;
            });
        });
    }
}