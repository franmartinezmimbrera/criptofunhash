package com.example.criptofunhash.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Paradoja del cumpleaños: intentos necesarios hasta la primera coincidencia. */
public final class Birthday {

    /** Seguridad: tope de intentos por iteración para no bloquear la aplicación. */
    private static final long MAX_ATTEMPTS = 60_000_000L;

    private Birthday() {
    }

    /** Resultado de una iteración. */
    public static final class Round {
        /** Intentos hasta la primera coincidencia (incluido el que coincide). */
        public int attempts;
        /** Modo cumpleaños: día del año (0..364) de cada persona, en orden de llegada. */
        public int[] days;
        /** Posiciones (desde 0) de los dos intentos que coinciden. */
        public int first;
        public int second;
        /** Modo hash: los dos mensajes distintos y sus resúmenes completos. */
        public byte[] messageA;
        public byte[] messageB;
        public byte[] digestA;
        public byte[] digestB;
        /** Modo hash: valor común de los bits comparados. */
        public long key;
        /** Modo hash: se alcanzó el tope de intentos sin encontrar colisión. */
        public boolean aborted;
    }

    /** Una iteración clásica: se generan cumpleaños aleatorios hasta que dos coinciden. */
    public static Round days(Random random, int daysInYear) {
        int[] seen = new int[daysInYear];
        java.util.Arrays.fill(seen, -1);
        int[] order = new int[daysInYear + 1];
        Round round = new Round();
        for (int i = 0; i <= daysInYear; i++) {
            int day = random.nextInt(daysInYear);
            order[i] = day;
            if (seen[day] >= 0) {
                round.attempts = i + 1;
                round.first = seen[day];
                round.second = i;
                round.days = java.util.Arrays.copyOf(order, i + 1);
                return round;
            }
            seen[day] = i;
        }
        throw new IllegalStateException("imposible: el principio del palomar garantiza una coincidencia");
    }

    /**
     * Ataque de cumpleaños sobre un resumen truncado: busca dos mensajes distintos cuyos primeros
     * {@code bits} bits del resumen coinciden.
     */
    public static Round hash(HashAlgorithm algorithm, int bits, Random random) {
        if (bits < 1 || bits > 62 || bits > algorithm.digestBits()) {
            throw new IllegalArgumentException("Número de bits no válido: " + bits);
        }
        byte[] salt = new byte[8];
        random.nextBytes(salt);
        Map<Long, Long> seen = new HashMap<>();
        Round round = new Round();
        for (long i = 0; i < MAX_ATTEMPTS; i++) {
            if ((i & 0xFFF) == 0 && Thread.currentThread().isInterrupted()) {
                break;
            }
            byte[] message = message(salt, i);
            byte[] digest = algorithm.digest(message);
            long key = prefix(digest, bits);
            Long previous = seen.get(key);
            if (previous != null) {
                round.attempts = (int) (i + 1);
                round.first = previous.intValue();
                round.second = (int) i;
                round.messageA = message(salt, previous);
                round.messageB = message;
                round.digestA = algorithm.digest(round.messageA);
                round.digestB = digest;
                round.key = key;
                return round;
            }
            seen.put(key, i);
        }
        round.aborted = true;
        round.attempts = (int) MAX_ATTEMPTS;
        return round;
    }

    /** Mensaje número {@code counter} de una ronda: sal aleatoria de 8 bytes + contador de 8 bytes. */
    static byte[] message(byte[] salt, long counter) {
        byte[] m = new byte[16];
        System.arraycopy(salt, 0, m, 0, 8);
        for (int i = 0; i < 8; i++) {
            m[15 - i] = (byte) (counter >>> (8 * i));
        }
        return m;
    }

    /** Primeros {@code bits} bits del resumen como número. */
    public static long prefix(byte[] digest, int bits) {
        long key = 0;
        for (int i = 0; i < bits; i++) {
            key = (key << 1) | ((digest[i / 8] >> (7 - i % 8)) & 1);
        }
        return key;
    }

    /** Intentos esperados hasta la primera coincidencia entre {@code n} valores: ≈ √(πn/2) + 2/3. */
    public static double expectedAttempts(double n) {
        return Math.sqrt(Math.PI * n / 2) + 2.0 / 3.0;
    }

    /** Probabilidad de que entre {@code people} personas haya al menos dos con el mismo cumpleaños. */
    public static double probability(int people, int daysInYear) {
        double none = 1;
        for (int i = 0; i < people; i++) {
            none *= (daysInYear - i) / (double) daysInYear;
        }
        return 1 - none;
    }
}
