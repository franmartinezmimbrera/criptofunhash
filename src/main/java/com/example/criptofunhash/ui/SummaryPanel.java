package com.example.criptofunhash.ui;

import com.example.criptofunhash.controller.InputModel;
import com.example.criptofunhash.model.Hex;
import com.example.criptofunhash.model.HashAlgorithm;
import com.example.criptofunhash.model.Algorithms;
import com.example.criptofunhash.model.TextEncoding;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

/** Resumen de todos los algoritmos para el mensaje actual y efecto de la codificación. */
final class SummaryPanel extends JPanel {

    private final InputModel input;
    private final DefaultTableModel digestModel = Ui.readOnlyModel("Algoritmo", "Familia", "Bits", "Resumen (hexadecimal)");
    private final JTable digestTable = Ui.monoTable(digestModel);
    private final DefaultTableModel encodingModel = Ui.readOnlyModel("Codificación", "Bytes de entrada", "Nº bytes", "Resumen");
    private final JTable encodingTable = Ui.monoTable(encodingModel);
    private final JComboBox<HashAlgorithm> encodingAlgorithm = new JComboBox<>(
            Algorithms.all().toArray(new HashAlgorithm[0]));
    private final JLabel encodingNotice = new JLabel(" ");
    private int generation;

    SummaryPanel(InputModel input) {
        super(new BorderLayout(0, 8));
        this.input = input;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel digestPanel = new JPanel(new BorderLayout(0, 4));
        digestPanel.setBorder(BorderFactory.createTitledBorder("Resumen del mensaje con cada algoritmo"));
        digestPanel.add(new JScrollPane(digestTable), BorderLayout.CENTER);
        JButton copy = new JButton("Copiar el resumen seleccionado");
        copy.addActionListener(e -> {
            int row = digestTable.getSelectedRow();
            if (row >= 0) {
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(String.valueOf(digestModel.getValueAt(row, 3))), null);
            }
        });
        JPanel copyRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        copyRow.add(copy);
        digestPanel.add(copyRow, BorderLayout.SOUTH);

        JPanel encodingPanel = new JPanel(new BorderLayout(0, 4));
        encodingPanel.setBorder(BorderFactory.createTitledBorder(
                "Efecto de la codificación del texto (¿el mismo texto da el mismo resumen?)"));
        JPanel encTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        encTop.add(new JLabel("Algoritmo:"));
        encodingAlgorithm.setSelectedItem(Algorithms.all().get(Algorithms.defaultIndex()));
        encodingAlgorithm.addActionListener(e -> refreshEncodings());
        encTop.add(encodingAlgorithm);
        encTop.add(encodingNotice);
        encodingPanel.add(encTop, BorderLayout.NORTH);
        encodingPanel.add(new JScrollPane(encodingTable), BorderLayout.CENTER);

        digestTable.getColumnModel().getColumn(0).setPreferredWidth(90);
        digestTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        digestTable.getColumnModel().getColumn(2).setPreferredWidth(50);
        digestTable.getColumnModel().getColumn(3).setPreferredWidth(900);
        encodingTable.getColumnModel().getColumn(0).setPreferredWidth(170);
        encodingTable.getColumnModel().getColumn(1).setPreferredWidth(260);
        encodingTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        encodingTable.getColumnModel().getColumn(3).setPreferredWidth(700);

        add(digestPanel, BorderLayout.CENTER);
        add(encodingPanel, BorderLayout.SOUTH);
        encodingPanel.setPreferredSize(new java.awt.Dimension(300, 200));

        input.addListener(this::refresh);
        refresh();
    }

    private void refresh() {
        refreshDigests();
        refreshEncodings();
    }

    private void refreshDigests() {
        final int mine = ++generation;
        digestModel.setRowCount(0);
        if (input.getError() != null) {
            return;
        }
        final byte[] message = input.getBytes();
        final List<HashAlgorithm> algorithms = Algorithms.all();
        for (HashAlgorithm a : algorithms) {
            digestModel.addRow(new Object[]{a.displayName(), a.family(), a.digestBits(), "calculando…"});
        }
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() {
                List<String> result = new ArrayList<>();
                for (HashAlgorithm a : algorithms) {
                    result.add(Hex.of(a.digest(message)));
                }
                return result;
            }

            @Override
            protected void done() {
                if (mine != generation) {
                    return;
                }
                try {
                    List<String> digests = get();
                    for (int i = 0; i < digests.size(); i++) {
                        digestModel.setValueAt(digests.get(i), i, 3);
                    }
                } catch (Exception ex) {
                    // la entrada cambió mientras se calculaba; se ignora
                }
            }
        }.execute();
    }

    private void refreshEncodings() {
        encodingModel.setRowCount(0);
        if (input.getError() != null) {
            encodingNotice.setText(" ");
            return;
        }
        if (input.isFile()) {
            encodingNotice.setText("(No aplicable: se está usando un fichero.)");
            return;
        }
        if (input.getEncoding().isHex()) {
            encodingNotice.setText("(No aplicable: el mensaje se ha introducido directamente en hexadecimal.)");
            return;
        }
        encodingNotice.setText("Texto introducido: «" + shorten(input.getText()) + "»");
        HashAlgorithm algorithm = (HashAlgorithm) encodingAlgorithm.getSelectedItem();
        for (TextEncoding enc : TextEncoding.values()) {
            if (enc.isHex()) {
                continue;
            }
            byte[] bytes = enc.encode(input.getText());
            String label = enc.toString() + (enc.canEncode(input.getText()) ? "" : "  ⚠ con pérdida");
            encodingModel.addRow(new Object[]{label, bytes.length > 40
                    ? Hex.spaced(bytes, 0, 40) + " …" : Hex.spaced(bytes, 0, bytes.length),
                    bytes.length, Hex.of(algorithm.digest(bytes))});
        }
    }

    private static String shorten(String s) {
        String oneLine = s.replace('\n', ' ');
        return oneLine.length() > 60 ? oneLine.substring(0, 60) + "…" : oneLine;
    }
}
