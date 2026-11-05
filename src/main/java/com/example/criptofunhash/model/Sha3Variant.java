package com.example.criptofunhash.model;

public enum Sha3Variant implements HashAlgorithm {
    SHA3_224("SHA3-224", 224),
    SHA3_256("SHA3-256", 256),
    SHA3_384("SHA3-384", 384),
    SHA3_512("SHA3-512", 512);

    private final String displayName;
    private final int digestBits;

    Sha3Variant(String displayName, int digestBits) {
        this.displayName = displayName;
        this.digestBits = digestBits;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public String family() {
        return "SHA-3";
    }

    @Override
    public int digestBits() {
        return digestBits;
    }

    /** Tasa (rate) en bytes: 200 - 2 * (bytes del resumen). */
    public int rateBytes() {
        return 200 - 2 * (digestBits / 8);
    }

    /** Capacidad en bits: 2 * bits del resumen. */
    public int capacityBits() {
        return 2 * digestBits;
    }

    @Override
    public int blockBits() {
        return rateBytes() * 8;
    }

    @Override
    public int stateBits() {
        return 1600;
    }

    @Override
    public int wordBits() {
        return 64;
    }

    @Override
    public int rounds() {
        return Keccak.ROUNDS;
    }

    @Override
    public byte[] digest(byte[] message) {
        return Sha3.digest(this, message);
    }

    @Override
    public String javaName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
