package com.example.criptofunhash.model;

public final class Hex {

    private static final char[] DIGITS = "0123456789abcdef".toCharArray();

    private Hex() {
    }

    public static String of(byte[] data) {
        return of(data, 0, data.length);
    }

    public static String of(byte[] data, int off, int len) {
        StringBuilder sb = new StringBuilder(len * 2);
        for (int i = off; i < off + len; i++) {
            sb.append(DIGITS[(data[i] >> 4) & 0xF]).append(DIGITS[data[i] & 0xF]);
        }
        return sb.toString();
    }

    /** Bytes separados por espacios: "48 6f 6c 61". */
    public static String spaced(byte[] data, int off, int len) {
        StringBuilder sb = new StringBuilder(len * 3);
        for (int i = off; i < off + len; i++) {
            if (i > off) {
                sb.append(' ');
            }
            sb.append(DIGITS[(data[i] >> 4) & 0xF]).append(DIGITS[data[i] & 0xF]);
        }
        return sb.toString();
    }

    /** Palabra de {@code bits} bits en hexadecimal, con ceros a la izquierda. */
    public static String word(long value, int bits) {
        int digits = bits / 4;
        StringBuilder sb = new StringBuilder(digits);
        for (int i = digits - 1; i >= 0; i--) {
            sb.append(DIGITS[(int) ((value >>> (4 * i)) & 0xF)]);
        }
        return sb.toString();
    }

    public static String binary(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 8);
        for (byte b : data) {
            for (int i = 7; i >= 0; i--) {
                sb.append(((b >> i) & 1) == 1 ? '1' : '0');
            }
        }
        return sb.toString();
    }

    /**
     * Interpreta texto hexadecimal. Se ignoran espacios, saltos de línea, comas y
     * prefijos "0x".
     *
     * @throws IllegalArgumentException si hay un número impar de dígitos o caracteres no hexadecimales
     */
    public static byte[] parse(String text) {
        String clean = text.replace("0x", "").replace("0X", "").replaceAll("[\\s,:;-]", "");
        if (clean.length() % 2 != 0) {
            throw new IllegalArgumentException("Número impar de dígitos hexadecimales (" + clean.length() + ")");
        }
        byte[] out = new byte[clean.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(clean.charAt(2 * i), 16);
            int lo = Character.digit(clean.charAt(2 * i + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("Carácter no hexadecimal cerca de la posición " + (2 * i));
            }
            out[i] = (byte) (hi * 16 + lo);
        }
        return out;
    }
}
