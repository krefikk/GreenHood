package gui.panels;

import app.AppUtils;
import app.Localization;
import database.AuthManager;
import database.AddressManager;
import data.DisposalData;
import data.DisposalData.*;
import gui.GuiHelper;
import gui.GuiHelper.ComboItem;
import gui.MainFrame;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import java.awt.*;

public class RegisterPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	public MainFrame mainFrame;
    
    private CardLayout formLayout = new CardLayout();
    private JPanel formsPanel = new JPanel(formLayout);

    public RegisterPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50)); // Edge margins

        // Header
        JPanel topPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        JLabel lblTitle = new JLabel(Localization.get("createnewaccount"), SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        
        String[] types = {Localization.get("personalregister"), Localization.get("corporateregister")};
        JComboBox<String> cmbRegType = new JComboBox<>(types);
        cmbRegType.setFont(new Font("Arial", Font.BOLD, 14));

        topPanel.add(lblTitle);
        topPanel.add(cmbRegType);
        add(topPanel, BorderLayout.NORTH);

        // Middle part
        formsPanel.add(createUserForm(), "USER_FORM");
        formsPanel.add(createCompanyForm(), "COMPANY_FORM");
        
        add(formsPanel, BorderLayout.CENTER);

        // Footer
        JButton btnBack = new JButton(Localization.get("returntologin"));
        btnBack.setPreferredSize(new Dimension(200, 40));
        
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(btnBack);
        add(bottomPanel, BorderLayout.SOUTH);

        // On selected role change
        cmbRegType.addActionListener(_ -> {
            if (cmbRegType.getSelectedIndex() == 0) {
                formLayout.show(formsPanel, "USER_FORM");
            } else {
                formLayout.show(formsPanel, "COMPANY_FORM");
            }
        });

        // Back button
        btnBack.addActionListener(_ -> mainFrame.switchPanel("LOGIN_SCREEN"));
    }
    
    // Format the panel for user (neighbor) registration
    private JPanel createUserForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Personal text fields
        JTextField txtName = new JTextField(15);
        JTextField txtMName = new JTextField(15);
        JTextField txtSurname = new JTextField(15);
        JTextField txtTCKN = new JTextField(15);
        JTextField txtBirth = new JTextField("YYYY-MM-DD");
        JTextField txtEmail = new JTextField(15);
        JTextField txtPhone = new JTextField(15);
        
        // Address text fields
        JComboBox<ComboItem> cmbProvince = new JComboBox<>();
        JComboBox<ComboItem> cmbDistrict = new JComboBox<>();
        JComboBox<ComboItem> cmbNeighborhood = new JComboBox<>();
        JComboBox<ComboItem> cmbStreet = new JComboBox<>();
        JTextField txtBuildingNo = new JTextField(5);
        JTextField txtFloorNo = new JTextField(5);
        JTextField txtDoorNo = new JTextField(5);
        
        // Handles address choosing
        setupAddressListeners(cmbProvince, cmbDistrict, cmbNeighborhood, cmbStreet);
        
        // Gender drop-down menu
        String[] genders = {Localization.get("male"), Localization.get("female"), Localization.get("dontspecify")};
        JComboBox<String> cmbGender = new JComboBox<>(genders);
        JPasswordField txtPass = new JPasswordField(15);
        
        JButton btnRegister = new JButton(Localization.get("register"));
        btnRegister.setBackground(new Color(60, 179, 113));
        btnRegister.setForeground(Color.WHITE);
        btnRegister.setOpaque(true);
        btnRegister.setBorderPainted(false);

        // Personal information placement
        int row = 0;
        addFormRow(panel, gbc, row++, Localization.get("fname") + "*:", txtName);
        addFormRow(panel, gbc, row++, Localization.get("mname") + ":", txtMName);
        addFormRow(panel, gbc, row++, Localization.get("lname") + "*:", txtSurname);
        addFormRow(panel, gbc, row++, Localization.get("tc") + "*:", txtTCKN);
        addFormRow(panel, gbc, row++, Localization.get("bdate") + "*:", txtBirth);
        addFormRow(panel, gbc, row++, Localization.get("email") + ":", txtEmail);
        addFormRow(panel, gbc, row++, Localization.get("phonenumber") + " (+90...):", txtPhone);
        
        // Address placement
        addFormRow(panel, gbc, row++, Localization.get("province") + "*:", cmbProvince);
        addFormRow(panel, gbc, row++, Localization.get("district") + "*:", cmbDistrict);
        addFormRow(panel, gbc, row++, Localization.get("neighborhood") + "*:", cmbNeighborhood);
        addFormRow(panel, gbc, row++, Localization.get("street") + "*:", cmbStreet);
        
        // Building, Floor and Door No
        JPanel pnlDetail = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlDetail.add(new JLabel(Localization.get("buildingno") + ": ")); pnlDetail.add(txtBuildingNo);
        pnlDetail.add(new JLabel(" " + Localization.get("floorno") + ": ")); pnlDetail.add(txtFloorNo);
        pnlDetail.add(new JLabel(" " + Localization.get("doorno") + ": ")); pnlDetail.add(txtDoorNo);
        
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1; panel.add(new JLabel(Localization.get("building") + ":"), gbc);
        gbc.gridx = 1; panel.add(pnlDetail, gbc);
        row++;

        // Gender and password placement
        addFormRow(panel, gbc, row++, Localization.get("gender") + ":", cmbGender);
        addFormRow(panel, gbc, row++, Localization.get("password") + "*:", txtPass);
        
        gbc.gridwidth = 2; gbc.gridx = 0; gbc.gridy = row;
        panel.add(btnRegister, gbc);

        // Format data for registration on click register button
        btnRegister.addActionListener(_ -> {
            String genderCode = "N";
            if(cmbGender.getSelectedIndex() == 0) genderCode = "M";
            if(cmbGender.getSelectedIndex() == 1) genderCode = "F";

            // Get selected street ID
            ComboItem streetItem = (ComboItem) cmbStreet.getSelectedItem();
            if (streetItem == null) {
                GuiHelper.showMessage(Localization.get("emptyaddressinput"));
                return;
            }
            
            int buildNo, floorNo, doorNo;
            try {
                buildNo = Integer.parseInt(txtBuildingNo.getText().trim());
                floorNo = Integer.parseInt(txtFloorNo.getText().trim());
                doorNo  = Integer.parseInt(txtDoorNo.getText().trim());
            } catch (NumberFormatException ex) {
                GuiHelper.showMessage(Localization.get("invalidbuildinginfo"));
                return;
            }
            
            // Lock the button
            btnRegister.setEnabled(false);
            btnRegister.setText(Localization.get("loading") + "...");
            
            final int fBuild = buildNo, fFloor = floorNo, fDoor = doorNo;
            final String fName = txtName.getText().trim();
            final String fMName = txtMName.getText().trim();
            final String fSurname = txtSurname.getText().trim();
            final String fTc = txtTCKN.getText().trim();
            final String fBirth = txtBirth.getText().trim();
            final String fEmail = txtEmail.getText().trim();
            final String fPhone = txtPhone.getText().trim();
            final String fPass = new String(txtPass.getPassword());
            final String fGen = genderCode;
            
            AppUtils.runAsync(this, 
                    () -> {
                        // Save the address
                        int addressID = AddressManager.registerAddress(streetItem.getID(), fBuild, fFloor, fDoor);

                        // If there is no problem, save the new user
                        AuthManager.registerUser(
                            fName, fMName, fSurname, fTc, fBirth, fEmail, fPhone,
                            addressID, fGen, fPass
                        );

                        SwingUtilities.invokeLater(() -> {
                        	GuiHelper.showMessage(Localization.get("successfulluserregister"));
                            mainFrame.switchPanel("LOGIN_SCREEN");
                        });
                    },
                    () -> {
                        // Unlock the button
                        SwingUtilities.invokeLater(() -> {
                            btnRegister.setEnabled(true);
                            btnRegister.setText(Localization.get("register"));
                        });
                    }
                );
        });

        return panel;
    }
    
    // Format the panel for company registration
    private JPanel createCompanyForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Company information
        JTextField txtTaxNumber = new JTextField(15);
        JTextField txtCName = new JTextField(15);
        JTextField txtPhone = new JTextField(15);
        JTextField txtFax = new JTextField(15);
        JPasswordField txtPass = new JPasswordField(15);

        // Address information
        JComboBox<ComboItem> cmbProvince = new JComboBox<>();
        JComboBox<ComboItem> cmbDistrict = new JComboBox<>();
        JComboBox<ComboItem> cmbNeighborhood = new JComboBox<>();
        JComboBox<ComboItem> cmbStreet = new JComboBox<>();
        JTextField txtBuildingNo = new JTextField(5);
        JTextField txtFloorNo = new JTextField(5);
        JTextField txtDoorNo = new JTextField(5);
        
        // Handles address choosing
        setupAddressListeners(cmbProvince, cmbDistrict, cmbNeighborhood, cmbStreet);

        // Loading disposal types asynchronously
        JPanel checkBoxPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        checkBoxPanel.setBackground(Color.WHITE);
        List<JCheckBox> disposalCheckBoxes = new ArrayList<>();
        JScrollPane scrollPane = new JScrollPane(checkBoxPanel);
        scrollPane.setPreferredSize(new Dimension(300, 150)); 
        scrollPane.setBorder(BorderFactory.createTitledBorder(Localization.get("recyclabledisposaltypes")));
        
        AppUtils.runAsync(this, () -> {
            List<DisposalType> disposalTypes = DisposalData.getAllDisposalTypes();
            
            SwingUtilities.invokeLater(() -> {
                for (DisposalType type : disposalTypes) {
                    JCheckBox cb = new JCheckBox(type.toString());
                    cb.setBackground(Color.WHITE);
                    cb.putClientProperty("id", type.id); 
                    disposalCheckBoxes.add(cb);
                    checkBoxPanel.add(cb);
                }
                checkBoxPanel.revalidate();
                checkBoxPanel.repaint();
            });
        });

        JButton btnRegister = new JButton(Localization.get("register"));
        btnRegister.setBackground(new Color(70, 130, 180));
        btnRegister.setForeground(Color.WHITE);
        btnRegister.setOpaque(true);
        btnRegister.setBorderPainted(false);

        // General information placement
        int row = 0;
        addFormRow(panel, gbc, row++, Localization.get("taxno") + "*:", txtTaxNumber);
        addFormRow(panel, gbc, row++, Localization.get("companyname") + "*:", txtCName);
        addFormRow(panel, gbc, row++, Localization.get("phonenumber") + "(+90...):", txtPhone);
        addFormRow(panel, gbc, row++, Localization.get("fax") + ":", txtFax);

        // Address information placement
        addFormRow(panel, gbc, row++, Localization.get("province") + ":", cmbProvince);
        addFormRow(panel, gbc, row++, Localization.get("district") + ":", cmbDistrict);
        addFormRow(panel, gbc, row++, Localization.get("neighborhood") + ":", cmbNeighborhood);
        addFormRow(panel, gbc, row++, Localization.get("street") + ":", cmbStreet);
        
        JPanel pnlDetail = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlDetail.add(new JLabel(Localization.get("buildingno") + ": ")); pnlDetail.add(txtBuildingNo);
        pnlDetail.add(new JLabel(" " + Localization.get("floorno") + ": ")); pnlDetail.add(txtFloorNo);
        pnlDetail.add(new JLabel(" " + Localization.get("dorrno") + ": ")); pnlDetail.add(txtDoorNo);
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1; panel.add(new JLabel(Localization.get("building") + ":"), gbc);
        gbc.gridx = 1; panel.add(pnlDetail, gbc);
        row++;

        // Password placement
        addFormRow(panel, gbc, row++, Localization.get("password") + "*:", txtPass);

        // Check box list placement for disposal types
        gbc.gridx = 0; 
        gbc.gridy = row++;
        gbc.gridwidth = 2; 
        gbc.fill = GridBagConstraints.BOTH; // To expand it only vertical
        gbc.weighty = 1.0;
        panel.add(scrollPane, gbc);

        // Register button
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;
        gbc.gridy = row++;
        panel.add(btnRegister, gbc);

        // Format data for registration on click register button
        btnRegister.addActionListener(_ -> {
            List<Integer> selectedIDs = new ArrayList<>();
            for (JCheckBox cb : disposalCheckBoxes) {
                if (cb.isSelected()) {
                    int id = (int) cb.getClientProperty("id");
                    selectedIDs.add(id);
                }
            }

            ComboItem streetItem = (ComboItem) cmbStreet.getSelectedItem();
            if (streetItem == null) {
                GuiHelper.showMessage(Localization.get("emptyaddress"));
                return;
            }
            
            int buildNo, floorNo, doorNo;
            try {
                buildNo = Integer.parseInt(txtBuildingNo.getText().trim());
                floorNo = Integer.parseInt(txtFloorNo.getText().trim());
                doorNo  = Integer.parseInt(txtDoorNo.getText().trim());
            } catch (NumberFormatException ex) {
                GuiHelper.showMessage(Localization.get("invalibuildinginfo"));
                return;
            }
            
            // Lock the button
            btnRegister.setEnabled(false);
            btnRegister.setText(Localization.get("loading") + "...");
            
            final int fBuild = buildNo, fFloor = floorNo, fDoor = doorNo;
            final String fTax = txtTaxNumber.getText().trim();
            final String fCName = txtCName.getText().trim();
            final String fPhone = txtPhone.getText().trim();
            final String fFax = txtFax.getText().trim();
            final String fPass = new String(txtPass.getPassword());
            final List<Integer> fSelectedIDs = new ArrayList<>(selectedIDs);
            
            AppUtils.runAsync(this, 
                    () -> {
                    	// Register given address into database and get its ID
                        int addressID = AddressManager.registerAddress(streetItem.getID(), fBuild, fFloor, fDoor);

                        // Register the company
                        AuthManager.registerCompany(
                            fTax, fCName, fPhone, fFax, addressID, fPass, fSelectedIDs
                        );

                        SwingUtilities.invokeLater(() -> {
                        	GuiHelper.showMessage(Localization.get("successfullcompanyregister"));
                            mainFrame.switchPanel("LOGIN_SCREEN");
                        });
                    },
                    () -> {
                        SwingUtilities.invokeLater(() -> {
                        	// Unlock the button
                            btnRegister.setEnabled(true);
                            btnRegister.setText(Localization.get("register"));
                        });
                    }
                );
        });

        return panel;
    }
    
    private void setupAddressListeners(JComboBox<ComboItem> cmbProvince, JComboBox<ComboItem> cmbDistrict, 
            JComboBox<ComboItem> cmbNeighborhood, JComboBox<ComboItem> cmbStreet) {

    	// Load provinces on start
    	AppUtils.runAsync(this, () -> {
    	    List<ComboItem> provinces = AddressManager.getProvinces();
    	    SwingUtilities.invokeLater(() -> {
    	        for (ComboItem p : provinces) cmbProvince.addItem(p);
    	    });
    	});

    	// On choose any province
    	cmbProvince.addActionListener(_ -> {
    		ComboItem selected = (ComboItem) cmbProvince.getSelectedItem();
    		if (selected == null) return;

    		cmbDistrict.removeAllItems(); // Clear old districts

    		// Get districts
    		AppUtils.runAsync(this, () -> {
    			List<ComboItem> districts = AddressManager.getDistricts(selected.getID());
        	    SwingUtilities.invokeLater(() -> {
        	    	for (ComboItem d : districts) cmbDistrict.addItem(d);
        	    });
        	});
    	});

    	// On choose any district
    	cmbDistrict.addActionListener(_ -> {
    		ComboItem selected = (ComboItem) cmbDistrict.getSelectedItem();
    		if (selected == null) return;

    		cmbNeighborhood.removeAllItems();

    		// Get neighborhoods
    		AppUtils.runAsync(this, () -> {
    			List<ComboItem> neighborhoods = AddressManager.getNeighborhoods(selected.getID());
        	    SwingUtilities.invokeLater(() -> {
        	    	for (ComboItem n : neighborhoods) cmbNeighborhood.addItem(n);
        	    });
        	});
    	});

    	// On choose any neighborhood
    	cmbNeighborhood.addActionListener(_ -> {
    		ComboItem selected = (ComboItem) cmbNeighborhood.getSelectedItem();
    		if (selected == null) return;

    		cmbStreet.removeAllItems();

    		// Get streets
    		AppUtils.runAsync(this, () -> {
    			List<ComboItem> streets = AddressManager.getStreets(selected.getID());
        	    SwingUtilities.invokeLater(() -> {
        	    	for (ComboItem s : streets) cmbStreet.addItem(s);
        	    });
        	});
    	});
    }

    // Structure methods
    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent comp) {
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel(label), gbc);
        
        gbc.gridx = 1; gbc.gridy = row;
        gbc.weightx = 1.0; 
        panel.add(comp, gbc);
        gbc.weightx = 0; // Reset
    }
}