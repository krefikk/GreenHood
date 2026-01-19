package app;

import java.awt.Component;
import java.awt.Window;
import database.DBConnection;
import javax.swing.SwingUtilities;
import gui.GuiHelper;
import gui.MainFrame;
import java.sql.*;
import java.net.*;

public class AppUtils {
	
	public static class DialogException extends RuntimeException {
	    
		private static final long serialVersionUID = 1L;

		public DialogException(String message) {
	        super(message);
	    }
	}
	
	@FunctionalInterface
	public interface DbTask {
	    void execute() throws Exception;
	}
	
	public static void runAsync(Component parent, DbTask task) {
        runAsync(parent, task, null);
    }
	
	public static void runAsync(Component parent, DbTask task, DbTask finalTask) {
        new Thread(() -> {
            try {
                // First task
                task.execute();
                
            } catch (DialogException de) {
            	SwingUtilities.invokeLater(() -> {
                    Component safeParent = getSafeWindow(parent);
                    GuiHelper.showWarningMessage(safeParent, de.getMessage());
                });
            	
            } catch (SQLTransientConnectionException | SQLRecoverableException ex) {
            	System.err.println("CONNECTION ERROR (TYPE 1)");
                handleNetworkError(parent);

            } catch (SQLException ex) {
            	String state = ex.getSQLState();
                // If error starts with 08, it is a connection error
                if (state != null && state.startsWith("08")) {
                    System.err.println("CONNECTION ERROR (TYPE 2 - STATE: " + state + ")");
                    handleNetworkError(parent);
                }
                else {
                    System.err.println("SQL QUERY ERROR");
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        Component safeParent = getSafeWindow(parent);
                        GuiHelper.showWarningMessage(safeParent, Localization.get("errordb"));
                    });
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                
                // Find the root cause of the error
                Throwable rootCause = ex;
                while (rootCause.getCause() != null) {
                    rootCause = rootCause.getCause();
                }
                
                final String msg;
                if (rootCause instanceof UnknownHostException || rootCause instanceof ConnectException) {
                	System.err.println("NETWORK ERROR");
                    msg = Localization.get("networkwarning");
                    SwingUtilities.invokeLater(() -> {
                        Component safeParent = getSafeWindow(parent);
                        GuiHelper.showWarningMessage(safeParent, msg);
                        triggerForceExit(parent);
                    });
                } else {
                	System.err.println("UNEXPECTED ERROR");
                    msg = Localization.get("errorunexpected");
                    SwingUtilities.invokeLater(() -> {
                        Component safeParent = getSafeWindow(parent);
                        GuiHelper.showWarningMessage(safeParent, msg);
                    });
                }

            } finally {
                if (finalTask != null) {
                	try {
                        finalTask.execute();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
	
	private static void handleNetworkError(Component parent) {
	    SwingUtilities.invokeLater(() -> {
	        Component safeParent = getSafeWindow(parent);
	        GuiHelper.showWarningMessage(safeParent, Localization.get("networkwarning"));
	        triggerForceExit(parent);
	    });
	}
	
	private static void triggerForceExit(Component parent) {
	    MainFrame frame = null;

	    // If parent is already main frame
	    if (parent instanceof MainFrame) {
	        frame = (MainFrame) parent;
	    } 
	    // Find root parent
	    else if (parent != null) {
	        Window window = SwingUtilities.getWindowAncestor(parent);
	        if (window instanceof MainFrame) {
	            frame = (MainFrame) window;
	        }
	    }

	    // If found main frame
	    if (frame != null) {
	        frame.forceExitApplication();
	    } else {
	        DBConnection.shutdown();
	        System.exit(0);
	    }
	}
	
	private static Component getSafeWindow(Component parent) {
        if (parent == null) return null;
        
        if (parent instanceof Window) {
            return parent;
        }
        
        Window window = SwingUtilities.getWindowAncestor(parent);
        if (window != null && window.isShowing()) {
            return window;
        }
        
        return null;
    }
}
