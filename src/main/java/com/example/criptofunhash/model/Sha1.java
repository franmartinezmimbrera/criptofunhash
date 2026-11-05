package com.example.criptofunhash.model;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

/** SHA-1 (FIPS 180-4). Obsoleta desde el ataque SHAttered (2017). */
public final class Sha1 implements HashAlgorithm {

    public static final Sha1 INSTANCE = new Sha1();
    public static final int STEPS = 80;

    private static final long MASK = 0xFFFFFFFFL;
    private static final long[] IV = {0x67452301L, 0xEFCDAB89L, 0x98BADCFEL, 0x10325476L, 0xC3D2E1F0L};
    private static final long[] K = {0x5A827999L, 0x6ED9EBA1L, 0x8F1BBCDCL, 0xCA62C1D6L};

    private Sha1() {
    }

    public static long[] initialHash() {
        return IV.clone();
    }

    /** Constante del paso t. */
    public static long constant(int t) {
        return K[t / 20];
    }

    @Override
    public String displayName() {
        return "SHA-1";
    }

    @Override
    public String family() {
        return "SHA-1 (obsoleto)";
    }

    @Override
    public int digestBits() {
        return 160;
    }

    @Override
    public int blockBits() {
        return 512;
    }

    @Override
    public int stateBits() {
        return 160;
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
        return "SHA-1";
    }

    @Override
    public byte[] digest(byte[] message) {
        return digestOf(walk(message, i -> false, b -> { }));
    }

    @Override
    public String toString() {
        return "SHA-1";
    }

    public static int blockCount(int messageBytes) {
        return (messageBytes + 8) / 64 + 1;
    }

    public static byte[] padTail(byte[] message) {
        return Padding.pad(message, message.length / 64 * 64, 64, 8, false);
    }

    public static byte[] digestOf(long[] h) {
        byte[] out = new byte[20];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 4; j++) {
                out[4 * i + j] = (byte) (h[i] >>> (8 * (3 - j)));
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
        long[] w = new long[STEPS];
        for (int t = 0; t < 16; t++) {
            long word = 0;
            for (int j = 0; j < 4; j++) {
                word = (word << 8) | (data[off + 4 * t + j] & 0xFF);
            }
            w[t] = word;
        }
        for (int t = 16; t < STEPS; t++) {
            w[t] = rotl(w[t - 3] ^ w[t - 8] ^ w[t - 14] ^ w[t - 16], 1);
        }
        long a = h[0];
        long b = h[1];
        long c = h[2];
        long d = h[3];
        long e = h[4];
        if (tr != null) {
            tr.w = w;
            tr.hIn = h.clone();
            tr.regs = new long[STEPS + 1][];
            tr.regs[0] = new long[]{a, b, c, d, e};
            tr.f = new long[STEPS];
            tr.k = new long[STEPS];
            tr.tmp = new long[STEPS];
        }
        for (int t = 0; t < STEPS; t++) {
            long f;
            if (t < 20) {
                f = (b & c) | (~b & d);
            } else if (t < 40 || t >= 60) {
                f = b ^ c ^ d;
            } else {
                f = (b & c) | (b & d) | (c & d);
            }
            f &= MASK;
            long k = K[t / 20];
            long temp = (rotl(a, 5) + f + e + k + w[t]) & MASK;
            e = d;
            d = c;
            c = rotl(b, 30);
            b = a;
            a = temp;
            if (tr != null) {
                tr.regs[t + 1] = new long[]{a, b, c, d, e};
                tr.f[t] = f;
                tr.k[t] = k;
                tr.tmp[t] = temp;
            }
        }
        h[0] = (h[0] + a) & MASK;
        h[1] = (h[1] + b) & MASK;
        h[2] = (h[2] + c) & MASK;
        h[3] = (h[3] + d) & MASK;
        h[4] = (h[4] + e) & MASK;
        if (tr != null) {
            tr.hOut = h.clone();
        }
    }
}
