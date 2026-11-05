package com.example.criptofunhash.ui;

import com.example.criptofunhash.model.Birthday;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Tabla de cumpleaños: columnas = día (1–31), filas = mes (1–12). */
final class CalendarPanel extends JPanel {

    private static final int[] MONTH_DAYS = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    private Birthday.Round round;
    private int shown;

    CalendarPanel() {
        setBackground(Color.WHITE);
    }

    void setRound(Birthday.Round round) {
        setRound(round, round == null ? 0 : round.days.length);
    }

    /** Muestra solo las primeras {@code shown} personas de la ronda (paso a paso). */
    void setRound(Birthday.Round round, int shown) {
        this.round = round;
        this.shown = shown;
        repaint();
    }

    /** Día del año (0..364) -> {mes 0..11, día 1..31}. */
    static int[] monthAndDay(int dayOfYear) {
        int d = dayOfYear;
        for (int m = 0; m < 12; m++) {
            if (d < MONTH_DAYS[m]) {
                return new int[]{m, d + 1};
            }
            d -= MONTH_DAYS[m];
        }
        throw new IllegalArgumentException("día fuera de rango: " + dayOfYear);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int left = 40;
        int topMargin = 26;
        int bottom = 36;
        int cw = Math.max(8, (getWidth() - left - 12) / 31);
        int ch = Math.max(8, (getHeight() - topMargin - bottom) / 12);

        g.setColor(Color.BLACK);
        g.setFont(Theme.TITLE_FONT);
        g.drawString("TABLA DE CUMPLEAÑOS", left, 16);
        g.setFont(getFont().deriveFont(10f));
        FontMetrics fm = g.getFontMetrics();

        for (int m = 0; m < 12; m++) {
            for (int d = 1; d <= 31; d++) {
                int x = left + (d - 1) * cw;
                int y = topMargin + (11 - m) * ch;
                g.setColor(d > MONTH_DAYS[m] ? new Color(215, 215, 215) : new Color(246, 244, 200));
                g.fillRect(x, y, cw, ch);
                g.setColor(new Color(200, 200, 215));
                g.drawRect(x, y, cw, ch);
            }
            g.setColor(Color.DARK_GRAY);
            g.drawString(String.valueOf(m + 1), left - 6 - fm.stringWidth(String.valueOf(m + 1)),
                    topMargin + (11 - m) * ch + ch / 2 + 4);
        }
        for (int d = 1; d <= 31; d++) {
            g.setColor(Color.DARK_GRAY);
            String s = String.valueOf(d);
            g.drawString(s, left + (d - 1) * cw + (cw - fm.stringWidth(s)) / 2, topMargin + 12 * ch + 14);
        }
        g.setColor(Color.BLACK);
        g.drawString("MES", 4, topMargin - 6);
        g.drawString("DÍA", left + 31 * cw / 2 - 8, topMargin + 12 * ch + 30);

        if (round == null) {
            return;
        }
        int people = shown;
        boolean complete = shown >= round.days.length;
        for (int i = 0; i < people; i++) {
            int[] md = monthAndDay(round.days[i]);
            int x = left + (md[1] - 1) * cw;
            int y = topMargin + (11 - md[0]) * ch;
            boolean collision = complete && i == round.second;
            boolean firstOfPair = complete && i == round.first;
            g.setColor(collision || firstOfPair ? new Color(220, 60, 60) : new Color(70, 120, 200));
            g.fillRect(x + 1, y + 1, cw - 1, ch - 1);
            g.setColor(Color.WHITE);
            String label = String.valueOf(i + 1);
            if (collision) {
                label = (round.first + 1) + "=" + (i + 1);
            }
            if (fm.stringWidth(label) <= cw && !collision) {
                g.drawString(label, x + (cw - fm.stringWidth(label)) / 2, y + ch / 2 + 4);
            }
        }
        if (!complete) {
            return;
        }
        g.setColor(Color.BLACK);
        g.setFont(getFont().deriveFont(11f));
        int[] mdA = monthAndDay(round.days[round.second]);
        g.drawString("Coincidencia tras " + round.attempts + " personas (personas " + (round.first + 1) + " y "
                + (round.second + 1) + "): día " + mdA[1] + " del mes " + (mdA[0] + 1) + "  (en rojo)", left + 230, 16);
    }
}
