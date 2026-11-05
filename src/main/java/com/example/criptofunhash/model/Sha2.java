package com.example.criptofunhash.model;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

/** Implementación de SHA-224/256/384/512 (FIPS 180-4) con registro opcional de cada paso. */
public final class Sha2 {

    private Sha2() {
    }

    public static byte[] digest(Sha2Variant v, byte[] message) {
        int bs = v.blockBytes();
        long[] h = v.initialHash();
        int full = message.length / bs;
        for (int i = 0; i < full; i++) {
            compress(v, h, message, i * bs, null);
        }
        byte[] tail = pad(v, message, full * bs);
        for (int off = 0; off < tail.length; off += bs) {
            compress(v, h, tail, off, null);
        }
        return output(v, h);
    }

    public static Sha2Trace trace(Sha2Variant v, byte[] message) {
        byte[] padded = pad(v, message, 0);
        int zeros = padded.length - message.length - 1 - v.lengthFieldBytes();
        Sha2Trace trace = new Sha2Trace(v, message, padded, zeros);
        long[] h = v.initialHash();
        int bs = v.blockBytes();
        for (int i = 0; i < padded.length / bs; i++) {
            Sha2Trace.Block block = new Sha2Trace.Block(i, Arrays.copyOfRange(padded, i * bs, (i + 1) * bs));
            compress(v, h, padded, i * bs, block);
            trace.blocks.add(block);
        }
        trace.finalHash = h;
        trace.digest = output(v, h);
        return trace;
    }

    /** Número de bloques del mensaje una vez rellenado. */
    public static int blockCount(Sha2Variant v, int messageBytes) {
        return (messageBytes + v.lengthFieldBytes()) / v.blockBytes() + 1;
    }

    /**
     * Recorre todos los bloques sin guardarlos. Solo para los bloques que cumplen {@code traced}
     * se registra el detalle y se entrega a {@code visitor}.
     *
     * @return los 8 registros H0..H7 finales
     */
    public static long[] walk(Sha2Variant v, byte[] message, IntPredicate traced, Consumer<Sha2Trace.Block> visitor) {
        int bs = v.blockBytes();
        long[] h = v.initialHash();
        int full = message.length / bs;
        for (int i = 0; i < full; i++) {
            walkBlock(v, h, message, i * bs, i, traced, visitor);
        }
        byte[] tail = pad(v, message, full * bs);
        for (int off = 0; off < tail.length; off += bs) {
            walkBlock(v, h, tail, off, full + off / bs, traced, visitor);
        }
        return h;
    }

    private static void walkBlock(Sha2Variant v, long[] h, byte[] data, int off, int index,
                                  IntPredicate traced, Consumer<Sha2Trace.Block> visitor) {
        int bs = v.blockBytes();
        Sha2Trace.Block block = traced.test(index)
                ? new Sha2Trace.Block(index, Arrays.copyOfRange(data, off, off + bs)) : null;
        compress(v, h, data, off, block);
        if (block != null) {
            visitor.accept(block);
        }
    }

    /** Bytes finales del mensaje ya rellenados (desde el último bloque completo). */
    public static byte[] padTail(Sha2Variant v, byte[] message) {
        return pad(v, message, message.length / v.blockBytes() * v.blockBytes());
    }

    /** Resumen (truncado) a partir de los registros finales. */
    public static byte[] digestOf(Sha2Variant v, long[] h) {
        return output(v, h);
    }

    /**
     * Relleno del mensaje a partir del byte {@code from} (que debe estar alineado a un bloque):
     * bytes restantes + 0x80 + ceros + longitud TOTAL del mensaje en bits.
     */
    static byte[] pad(Sha2Variant v, byte[] message, int from) {
        int bs = v.blockBytes();
        int lf = v.lengthFieldBytes();
        int rem = message.length - from;
        int zeros = Math.floorMod(-(message.length + 1 + lf), bs);
        byte[] out = new byte[rem + 1 + zeros + lf];
        System.arraycopy(message, from, out, 0, rem);
        out[rem] = (byte) 0x80;
        long bits = message.length * 8L;
        for (int i = 0; i < 8; i++) {
            out[out.length - 1 - i] = (byte) (bits >>> (8 * i));
        }
        return out;
    }

    private static byte[] output(Sha2Variant v, long[] h) {
        int wb = v.wordBytes();
        byte[] full = new byte[8 * wb];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < wb; j++) {
                full[i * wb + j] = (byte) (h[i] >>> (8 * (wb - 1 - j)));
            }
        }
        return Arrays.copyOf(full, v.digestBits() / 8);
    }

    private static void compress(Sha2Variant v, long[] h, byte[] data, int off, Sha2Trace.Block tr) {
        int wb = v.wordBytes();
        int rounds = v.rounds();
        long mask = v.mask();
        long[] k = v.roundConstants();

        long[] w = new long[rounds];
        for (int t = 0; t < 16; t++) {
            long word = 0;
            for (int j = 0; j < wb; j++) {
                word = (word << 8) | (data[off + t * wb + j] & 0xFF);
            }
            w[t] = word;
        }
        for (int t = 16; t < rounds; t++) {
            w[t] = (smallSigma1(v, w[t - 2]) + w[t - 7] + smallSigma0(v, w[t - 15]) + w[t - 16]) & mask;
        }

        long[] r = h.clone();
        if (tr != null) {
            tr.w = w;
            tr.hIn = h.clone();
            tr.regs = new long[rounds + 1][];
            tr.regs[0] = r.clone();
            tr.t1 = new long[rounds];
            tr.t2 = new long[rounds];
            tr.ch = new long[rounds];
            tr.maj = new long[rounds];
            tr.sigma0 = new long[rounds];
            tr.sigma1 = new long[rounds];
        }
        for (int t = 0; t < rounds; t++) {
            long s1 = bigSigma1(v, r[4]);
            long ch = ch(r[4], r[5], r[6]);
            long t1 = (r[7] + s1 + ch + k[t] + w[t]) & mask;
            long s0 = bigSigma0(v, r[0]);
            long maj = maj(r[0], r[1], r[2]);
            long t2 = (s0 + maj) & mask;
            r[7] = r[6];
            r[6] = r[5];
            r[5] = r[4];
            r[4] = (r[3] + t1) & mask;
            r[3] = r[2];
            r[2] = r[1];
            r[1] = r[0];
            r[0] = (t1 + t2) & mask;
            if (tr != null) {
                tr.regs[t + 1] = r.clone();
                tr.t1[t] = t1;
                tr.t2[t] = t2;
                tr.ch[t] = ch;
                tr.maj[t] = maj;
                tr.sigma0[t] = s0;
                tr.sigma1[t] = s1;
            }
        }
        for (int i = 0; i < 8; i++) {
            h[i] = (h[i] + r[i]) & mask;
        }
        if (tr != null) {
            tr.hOut = h.clone();
        }
    }

    public static long ch(long x, long y, long z) {
        return (x & y) ^ (~x & z);
    }

    public static long maj(long x, long y, long z) {
        return (x & y) ^ (x & z) ^ (y & z);
    }

    public static long bigSigma0(Sha2Variant v, long x) {
        return v.wordBits() == 32
                ? rotr(x, 2, 32) ^ rotr(x, 13, 32) ^ rotr(x, 22, 32)
                : rotr(x, 28, 64) ^ rotr(x, 34, 64) ^ rotr(x, 39, 64);
    }

    public static long bigSigma1(Sha2Variant v, long x) {
        return v.wordBits() == 32
                ? rotr(x, 6, 32) ^ rotr(x, 11, 32) ^ rotr(x, 25, 32)
                : rotr(x, 14, 64) ^ rotr(x, 18, 64) ^ rotr(x, 41, 64);
    }

    public static long smallSigma0(Sha2Variant v, long x) {
        return v.wordBits() == 32
                ? rotr(x, 7, 32) ^ rotr(x, 18, 32) ^ (x >>> 3)
                : rotr(x, 1, 64) ^ rotr(x, 8, 64) ^ (x >>> 7);
    }

    public static long smallSigma1(Sha2Variant v, long x) {
        return v.wordBits() == 32
                ? rotr(x, 17, 32) ^ rotr(x, 19, 32) ^ (x >>> 10)
                : rotr(x, 19, 64) ^ rotr(x, 61, 64) ^ (x >>> 6);
    }

    private static long rotr(long x, int n, int bits) {
        if (bits == 64) {
            return Long.rotateRight(x, n);
        }
        return ((x >>> n) | (x << (32 - n))) & 0xFFFFFFFFL;
    }
}
