package com.example.criptofunhash.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/** Pequeños constructores de componentes repetidos en varias pestañas. */
final class Ui {

    private Ui() {
    }

    static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    static JTable monoTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(Theme.MONO);
        table.setRowHeight(22);
        table.getTableHeader().setReorderingAllowed(false);
        return table;
    }

    /** Área de texto de solo lectura, con letra monoespaciada. */
    static JTextArea monoArea(int rows) {
        JTextArea area = new JTextArea(rows, 60);
        area.setEditable(false);
        area.setFont(Theme.MONO);
        area.setMargin(new java.awt.Insets(6, 8, 6, 8));
        return area;
    }

    /** Área de texto explicativa con ajuste de línea. */
    static JTextArea prose(int rows) {
        JTextArea area = new JTextArea(rows, 60);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(new Color(250, 248, 238));
        area.setMargin(new java.awt.Insets(6, 8, 6, 8));
        return area;
    }

    static JScrollPane scroll(java.awt.Component c, int height) {
        JScrollPane pane = new JScrollPane(c);
        pane.setPreferredSize(new Dimension(300, height));
        pane.getVerticalScrollBar().setUnitIncrement(16);
        return pane;
    }

    static JLabel legend(Object... colorAndLabel) {
        StringBuilder sb = new StringBuilder("<html>");
        for (int i = 0; i < colorAndLabel.length; i += 2) {
            Color c = (Color) colorAndLabel[i];
            sb.append("<span style='background:#")
                    .append(String.format("%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue()))
                    .append("'>&nbsp;").append(colorAndLabel[i + 1]).append("&nbsp;</span>&nbsp;&nbsp;");
        }
        JLabel label = new JLabel(sb.append("</html>").toString());
        label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        return label;
    }

    static List<String> blockNames(int count) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            names.add("Bloque " + (i + 1) + " de " + count);
        }
        return names;
    }
}
