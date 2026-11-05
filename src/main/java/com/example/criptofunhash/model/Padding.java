package com.example.criptofunhash.model;

/** Relleno de Merkle–Damgård: 1 + ceros + longitud en bits (little-endian en MD5, big-endian en el resto). */
final class Padding {

    private Padding() {
    }

    static byte[] pad(byte[] message, int from, int blockBytes, int lengthBytes, boolean littleEndian) {
        int rem = message.length - from;
        int zeros = Math.floorMod(-(message.length + 1 + lengthBytes), blockBytes);
        byte[] out = new byte[rem + 1 + zeros + lengthBytes];
        System.arraycopy(message, from, out, 0, rem);
        out[rem] = (byte) 0x80;
        long bits = message.length * 8L;
        for (int i = 0; i < 8; i++) {
            int pos = littleEndian ? out.length - lengthBytes + i : out.length - 1 - i;
            out[pos] = (byte) (bits >>> (8 * i));
        }
        return out;
    }
}
