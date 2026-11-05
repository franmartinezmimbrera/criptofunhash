package com.example.criptofunhash.ui;

import com.example.criptofunhash.controller.InputModel;
import com.example.criptofunhash.model.Algorithms;
import com.example.criptofunhash.model.Avalanche;
import com.example.criptofunhash.model.HashAlgorithm;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

/** Efecto avalancha: cuántos bits del resumen cambian al modificar el mensaje. */
final class AvalanchePanel extends JPanel {

    /** Por encima de este tamaño no se calcula la media sobre todos los bits. */
    private static final int MAX_STATS_BYTES = 256;
    private static final int MAX_BYTES = 1 << 20;

    private final InputModel input;
    private final JRadioButton flipMode = new JRadioButton("Invertir un único bit del mensaje original:", true);
    private final JRadioButton editMode = new JRadioButton("Mensaje modificado (editable):");
    private final JSpinner bitSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 0, 1));
    private final JLabel bitLabel = new JLabel(" ");
    private final JTextArea modifiedArea = new JTextArea(2, 50);
    private final JButton resetButton = new JButton("Restablecer");
    private final JLabel notice = new JLabel(" ");
    private final DefaultTableModel tableModel = Ui.readOnlyModel(
            "Algoritmo", "Bits del resumen", "Bits distintos", "% cambiado", "Media (invirtiendo cada bit)", "Mín – máx");
    private final JTable table = Ui.monoTable(tableModel);
    private final JTextPane diffPane = new JTextPane();
    private boolean updating;

    AvalanchePanel(InputModel input) {
        super(new BorderLayout(0, 6));
        this.input = input;
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        ButtonGroup group = new ButtonGroup();
        group.add(flipMode);
        group.add(editMode);
        flipMode.addActionListener(e -> recompute());
        editMode.addActionListener(e -> recompute());
        bitSpinner.addChangeListener(e -> recompute());
        modifiedArea.setFont(Theme.MONO);
        modifiedArea.setLineWrap(true);
        modifiedArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                edited();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                edited();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                edited();
            }
        });
        resetButton.addActionListener(e -> resetModified());

        JPanel flipRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        flipRow.add(flipMode);
        flipRow.add(new JLabel("bit nº"));
        flipRow.add(bitSpinner);
        flipRow.add(bitLabel);
        JPanel editRow = new JPanel(new BorderLayout(6, 0));
        JPanel editLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        editLeft.add(editMode);
        editRow.add(editLeft, BorderLayout.NORTH);
        editRow.add(new JScrollPane(modifiedArea), BorderLayout.CENTER);
        JPanel resetHolder = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        resetHolder.add(resetButton);
        resetHolder.add(notice);
        editRow.add(resetHolder, BorderLayout.SOUTH);

        JPanel controls = new JPanel(new GridLayout(0, 1));
        controls.setBorder(BorderFactory.createTitledBorder("Modificación del mensaje"));
        JPanel inner = new JPanel(new BorderLayout());
        inner.add(flipRow, BorderLayout.NORTH);
        inner.add(editRow, BorderLayout.CENTER);
        controls.add(inner);
        add(controls, BorderLayout.NORTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showDiff();
            }
        });
        JPanel diffPanel = new JPanel(new BorderLayout());
        diffPanel.setBorder(BorderFactory.createTitledBorder(
                "Resumen original (O) y modificado (M) en binario; los bits distintos se marcan en rojo"));
        diffPane.setEditable(false);
        diffPane.setFont(Theme.MONO);
        diffPanel.add(new JScrollPane(diffPane), BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(table), diffPanel);
        split.setResizeWeight(0.35);
        add(split, BorderLayout.CENTER);

        input.addListener(this::inputChanged);
        inputChanged();
    }

    /** El mensaje original cambió: se reconfiguran el selector de bit y el texto modificado. */
    private void inputChanged() {
        updating = true;
        try {
            int bits = Math.max(1, input.getBytes().length * 8);
            SpinnerNumberModel model = (SpinnerNumberModel) bitSpinner.getModel();
            int value = Math.min(((Number) model.getValue()).intValue(), bits - 1);
            model.setMaximum(bits - 1);
            model.setValue(value);
            boolean file = input.isFile();
            editMode.setEnabled(!file);
            modifiedArea.setEnabled(!file);
            resetButton.setEnabled(!file);
            if (file) {
                flipMode.setSelected(true);
            }
            resetModified();
        } finally {
            updating = false;
        }
        recompute();
    }

    /** Texto modificado por defecto: el original con el último carácter incrementado. */
    private void resetModified() {
        boolean was = updating;
        updating = true;
        try {
            String text = input.getText();
            if (!input.isFile() && !input.getEncoding().isHex() && !text.isEmpty()) {
                char last = text.charAt(text.length() - 1);
                text = text.substring(0, text.length() - 1) + (char) (last + 1);
            }
            modifiedArea.setText(text);
        } finally {
            updating = was;
        }
        if (!was) {
            recompute();
        }
    }

    private void edited() {
        if (!updating) {
            editMode.setSelected(true);
            recompute();
        }
    }

    private byte[] modifiedBytes() {
        byte[] original = input.getBytes();
        notice.setText(" ");
        if (flipMode.isSelected() || input.isFile()) {
            if (original.length == 0) {
                return original;
            }
            int bit = ((Number) bitSpinner.getValue()).intValue();
            return Avalanche.flipBit(original, bit);
        }
        try {
            return input.getEncoding().encode(modifiedArea.getText());
        } catch (IllegalArgumentException e) {
            notice.setText("⚠ " + e.getMessage());
            return null;
        }
    }

    private void recompute() {
        if (updating) {
            return;
        }
        tableModel.setRowCount(0);
        diffPane.setText("");
        int bit = ((Number) bitSpinner.getValue()).intValue();
        bitLabel.setText("(byte " + bit / 8 + ", bit " + bit % 8 + " contando desde el más significativo)");
        if (input.getError() != null) {
            notice.setText("⚠ El mensaje original no es válido.");
            return;
        }
        if (input.getBytes().length > MAX_BYTES) {
            notice.setText("El mensaje supera 1 MB: el efecto avalancha solo se calcula para mensajes más pequeños.");
            return;
        }
        byte[] original = input.getBytes();
        byte[] modified = modifiedBytes();
        if (modified == null) {
            return;
        }
        boolean stats = original.length <= MAX_STATS_BYTES;
        for (HashAlgorithm a : Algorithms.all()) {
            byte[] d1 = a.digest(original);
            byte[] d2 = a.digest(modified);
            int diff = Avalanche.distance(d1, d2);
            String mean = "(mensaje > " + MAX_STATS_BYTES + " bytes)";
            String range = "";
            if (stats) {
                Avalanche.Stats s = Avalanche.overAllBits(a, original);
                mean = String.format("%.2f %%  (%d pruebas)", s.meanPercent, s.trials);
                range = String.format("%.1f – %.1f %%", s.minPercent, s.maxPercent);
            }
            tableModel.addRow(new Object[]{a.displayName(), a.digestBits(), diff,
                    String.format("%.2f %%", 100.0 * diff / a.digestBits()), mean, range});
        }
        table.getColumnModel().getColumn(0).setPreferredWidth(90);
        table.getColumnModel().getColumn(4).setPreferredWidth(230);
        int selected = Math.min(Algorithms.defaultIndex(), tableModel.getRowCount() - 1);
        if (selected >= 0) {
            table.setRowSelectionInterval(selected, selected);
        }
        showDiff();
        if (java.util.Arrays.equals(original, modified)) {
            notice.setText("Los dos mensajes son idénticos: no cambia ningún bit.");
        }
    }

    private void showDiff() {
        int row = table.getSelectedRow();
        diffPane.setText("");
        if (row < 0 || input.getError() != null) {
            return;
        }
        List<HashAlgorithm> all = Algorithms.all();
        HashAlgorithm a = all.get(row);
        byte[] modified = modifiedBytes();
        if (modified == null) {
            return;
        }
        byte[] d1 = a.digest(input.getBytes());
        byte[] d2 = a.digest(modified);
        StyledDocument doc = diffPane.getStyledDocument();
        SimpleAttributeSet normal = new SimpleAttributeSet();
        StyleConstants.setForeground(normal, Color.BLACK);
        SimpleAttributeSet bad = new SimpleAttributeSet();
        StyleConstants.setForeground(bad, Color.BLACK);
        StyleConstants.setBackground(bad, Theme.DIFF);
        try {
            doc.insertString(doc.getLength(), a.displayName() + ": " + Avalanche.distance(d1, d2) + " de "
                    + a.digestBits() + " bits distintos (esperado ≈ " + a.digestBits() / 2 + ")\n\n", normal);
            for (int chunk = 0; chunk < d1.length; chunk += 8) {
                int end = Math.min(d1.length, chunk + 8);
                doc.insertString(doc.getLength(), "O: ", normal);
                for (int i = chunk; i < end; i++) {
                    doc.insertString(doc.getLength(), bits(d1[i]) + " ", normal);
                }
                doc.insertString(doc.getLength(), "\nM: ", normal);
                for (int i = chunk; i < end; i++) {
                    String o = bits(d1[i]);
                    String m = bits(d2[i]);
                    for (int k = 0; k < 8; k++) {
                        doc.insertString(doc.getLength(), String.valueOf(m.charAt(k)),
                                o.charAt(k) != m.charAt(k) ? bad : normal);
                    }
                    doc.insertString(doc.getLength(), " ", normal);
                }
                doc.insertString(doc.getLength(), "\n\n", normal);
            }
        } catch (BadLocationException e) {
            throw new IllegalStateException(e);
        }
        diffPane.setCaretPosition(0);
    }

    private static String bits(byte b) {
        String s = Integer.toBinaryString(b & 0xFF);
        StringBuilder sb = new StringBuilder();
        for (int i = s.length(); i < 8; i++) {
            sb.append('0');
        }
        return sb.append(s).toString();
    }
}
