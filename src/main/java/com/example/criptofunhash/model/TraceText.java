package com.example.criptofunhash.model;

import java.util.Locale;

/** Utilidades de formato para los informes de seguimiento en texto. */
final class TraceText {

    private TraceText() {
    }

    static String hex(long value, int bits) {
        return Hex.word(value, bits).toUpperCase(Locale.ROOT);
    }

    static String binary(long value, int bits) {
        StringBuilder sb = new StringBuilder(bits);
        for (int i = bits - 1; i >= 0; i--) {
            sb.append(((value >>> i) & 1) == 1 ? '1' : '0');
        }
        return sb.toString();
    }

    static String bytesHex(byte[] data, int off, int len) {
        return Hex.spaced(data, off, len).toUpperCase(Locale.ROOT);
    }

    /** Bytes en líneas de 16, con el desplazamiento al principio de cada línea. */
    static void hexDump(StringBuilder sb, byte[] data, int from, int to) {
        for (int i = from; i < to; i += 16) {
            sb.append(String.format("%6d: ", i)).append(bytesHex(data, i, Math.min(16, to - i))).append('\n');
        }
    }

    static String line(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
