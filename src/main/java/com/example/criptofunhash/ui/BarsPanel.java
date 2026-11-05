package com.example.criptofunhash.ui;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Número de intentos por iteración hasta conseguir una coincidencia, con su media. */
final class BarsPanel extends JPanel {

    private List<Integer> data = new ArrayList<>();
    private double expected;
    private int total = 10;

    BarsPanel() {
        setBackground(Color.WHITE);
    }

    /** @param total iteraciones previstas (anchura del eje X) */
    void setData(List<Integer> data, double expected, int total) {
        this.data = new ArrayList<>(data);
        this.expected = expected;
        this.total = Math.max(total, 1);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(Theme.TITLE_FONT);
        g.setColor(Color.BLACK);
        g.drawString("NÚMERO DE INTENTOS POR ITERACIÓN HASTA CONSEGUIR UNA COINCIDENCIA Y SU MEDIA", 50, 16);
        g.setFont(getFont().deriveFont(10f));
        FontMetrics fm = g.getFontMetrics();

        int left = 60;
        int top = 28;
        int bottom = 34;
        int w = getWidth() - left - 16;
        int h = getHeight() - top - bottom;
        if (w <= 10 || h <= 10) {
            return;
        }
        double mean = 0;
        int max = 1;
        for (int a : data) {
            mean += a;
            max = Math.max(max, a);
        }
        if (!data.isEmpty()) {
            mean /= data.size();
        }
        double scaleMax = Math.max(max, Math.max(expected, mean)) * 1.1;
        if (scaleMax <= 0) {
            scaleMax = 1;
        }

        g.setColor(new Color(246, 244, 200));
        g.fillRect(left, top, w, h);
        g.setColor(Color.GRAY);
        g.drawRect(left, top, w, h);
        for (int i = 0; i <= 5; i++) {
            double v = scaleMax * i / 5;
            int y = top + h - (int) (h * i / 5.0);
            g.setColor(new Color(215, 215, 200));
            g.drawLine(left, y, left + w, y);
            g.setColor(Color.DARK_GRAY);
            String s = format(v);
            g.drawString(s, left - 6 - fm.stringWidth(s), y + 4);
        }
        g.setColor(Color.BLACK);
        g.drawString("Nº intentos", 2, top - 4);
        g.drawString("Iteración", left + w / 2 - 20, top + h + 30);

        int n = Math.max(data.size(), total);
        double slot = w / (double) Math.max(n, 1);
        int step = Math.max(1, (int) Math.ceil(n / 20.0));
        for (int i = 0; i < data.size(); i++) {
            int bh = (int) (h * data.get(i) / scaleMax);
            int x = left + (int) (i * slot);
            int bw = Math.max(1, (int) slot - (slot > 6 ? 3 : 0));
            g.setColor(new Color(70, 120, 200));
            g.fillRect(x + 1, top + h - bh, bw, bh);
            if ((i + 1) % step == 0 || i == 0) {
                g.setColor(Color.DARK_GRAY);
                g.drawString(String.valueOf(i + 1), x + 1, top + h + 14);
            }
        }
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 4f}, 0f));
        if (expected > 0) {
            int y = top + h - (int) (h * expected / scaleMax);
            g.setColor(new Color(40, 150, 60));
            g.drawLine(left, y, left + w, y);
            g.drawString(String.format(Locale.getDefault(), "teórico %.1f", expected), left + w - 90, y + 14);
        }
        if (!data.isEmpty()) {
            int y = top + h - (int) (h * mean / scaleMax);
            g.setColor(new Color(210, 40, 40));
            g.drawLine(left, y, left + w, y);
            g.drawString(String.format(Locale.getDefault(), "media %.1f", mean), left + 4, y - 4);
        }
        g.setStroke(new BasicStroke(1f));
    }

    private static String format(double v) {
        return v >= 100 ? String.valueOf(Math.round(v)) : String.format(Locale.getDefault(), "%.1f", v);
    }
}
