package com.example.criptofunhash.ui;

import com.example.criptofunhash.model.HashAlgorithm;
import com.example.criptofunhash.model.Hex;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/** Ventana sencilla (como «MD5» en CriptoRES): datos de entrada, valor hash, Transformar y Cerrar. */
final class SimpleHashFrame extends JInternalFrame {

    private final JComboBox<HashAlgorithm> variantBox;
    private final SourcePanel source = new SourcePanel("Hola mundo");
    private final JTextArea hashArea = new JTextArea(3, 40);
    private final JButton transform = new JButton("Transformar");
    private final JLabel status = new JLabel(" ");

    SimpleHashFrame(String title, HashAlgorithm[] variants, HashAlgorithm initial) {
        super(title, true, true, false, true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        variantBox = new JComboBox<>(variants);
        variantBox.setSelectedItem(initial);
        variantBox.addActionListener(e -> hashArea.setText(""));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        if (variants.length > 1) {
            top.add(new JLabel("Algoritmo:"));
            top.add(variantBox);
        }

        hashArea.setEditable(false);
        hashArea.setLineWrap(true);
        hashArea.setFont(Theme.MONO_BOLD);
        JPanel hashPanel = new JPanel(new BorderLayout());
        hashPanel.setBorder(BorderFactory.createTitledBorder("Valor Hash o Resumen"));
        hashPanel.add(new JScrollPane(hashArea), BorderLayout.CENTER);

        transform.setMnemonic('T');
        transform.addActionListener(e -> transform());
        JButton copy = new JButton("Copiar");
        copy.setToolTipText("Copia el resumen al portapapeles");
        copy.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(hashArea.getText()), null));
        JButton close = new JButton("Cerrar");
        close.setMnemonic('C');
        close.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        buttons.add(status);
        buttons.add(copy);
        buttons.add(transform);
        buttons.add(close);
        source.setOnEnter(this::transform);

        JPanel north = new JPanel(new BorderLayout());
        north.add(top, BorderLayout.NORTH);
        north.add(source, BorderLayout.CENTER);
        JPanel content = new JPanel(new BorderLayout(0, 4));
        content.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        content.add(north, BorderLayout.NORTH);
        content.add(hashPanel, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
        getRootPane().setDefaultButton(transform);
        setSize(520, 380);
    }

    void transform() {
        final SourcePanel.Request request = source.request();
        final HashAlgorithm algorithm = (HashAlgorithm) variantBox.getSelectedItem();
        transform.setEnabled(false);
        status.setText("Calculando…");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return Hex.of(algorithm.digest(request.load().bytes)).toUpperCase(Locale.ROOT);
            }

            @Override
            protected void done() {
                transform.setEnabled(true);
                status.setText(" ");
                try {
                    hashArea.setText(get());
                } catch (ExecutionException e) {
                    hashArea.setText("");
                    JOptionPane.showMessageDialog(SimpleHashFrame.this, e.getCause().getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }
}
