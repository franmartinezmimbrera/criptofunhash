package com.example.criptofunhash.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

final class AppIcon {

    private AppIcon() {
    }

    static BufferedImage create(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int margin = size / 12;
        int diameter = size - margin * 2;
        g2.setColor(Theme.PANEL_BACKGROUND);
        g2.fillOval(margin, margin, diameter, diameter);
        g2.setColor(Theme.ACCENT);
        g2.setStroke(new BasicStroke(Math.max(1f, size / 24f)));
        g2.drawOval(margin, margin, diameter, diameter);

        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, size / 2));
        FontMetrics fm = g2.getFontMetrics();
        String s = "#";
        g2.setColor(new Color(255, 205, 60));
        g2.drawString(s, (size - fm.stringWidth(s)) / 2, size / 2 + fm.getAscent() / 2 - fm.getDescent() / 2);
        g2.dispose();
        return image;
    }
}
