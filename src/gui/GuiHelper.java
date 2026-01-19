package gui;

import javax.swing.JOptionPane;
import app.Localization;
import java.awt.Component;

public class GuiHelper {
	
	// To show a message box at the center of the MONITOR
    public static void showMessage(String message) {
        JOptionPane.showMessageDialog(null, message, Localization.get("notification"), JOptionPane.INFORMATION_MESSAGE);
    }
    
    // To show a message box at the center of the CURRENT PANEL
    public static void showMessage(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, Localization.get("notification"), JOptionPane.INFORMATION_MESSAGE);
    }
    
    public static void showWarningMessage(String message) {
        JOptionPane.showMessageDialog(null, message, Localization.get("warning"), JOptionPane.WARNING_MESSAGE);
    }
    
    public static void showWarningMessage(Component parent, String message) {
    	Component actualParent = (parent != null && parent.isShowing()) ? parent : null;
        JOptionPane.showMessageDialog(actualParent, message, Localization.get("warning"), JOptionPane.WARNING_MESSAGE);
    }
    
    public static int showConfirmMessage(Component parent, String title, String message, String positive, String negative) {
    	Object[] options = { positive, negative };
    	
    	int result = JOptionPane.showOptionDialog(
                parent, 
                message, 
                title,
                JOptionPane.YES_NO_OPTION, 
                JOptionPane.QUESTION_MESSAGE, 
                null,     // No special icon
                options,  // Special button texts
                options[0] // Default chosen button text
        );
    	
    	return result;
    }
    
    public static boolean confirm(Component parent, String title, String message, String positive, String negative) {
        int result = showConfirmMessage(parent, title, message, positive, negative);
        return result == JOptionPane.YES_OPTION;
    }
    
    // To store drop-down menu items
    public static class ComboItem {
        private int id;
        private String label;

        public ComboItem(int id, String label) {
            this.id = id;
            this.label = label;
        }

        public int getID() { return id; }
        public String getLabel() { return label; }
        
        @Override
        public String toString() { return label; }
    }
}