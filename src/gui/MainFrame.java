package gui;

import database.*;
import gui.panels.*;
import javax.swing.*;
import app.Localization;
import app.Localization.Language;
import java.awt.*;

public class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);
    
    private String currentRole = "USER";
    private String currentUsername = "";
    private String currentScreenName = "LOGIN_SCREEN";

    public MainFrame() {
        Localization.setLanguage(Language.TR);
        setTitle("GreenHood");
        setSize(1280, 720);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); 
        
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                DBConnection.shutdown();
                System.exit(0);
            }
        });

        JPanel loginP = new LoginPanel(this); 
        mainPanel.add(loginP, "LOGIN_SCREEN");

        JPanel regP = new RegisterPanel(this);
        mainPanel.add(regP, "REG_PANEL");

        add(mainPanel);
        cardLayout.show(mainPanel, "LOGIN_SCREEN");
        
        checkNetwork();
    }

    public void switchPanel(String panelName) {
        this.currentScreenName = panelName;
        
        switch (panelName) {
            case "LOGIN_SCREEN":
                mainPanel.add(new LoginPanel(this), "LOGIN_SCREEN");
                break;
                
            case "REG_PANEL":
                mainPanel.add(new RegisterPanel(this), "REG_PANEL");
                break;
                
            case "DASHBOARD":
                mainPanel.add(new DashboardPanel(this), "DASHBOARD");
                break;
                
            case "PROFILE_PANEL":
                mainPanel.add(new ProfilePanel(this), "PROFILE_PANEL");
                break;
                
            case "EDIT_PROFILE":
                mainPanel.add(new EditProfilePanel(this), "EDIT_PROFILE");
                break;
        }
        
        cardLayout.show(mainPanel, panelName);
        mainPanel.revalidate();
        mainPanel.repaint();
    }
    
    private void refreshCurrentScreen() {
        switch (currentScreenName) {
            case "USER_DASHBOARD":
                showUserDashboard();
                break;
            case "COMPANY_DASHBOARD":
                showCompanyDashboard();
                break;
            default:
                switchPanel(currentScreenName);
                break;
        }
    }
    
    public void logout() {
        this.currentUsername = null;
        this.currentRole = null;
        
        switchPanel("LOGIN_SCREEN");
        
        GuiHelper.showMessage(this, Localization.get("successfulllogout"));
    }
    
    public void showUserDashboard() {
        this.currentScreenName = "USER_DASHBOARD";
        String tckn = this.getCurrentUsername(); 
        
        if (tckn == null || tckn.isEmpty()) {
            GuiHelper.showMessage(Localization.get("errornotfounduser"));
            logout();
            return;
        }

        mainPanel.add(new UserDashboardPanel(this, tckn), "USER_DASHBOARD");
        cardLayout.show(mainPanel, "USER_DASHBOARD");
    }
    
    public void showCompanyDashboard() {
        this.currentScreenName = "COMPANY_DASHBOARD";
        
        if (currentUsername == null || currentUsername.isEmpty()) {
            GuiHelper.showMessage(Localization.get("errorsession"));
            switchPanel("LOGIN_SCREEN");
            return;
        }
        
        mainPanel.add(new CompanyDashboardPanel(this, currentUsername), "COMPANY_DASHBOARD");
        cardLayout.show(mainPanel, "COMPANY_DASHBOARD");
    }
    
    private void checkNetwork() {
        new Thread(() -> {
            System.out.println("Checking network...");
            
            String host = Variables.HOST;
            int port = Integer.parseInt(Variables.PORT);
            int timeout = 3000; // 3 seconds wait

            try {
                if (host.isEmpty()) throw new Exception("Host couldn't be found!");

                try (java.net.Socket socket = new java.net.Socket()) {
                    socket.connect(new java.net.InetSocketAddress(host, port), timeout);
                    System.out.println("Connection successfull! (" + port + " is allowed.)");
                } catch (java.io.IOException e) {
                    System.err.println("Connection failed: " + e.getMessage());
                    
                    SwingUtilities.invokeLater(() -> {
                        String title = Localization.get("networkwarningtitle");
                        String message = Localization.get("networkwarning");
                        JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE);
                        forceExitApplication();
                    });
                }

            } catch (Exception e) {
                System.out.println("An error occured while checking network: " + e.getMessage());
            }
        }).start();
    }
    
    public void exitApplication() {      
        DBConnection.shutdown();
        this.dispose();
        System.exit(0);
    }
    
    public void forceExitApplication() {      
        this.dispose();
        System.exit(0);
    }
    
    // Getter & Setters
    public void setCurrentRole(String role) { this.currentRole = role; }
    public String getCurrentRole() { return this.currentRole; }
    
    public void setCurrentUsername(String username) { this.currentUsername = username; }
    public String getCurrentUsername() { return this.currentUsername; }
    
    // Language
    public void changeLanguage(Language lang) {
        Localization.setLanguage(lang);
        refreshCurrentScreen();
    }
    public Language getLanguage() { return Localization.getLanguage(); }
}