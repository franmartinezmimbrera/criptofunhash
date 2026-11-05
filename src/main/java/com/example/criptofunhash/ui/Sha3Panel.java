package com.example.criptofunhash.ui;

import com.example.criptofunhash.controller.InputModel;
import com.example.criptofunhash.model.Algorithms;
import com.example.criptofunhash.model.Hex;
import com.example.criptofunhash.model.Keccak;
import com.example.criptofunhash.model.Sha3;
import com.example.criptofunhash.model.Sha3Trace;
import com.example.criptofunhash.model.Sha3Variant;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.BoxLayout;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.Arrays;

/** SHA-3 paso a paso: relleno, absorción, permutación Keccak-f (θ ρ π χ ι) y salida. */
final class Sha3Panel extends JPanel implements VariantPanel {

    private static final String[] STEP_HELP = {
            "Entrada de la permutación: es el estado tras hacer XOR del bloque con la parte «rate».",
            "θ (theta): difusión entre columnas. C[x] = A[x,0]⊕A[x,1]⊕…⊕A[x,4];  D[x] = C[x−1] ⊕ ROTL1(C[x+1]);  "
                    + "A[x,y] ← A[x,y] ⊕ D[x]. Cada bit pasa a depender de 10 bits de las columnas vecinas.",
            "ρ (rho): rotación de cada palabra de 64 bits un número fijo de posiciones (distinto para cada "
                    + "posición (x,y); la palabra A[0,0] no rota).",
            "π (pi): permutación de las posiciones de las palabras: la palabra (x,y) pasa a (y, 2x+3y mod 5). "
                    + "Solo cambia de sitio; no modifica ningún bit.",
            "χ (chi): única operación no lineal. Dentro de cada fila: A[x] ← A[x] ⊕ (¬A[x+1] ∧ A[x+2]).",
            "ι (iota): se hace XOR de la constante de ronda RC[ronda] con la palabra A[0,0] para romper la "
                    + "simetría entre rondas."};

    private final InputModel input;
    private final JComboBox<Sha3Variant> variantBox = new JComboBox<>(Sha3Variant.values());
    private final JComboBox<String> blockBox = new JComboBox<>();
    private final JLabel notice = new JLabel(" ");

    private final JTextPane paddingPane = new JTextPane();
    private final JTextArea paddingText = Ui.prose(6);
    private final JLabel legendHolder = new JLabel(" ");

    private final LaneGridPanel beforeGrid = new LaneGridPanel();
    private final LaneGridPanel blockGrid = new LaneGridPanel();
    private final LaneGridPanel afterGrid = new LaneGridPanel();
    private final JTextArea absorbText = Ui.prose(4);

    private final LaneGridPanel permGrid = new LaneGridPanel();
    private final JSlider slider = new JSlider(0, 120, 0);
    private final JLabel stepLabel = new JLabel(" ");
    private final JTextArea stepText = Ui.monoArea(8);

    private final LaneGridPanel outGrid = new LaneGridPanel();
    private final JTextArea outText = Ui.monoArea(8);

    private Sha3Trace trace;
    private boolean updating;

    Sha3Panel(InputModel input, boolean showVariantChooser) {
        super(new BorderLayout());
        this.input = input;

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        if (showVariantChooser) {
            top.add(new JLabel("Algoritmo:"));
        }
        variantBox.setSelectedItem(Sha3Variant.SHA3_256);
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

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("1. Relleno", buildPaddingTab());
        tabs.addTab("2. Absorción (XOR con el rate)", buildAbsorbTab());
        tabs.addTab("3. Permutación Keccak-f: θ ρ π χ ι", buildPermutationTab());
        tabs.addTab("4. Salida (squeeze)", buildOutputTab());
        add(tabs, BorderLayout.CENTER);

        input.addListener(this::refresh);
        refresh();
    }

    private JPanel buildPaddingTab() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel north = new JPanel(new BorderLayout());
        north.add(legendHolder, BorderLayout.NORTH);
        north.add(Ui.scroll(paddingText, 130), BorderLayout.CENTER);
        panel.add(north, BorderLayout.NORTH);
        panel.add(new JScrollPane(paddingPane), BorderLayout.CENTER);
        return panel;
    }

    private JPanel titled(String title, LaneGridPanel grid) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(grid, BorderLayout.WEST);
        return p;
    }

    private JPanel buildAbsorbTab() {
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.add(Ui.legend(Theme.RATE, "rate r (se mezcla con el mensaje)",
                Theme.CAPACITY, "capacidad c (nunca se toca directamente)", Theme.CHANGED, "palabra modificada"));
        column.add(titled("Estado antes de absorber el bloque", beforeGrid));
        column.add(titled("Bloque del mensaje (rate), leído en palabras little-endian de 64 bits", blockGrid));
        column.add(titled("Estado tras el XOR (entrada de Keccak-f)", afterGrid));
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(Ui.scroll(absorbText, 90), BorderLayout.NORTH);
        panel.add(new JScrollPane(column), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildPermutationTab() {
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton prev = new JButton("◀ Paso anterior");
        JButton next = new JButton("Paso siguiente ▶");
        prev.addActionListener(e -> slider.setValue(slider.getValue() - 1));
        next.addActionListener(e -> slider.setValue(slider.getValue() + 1));
        controls.add(prev);
        controls.add(next);
        slider.setPreferredSize(new java.awt.Dimension(520, 40));
        slider.setMajorTickSpacing(5);
        slider.setPaintTicks(true);
        slider.addChangeListener(e -> showStep());
        controls.add(slider);
        stepLabel.setFont(Theme.TITLE_FONT);
        controls.add(stepLabel);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(controls, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout());
        center.add(Ui.legend(Theme.CHANGED, "palabra que ha cambiado respecto al paso anterior"), BorderLayout.NORTH);
        JPanel gridHolder = new JPanel(new FlowLayout(FlowLayout.LEFT));
        gridHolder.add(permGrid);
        center.add(gridHolder, BorderLayout.CENTER);
        center.add(Ui.scroll(stepText, 170), BorderLayout.SOUTH);
        panel.add(new JScrollPane(center), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildOutputTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(Ui.legend(Theme.RATE, "bits del estado que forman el resumen",
                Theme.CAPACITY, "bits que no se publican"), BorderLayout.NORTH);
        JPanel holder = new JPanel(new FlowLayout(FlowLayout.LEFT));
        holder.add(outGrid);
        panel.add(new JScrollPane(holder), BorderLayout.CENTER);
        panel.add(Ui.scroll(outText, 170), BorderLayout.SOUTH);
        return panel;
    }

    @Override
    public javax.swing.JComponent component() {
        return this;
    }

    @Override
    public void setVariant(com.example.criptofunhash.model.HashAlgorithm variant) {
        variantBox.setSelectedItem(variant);
    }

    private Sha3Variant variant() {
        return (Sha3Variant) variantBox.getSelectedItem();
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
                paddingPane.setText("");
                paddingText.setText("");
                absorbText.setText("");
                stepText.setText("");
                outText.setText("");
                legendHolder.setText(" ");
                return;
            }
            notice.setText(" ");
            trace = Sha3.trace(variant(), input.getBytes());
            for (String name : Ui.blockNames(trace.blocks.size())) {
                blockBox.addItem(name);
            }
        } finally {
            updating = false;
        }
        showPadding();
        showBlock();
        showOutput();
    }

    // ---------- 1. Relleno ----------

    private void showPadding() {
        Sha3Variant v = trace.variant;
        int n = trace.message.length;
        int total = trace.padded.length;
        legendHolder.setText(Ui.legend(Theme.MESSAGE, "mensaje", Theme.ONE_BIT, "sufijo 01 + primer 1 del pad10*1 (0x06)",
                Theme.ZEROS, "ceros", Theme.PAD_END, "último bit 1 (0x80)").getText());
        SegmentView sv = new SegmentView().add(0, n, Theme.MESSAGE);
        if (trace.padBytes() == 1) {
            sv.add(n, n + 1, Theme.PAD_END);
        } else {
            sv.add(n, n + 1, Theme.ONE_BIT).add(n + 1, total - 1, Theme.ZEROS).add(total - 1, total, Theme.PAD_END);
        }
        sv.show(paddingPane, trace.padded, v.rateBytes());
        paddingText.setText(
                "Mensaje: " + n + " bytes. Parámetros de " + v.displayName() + ": tasa (rate) r = " + v.blockBits()
                        + " bits = " + v.rateBytes() + " bytes; capacidad c = " + v.capacityBits()
                        + " bits (= 2 × " + v.digestBits() + "); r + c = 1600 bits.\n"
                        + "Relleno: se añade el sufijo de dominio SHA-3 «01» y el relleno pad10*1 (un 1, ceros, un 1) "
                        + "hasta completar un múltiplo de r. En bytes (los bits se escriben de menor a mayor peso) "
                        + "queda 0x06 … 0x80, o 0x86 si solo falta un byte.\n"
                        + "Se añaden " + trace.padBytes() + " bytes: " + total + " bytes en total = "
                        + trace.blocks.size() + " bloque(s) de " + v.rateBytes() + " bytes. A diferencia de SHA-2, "
                        + "el relleno NO incluye la longitud del mensaje.");
        paddingText.setCaretPosition(0);
    }

    // ---------- bloque seleccionado ----------

    private Sha3Trace.Block block() {
        return trace.blocks.get(Math.max(0, blockBox.getSelectedIndex()));
    }

    private void showBlock() {
        if (trace == null || blockBox.getItemCount() == 0) {
            return;
        }
        Sha3Variant v = trace.variant;
        Sha3Trace.Block b = block();
        int rateLanes = v.rateBytes() / 8;
        long[] blockLanes = new long[25];
        for (int i = 0; i < 25; i++) {
            blockLanes[i] = b.afterXor[i] ^ b.before[i];
        }
        beforeGrid.setState(b.before, null, rateLanes);
        blockGrid.setState(blockLanes, null, rateLanes);
        afterGrid.setState(b.afterXor, b.before, rateLanes);
        absorbText.setText("Fase de absorción del bloque " + (b.index + 1) + ": los " + v.rateBytes() + " bytes del bloque "
                + "se leen como " + rateLanes + " palabras de 64 bits y se combinan con XOR con las primeras " + rateLanes
                + " palabras del estado (la parte rate). Las otras " + (25 - rateLanes) + " palabras (la capacidad) "
                + "no se tocan. Después se aplica Keccak-f[1600] a todo el estado."
                + (b.index == 0 ? " Como es el primer bloque, el estado inicial es todo ceros." : ""));
        absorbText.setCaretPosition(0);
        slider.setValue(0);
        showStep();
    }

    // ---------- 3. Permutación ----------

    private void showStep() {
        if (trace == null || blockBox.getItemCount() == 0) {
            return;
        }
        Sha3Trace.Block b = block();
        Keccak.Permutation p = b.permutation;
        int index = slider.getValue();
        long[] state = p.stateAt(index);
        long[] previous = index == 0 ? null : p.stateAt(index - 1);
        permGrid.setState(state, previous, 25);
        StringBuilder sb = new StringBuilder();
        if (index == 0) {
            stepLabel.setText("Entrada de Keccak-f");
            sb.append(STEP_HELP[0]);
        } else {
            int round = (index - 1) / 5;
            int step = (index - 1) % 5;
            stepLabel.setText("Ronda " + (round + 1) + " de " + Keccak.ROUNDS + " · " + Keccak.STEP_NAMES[step]);
            sb.append(STEP_HELP[step + 1]).append("\n\n");
            switch (step) {
                case 0:
                    for (int x = 0; x < 5; x++) {
                        sb.append("C[").append(x).append("] = ").append(Hex.word(p.c[round][x], 64))
                                .append("    D[").append(x).append("] = ").append(Hex.word(p.d[round][x], 64)).append('\n');
                    }
                    break;
                case 1:
                    sb.append("Desplazamientos ρ (filas y = 0..4, columnas x = 0..4):\n");
                    for (int y = 0; y < 5; y++) {
                        for (int x = 0; x < 5; x++) {
                            sb.append(String.format("%3d ", Keccak.rotationOffset(x, y)));
                        }
                        sb.append('\n');
                    }
                    break;
                case 4:
                    sb.append("RC[").append(round).append("] = ").append(Hex.word(Keccak.roundConstant(round), 64));
                    break;
                default:
                    break;
            }
        }
        stepText.setText(sb.toString());
        stepText.setCaretPosition(0);
    }

    // ---------- 4. Salida ----------

    private void showOutput() {
        Sha3Variant v = trace.variant;
        int outLanes = (v.digestBits() / 8 + 7) / 8;
        outGrid.setState(trace.finalState, null, outLanes);
        StringBuilder sb = new StringBuilder();
        sb.append("Tras absorber todo el mensaje se «exprime» la esponja: el resumen son los primeros ")
                .append(v.digestBits()).append(" bits del estado (palabras leídas en little-endian),\n")
                .append("que caben en la parte rate (").append(v.blockBits()).append(" bits), así que basta una sola pasada.\n\n");
        sb.append("Resumen ").append(v.displayName()).append(":\n").append(Hex.of(trace.digest)).append("\n\n");
        byte[] ref = Algorithms.referenceDigest(v, trace.message);
        sb.append("Comprobación con java.security.MessageDigest: ");
        if (ref == null) {
            sb.append("no disponible en esta versión de Java (SHA-3 se incluyó en Java 9).");
        } else {
            sb.append(Arrays.equals(ref, trace.digest) ? "✔ coincide." : "✘ NO coincide.");
        }
        outText.setText(sb.toString());
        outText.setCaretPosition(0);
    }
}
