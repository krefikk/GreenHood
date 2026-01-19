package gui;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import app.Localization;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;

public class GuiListHelper {

    // Colors
    public static final Color CARD_BG = Color.WHITE;
    public static final Color COLOR_RECYCLED = new Color(46, 204, 113); 
    public static final Color COLOR_RESERVED = new Color(230, 126, 34); 
    public static final Color COLOR_AVAILABLE = new Color(52, 152, 219); 
    public static final Color COLOR_DEFAULT = Color.LIGHT_GRAY;

    // List container methods
    public static JPanel createListPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false); 
        return p;
    }

    public static JScrollPane createScrollPane(JPanel panel) {
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }
    
    public static JPanel createBaseCard(Color borderColor, int fixedHeight) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 5, 0, 0, borderColor),
            new EmptyBorder(10, 12, 10, 12)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, fixedHeight));
        return card;
    }

    public static JPanel createFeedCard(String htmlContent, Color borderColor, int fixedHeight) {
        JPanel card = createBaseCard(borderColor, fixedHeight);
        
        JLabel lblContent = new JLabel(htmlContent);
        lblContent.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        card.add(lblContent, BorderLayout.CENTER);
        return card;
    }

    public static JPanel createLeaderboardCard(int rank, String name, double score, boolean isMe, int fixedHeight) {
        JPanel card = new JPanel(new BorderLayout());

        Dimension dim = new Dimension(Integer.MAX_VALUE, fixedHeight);
        card.setMaximumSize(dim);
        card.setPreferredSize(new Dimension(300, fixedHeight));
        card.setMinimumSize(new Dimension(100, fixedHeight));     
        card.setBorder(new EmptyBorder(0, 15, 0, 15));
        
        if (isMe) {
            card.setBackground(new Color(220, 240, 255)); 
            name = name + " (Siz)";
            card.setBorder(BorderFactory.createCompoundBorder(
                 BorderFactory.createMatteBorder(0, 4, 0, 0, COLOR_AVAILABLE),
                 new EmptyBorder(0, 11, 0, 15)
            ));
        } else {
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                 BorderFactory.createMatteBorder(0, 4, 0, 0, new Color(240, 240, 240)),
                 new EmptyBorder(0, 11, 0, 15)
            ));
        }

        JLabel lblName = new JLabel(rank + ". " + name);
        lblName.setFont(isMe ? new Font("Segoe UI", Font.BOLD, 14) : new Font("Segoe UI", Font.PLAIN, 14));
        
        JLabel lblScore = new JLabel(String.format("%.0f P", score));
        lblScore.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblScore.setForeground(new Color(52, 73, 94));

        card.add(lblName, BorderLayout.WEST);
        card.add(lblScore, BorderLayout.EAST);
        
        return card;
    }
    
    public static JPanel createStyledLeaderboardCard(int rank, String name, double score, boolean isMe, int height) {
        // BorderLayout
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        DecimalFormat df = new DecimalFormat("#0.00");
        
        // Colors
        Color rankColor;
        Color bgColor = Color.WHITE;
        
        if (rank == 1) rankColor = new Color(255, 215, 0);       // Gold
        else if (rank == 2) rankColor = new Color(192, 192, 192); // Silver
        else if (rank == 3) rankColor = new Color(205, 127, 50);  // Bronze
        else rankColor = new Color(180, 180, 180);                // Gray

        if (isMe) bgColor = new Color(235, 245, 251); 
        
        panel.setBackground(bgColor);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        panel.setPreferredSize(new Dimension(0, height));
        
        // Borders
        panel.setBorder(new CompoundBorder(
            BorderFactory.createMatteBorder(0, 5, 0, 0, rankColor),
            new EmptyBorder(5, 10, 5, 10)
        ));

        // Rankings
        JLabel lblRank = new JLabel(rank + ".");
        lblRank.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblRank.setForeground(rankColor);
        lblRank.setPreferredSize(new Dimension(25, 0));
        lblRank.setVerticalAlignment(SwingConstants.CENTER);
        
        // Names
        JLabel lblName = new JLabel("<html>" + name + "</html>");
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblName.setForeground(new Color(44, 62, 80));
        lblName.setVerticalAlignment(SwingConstants.CENTER);

        // Points
        JLabel lblScore = new JLabel("<html><div style='text-align:right;'>" + 
                                     "<b>" + df.format(score) + "</b><br>" + 
                                     "<span style='font-size:11px; color:gray;'>" + Localization.get("score") + "</span>" +
                                     "</div></html>");
        lblScore.setHorizontalAlignment(SwingConstants.RIGHT);
        
        // Add elements into panel
        panel.add(lblRank, BorderLayout.WEST);
        panel.add(lblName, BorderLayout.CENTER);
        panel.add(lblScore, BorderLayout.EAST);
        
        // Hover
        Color finalBgColor = bgColor;
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                panel.setBackground(new Color(250, 250, 250));
            }
            @Override
            public void mouseExited(MouseEvent evt) {
                panel.setBackground(finalBgColor);
            }
        });

        return panel;
    }

    // Helper methods
    public static void addEmptyMessage(JPanel panel, String message) {
        JLabel empty = new JLabel(message);
        empty.setForeground(Color.GRAY);
        empty.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        empty.setAlignmentX(Component.CENTER_ALIGNMENT);
        empty.setBorder(new EmptyBorder(15, 0, 0, 0));
        panel.add(empty);
    }

    public static void refreshPanel(JPanel p) {
        p.add(Box.createVerticalGlue());
        p.revalidate();
        p.repaint();
    }
}