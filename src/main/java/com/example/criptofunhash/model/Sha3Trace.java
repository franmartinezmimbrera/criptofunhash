package com.example.criptofunhash.model;

import java.util.ArrayList;
import java.util.List;

/** Registro completo del cálculo de un SHA-3 (esponja). */
public final class Sha3Trace {

    public final Sha3Variant variant;
    public final byte[] message;
    /** Mensaje con el relleno pad10*1 y el sufijo de dominio 01 (bytes 0x06 ... 0x80). */
    public final byte[] padded;
    public final List<Block> blocks = new ArrayList<>();
    public long[] finalState;
    public byte[] digest;

    Sha3Trace(Sha3Variant variant, byte[] message, byte[] padded) {
        this.variant = variant;
        this.message = message;
        this.padded = padded;
    }

    public int padBytes() {
        return padded.length - message.length;
    }

    /** Un bloque de {@code rate} bytes absorbido. */
    public static final class Block {
        public final int index;
        public final byte[] data;
        /** Estado antes de mezclar el bloque. */
        public long[] before;
        /** Estado tras hacer XOR del bloque con la parte "rate". */
        public long[] afterXor;
        public Keccak.Permutation permutation;
        /** Estado tras Keccak-f. */
        public long[] after;

        Block(int index, byte[] data) {
            this.index = index;
            this.data = data;
        }
    }
}
