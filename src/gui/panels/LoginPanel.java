package gui.panels;

import database.AuthManager;
import gui.GuiHelper;
import gui.MainFrame;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import app.AppUtils;
import app.Localization;
import app.Localization.Language;

import java.awt.*;

public class LoginPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    public MainFrame mainFrame;

    // UI Elements
    private JComboBox<String> cmbRole;
    private JLabel lblUsername;
    private JTextField txtUsername;
    private JLabel lblPassword;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnRegister;
    private JLabel lblNoAccount;
    private JButton btnForgotPass;

    public LoginPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        
        setLayout(new BorderLayout());
        
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.setOpaque(false);
        topPanel.setBorder(new EmptyBorder(5, 0, 0, 10));

        // Select language text
        String btnText = (Localization.getLanguage() == Language.TR) ? "EN" : "TR";
        JButton btnLang = new JButton(btnText);
        
        // Change language button
        btnLang.setOpaque(true);
        btnLang.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnLang.setFocusPainted(false);
        btnLang.setBackground(new Color(240, 240, 240));
        btnLang.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Change language
        btnLang.addActionListener(_ -> {
            Language newLang = (Localization.getLanguage() == Language.TR) ? Language.EN : Language.TR;
            mainFrame.changeLanguage(newLang);
        });

        topPanel.add(btnLang);
        add(topPanel, BorderLayout.NORTH);

        // Middle panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Role selection box
        String[] roles = {Localization.get("personallogin"), Localization.get("corporatelogin"), Localization.get("guest")};
        cmbRole = new JComboBox<>(roles);
        cmbRole.setFont(new Font("Arial", Font.BOLD, 14));
        
        // Form elements
        lblUsername = new JLabel(Localization.get("tc") + ":"); 
        lblUsername.setFont(new Font("Arial", Font.PLAIN, 14));
        
        txtUsername = new JTextField(20);
        txtUsername.setPreferredSize(new Dimension(250, 30));
        
        lblPassword = new JLabel(Localization.get("password") + ":");
        lblPassword.setFont(new Font("Arial", Font.PLAIN, 14));
        
        txtPassword = new JPasswordField(20);
        txtPassword.setPreferredSize(new Dimension(250, 30));

        btnLogin = new JButton(Localization.get("login"));
        btnLogin.setOpaque(true);
        btnLogin.setBorderPainted(false);
        btnLogin.setPreferredSize(new Dimension(150, 40));
        btnLogin.setBackground(new Color(70, 130, 180));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFont(new Font("Arial", Font.BOLD, 14));

        // Register
        lblNoAccount = new JLabel(Localization.get("noaccount"));
        btnRegister = new JButton(Localization.get("register"));
        styleLinkButton(btnRegister);

        // Forgot pass
        btnForgotPass = new JButton(Localization.get("forgotpass"));
        styleLinkButton(btnForgotPass);

        // Title
        JLabel lblTitle = new JLabel("GreenHood", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        gbc.gridwidth = 2;
        gbc.gridx = 0; gbc.gridy = 0; 
        formPanel.add(lblTitle, gbc);

        // Role selection
        gbc.gridy = 1; 
        formPanel.add(new JLabel(Localization.get("logintype")), gbc);
        gbc.gridy = 2; 
        formPanel.add(cmbRole, gbc);

        // User name
        gbc.gridwidth = 1;
        gbc.gridy = 3; gbc.gridx = 0; formPanel.add(lblUsername, gbc);
        gbc.gridx = 1; formPanel.add(txtUsername, gbc);

        // Password
        gbc.gridy = 4; gbc.gridx = 0; formPanel.add(lblPassword, gbc);
        gbc.gridx = 1; formPanel.add(txtPassword, gbc);

        // Login button
        gbc.gridwidth = 2;
        gbc.gridy = 5; gbc.gridx = 0; 
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(btnLogin, gbc);

        // Footer
        JPanel bottomOptionsPanel = new JPanel();
        bottomOptionsPanel.setLayout(new BoxLayout(bottomOptionsPanel, BoxLayout.Y_AXIS));
        
        // Register
        JPanel registerRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        registerRow.add(lblNoAccount);
        registerRow.add(btnRegister);
        
        // Forgot my pass
        JPanel forgotPassRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        forgotPassRow.add(btnForgotPass);
        
        bottomOptionsPanel.add(registerRow);
        bottomOptionsPanel.add(Box.createVerticalStrut(5));
        bottomOptionsPanel.add(forgotPassRow);
        
        gbc.gridy = 6; 
        formPanel.add(bottomOptionsPanel, gbc);

        add(formPanel, BorderLayout.CENTER);

        // On selected role change
        cmbRole.addActionListener(_ -> {
            int selectedIndex = cmbRole.getSelectedIndex();
            
            if (selectedIndex == 0) { // Neighbor
                updateRoleVisibility(true, Localization.get("tcextended"));
                mainFrame.setCurrentRole("USER");
            } 
            else if (selectedIndex == 1) { // Company
                updateRoleVisibility(true, Localization.get("taxno"));
                setForgotPassVisible(false); // Companies don't have an email
                mainFrame.setCurrentRole("COMPANY");
            } 
            else { // Guest
                updateRoleVisibility(false, "");
                mainFrame.setCurrentRole("GUEST");
            }
        });

        // On choose register option
        btnRegister.addActionListener(_ -> mainFrame.switchPanel("REG_PANEL"));
        
        // On forgot password
        btnForgotPass.addActionListener(_ -> showForgotPasswordDialog());

        // On login
        btnLogin.addActionListener(_ -> performLogin());
    }
    
    private void updateRoleVisibility(boolean isVisible, String usernameLabel) {
        lblUsername.setText(usernameLabel);
        lblUsername.setVisible(isVisible);
        txtUsername.setVisible(isVisible);
        txtUsername.setEnabled(isVisible);
        
        lblPassword.setVisible(isVisible);
        txtPassword.setVisible(isVisible);
        txtPassword.setEnabled(isVisible);
        
        btnRegister.setVisible(isVisible);
        lblNoAccount.setVisible(isVisible);
        
        // Only for neighbors
        setForgotPassVisible(isVisible && "USER".equals(mainFrame.getCurrentRole()));
    }
    
    private void setForgotPassVisible(boolean visible) {
        btnForgotPass.setVisible(visible);
    }

    private void styleLinkButton(JButton btn) {
        btn.setForeground(Color.BLUE);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font("Arial", Font.BOLD, 12));
    }

    private void performLogin() {
        int selectedIndex = cmbRole.getSelectedIndex();
        String tempRole = "";
        
        if (selectedIndex == 0) tempRole = "USER";
        else if (selectedIndex == 1) tempRole = "COMPANY";
        else tempRole = "GUEST";

        mainFrame.setCurrentRole(tempRole);

        String u = txtUsername.getText().trim();
        String p = new String(txtPassword.getPassword());
        
        setLoginLoading(true); 
        final String roleFinal = tempRole;
        
        AppUtils.runAsync(this, 
                () -> {
                    boolean success = AuthManager.authenticate(u, p, roleFinal);

                    SwingUtilities.invokeLater(() -> {
                        if (success) {
                            GuiHelper.showMessage(Localization.get("successfulllogin"));
                            if (selectedIndex == 2) {
                                mainFrame.setCurrentUsername(Localization.get("guest"));
                            } else {
                                mainFrame.setCurrentUsername(u);
                            }
                            mainFrame.switchPanel("DASHBOARD");
                        }
                    });
                }, 
                () -> {
                    SwingUtilities.invokeLater(() -> setLoginLoading(false)); 
                }
            );
    }
    
    private void showForgotPasswordDialog() {
        JTextField txtEmail = new JTextField();
        
        // Message and input box
        Object[] messageContent = {
            Localization.get("enteremail"),
            txtEmail
        };

        // Button texts
        Object[] options = { 
            Localization.get("ok"),
            Localization.get("cancel")
        };

        // Window
        int result = JOptionPane.showOptionDialog(
            this,
            messageContent,
            Localization.get("resetpasstitle"),
            JOptionPane.OK_CANCEL_OPTION,    
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );

        // If answer is OK
        if (result == 0) {
            String email = txtEmail.getText().trim();

            if (!email.isEmpty()) {
            	AppUtils.runAsync(this, () -> {
                	boolean sent = AuthManager.resetPasswordViaEmailForNeighbor(email);
                	
                	// Lock the button to prevent repeated requests
                	btnForgotPass.setEnabled(false);
                    Timer timer = new Timer(60000, _ -> btnForgotPass.setEnabled(true));
                    timer.setRepeats(false);
                    timer.start();
                    
                    SwingUtilities.invokeLater(() -> {
                        if (sent) {
                            GuiHelper.showMessage(Localization.get("passwordsent"));
                        } else {
                            GuiHelper.showWarningMessage(Localization.get("emailnotfound"));
                        }
                    });
                });
            }
        }
    }

    // Loading
    private void setLoginLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        txtUsername.setEnabled(!loading);
        txtPassword.setEnabled(!loading);
        cmbRole.setEnabled(!loading);
        btnRegister.setEnabled(!loading);
        
        if (loading) {
            btnLogin.setText(Localization.get("loggingin") + "...");
            btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        } else {
            btnLogin.setText(Localization.get("login"));
            btnLogin.setCursor(Cursor.getDefaultCursor());
        }
    }
}