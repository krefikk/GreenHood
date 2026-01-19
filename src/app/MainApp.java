package app;

import javax.swing.SwingUtilities;
import gui.MainFrame;

public class MainApp {
	public static void main(String[] args) {
	    SwingUtilities.invokeLater(() -> {
	        try {
	            MainFrame frame = new MainFrame();
	            frame.setVisible(true);
	        } catch (Throwable e) {
	            System.out.println("MainApp Error!");
	            e.printStackTrace();
	        }
	    });
	}
}