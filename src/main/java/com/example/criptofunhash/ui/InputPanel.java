package com.example.criptofunhash.ui;

import com.example.criptofunhash.controller.InputModel;
import com.example.criptofunhash.model.Hex;
import com.example.criptofunhash.model.TextEncoding;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.io.IOException;

/** Franja superior con el mensaje de entrada, común a todas las pestañas. */
final class InputPanel extends JPanel {

    private final InputModel model;
    private final JTextArea textArea = new JTextArea(3, 60);
    private final JComboBox<TextEncoding> encodingBox = new JComboBox<>(TextEncoding.values());
    private final JLabel info = new JLabel(" ");
    private final JButton clearFile = new JButton("Volver al texto");
    private boolean updating;

    InputPanel(final InputModel model) {
        super(new BorderLayout(8, 6));
        this.model = model;
        setBackground(Theme.FRAME_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JLabel title = new JLabel("MENSAJE DE ENTRADA");
        title.setFont(Theme.TITLE_FONT);
        title.setForeground(Theme.TEXT_LIGHT);
        add(title, BorderLayout.NORTH);

        textArea.setFont(Theme.MONO);
        textArea.setLineWrap(true);
        textArea.setText(model.getText());
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                changed();
            }
        });
        add(new JScrollPane(textArea), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 4));
        bottom.setOpaque(false);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        controls.setOpaque(false);
        JLabel encLabel = new JLabel("Codificación:");
        encLabel.setForeground(Theme.TEXT_LIGHT);
        controls.add(encLabel);
        encodingBox.addActionListener(e -> {
            if (!updating) {
                model.setEncoding((TextEncoding) encodingBox.getSelectedItem());
            }
        });
        controls.add(encodingBox);
        JButton open = new JButton("Abrir fichero...");
        open.addActionListener(e -> openFile());
        controls.add(open);
        clearFile.addActionListener(e -> model.clearFile());
        controls.add(clearFile);
        bottom.add(controls, BorderLayout.WEST);

        info.setForeground(Theme.TEXT_LIGHT);
        info.setFont(Theme.MONO.deriveFont(12f));
        bottom.add(info, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        model.addListener(this::refresh);
        refresh();
    }

    private void changed() {
        if (!updating) {
            model.setText(textArea.getText());
        }
    }

    private void openFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Elige el fichero cuyo resumen quieres calcular");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                model.loadFile(file);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "No se pudo leer el fichero:\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void refresh() {
        updating = true;
        try {
            boolean file = model.isFile();
            textArea.setEnabled(!file);
            encodingBox.setEnabled(!file);
            clearFile.setVisible(file);
            if (!file && !textArea.getText().equals(model.getText())) {
                textArea.setText(model.getText());
            }
            encodingBox.setSelectedItem(model.getEncoding());
            if (model.getError() != null) {
                info.setForeground(new Color(255, 150, 150));
                info.setText("⚠ " + model.getError());
                return;
            }
            info.setForeground(Theme.TEXT_LIGHT);
            byte[] bytes = model.getBytes();
            StringBuilder sb = new StringBuilder();
            if (file) {
                sb.append("Fichero «").append(model.getFileName()).append("» · ");
            }
            sb.append(bytes.length).append(" bytes (").append((long) bytes.length * 8).append(" bits)");
            if (bytes.length <= 24) {
                sb.append(" · ").append(Hex.spaced(bytes, 0, bytes.length));
            } else {
                sb.append(" · ").append(Hex.spaced(bytes, 0, 24)).append(" …");
            }
            if (model.isLossy()) {
                sb.append("  ⚠ hay caracteres que esta codificación no puede representar (se sustituyen por '?')");
                info.setForeground(new Color(255, 205, 120));
            }
            info.setText(sb.toString());
            info.setFont(info.getFont().deriveFont(Font.PLAIN));
        } finally {
            updating = false;
        }
    }
}
