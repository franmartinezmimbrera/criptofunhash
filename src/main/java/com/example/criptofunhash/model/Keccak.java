package com.example.criptofunhash.model;

/**
 * Permutación Keccak-f[1600]. El estado son 25 palabras de 64 bits; la palabra (x, y) está en
 * el índice {@code x + 5 * y}.
 */
public final class Keccak {

    public static final int ROUNDS = 24;
    public static final String[] STEP_NAMES = {"θ (theta)", "ρ (rho)", "π (pi)", "χ (chi)", "ι (iota)"};

    /** Desplazamientos de la rotación ρ, por índice x + 5y. */
    static final int[] ROT = new int[25];
    /** Constantes de ronda de ι. */
    static final long[] RC = new long[ROUNDS];

    static {
        int x = 1;
        int y = 0;
        for (int t = 0; t < 24; t++) {
            ROT[x + 5 * y] = ((t + 1) * (t + 2) / 2) % 64;
            int nx = y;
            int ny = (2 * x + 3 * y) % 5;
            x = nx;
            y = ny;
        }
        for (int r = 0; r < ROUNDS; r++) {
            long rc = 0;
            for (int j = 0; j < 7; j++) {
                if (lfsr(j + 7 * r)) {
                    rc |= 1L << ((1 << j) - 1);
                }
            }
            RC[r] = rc;
        }
    }

    private Keccak() {
    }

    public static int rotationOffset(int x, int y) {
        return ROT[x + 5 * y];
    }

    public static long roundConstant(int round) {
        return RC[round];
    }

    /** Bit rc(t) del LFSR x^8 + x^6 + x^5 + x^4 + 1 definido en FIPS 202. */
    private static boolean lfsr(int t) {
        int r = 1;
        for (int i = 0; i < t % 255; i++) {
            r <<= 1;
            if ((r & 0x100) != 0) {
                r ^= 0x171;
            }
        }
        return (r & 1) != 0;
    }

    public static void permute(long[] a) {
        permute(a, null);
    }

    /** Aplica Keccak-f[1600] sobre {@code a}; si {@code trace} no es null, registra cada paso. */
    public static void permute(long[] a, Permutation trace) {
        if (trace != null) {
            trace.input = a.clone();
        }
        long[] c = new long[5];
        long[] d = new long[5];
        long[] rho = new long[25];
        long[] pi = new long[25];
        for (int round = 0; round < ROUNDS; round++) {
            // θ
            for (int x = 0; x < 5; x++) {
                c[x] = a[x] ^ a[x + 5] ^ a[x + 10] ^ a[x + 15] ^ a[x + 20];
            }
            for (int x = 0; x < 5; x++) {
                d[x] = c[(x + 4) % 5] ^ Long.rotateLeft(c[(x + 1) % 5], 1);
            }
            for (int i = 0; i < 25; i++) {
                a[i] ^= d[i % 5];
            }
            if (trace != null) {
                trace.c[round] = c.clone();
                trace.d[round] = d.clone();
                trace.theta[round] = a.clone();
            }
            // ρ
            for (int i = 0; i < 25; i++) {
                rho[i] = Long.rotateLeft(a[i], ROT[i]);
            }
            if (trace != null) {
                trace.rho[round] = rho.clone();
            }
            // π: la palabra (x, y) pasa a la posición (y, 2x + 3y)
            for (int x = 0; x < 5; x++) {
                for (int y = 0; y < 5; y++) {
                    pi[y + 5 * ((2 * x + 3 * y) % 5)] = rho[x + 5 * y];
                }
            }
            if (trace != null) {
                trace.pi[round] = pi.clone();
            }
            // χ
            for (int y = 0; y < 5; y++) {
                for (int x = 0; x < 5; x++) {
                    a[x + 5 * y] = pi[x + 5 * y] ^ (~pi[(x + 1) % 5 + 5 * y] & pi[(x + 2) % 5 + 5 * y]);
                }
            }
            if (trace != null) {
                trace.chi[round] = a.clone();
            }
            // ι
            a[0] ^= RC[round];
            if (trace != null) {
                trace.iota[round] = a.clone();
            }
        }
    }

    /** Estado tras cada paso de cada una de las 24 rondas. */
    public static final class Permutation {
        public long[] input;
        public final long[][] c = new long[ROUNDS][];
        public final long[][] d = new long[ROUNDS][];
        public final long[][] theta = new long[ROUNDS][];
        public final long[][] rho = new long[ROUNDS][];
        public final long[][] pi = new long[ROUNDS][];
        public final long[][] chi = new long[ROUNDS][];
        public final long[][] iota = new long[ROUNDS][];

        public long[] output() {
            return iota[ROUNDS - 1];
        }

        /**
         * Estado tras el paso global {@code index}: 0 = entrada de la permutación; 1..120 =
         * (ronda, paso) en el orden θ, ρ, π, χ, ι de cada ronda.
         */
        public long[] stateAt(int index) {
            if (index == 0) {
                return input;
            }
            int round = (index - 1) / 5;
            switch ((index - 1) % 5) {
                case 0:
                    return theta[round];
                case 1:
                    return rho[round];
                case 2:
                    return pi[round];
                case 3:
                    return chi[round];
                default:
                    return iota[round];
            }
        }
    }
}
