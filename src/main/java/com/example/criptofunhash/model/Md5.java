package com.example.criptofunhash.model;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

/** MD5 (RFC 1321). Obsoleta: se incluye para comprobar sus colisiones. */
public final class Md5 implements HashAlgorithm {

    public static final Md5 INSTANCE = new Md5();
    public static final int STEPS = 64;

    private static final long MASK = 0xFFFFFFFFL;
    private static final int[] SHIFTS = {
            7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
            5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
            4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
            6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21};
    private static final long[] K = new long[STEPS];
    private static final long[] IV = {0x67452301L, 0xefcdab89L, 0x98badcfeL, 0x10325476L};

    static {
        for (int i = 0; i < STEPS; i++) {
            K[i] = (long) Math.floor(Math.abs(Math.sin(i + 1)) * 4294967296.0);
        }
    }

    private Md5() {
    }

    public static long[] initialHash() {
        return IV.clone();
    }

    public static long[] constants() {
        return K.clone();
    }

    public static int shift(int step) {
        return SHIFTS[step];
    }

    @Override
    public String displayName() {
        return "MD5";
    }

    @Override
    public String family() {
        return "MD5 (obsoleto)";
    }

    @Override
    public int digestBits() {
        return 128;
    }

    @Override
    public int blockBits() {
        return 512;
    }

    @Override
    public int stateBits() {
        return 128;
    }

    @Override
    public int wordBits() {
        return 32;
    }

    @Override
    public int rounds() {
        return STEPS;
    }

    @Override
    public String javaName() {
        return "MD5";
    }

    @Override
    public byte[] digest(byte[] message) {
        return digestOf(walk(message, i -> false, b -> { }));
    }

    @Override
    public String toString() {
        return "MD5";
    }

    public static int blockCount(int messageBytes) {
        return (messageBytes + 8) / 64 + 1;
    }

    public static byte[] padTail(byte[] message) {
        return Padding.pad(message, message.length / 64 * 64, 64, 8, true);
    }

    public static byte[] digestOf(long[] h) {
        byte[] out = new byte[16];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                out[4 * i + j] = (byte) (h[i] >>> (8 * j));
            }
        }
        return out;
    }

    public static long[] walk(byte[] message, IntPredicate traced, Consumer<ClassicBlock> visitor) {
        long[] h = IV.clone();
        int full = message.length / 64;
        for (int i = 0; i < full; i++) {
            walkBlock(h, message, i * 64, i, traced, visitor);
        }
        byte[] tail = padTail(message);
        for (int off = 0; off < tail.length; off += 64) {
            walkBlock(h, tail, off, full + off / 64, traced, visitor);
        }
        return h;
    }

    private static void walkBlock(long[] h, byte[] data, int off, int index, IntPredicate traced,
                                  Consumer<ClassicBlock> visitor) {
        ClassicBlock block = traced.test(index) ? new ClassicBlock(index, Arrays.copyOfRange(data, off, off + 64)) : null;
        compress(h, data, off, block);
        if (block != null) {
            visitor.accept(block);
        }
    }

    private static long rotl(long x, int n) {
        return ((x << n) | (x >>> (32 - n))) & MASK;
    }

    private static void compress(long[] h, byte[] data, int off, ClassicBlock tr) {
        long[] m = new long[16];
        for (int i = 0; i < 16; i++) {
            long word = 0;
            for (int j = 3; j >= 0; j--) {
                word = (word << 8) | (data[off + 4 * i + j] & 0xFF);
            }
            m[i] = word;
        }
        long a = h[0];
        long b = h[1];
        long c = h[2];
        long d = h[3];
        if (tr != null) {
            tr.w = m;
            tr.hIn = h.clone();
            tr.regs = new long[STEPS + 1][];
            tr.regs[0] = new long[]{a, b, c, d};
            tr.f = new long[STEPS];
            tr.k = K.clone();
            tr.tmp = new long[STEPS];
            tr.g = new int[STEPS];
        }
        for (int i = 0; i < STEPS; i++) {
            long f;
            int g;
            if (i < 16) {
                f = (b & c) | (~b & d);
                g = i;
            } else if (i < 32) {
                f = (d & b) | (~d & c);
                g = (5 * i + 1) % 16;
            } else if (i < 48) {
                f = b ^ c ^ d;
                g = (3 * i + 5) % 16;
            } else {
                f = c ^ (b | (~d & MASK));
                g = (7 * i) % 16;
            }
            f &= MASK;
            long sum = (a + f + K[i] + m[g]) & MASK;
            long nb = (b + rotl(sum, SHIFTS[i])) & MASK;
            a = d;
            d = c;
            c = b;
            b = nb;
            if (tr != null) {
                tr.regs[i + 1] = new long[]{a, b, c, d};
                tr.f[i] = f;
                tr.tmp[i] = sum;
                tr.g[i] = g;
            }
        }
        h[0] = (h[0] + a) & MASK;
        h[1] = (h[1] + b) & MASK;
        h[2] = (h[2] + c) & MASK;
        h[3] = (h[3] + d) & MASK;
        if (tr != null) {
            tr.hOut = h.clone();
        }
    }
}
