package com.example.criptofunhash.ui;

import com.example.criptofunhash.controller.InputModel;
import com.example.criptofunhash.model.Algorithms;
import com.example.criptofunhash.model.Hex;
import com.example.criptofunhash.model.Sha2;
import com.example.criptofunhash.model.Sha2Trace;
import com.example.criptofunhash.model.Sha2Variant;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.Arrays;

/** SHA-2 paso a paso: relleno, bloques, expansión del mensaje (W) y rondas (a..h). */
final class Sha2Panel extends JPanel implements VariantPanel {

    private final InputModel input;
    private final JComboBox<Sha2Variant> variantBox = new JComboBox<>(Sha2Variant.values());
    private final JComboBox<String> blockBox = new JComboBox<>();
    private final JLabel notice = new JLabel(" ");

    private final JTextPane paddingPane = new JTextPane();
    private final JTextArea paddingText = Ui.prose(6);
    private final JLabel paddingLegend = new JLabel(" ");

    private final DefaultTableModel wModel = Ui.readOnlyModel(
            "t", "W[t]", "σ1(W[t−2])", "W[t−7]", "σ0(W[t−15])", "W[t−16]", "Origen");
    private final JTable wTable = Ui.monoTable(wModel);

    private final DefaultTableModel roundModel = Ui.readOnlyModel(
            "t", "K[t]", "W[t]", "a", "b", "c", "d", "e", "f", "g", "h");
    private final JTable roundTable = Ui.monoTable(roundModel);
    private final JTextArea roundDetail = Ui.monoArea(11);

    private final JTextArea resultArea = Ui.monoArea(20);
    private final JTabbedPane tabs = new JTabbedPane();

    private Sha2Trace trace;
    private boolean updating;

    Sha2Panel(InputModel input, boolean showVariantChooser) {
        super(new BorderLayout());
        this.input = input;

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        if (showVariantChooser) {
            top.add(new JLabel("Algoritmo:"));
        }
        variantBox.setSelectedItem(Sha2Variant.SHA_256);
        variantBox.addActionListener(e -> {
            if (!updating) {
                refresh();
            }
        });
        if (showVariantChooser) {
            top.add(variantBox);
        }
        top.add(new JLabel("Bloque:"));
        blockBox.addActionListener(e -> {
            if (!updating) {
                showBlock();
            }
        });
        top.add(blockBox);
        notice.setForeground(new Color(170, 40, 40));
        top.add(notice);
        add(top, BorderLayout.NORTH);

        tabs.addTab("1. Relleno y longitud", buildPaddingTab());
        tabs.addTab("2. Expansión del mensaje (W)", new JScrollPane(wTable));
        tabs.addTab("3. Rondas (a…h)", buildRoundsTab());
        tabs.addTab("4. Resultado del bloque", new JScrollPane(resultArea));
        add(tabs, BorderLayout.CENTER);

        roundTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        roundTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showRoundDetail();
            }
        });
        DefaultTableCellRenderer highlight = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                           boolean focus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                if (!selected) {
                    c.setBackground(column == 3 || column == 7 ? Theme.CHANGED : Color.WHITE);
                }
                return c;
            }
        };
        for (int col = 0; col < roundTable.getColumnCount(); col++) {
            roundTable.getColumnModel().getColumn(col).setCellRenderer(highlight);
        }
        wTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        input.addListener(this::refresh);
        refresh();
    }

    private JPanel buildPaddingTab() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel north = new JPanel(new BorderLayout());
        north.add(paddingLegend, BorderLayout.NORTH);
        north.add(Ui.scroll(paddingText, 130), BorderLayout.CENTER);
        panel.add(north, BorderLayout.NORTH);
        panel.add(new JScrollPane(paddingPane), BorderLayout.CENTER);
        return panel;
    }

    private JSplitPane buildRoundsTab() {
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(roundTable),
                new JScrollPane(roundDetail));
        split.setResizeWeight(0.65);
        split.setBorder(BorderFactory.createEmptyBorder());
        return split;
    }

    @Override
    public javax.swing.JComponent component() {
        return this;
    }

    @Override
    public void setVariant(com.example.criptofunhash.model.HashAlgorithm variant) {
        variantBox.setSelectedItem(variant);
    }

    private Sha2Variant variant() {
        return (Sha2Variant) variantBox.getSelectedItem();
    }

    private void refresh() {
        trace = null;
        String problem = null;
        if (input.getError() != null) {
            problem = "Mensaje no válido: " + input.getError();
        } else if (!input.isTraceable()) {
            problem = "El mensaje supera " + InputModel.MAX_TRACE_BYTES
                    + " bytes: el paso a paso solo se genera para mensajes más cortos.";
        }
        updating = true;
        try {
            blockBox.removeAllItems();
            if (problem != null) {
                notice.setText(problem);
                clear();
                return;
            }
            notice.setText(" ");
            trace = Sha2.trace(variant(), input.getBytes());
            for (String name : Ui.blockNames(trace.blocks.size())) {
                blockBox.addItem(name);
            }
        } finally {
            updating = false;
        }
        showPadding();
        showBlock();
    }

    private void clear() {
        paddingPane.setText("");
        paddingText.setText("");
        paddingLegend.setText(" ");
        wModel.setRowCount(0);
        roundModel.setRowCount(0);
        roundDetail.setText("");
        resultArea.setText("");
    }

    // ---------- 1. Relleno ----------

    private void showPadding() {
        Sha2Variant v = trace.variant;
        int n = trace.messageBytes();
        int lf = v.lengthFieldBytes();
        int bits = v.blockBits();
        paddingLegend.setText(Ui.legend(Theme.MESSAGE, "mensaje", Theme.ONE_BIT, "bit 1 (0x80)",
                Theme.ZEROS, "ceros", Theme.LENGTH, "longitud en bits (" + lf * 8 + " bits)").getText());
        new SegmentView()
                .add(0, n, Theme.MESSAGE)
                .add(n, n + 1, Theme.ONE_BIT)
                .add(n + 1, n + 1 + trace.zeroBytes, Theme.ZEROS)
                .add(trace.lengthFieldStart(), trace.padded.length, Theme.LENGTH)
                .show(paddingPane, trace.padded, v.blockBytes());

        long l = n * 8L;
        paddingText.setText(
                "Mensaje: " + n + " bytes = L = " + l + " bits.\n"
                        + "El relleno añade un bit 1 (el byte 0x80), después k bits a cero y por último L escrito en "
                        + lf * 8 + " bits (big-endian), de forma que la longitud total sea múltiplo de " + bits
                        + " bits:  L + 1 + k + " + lf * 8 + " ≡ 0 (mod " + bits + ").\n"
                        + "Aquí k = " + trace.zeroBytes * 8 + " bits (" + trace.zeroBytes + " bytes a cero). "
                        + "Longitud con relleno: " + trace.padded.length + " bytes = " + trace.padded.length * 8
                        + " bits = " + trace.blocks.size() + " bloque(s) de " + bits + " bits.\n"
                        + "Cada bloque son 16 palabras de " + v.wordBits() + " bits (M0…M15). El resumen final tiene "
                        + v.digestBits() + " bits = " + v.digestBits() / 4 + " dígitos hexadecimales.");
        paddingText.setCaretPosition(0);
    }

    // ---------- bloque seleccionado ----------

    private Sha2Trace.Block block() {
        int i = Math.max(0, blockBox.getSelectedIndex());
        return trace.blocks.get(i);
    }

    private void showBlock() {
        if (trace == null || blockBox.getItemCount() == 0) {
            return;
        }
        showExpansion();
        showRounds();
        showResult();
    }

    // ---------- 2. Expansión ----------

    private void showExpansion() {
        Sha2Variant v = trace.variant;
        Sha2Trace.Block b = block();
        int wb = v.wordBits();
        wModel.setRowCount(0);
        for (int t = 0; t < v.rounds(); t++) {
            if (t < 16) {
                wModel.addRow(new Object[]{t, Hex.word(b.w[t], wb), "", "", "", "",
                        "M" + t + ": bytes " + t * v.wordBytes() + "–" + ((t + 1) * v.wordBytes() - 1) + " del bloque"});
            } else {
                wModel.addRow(new Object[]{t, Hex.word(b.w[t], wb),
                        Hex.word(Sha2.smallSigma1(v, b.w[t - 2]), wb), Hex.word(b.w[t - 7], wb),
                        Hex.word(Sha2.smallSigma0(v, b.w[t - 15]), wb), Hex.word(b.w[t - 16], wb),
                        "σ1(W" + (t - 2) + ") + W" + (t - 7) + " + σ0(W" + (t - 15) + ") + W" + (t - 16)
                                + "  (mod 2^" + wb + ")"});
            }
        }
        int hexWidth = wb / 4 * 9 + 14;
        int[] widths = {40, hexWidth, hexWidth, hexWidth, hexWidth, hexWidth, 420};
        for (int c = 0; c < widths.length; c++) {
            wTable.getColumnModel().getColumn(c).setPreferredWidth(widths[c]);
        }
    }

    // ---------- 3. Rondas ----------

    private void showRounds() {
        Sha2Variant v = trace.variant;
        Sha2Trace.Block b = block();
        int wb = v.wordBits();
        long[] k = v.roundConstants();
        roundModel.setRowCount(0);
        roundModel.addRow(rowOf("ini", "", "", b.regs[0], wb));
        for (int t = 0; t < v.rounds(); t++) {
            roundModel.addRow(rowOf(String.valueOf(t), Hex.word(k[t], wb), Hex.word(b.w[t], wb), b.regs[t + 1], wb));
        }
        int hexWidth = wb / 4 * 9 + 14;
        for (int c = 0; c < roundTable.getColumnCount(); c++) {
            roundTable.getColumnModel().getColumn(c).setPreferredWidth(c == 0 ? 42 : hexWidth);
        }
        roundTable.setRowSelectionInterval(1, 1);
    }

    private static Object[] rowOf(String t, String k, String w, long[] regs, int wb) {
        Object[] row = new Object[11];
        row[0] = t;
        row[1] = k;
        row[2] = w;
        for (int i = 0; i < 8; i++) {
            row[3 + i] = Hex.word(regs[i], wb);
        }
        return row;
    }

    private void showRoundDetail() {
        if (trace == null || blockBox.getItemCount() == 0) {
            return;
        }
        int row = roundTable.getSelectedRow();
        Sha2Variant v = trace.variant;
        Sha2Trace.Block b = block();
        int wb = v.wordBits();
        if (row <= 0) {
            roundDetail.setText("Valores iniciales de los registros a…h = H0…H7 del bloque.\n"
                    + (b.index == 0 ? "Para el primer bloque son los valores iniciales (IV) del algoritmo,\n"
                    + "derivados de las raíces cuadradas de los primeros primos."
                    : "Para este bloque son el resultado del bloque anterior (encadenamiento Merkle–Damgård)."));
            return;
        }
        int t = row - 1;
        long[] r = b.regs[t];
        long[] k = v.roundConstants();
        String mod = "mod 2^" + wb;
        StringBuilder sb = new StringBuilder();
        sb.append("Ronda ").append(t).append("   (registros antes de la ronda: a…h = ")
                .append(Hex.word(r[0], wb)).append(' ').append(Hex.word(r[1], wb)).append(" …)\n\n");
        sb.append("Σ1(e)      = ").append(Hex.word(b.sigma1[t], wb)).append('\n');
        sb.append("Ch(e,f,g)  = ").append(Hex.word(b.ch[t], wb)).append('\n');
        sb.append("T1 = h + Σ1(e) + Ch(e,f,g) + K[").append(t).append("] + W[").append(t).append("]  (").append(mod).append(")\n");
        sb.append("   = ").append(Hex.word(r[7], wb)).append(" + ").append(Hex.word(b.sigma1[t], wb)).append(" + ")
                .append(Hex.word(b.ch[t], wb)).append(" + ").append(Hex.word(k[t], wb)).append(" + ")
                .append(Hex.word(b.w[t], wb)).append("\n   = ").append(Hex.word(b.t1[t], wb)).append("\n\n");
        sb.append("Σ0(a)      = ").append(Hex.word(b.sigma0[t], wb)).append('\n');
        sb.append("Maj(a,b,c) = ").append(Hex.word(b.maj[t], wb)).append('\n');
        sb.append("T2 = Σ0(a) + Maj(a,b,c) = ").append(Hex.word(b.t2[t], wb)).append("\n\n");
        sb.append("Nuevos registros: a = T1 + T2 = ").append(Hex.word(b.regs[t + 1][0], wb))
                .append(",  e = d + T1 = ").append(Hex.word(b.regs[t + 1][4], wb)).append('\n');
        sb.append("El resto se desplaza: h←g, g←f, f←e, d←c, c←b, b←a.");
        roundDetail.setText(sb.toString());
        roundDetail.setCaretPosition(0);
    }

    // ---------- 4. Resultado ----------

    private void showResult() {
        Sha2Variant v = trace.variant;
        Sha2Trace.Block b = block();
        int wb = v.wordBits();
        boolean last = b.index == trace.blocks.size() - 1;
        StringBuilder sb = new StringBuilder();
        sb.append("Bloque ").append(b.index + 1).append(" de ").append(trace.blocks.size()).append(": tras ")
                .append(v.rounds()).append(" rondas se suman (mod 2^").append(wb)
                .append(") los registros al valor de H anterior:\n\n");
        sb.append("        H anterior        registro final        H nuevo\n");
        for (int i = 0; i < 8; i++) {
            sb.append("H").append(i).append(" = ").append(Hex.word(b.hIn[i], wb)).append(" + ")
                    .append(Hex.word(b.regs[v.rounds()][i], wb)).append(" = ").append(Hex.word(b.hOut[i], wb)).append('\n');
        }
        sb.append('\n');
        if (last) {
            sb.append("Es el último bloque: el resumen es H0‖H1‖…");
            if (v.digestBits() < 8 * wb) {
                sb.append(" truncado a ").append(v.digestBits()).append(" bits (").append(v.digestBits() / wb)
                        .append(" palabras").append(v.digestBits() % wb != 0 ? " y media" : "").append(')');
            }
            sb.append("\n\nResumen ").append(v.displayName()).append(" (").append(v.digestBits()).append(" bits):\n")
                    .append(Hex.of(trace.digest)).append('\n');
            byte[] ref = Algorithms.referenceDigest(v, trace.message);
            sb.append("\nComprobación con java.security.MessageDigest: ");
            if (ref == null) {
                sb.append("no disponible en esta versión de Java.");
            } else {
                sb.append(Arrays.equals(ref, trace.digest) ? "✔ coincide." : "✘ NO coincide.");
            }
        } else {
            sb.append("Este H nuevo es el valor inicial del bloque siguiente: así se encadenan los bloques\n"
                    + "(construcción de Merkle–Damgård).");
        }
        resultArea.setText(sb.toString());
        resultArea.setCaretPosition(0);
    }
}
