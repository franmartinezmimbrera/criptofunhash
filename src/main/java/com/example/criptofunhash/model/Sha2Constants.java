package com.example.criptofunhash.model;

import java.math.BigInteger;

/**
 * Constantes de SHA-2 derivadas, tal y como define el estándar, de las partes fraccionarias
 * de las raíces cuadradas (valores iniciales) y cúbicas (constantes de ronda) de los primeros
 * números primos. Calcularlas evita transcribir 80 constantes a mano.
 */
final class Sha2Constants {

    /** Constantes de ronda de 64 bits (SHA-384/512). Las de 32 bits son sus 32 bits altos. */
    static final long[] K64 = new long[80];
    /** Raíces cuadradas de los primos 1..8 (SHA-512) y 9..16 (SHA-384), 64 bits. */
    static final long[] IV512 = new long[8];
    static final long[] IV384 = new long[8];

    static {
        int[] primes = firstPrimes(80);
        for (int i = 0; i < 80; i++) {
            K64[i] = iroot(BigInteger.valueOf(primes[i]).shiftLeft(192), 3).longValue();
        }
        for (int i = 0; i < 8; i++) {
            IV512[i] = iroot(BigInteger.valueOf(primes[i]).shiftLeft(128), 2).longValue();
            IV384[i] = iroot(BigInteger.valueOf(primes[i + 8]).shiftLeft(128), 2).longValue();
        }
    }

    private Sha2Constants() {
    }

    static long[] k32() {
        long[] k = new long[64];
        for (int i = 0; i < 64; i++) {
            k[i] = K64[i] >>> 32;
        }
        return k;
    }

    static long[] iv256() {
        long[] iv = new long[8];
        for (int i = 0; i < 8; i++) {
            iv[i] = IV512[i] >>> 32;
        }
        return iv;
    }

    static long[] iv224() {
        long[] iv = new long[8];
        for (int i = 0; i < 8; i++) {
            iv[i] = IV384[i] & 0xFFFFFFFFL;
        }
        return iv;
    }

    private static int[] firstPrimes(int count) {
        int[] primes = new int[count];
        int found = 0;
        for (int n = 2; found < count; n++) {
            boolean prime = true;
            for (int d = 2; d * d <= n; d++) {
                if (n % d == 0) {
                    prime = false;
                    break;
                }
            }
            if (prime) {
                primes[found++] = n;
            }
        }
        return primes;
    }

    /** Parte entera de la raíz k-ésima de n (búsqueda binaria; BigInteger.sqrt no existe en Java 8). */
    private static BigInteger iroot(BigInteger n, int k) {
        BigInteger lo = BigInteger.ZERO;
        BigInteger hi = BigInteger.ONE.shiftLeft(n.bitLength() / k + 1);
        while (lo.compareTo(hi) < 0) {
            BigInteger mid = lo.add(hi).add(BigInteger.ONE).shiftRight(1);
            if (mid.pow(k).compareTo(n) <= 0) {
                lo = mid;
            } else {
                hi = mid.subtract(BigInteger.ONE);
            }
        }
        return lo;
    }
}
