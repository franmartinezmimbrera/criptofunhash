package com.example.criptofunhash.ui;

import com.example.criptofunhash.controller.InputModel;
import com.example.criptofunhash.model.Hex;
import com.example.criptofunhash.model.Sha2Variant;
import com.example.criptofunhash.model.Sha3Variant;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/** Compara la construcción de Merkle–Damgård (SHA-2) con la de esponja (SHA-3). */
final class ComparePanel extends JPanel {

    private final InputModel input;
    private final JComboBox<Sha2Variant> sha2Box = new JComboBox<>(Sha2Variant.values());
    private final JComboBox<Sha3Variant> sha3Box = new JComboBox<>(Sha3Variant.values());
    private final Diagram diagram = new Diagram();
    private final DefaultTableModel tableModel = Ui.readOnlyModel("Propiedad", "SHA-2", "SHA-3");
    private final JTable table = Ui.monoTable(tableModel);

    ComparePanel(InputModel input) {
        super(new BorderLayout());
        this.input = input;

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        top.add(new JLabel("SHA-2:"));
        sha2Box.setSelectedItem(Sha2Variant.SHA_256);
        sha2Box.addActionListener(e -> refresh());
        top.add(sha2Box);
        top.add(new JLabel("SHA-3:"));
        sha3Box.setSelectedItem(Sha3Variant.SHA3_256);
        sha3Box.addActionListener(e -> refresh());
        top.add(sha3Box);
        add(top, BorderLayout.NORTH);

        table.getColumnModel().getColumn(0).setPreferredWidth(300);
        table.getColumnModel().getColumn(1).setPreferredWidth(430);
        table.getColumnModel().getColumn(2).setPreferredWidth(430);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(diagram), new JScrollPane(table));
        split.setResizeWeight(0.55);
        split.setBorder(BorderFactory.createEmptyBorder());
        add(split, BorderLayout.CENTER);

        input.addListener(this::refresh);
        refresh();
    }

    private void refresh() {
        Sha2Variant a = (Sha2Variant) sha2Box.getSelectedItem();
        Sha3Variant b = (Sha3Variant) sha3Box.getSelectedItem();
        int n = input.getBytes().length;
        int mdBlocks = (n + a.lengthFieldBytes()) / a.blockBytes() + 1;
        int spongeBlocks = n / b.rateBytes() + 1;
        diagram.set(a, b, mdBlocks, spongeBlocks);

        tableModel.setRowCount(0);
        row("Construcción", "Merkle–Damgård", "Esponja (sponge)");
        row("Primitiva interna", "Función de compresión con suma (Davies–Meyer)", "Permutación Keccak-f[1600] (sin clave)");
        row("Estado interno", a.stateBits() + " bits (8 registros a…h de " + a.wordBits() + " bits)",
                "1600 bits (matriz 5×5 de palabras de 64 bits)");
        row("Entrada por invocación", "bloque de " + a.blockBits() + " bits", "rate r = " + b.blockBits() + " bits");
        row("Capacidad (parte oculta)", "— (el estado completo se publica en SHA-256/512)",
                "c = " + b.capacityBits() + " bits (2 × resumen)");
        row("Rondas", a.rounds(), b.rounds());
        row("Operaciones básicas", "suma módulo 2^" + a.wordBits() + ", rotaciones/desplazamientos, Ch, Maj",
                "XOR, AND, NOT y rotaciones (θ ρ π χ ι); sin sumas");
        row("Relleno", "1 + ceros + longitud del mensaje en " + a.lengthFieldBytes() * 8 + " bits",
                "sufijo 01 + pad10*1 (sin longitud)");
        row("Orden de bytes", "big-endian", "little-endian");
        boolean truncated = a.digestBits() < a.stateBits();
        row("Ataque de extensión de longitud", truncated ? "no aplicable directamente (resumen truncado)" : "VULNERABLE: el resumen es el estado interno",
                "no vulnerable (la capacidad no se revela)");
        row("Bloques del mensaje actual (" + n + " bytes)", mdBlocks, spongeBlocks);
        if (input.getError() == null) {
            row("Resumen del mensaje actual", Hex.of(a.digest(input.getBytes())), Hex.of(b.digest(input.getBytes())));
        }
    }

    private void row(String property, Object sha2, Object sha3) {
        tableModel.addRow(new Object[]{property, String.valueOf(sha2), String.valueOf(sha3)});
    }

    /** Dibujo de las dos construcciones, una al lado de la otra. */
    private static final class Diagram extends JPanel {

        private static final Color BOX = new Color(225, 236, 252);
        private static final Color PERM = new Color(255, 236, 200);
        private Sha2Variant sha2 = Sha2Variant.SHA_256;
        private Sha3Variant sha3 = Sha3Variant.SHA3_256;
        private int mdBlocks = 1;
        private int spongeBlocks = 1;

        Diagram() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(1000, 305));
        }

        void set(Sha2Variant a, Sha3Variant b, int mdBlocks, int spongeBlocks) {
            this.sha2 = a;
            this.sha3 = b;
            this.mdBlocks = mdBlocks;
            this.spongeBlocks = spongeBlocks;
            repaint();
        }

        /** Índices de bloque a dibujar; -1 representa los puntos suspensivos. */
        private static List<Integer> shown(int blocks) {
            List<Integer> list = new ArrayList<>();
            if (blocks <= 3) {
                for (int i = 0; i < blocks; i++) {
                    list.add(i);
                }
            } else {
                list.add(0);
                list.add(1);
                list.add(-1);
                list.add(blocks - 1);
            }
            return list;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int half = getWidth() / 2;
            paintMerkleDamgard(g, 0, half);
            g.setColor(Color.LIGHT_GRAY);
            g.drawLine(half, 10, half, getHeight() - 10);
            paintSponge(g, half, getWidth() - half);
        }

        private void paintMerkleDamgard(Graphics2D g, int x0, int width) {
            title(g, "Merkle–Damgård · " + sha2.displayName(), x0 + 12, 22);
            List<Integer> idx = shown(mdBlocks);
            int ivW = 54;
            int outW = 96;
            int unitW = (width - 20 - ivW - outW) / idx.size();
            int fy = 150;
            int fh = 70;
            int fw = Math.min(70, unitW - 40);
            int cy = fy + fh / 2;
            int prevX = x0 + 10 + ivW;
            box(g, x0 + 10, cy - 18, ivW, 36, "IV", BOX);
            for (int i = 0; i < idx.size(); i++) {
                int cx = x0 + 10 + ivW + i * unitW + unitW / 2;
                int fx = cx - fw / 2;
                arrow(g, prevX, cy, fx, cy);
                if (i > 0) {
                    small(g, "H" + (idx.get(i) < 0 ? "…" : String.valueOf(idx.get(i))), (prevX + fx) / 2 - 10, cy - 6);
                }
                if (idx.get(i) < 0) {
                    big(g, "· · ·", cx - 18, cy + 6);
                } else {
                    box(g, fx, fy, fw, fh, "f", PERM);
                    box(g, cx - 28, 70, 56, 30, "M" + (idx.get(i) + 1), BOX);
                    small(g, sha2.blockBits() + " bits", cx - 28, 62);
                    arrow(g, cx, 100, cx, fy);
                }
                prevX = fx + fw;
            }
            int outX = x0 + width - outW - 10;
            arrow(g, prevX, cy, outX, cy);
            small(g, sha2.stateBits() + " bits", prevX + 2, cy - 8);
            box(g, outX, cy - 22, outW, 44, "Hash", new Color(205, 240, 205));
            small(g, sha2.digestBits() + " bits", outX + 18, cy + 38);
            small(g, "El resumen es el estado completo tras el último bloque", x0 + 12, 268);
            small(g, sha2.digestBits() < sha2.stateBits() ? "(truncado a " + sha2.digestBits() + " bits)" : "(sin truncar)",
                    x0 + 12, 285);
        }

        private void paintSponge(Graphics2D g, int x0, int width) {
            title(g, "Esponja · " + sha3.displayName(), x0 + 12, 22);
            List<Integer> idx = shown(spongeBlocks);
            int labelW = 56;
            int outW = 96;
            int unitW = (width - 20 - labelW - outW) / idx.size();
            int fw = Math.min(64, unitW - 56);
            int rY = 140;
            int cY = 200;
            int laneH = 40;
            small(g, "r = " + sha3.blockBits() + " bits", x0 + 10, rY - 6);
            small(g, "c = " + sha3.capacityBits() + " bits", x0 + 10, cY + laneH + 14);
            g.setColor(Theme.RATE);
            g.fillRect(x0 + 10, rY, 40, laneH);
            g.setColor(Theme.CAPACITY);
            g.fillRect(x0 + 10, cY, 40, laneH);
            g.setColor(Color.GRAY);
            g.drawRect(x0 + 10, rY, 40, laneH);
            g.drawRect(x0 + 10, cY, 40, laneH);
            small(g, "0…0", x0 + 18, rY + 25);
            int prevX = x0 + 50;
            for (int i = 0; i < idx.size(); i++) {
                int cx = x0 + 10 + labelW + i * unitW + unitW / 2;
                int fx = cx + 6;
                int xorX = cx - 22;
                if (idx.get(i) < 0) {
                    arrow(g, prevX, rY + laneH / 2, cx + 10, rY + laneH / 2);
                    arrow(g, prevX, cY + laneH / 2, cx + 10, cY + laneH / 2);
                    big(g, "· · ·", cx - 18, rY + laneH + 18);
                    prevX = cx + 10;
                    continue;
                }
                arrow(g, prevX, rY + laneH / 2, xorX - 9, rY + laneH / 2);
                g.setColor(Color.BLACK);
                g.drawOval(xorX - 9, rY + laneH / 2 - 9, 18, 18);
                g.drawLine(xorX - 9, rY + laneH / 2, xorX + 9, rY + laneH / 2);
                g.drawLine(xorX, rY + laneH / 2 - 9, xorX, rY + laneH / 2 + 9);
                box(g, xorX - 28, 66, 56, 30, "M" + (idx.get(i) + 1), BOX);
                small(g, sha3.blockBits() + " bits", xorX - 28, 58);
                arrow(g, xorX, 96, xorX, rY + laneH / 2 - 9);
                arrow(g, xorX + 9, rY + laneH / 2, fx, rY + laneH / 2);
                arrow(g, prevX, cY + laneH / 2, fx, cY + laneH / 2);
                box(g, fx, rY - 6, fw, cY + laneH - rY + 12, "f", PERM);
                prevX = fx + fw;
            }
            int outX = x0 + width - outW - 10;
            arrow(g, prevX, rY + laneH / 2, outX, rY + laneH / 2);
            box(g, outX, rY + 2, outW, laneH - 4, "Hash", new Color(205, 240, 205));
            small(g, sha3.digestBits() + " bits", outX + 18, rY - 6);
            small(g, "(parte rate)", outX + 14, rY + laneH + 14);
            g.setColor(Color.GRAY);
            g.drawLine(prevX, cY + laneH / 2, prevX + 30, cY + laneH / 2);
            small(g, "capacidad: nunca se publica", prevX + 34, cY + laneH / 2 + 4);
            small(g, "Estado interno de 1600 bits: solo el rate se mezcla con el mensaje y se publica", x0 + 12, 285);
        }

        private void title(Graphics2D g, String text, int x, int y) {
            g.setColor(Color.BLACK);
            g.setFont(Theme.TITLE_FONT.deriveFont(15f));
            g.drawString(text, x, y);
        }

        private void small(Graphics2D g, String text, int x, int y) {
            g.setColor(Color.DARK_GRAY);
            g.setFont(getFont().deriveFont(Font.PLAIN, 11f));
            g.drawString(text, x, y);
        }

        private void big(Graphics2D g, String text, int x, int y) {
            g.setColor(Color.BLACK);
            g.setFont(getFont().deriveFont(Font.BOLD, 16f));
            g.drawString(text, x, y);
        }

        private void box(Graphics2D g, int x, int y, int w, int h, String text, Color fill) {
            g.setColor(fill);
            g.fillRect(x, y, w, h);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(1.4f));
            g.drawRect(x, y, w, h);
            g.setFont(getFont().deriveFont(Font.BOLD, 14f));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, x + (w - fm.stringWidth(text)) / 2, y + (h + fm.getAscent()) / 2 - 3);
        }

        private void arrow(Graphics2D g, int x1, int y1, int x2, int y2) {
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(1.4f));
            g.drawLine(x1, y1, x2, y2);
            double angle = Math.atan2(y2 - y1, x2 - x1);
            int len = 7;
            int ax = (int) (x2 - len * Math.cos(angle - 0.4));
            int ay = (int) (y2 - len * Math.sin(angle - 0.4));
            int bx = (int) (x2 - len * Math.cos(angle + 0.4));
            int by = (int) (y2 - len * Math.sin(angle + 0.4));
            g.drawLine(x2, y2, ax, ay);
            g.drawLine(x2, y2, bx, by);
        }
    }
}
