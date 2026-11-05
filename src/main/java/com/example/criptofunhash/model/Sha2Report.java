package com.example.criptofunhash.model;

/**
 * Informe de texto del seguimiento de SHA-2, al estilo de CriptoRES.
 * <ul>
 *   <li>Nivel de bloques: para cada bloque, sus palabras, el resultado de las rondas y la actualización de H.</li>
 *   <li>Nivel de pasos: todo el cálculo (expansión W y cada ronda) pero solo del primer bloque.</li>
 * </ul>
 */
public final class Sha2Report {

    /** Con más bloques que estos, el nivel de bloques solo detalla los primeros y los últimos. */
    static final int ALL_BLOCKS_LIMIT = 16;
    static final int HEAD_BLOCKS = 4;
    static final int TAIL_BLOCKS = 2;

    private Sha2Report() {
    }

    public static String build(final Sha2Variant v, byte[] message, final boolean steps) {
        final StringBuilder sb = new StringBuilder();
        final int wb = v.wordBits();
        final int n = message.length;
        final int total = Sha2.blockCount(v, n);
        int lf = v.lengthFieldBytes();
        int zeros = Math.floorMod(-(n + 1 + lf), v.blockBytes());

        sb.append("Seguimiento del algoritmo ").append(v.displayName()).append(steps ? " (a nivel de pasos)" : " (a nivel de bloques)")
                .append('\n');
        sb.append("Palabras de ").append(wb).append(" bits, bloques de ").append(v.blockBits()).append(" bits, ")
                .append(v.rounds()).append(" rondas, resumen de ").append(v.digestBits()).append(" bits.\n\n");

        sb.append("Valor inicial (IV): derivado de las raíces cuadradas de los primeros primos\n");
        long[] iv = v.initialHash();
        for (int i = 0; i < 8; i++) {
            sb.append("H").append(i).append(" = ").append(TraceText.hex(iv[i], wb)).append(i % 4 == 3 || i == 7 ? "\n" : "   ");
        }
        sb.append('\n');
        if (steps) {
            sb.append("Constantes K0..K").append(v.rounds() - 1).append(": derivadas de las raíces cúbicas de los primeros primos\n");
            long[] k = v.roundConstants();
            for (int i = 0; i < k.length; i++) {
                sb.append(TraceText.hex(k[i], wb)).append((i % 4 == 3 || i == k.length - 1) ? "\n" : " ");
            }
            sb.append('\n');
        }

        sb.append(TraceText.line('=', 20)).append(" Relleno ").append(TraceText.line('=', 20)).append('\n');
        sb.append("Longitud del mensaje: ").append(n).append(" bytes = ").append(n * 8L).append(" bits\n");
        sb.append("Se añade un bit 1 (byte 80), ").append(zeros * 8).append(" bits a cero (").append(zeros)
                .append(" bytes) y la longitud del mensaje en ").append(lf * 8).append(" bits.\n");
        sb.append("Longitud con relleno: ").append((long) total * v.blockBytes()).append(" bytes = ").append(total)
                .append(" bloque(s) de ").append(v.blockBits()).append(" bits.\n");
        byte[] tail = Sha2.padTail(v, message);
        int tailFrom = n - n % v.blockBytes();
        if (tail.length <= 2 * v.blockBytes()) {
            sb.append("Mensaje con relleno").append(tailFrom > 0 ? " (último(s) bloque(s), a partir del byte " + tailFrom + ")" : "")
                    .append(":\n");
            TraceText.hexDump(sb, tail, 0, tail.length);
        }
        sb.append('\n');

        final int[] last = {-1};
        long[] h = Sha2.walk(v, message, i -> isTraced(i, total, steps), b -> {
            if (b.index > last[0] + 1) {
                sb.append("   [ ... se omiten los bloques ").append(last[0] + 2).append(" a ").append(b.index)
                        .append(" ... ]\n\n");
            }
            last[0] = b.index;
            block(sb, v, b, total, steps);
        });
        if (steps && total > 1) {
            sb.append("[ En el seguimiento a nivel de pasos solo se detalla el primer bloque; el resumen final\n"
                    + "  incluye los ").append(total).append(" bloques. ]\n\n");
        }

        sb.append(TraceText.line('=', 20)).append(" Resultado ").append(TraceText.line('=', 20)).append('\n');
        sb.append("H0..H7 finales: ");
        for (int i = 0; i < 8; i++) {
            sb.append(TraceText.hex(h[i], wb)).append(' ');
        }
        sb.append("\nResumen ").append(v.displayName()).append(" (").append(v.digestBits()).append(" bits):\n")
                .append(Hex.of(Sha2.digestOf(v, h)).toUpperCase(java.util.Locale.ROOT)).append('\n');
        return sb.toString();
    }

    private static boolean isTraced(int index, int total, boolean steps) {
        if (steps) {
            return index == 0;
        }
        return total <= ALL_BLOCKS_LIMIT || index < HEAD_BLOCKS || index >= total - TAIL_BLOCKS;
    }

    private static void block(StringBuilder sb, Sha2Variant v, Sha2Trace.Block b, int total, boolean steps) {
        int wb = v.wordBits();
        int rounds = v.rounds();
        long[] k = v.roundConstants();
        sb.append(TraceText.line('=', 12)).append(" Procesamiento del Bloque ").append(b.index + 1).append(" (de ")
                .append(total).append(") ").append(TraceText.line('=', 12)).append('\n');
        sb.append("Las palabras del bloque ").append(b.index + 1).append(" del mensaje son:\n");
        sb.append("(Los bytes menos significativos a la derecha)\n");
        for (int t = 0; t < 16; t++) {
            sb.append(String.format("Palabra %2d  %s = %s%n", t + 1, TraceText.binary(b.w[t], wb), TraceText.hex(b.w[t], wb)));
        }
        sb.append('\n');
        if (steps) {
            sb.append("--- Expansión del mensaje: W[t] = σ1(W[t-2]) + W[t-7] + σ0(W[t-15]) + W[t-16]  (mod 2^")
                    .append(wb).append(") ---\n");
            for (int t = 16; t < rounds; t++) {
                sb.append(String.format("W[%2d] = %s + %s + %s + %s = %s%n", t,
                        TraceText.hex(Sha2.smallSigma1(v, b.w[t - 2]), wb), TraceText.hex(b.w[t - 7], wb),
                        TraceText.hex(Sha2.smallSigma0(v, b.w[t - 15]), wb), TraceText.hex(b.w[t - 16], wb),
                        TraceText.hex(b.w[t], wb)));
            }
            sb.append("\n--- Rondas ---\n");
            sb.append("Registros iniciales:  ").append(regs(b.regs[0], wb)).append("\n\n");
            for (int t = 0; t < rounds; t++) {
                sb.append("Ronda ").append(t).append("   K[").append(t).append("] = ").append(TraceText.hex(k[t], wb))
                        .append("   W[").append(t).append("] = ").append(TraceText.hex(b.w[t], wb)).append('\n');
                sb.append("   Σ1(e) = ").append(TraceText.hex(b.sigma1[t], wb)).append("   Ch(e,f,g) = ")
                        .append(TraceText.hex(b.ch[t], wb)).append("   T1 = h + Σ1 + Ch + K + W = ")
                        .append(TraceText.hex(b.t1[t], wb)).append('\n');
                sb.append("   Σ0(a) = ").append(TraceText.hex(b.sigma0[t], wb)).append("   Maj(a,b,c) = ")
                        .append(TraceText.hex(b.maj[t], wb)).append("   T2 = Σ0 + Maj = ")
                        .append(TraceText.hex(b.t2[t], wb)).append('\n');
                sb.append("   ").append(regs(b.regs[t + 1], wb)).append("\n\n");
            }
        } else {
            sb.append("Registros tras las ").append(rounds).append(" rondas:\n").append(regs(b.regs[rounds], wb)).append("\n\n");
        }
        sb.append("--- Suma con el valor anterior de H (mod 2^").append(wb).append(") ---\n");
        for (int i = 0; i < 8; i++) {
            sb.append("H").append(i).append(" = ").append(TraceText.hex(b.hIn[i], wb)).append(" + ")
                    .append(TraceText.hex(b.regs[rounds][i], wb)).append(" = ").append(TraceText.hex(b.hOut[i], wb)).append('\n');
        }
        sb.append('\n');
    }

    private static String regs(long[] r, int wb) {
        String[] names = {"a", "b", "c", "d", "e", "f", "g", "h"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(names[i]).append('=').append(TraceText.hex(r[i], wb)).append(i < 7 ? " " : "");
        }
        return sb.toString();
    }
}
