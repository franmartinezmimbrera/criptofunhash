package com.example.criptofunhash.model;

public enum Sha2Variant implements HashAlgorithm {
    SHA_224("SHA-224", 32, 224),
    SHA_256("SHA-256", 32, 256),
    SHA_384("SHA-384", 64, 384),
    SHA_512("SHA-512", 64, 512);

    private final String displayName;
    private final int wordBits;
    private final int digestBits;

    Sha2Variant(String displayName, int wordBits, int digestBits) {
        this.displayName = displayName;
        this.wordBits = wordBits;
        this.digestBits = digestBits;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public String family() {
        return "SHA-2";
    }

    @Override
    public int digestBits() {
        return digestBits;
    }

    @Override
    public int wordBits() {
        return wordBits;
    }

    public int wordBytes() {
        return wordBits / 8;
    }

    @Override
    public int blockBits() {
        return 16 * wordBits;
    }

    public int blockBytes() {
        return 16 * wordBytes();
    }

    @Override
    public int stateBits() {
        return 8 * wordBits;
    }

    @Override
    public int rounds() {
        return wordBits == 32 ? 64 : 80;
    }

    /** Bytes del campo de longitud al final del relleno (64 bits en SHA-224/256, 128 en SHA-384/512). */
    public int lengthFieldBytes() {
        return 2 * wordBytes();
    }

    public long mask() {
        return wordBits == 32 ? 0xFFFFFFFFL : -1L;
    }

    public long[] roundConstants() {
        if (wordBits == 32) {
            return Sha2Constants.k32();
        }
        return Sha2Constants.K64.clone();
    }

    public long[] initialHash() {
        switch (this) {
            case SHA_224:
                return Sha2Constants.iv224();
            case SHA_256:
                return Sha2Constants.iv256();
            case SHA_384:
                return Sha2Constants.IV384.clone();
            default:
                return Sha2Constants.IV512.clone();
        }
    }

    @Override
    public byte[] digest(byte[] message) {
        return Sha2.digest(this, message);
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
