package com.example.criptofunhash.model;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

/** Implementación de SHA3-224/256/384/512 (FIPS 202) con registro opcional de cada paso. */
public final class Sha3 {

    private Sha3() {
    }

    public static byte[] digest(Sha3Variant v, byte[] message) {
        int rate = v.rateBytes();
        long[] state = new long[25];
        int full = message.length / rate;
        for (int i = 0; i < full; i++) {
            absorb(state, message, i * rate, rate);
            Keccak.permute(state);
        }
        byte[] tail = pad(v, message, full * rate);
        for (int off = 0; off < tail.length; off += rate) {
            absorb(state, tail, off, rate);
            Keccak.permute(state);
        }
        return squeeze(v, state);
    }

    public static Sha3Trace trace(Sha3Variant v, byte[] message) {
        byte[] padded = pad(v, message, 0);
        Sha3Trace trace = new Sha3Trace(v, message, padded);
        int rate = v.rateBytes();
        long[] state = new long[25];
        for (int i = 0; i < padded.length / rate; i++) {
            Sha3Trace.Block block = new Sha3Trace.Block(i, Arrays.copyOfRange(padded, i * rate, (i + 1) * rate));
            block.before = state.clone();
            absorb(state, padded, i * rate, rate);
            block.afterXor = state.clone();
            block.permutation = new Keccak.Permutation();
            Keccak.permute(state, block.permutation);
            block.after = state.clone();
            trace.blocks.add(block);
        }
        trace.finalState = state;
        trace.digest = squeeze(v, state);
        return trace;
    }

    public static int blockCount(Sha3Variant v, int messageBytes) {
        return messageBytes / v.rateBytes() + 1;
    }

    /** Como {@link Sha2#walk}: recorre los bloques y solo detalla los que cumplen {@code traced}. */
    public static long[] walk(Sha3Variant v, byte[] message, IntPredicate traced, Consumer<Sha3Trace.Block> visitor) {
        int rate = v.rateBytes();
        long[] state = new long[25];
        int full = message.length / rate;
        for (int i = 0; i < full; i++) {
            walkBlock(state, message, i * rate, rate, i, traced, visitor);
        }
        byte[] tail = pad(v, message, full * rate);
        for (int off = 0; off < tail.length; off += rate) {
            walkBlock(state, tail, off, rate, full + off / rate, traced, visitor);
        }
        return state;
    }

    private static void walkBlock(long[] state, byte[] data, int off, int rate, int index,
                                  IntPredicate traced, Consumer<Sha3Trace.Block> visitor) {
        if (!traced.test(index)) {
            absorb(state, data, off, rate);
            Keccak.permute(state);
            return;
        }
        Sha3Trace.Block block = new Sha3Trace.Block(index, Arrays.copyOfRange(data, off, off + rate));
        block.before = state.clone();
        absorb(state, data, off, rate);
        block.afterXor = state.clone();
        block.permutation = new Keccak.Permutation();
        Keccak.permute(state, block.permutation);
        block.after = state.clone();
        visitor.accept(block);
    }

    public static byte[] padTail(Sha3Variant v, byte[] message) {
        return pad(v, message, message.length / v.rateBytes() * v.rateBytes());
    }

    public static byte[] digestOf(Sha3Variant v, long[] state) {
        return squeeze(v, state);
    }

    /** Relleno desde el byte {@code from} (alineado a bloque): sufijo 01 + pad10*1 => 0x06 ... 0x80. */
    static byte[] pad(Sha3Variant v, byte[] message, int from) {
        int rate = v.rateBytes();
        int rem = message.length - from;
        byte[] out = new byte[(rem / rate + 1) * rate];
        System.arraycopy(message, from, out, 0, rem);
        out[rem] ^= 0x06;
        out[out.length - 1] ^= (byte) 0x80;
        return out;
    }

    /** XOR de {@code len} bytes (little-endian por palabras) con el principio del estado. */
    static void absorb(long[] state, byte[] data, int off, int len) {
        for (int i = 0; i < len / 8; i++) {
            long lane = 0;
            for (int j = 7; j >= 0; j--) {
                lane = (lane << 8) | (data[off + 8 * i + j] & 0xFF);
            }
            state[i] ^= lane;
        }
    }

    private static byte[] squeeze(Sha3Variant v, long[] state) {
        int out = v.digestBits() / 8;
        byte[] result = new byte[out];
        for (int i = 0; i < out; i++) {
            result[i] = (byte) (state[i / 8] >>> (8 * (i % 8)));
        }
        return result;
    }
}
