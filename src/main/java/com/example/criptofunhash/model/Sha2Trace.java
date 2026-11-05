package com.example.criptofunhash.model;

import java.util.ArrayList;
import java.util.List;

/** Registro completo del cálculo de un SHA-2, pensado para mostrarlo paso a paso. */
public final class Sha2Trace {

    public final Sha2Variant variant;
    public final byte[] message;
    /** Mensaje + bit 1 (0x80) + ceros + longitud en bits. */
    public final byte[] padded;
    /** Número de bytes a cero entre el 0x80 y el campo de longitud. */
    public final int zeroBytes;
    public final List<Block> blocks = new ArrayList<>();
    /** Los 8 registros H0..H7 tras el último bloque (sin truncar). */
    public long[] finalHash;
    public byte[] digest;

    Sha2Trace(Sha2Variant variant, byte[] message, byte[] padded, int zeroBytes) {
        this.variant = variant;
        this.message = message;
        this.padded = padded;
        this.zeroBytes = zeroBytes;
    }

    public int messageBytes() {
        return message.length;
    }

    public int lengthFieldStart() {
        return padded.length - variant.lengthFieldBytes();
    }

    /** Un bloque de 512 (o 1024) bits procesado. */
    public static final class Block {
        public final int index;
        public final byte[] data;
        /** Palabras expandidas W[0..rounds-1]. */
        public long[] w;
        /** Valor de H antes del bloque. */
        public long[] hIn;
        /** regs[t] = (a..h) antes de la ronda t; regs[rounds] = tras la última ronda. */
        public long[][] regs;
        public long[] t1;
        public long[] t2;
        public long[] ch;
        public long[] maj;
        public long[] sigma0;
        public long[] sigma1;
        /** Valor de H tras el bloque (hIn + registros finales, módulo 2^w). */
        public long[] hOut;

        Block(int index, byte[] data) {
            this.index = index;
            this.data = data;
        }
    }
}
