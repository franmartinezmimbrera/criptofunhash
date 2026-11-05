package com.example.criptofunhash.ui;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/** Muestra bytes en hexadecimal, coloreando por tramos (mensaje, relleno, longitud...). */
final class SegmentView {

    private final List<int[]> bounds = new ArrayList<>();
    private final List<Color> colors = new ArrayList<>();

    /** Tramo [from, to) de bytes con el color de fondo dado. */
    SegmentView add(int from, int to, Color color) {
        if (to > from) {
            bounds.add(new int[]{from, to});
            colors.add(color);
        }
        return this;
    }

    void show(JTextPane pane, byte[] data, int blockBytes) {
        pane.setText("");
        pane.setFont(Theme.MONO);
        pane.setEditable(false);
        StyledDocument doc = pane.getStyledDocument();
        final int perLine = 16;
        try {
            for (int i = 0; i < data.length; i++) {
                if (i % blockBytes == 0) {
                    doc.insertString(doc.getLength(), (i == 0 ? "" : "\n\n")
                            + "── Bloque " + (i / blockBytes + 1) + " (bytes " + i + "–" + (i + blockBytes - 1)
                            + ") ──\n", plain());
                } else if (i % perLine == 0) {
                    doc.insertString(doc.getLength(), "\n", plain());
                }
                String hex = String.format("%02x", data[i] & 0xFF);
                SimpleAttributeSet style = plain();
                Color c = colorOf(i);
                if (c != null) {
                    StyleConstants.setBackground(style, c);
                }
                doc.insertString(doc.getLength(), hex, style);
                doc.insertString(doc.getLength(), " ", plain());
            }
        } catch (BadLocationException e) {
            throw new IllegalStateException(e);
        }
        pane.setCaretPosition(0);
    }

    private Color colorOf(int index) {
        for (int s = 0; s < bounds.size(); s++) {
            if (index >= bounds.get(s)[0] && index < bounds.get(s)[1]) {
                return colors.get(s);
            }
        }
        return null;
    }

    private static SimpleAttributeSet plain() {
        SimpleAttributeSet style = new SimpleAttributeSet();
        StyleConstants.setForeground(style, Color.BLACK);
        return style;
    }
}
