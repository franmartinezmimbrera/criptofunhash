package com.example.criptofunhash.ui;

import com.example.criptofunhash.model.TextEncoding;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/** Grupo «Datos de Entrada» de CriptoRES: desde fichero o desde cadena de texto. */
final class SourcePanel extends JPanel {

    private static final long MAX_FILE_BYTES = 512L * 1024 * 1024;

    /** Contenido leído de la fuente. */
    static final class Data {
        final byte[] bytes;
        final String fileName;
        final String text;
        final TextEncoding encoding;

        Data(byte[] bytes, String fileName, String text, TextEncoding encoding) {
            this.bytes = bytes;
            this.fileName = fileName;
            this.text = text;
            this.encoding = encoding;
        }
    }

    /** Instantánea de lo escrito en el panel, que se puede cargar fuera del EDT. */
    static final class Request {
        private final File file;
        private final String text;
        private final TextEncoding encoding;

        Request(File file, String text, TextEncoding encoding) {
            this.file = file;
            this.text = text;
            this.encoding = encoding;
        }

        Data load() throws IOException {
            if (encoding == null) {
                if (file == null) {
                    throw new IOException("Elige un fichero con «Examinar...».");
                }
                if (file.length() > MAX_FILE_BYTES) {
                    throw new IOException("El fichero es demasiado grande (máximo " + (MAX_FILE_BYTES >> 20) + " MB).");
                }
                return new Data(Files.readAllBytes(file.toPath()), file.getName(), null, null);
            }
            try {
                return new Data(encoding.encode(text), null, text, encoding);
            } catch (IllegalArgumentException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
    }

    private final JRadioButton fileRadio = new JRadioButton("Fichero");
    private final JRadioButton textRadio = new JRadioButton("Cadena de Texto", true);
    private final JButton browse = new JButton("Examinar...");
    private final JTextField field = new JTextField(30);
    private final JComboBox<TextEncoding> encodingBox = new JComboBox<>(TextEncoding.values());
    private File file;
    private String text;
    private Runnable onEnter = () -> { };

    SourcePanel(String initialText) {
        super(new GridBagLayout());
        this.text = initialText;
        setBorder(BorderFactory.createTitledBorder("Datos de Entrada"));
        field.setText(initialText);

        ButtonGroup group = new ButtonGroup();
        group.add(fileRadio);
        group.add(textRadio);
        fileRadio.setMnemonic('F');
        textRadio.setMnemonic('T');
        fileRadio.addActionListener(e -> modeChanged());
        textRadio.addActionListener(e -> modeChanged());
        browse.addActionListener(e -> browseFile());
        field.addActionListener(e -> onEnter.run());
        browse.setEnabled(false);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row1.add(new JLabel("Desde:"));
        row1.add(fileRadio);
        row1.add(browse);
        row1.add(textRadio);
        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row3.add(new JLabel("Codificación del texto:"));
        row3.add(encodingBox);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(1, 4, 1, 4);
        c.gridy = 0;
        add(row1, c);
        c.gridy = 1;
        add(field, c);
        c.gridy = 2;
        add(row3, c);
    }

    /** Acción al pulsar Intro en el campo de texto. */
    void setOnEnter(Runnable onEnter) {
        this.onEnter = onEnter;
    }

    Request request() {
        if (fileRadio.isSelected()) {
            return new Request(file, null, null);
        }
        return new Request(null, field.getText(), (TextEncoding) encodingBox.getSelectedItem());
    }

    private void modeChanged() {
        if (fileRadio.isSelected()) {
            text = field.getText();
            field.setText(file == null ? "" : file.getPath());
            field.setEditable(false);
        } else {
            field.setText(text);
            field.setEditable(true);
        }
        browse.setEnabled(fileRadio.isSelected());
        encodingBox.setEnabled(textRadio.isSelected());
    }

    private void browseFile() {
        JFileChooser chooser = new JFileChooser(file == null ? null : file.getParentFile());
        chooser.setDialogTitle("Elige el fichero cuyo resumen quieres calcular");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            file = chooser.getSelectedFile();
            field.setText(file.getPath());
        }
    }
}
