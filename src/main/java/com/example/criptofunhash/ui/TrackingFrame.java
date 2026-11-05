package com.example.criptofunhash.ui;

import com.example.criptofunhash.controller.InputModel;
import com.example.criptofunhash.model.HashAlgorithm;
import com.example.criptofunhash.model.Hex;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * «Seguimiento del algoritmo»: igual que en CriptoRES, una pestaña con los datos del proceso y otra con el
 * seguimiento en texto; se añade una tercera con la vista gráfica paso a paso.
 */
final class TrackingFrame extends JInternalFrame {

    /** Genera el informe de texto de una familia de algoritmos. */
    interface Reporter {
        String build(HashAlgorithm algorithm, byte[] message, boolean steps);
    }

    private static final class Result {
        SourcePanel.Data data;
        String hash;
        String report;
        long nanos;
    }

    private final JComboBox<HashAlgorithm> variantBox;
    private final Reporter reporter;
    private final VariantPanel graphical;
    private final InputModel model = new InputModel();
    private final SourcePanel source = new SourcePanel(model.getText());
    private final JTextArea hashArea = new JTextArea(3, 28);
    private final JRadioButton blocksMode = new JRadioButton("A Nivel de Bloques", true);
    private final JRadioButton stepsMode = new JRadioButton("A Nivel de Pasos");
    private final JLabel timeLabel = new JLabel("");
    private final JLabel bytesLabel = new JLabel("");
    private final JLabel speedLabel = new JLabel("");
    private final JTextArea reportArea = new JTextArea();
    private final JButton save = new JButton("Guardar");
    private final JButton print = new JButton("Imprimir");
    private final JButton transform = new JButton("Transformar");
    private final JTabbedPane tabs = new JTabbedPane(JTabbedPane.BOTTOM);

    TrackingFrame(String title, HashAlgorithm[] variants, HashAlgorithm initial, Reporter reporter,
                  Function<InputModel, VariantPanel> graphicalFactory) {
        super(title, true, true, true, true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.reporter = reporter;
        this.graphical = graphicalFactory == null ? null : graphicalFactory.apply(model);
        variantBox = new JComboBox<>(variants);
        variantBox.setSelectedItem(initial);
        variantBox.addActionListener(e -> {
            if (graphical != null) {
                graphical.setVariant((HashAlgorithm) variantBox.getSelectedItem());
            }
            clearResults();
        });
        if (graphical != null) {
            graphical.setVariant(initial);
        }

        reportArea.setEditable(false);
        reportArea.setFont(Theme.MONO);
        reportArea.setMargin(new Insets(6, 8, 6, 8));

        tabs.addTab("Datos sobre el proceso de Transformación", buildControls());
        tabs.addTab("Seguimiento del Algoritmo", new JScrollPane(reportArea));
        if (graphical != null) {
            tabs.addTab("Vista gráfica", graphical.component());
        }
        setContentPane(tabs);
        getRootPane().setDefaultButton(transform);
        setSize(780, 640);
        transform();
    }

    private JPanel buildControls() {
        JPanel variantRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        if (variantBox.getItemCount() > 1) {
            variantRow.add(new JLabel("Algoritmo:"));
            variantRow.add(variantBox);
        }

        hashArea.setEditable(false);
        hashArea.setLineWrap(true);
        hashArea.setFont(Theme.MONO_BOLD);
        JPanel hashPanel = new JPanel(new BorderLayout());
        hashPanel.setBorder(BorderFactory.createTitledBorder("Valor Hash o Resumen"));
        hashPanel.add(new JScrollPane(hashArea), BorderLayout.CENTER);

        ButtonGroup group = new ButtonGroup();
        group.add(blocksMode);
        group.add(stepsMode);
        JPanel modePanel = new JPanel(new GridBagLayout());
        modePanel.setBorder(BorderFactory.createTitledBorder("Tipo de Seguimiento:"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 4, 2, 4);
        modePanel.add(blocksMode, c);
        modePanel.add(small("(Visualiza los resultados de cada paso para cada bloque del mensaje)"), c);
        modePanel.add(stepsMode, c);
        modePanel.add(small("(Sólo se mostrará el cálculo de todos los pasos para el primer bloque del mensaje)"), c);

        JPanel stats = new JPanel(new GridBagLayout());
        stats.setBorder(BorderFactory.createTitledBorder("Datos Estadísticos"));
        GridBagConstraints s = new GridBagConstraints();
        s.anchor = GridBagConstraints.WEST;
        s.insets = new Insets(1, 6, 1, 6);
        String[] names = {"Tiempo:", "Bytes procesados:", "Bytes por segundo:"};
        JLabel[] values = {timeLabel, bytesLabel, speedLabel};
        for (int i = 0; i < 3; i++) {
            s.gridy = i;
            s.gridx = 0;
            stats.add(new JLabel(names[i]), s);
            s.gridx = 1;
            stats.add(values[i], s);
        }

        save.setMnemonic('G');
        save.addActionListener(e -> save());
        print.setMnemonic('I');
        print.addActionListener(e -> print());
        transform.setMnemonic('T');
        transform.addActionListener(e -> transform());
        JButton close = new JButton("Cerrar");
        close.setMnemonic('C');
        close.addActionListener(e -> dispose());
        source.setOnEnter(this::transform);
        save.setEnabled(false);
        print.setEnabled(false);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 6));
        buttons.add(save);
        buttons.add(print);
        buttons.add(transform);
        buttons.add(close);

        JPanel middle = new JPanel(new GridBagLayout());
        GridBagConstraints m = new GridBagConstraints();
        m.fill = GridBagConstraints.BOTH;
        m.weighty = 1;
        m.gridx = 0;
        m.weightx = 1;
        middle.add(hashPanel, m);
        m.gridx = 1;
        m.weightx = 0;
        middle.add(modePanel, m);

        JPanel left = new JPanel(new BorderLayout());
        left.add(variantRow, BorderLayout.NORTH);
        left.add(source, BorderLayout.CENTER);

        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statsRow.add(stats);

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        JPanel north = new JPanel(new BorderLayout());
        north.add(left, BorderLayout.NORTH);
        north.add(middle, BorderLayout.CENTER);
        panel.add(north, BorderLayout.NORTH);
        panel.add(statsRow, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private static JLabel small(String text) {
        JLabel label = new JLabel("<html><body style='width:210px'>" + text + "</body></html>");
        label.setFont(label.getFont().deriveFont(java.awt.Font.PLAIN, 11f));
        label.setBorder(BorderFactory.createEmptyBorder(0, 20, 4, 0));
        return label;
    }

    private void clearResults() {
        hashArea.setText("");
        timeLabel.setText("");
        bytesLabel.setText("");
        speedLabel.setText("");
        reportArea.setText("");
        save.setEnabled(false);
        print.setEnabled(false);
    }

    private void transform() {
        final SourcePanel.Request request = source.request();
        final HashAlgorithm algorithm = (HashAlgorithm) variantBox.getSelectedItem();
        final boolean steps = stepsMode.isSelected();
        transform.setEnabled(false);
        hashArea.setText("Calculando…");
        new SwingWorker<Result, Void>() {
            @Override
            protected Result doInBackground() throws Exception {
                Result r = new Result();
                r.data = request.load();
                long t0 = System.nanoTime();
                byte[] digest = algorithm.digest(r.data.bytes);
                r.nanos = Math.max(1, System.nanoTime() - t0);
                r.hash = Hex.of(digest).toUpperCase(Locale.ROOT);
                r.report = reporter.build(algorithm, r.data.bytes, steps);
                return r;
            }

            @Override
            protected void done() {
                transform.setEnabled(true);
                try {
                    show(get());
                } catch (ExecutionException e) {
                    clearResults();
                    JOptionPane.showMessageDialog(TrackingFrame.this, e.getCause().getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void show(Result r) {
        hashArea.setText(r.hash);
        timeLabel.setText(String.format("%.2f ms", r.nanos / 1e6));
        bytesLabel.setText(String.valueOf(r.data.bytes.length));
        speedLabel.setText(String.format("%,.0f", r.data.bytes.length / (r.nanos / 1e9)));
        reportArea.setText(r.report);
        reportArea.setCaretPosition(0);
        save.setEnabled(true);
        print.setEnabled(true);
        if (r.data.fileName != null) {
            model.loadBytes(r.data.fileName, r.data.bytes);
        } else {
            model.setTextAndEncoding(r.data.text, r.data.encoding);
        }
    }

    private void save() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar el seguimiento");
        chooser.setSelectedFile(new File("seguimiento.txt"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.write(chooser.getSelectedFile().toPath(), reportArea.getText().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "No se pudo guardar:\n" + e.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void print() {
        try {
            reportArea.print();
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(this, "No se pudo imprimir:\n" + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
