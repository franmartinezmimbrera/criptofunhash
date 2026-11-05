package com.example.criptofunhash.ui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Estado de Keccak: matriz 5×5 de palabras de 64 bits. La palabra (x, y) está en el índice
 * x + 5y. Las palabras que cambian respecto al estado anterior se resaltan.
 */
final class LaneGridPanel extends JPanel {

    private static final int LABEL = 26;
    private static final int CELL_W = 142;
    private static final int CELL_H = 42;

    private long[] state = new long[25];
    private long[] previous;
    private int rateLanes = 25;

    LaneGridPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(LABEL + 5 * CELL_W + 8, LABEL + 5 * CELL_H + 8));
    }

    /**
     * @param state     estado a dibujar
     * @param previous  estado anterior para resaltar cambios (o null)
     * @param rateLanes número de palabras que pertenecen a la parte "rate" (25 = sin distinguir)
     */
    void setState(long[] state, long[] previous, int rateLanes) {
        this.state = state == null ? new long[25] : state;
        this.previous = previous;
        this.rateLanes = rateLanes;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        FontMetrics small = g.getFontMetrics(getFont().deriveFont(10f));
        for (int i = 0; i < 5; i++) {
            g.setFont(getFont().deriveFont(java.awt.Font.BOLD, 11f));
            g.setColor(Color.DARK_GRAY);
            g.drawString("x=" + i, LABEL + i * CELL_W + CELL_W / 2 - 10, 14);
            g.drawString("y=" + i, 2, LABEL + i * CELL_H + CELL_H / 2 + 4);
        }
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                int idx = x + 5 * y;
                int px = LABEL + x * CELL_W;
                int py = LABEL + y * CELL_H;
                boolean changed = previous != null && previous[idx] != state[idx];
                Color bg = Color.WHITE;
                if (rateLanes < 25) {
                    bg = idx < rateLanes ? Theme.RATE : Theme.CAPACITY;
                }
                if (changed) {
                    bg = Theme.CHANGED;
                }
                g.setColor(bg);
                g.fillRect(px + 1, py + 1, CELL_W - 2, CELL_H - 2);
                g.setColor(Color.GRAY);
                g.drawRect(px, py, CELL_W - 1, CELL_H - 1);
                g.setColor(Color.DARK_GRAY);
                g.setFont(getFont().deriveFont(10f));
                g.drawString("A[" + x + "," + y + "]  #" + idx, px + 5, py + 4 + small.getAscent() - 2);
                g.setColor(Color.BLACK);
                g.setFont(Theme.MONO_BOLD.deriveFont(12f));
                g.drawString(String.format("%016x", state[idx]), px + 5, py + CELL_H - 9);
            }
        }
    }
}
