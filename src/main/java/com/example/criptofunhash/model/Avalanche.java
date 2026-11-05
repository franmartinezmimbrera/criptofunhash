package com.example.criptofunhash.model;

/** Utilidades para estudiar el efecto avalancha. */
public final class Avalanche {

    private Avalanche() {
    }

    /** Copia del mensaje con el bit {@code bit} invertido (0 = bit más significativo del primer byte). */
    public static byte[] flipBit(byte[] message, int bit) {
        byte[] copy = message.clone();
        copy[bit / 8] ^= (byte) (0x80 >> (bit % 8));
        return copy;
    }

    /** Número de bits en los que difieren dos resúmenes de la misma longitud. */
    public static int distance(byte[] a, byte[] b) {
        int bits = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            bits += Integer.bitCount((a[i] ^ b[i]) & 0xFF);
        }
        return bits;
    }

    /** Estadísticas al invertir, uno a uno, todos los bits del mensaje. */
    public static final class Stats {
        public final double meanPercent;
        public final double minPercent;
        public final double maxPercent;
        public final int trials;

        Stats(double mean, double min, double max, int trials) {
            this.meanPercent = mean;
            this.minPercent = min;
            this.maxPercent = max;
            this.trials = trials;
        }
    }

    public static Stats overAllBits(HashAlgorithm algorithm, byte[] message) {
        int totalBits = message.length * 8;
        if (totalBits == 0) {
            return new Stats(0, 0, 0, 0);
        }
        byte[] base = algorithm.digest(message);
        double sum = 0;
        double min = 100;
        double max = 0;
        for (int bit = 0; bit < totalBits; bit++) {
            double p = 100.0 * distance(base, algorithm.digest(flipBit(message, bit))) / algorithm.digestBits();
            sum += p;
            min = Math.min(min, p);
            max = Math.max(max, p);
        }
        return new Stats(sum / totalBits, min, max, totalBits);
    }
}
