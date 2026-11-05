package com.example.criptofunhash.model;

import java.util.Locale;

/** Informe de texto del seguimiento de SHA-3, al estilo de CriptoRES (bloques / pasos). */
public final class Sha3Report {

    private Sha3Report() {
    }

    public static String build(final Sha3Variant v, byte[] message, final boolean steps) {
        final StringBuilder sb = new StringBuilder();
        final int n = message.length;
        final int total = Sha3.blockCount(v, n);
        final int rate = v.rateBytes();

        sb.append("Seguimiento del algoritmo ").append(v.displayName()).append(steps ? " (a nivel de pasos)" : " (a nivel de bloques)")
                .append('\n');
        sb.append("Esponja: tasa r = ").append(v.blockBits()).append(" bits (").append(rate).append(" bytes), capacidad c = ")
                .append(v.capacityBits()).append(" bits, estado de 1600 bits (5×5 palabras de 64 bits), ")
                .append(Keccak.ROUNDS).append(" rondas, resumen de ").append(v.digestBits()).append(" bits.\n\n");

        sb.append(TraceText.line('=', 20)).append(" Relleno ").append(TraceText.line('=', 20)).append('\n');
        sb.append("Longitud del mensaje: ").append(n).append(" bytes.\n");
        int padBytes = rate - n % rate;
        sb.append("Se añade el sufijo SHA-3 \"01\" y el relleno pad10*1 hasta completar un múltiplo de ").append(rate)
                .append(" bytes: ").append(padBytes).append(" byte(s) de relleno (")
                .append(padBytes == 1 ? "86" : "06 00 ... 00 80").append(").\n");
        sb.append("Longitud con relleno: ").append((long) total * rate).append(" bytes = ").append(total).append(" bloque(s).\n");
        byte[] tail = Sha3.padTail(v, message);
        if (tail.length <= 2 * rate) {
            sb.append("Mensaje con relleno").append(n - n % rate > 0 ? " (a partir del byte " + (n - n % rate) + ")" : "").append(":\n");
            TraceText.hexDump(sb, tail, 0, tail.length);
        }
        sb.append("Estado inicial: 25 palabras a cero.\n\n");

        final int[] last = {-1};
        long[] state = Sha3.walk(v, message, i -> steps ? i == 0 : (total <= Sha2Report.ALL_BLOCKS_LIMIT
                || i < Sha2Report.HEAD_BLOCKS || i >= total - Sha2Report.TAIL_BLOCKS), b -> {
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

        sb.append(TraceText.line('=', 20)).append(" Salida (squeeze) ").append(TraceText.line('=', 20)).append('\n');
        sb.append("Estado final:\n").append(grid(state));
        sb.append("\nResumen = primeros ").append(v.digestBits() / 8).append(" bytes del estado (palabras en little-endian):\n")
                .append(Hex.of(Sha3.digestOf(v, state)).toUpperCase(Locale.ROOT)).append('\n');
        return sb.toString();
    }

    private static void block(StringBuilder sb, Sha3Variant v, Sha3Trace.Block b, int total, boolean steps) {
        int rateLanes = v.rateBytes() / 8;
        sb.append(TraceText.line('=', 12)).append(" Absorción del Bloque ").append(b.index + 1).append(" (de ").append(total)
                .append(") ").append(TraceText.line('=', 12)).append('\n');
        sb.append("Las ").append(rateLanes).append(" palabras del bloque ").append(b.index + 1)
                .append(" (bytes en orden little-endian) son:\n");
        for (int i = 0; i < rateLanes; i++) {
            long lane = b.afterXor[i] ^ b.before[i];
            sb.append(String.format("Palabra %2d  %s = %s%n", i, TraceText.binary(lane, 64), TraceText.hex(lane, 64)));
        }
        sb.append("\nEstado antes del XOR:\n").append(grid(b.before));
        sb.append("\nEstado tras el XOR con el bloque (primeras ").append(rateLanes).append(" palabras):\n").append(grid(b.afterXor));
        sb.append('\n');
        if (steps) {
            Keccak.Permutation p = b.permutation;
            for (int r = 0; r < Keccak.ROUNDS; r++) {
                sb.append("--- Ronda ").append(r).append(" ---\n");
                sb.append("θ (theta): C[x] = XOR de la columna;  D[x] = C[x-1] ⊕ ROTL1(C[x+1]);  A[x,y] ⊕= D[x]\n");
                for (int x = 0; x < 5; x++) {
                    sb.append("  C[").append(x).append("] = ").append(TraceText.hex(p.c[r][x], 64)).append("   D[").append(x)
                            .append("] = ").append(TraceText.hex(p.d[r][x], 64)).append('\n');
                }
                sb.append(grid(p.theta[r]));
                sb.append("ρ (rho): rotación de cada palabra\n").append(grid(p.rho[r]));
                sb.append("π (pi): las palabras cambian de posición\n").append(grid(p.pi[r]));
                sb.append("χ (chi): A[x] ⊕= (¬A[x+1] ∧ A[x+2]) en cada fila\n").append(grid(p.chi[r]));
                sb.append("ι (iota): A[0,0] ⊕= RC[").append(r).append("] = ").append(TraceText.hex(Keccak.roundConstant(r), 64))
                        .append('\n').append(grid(p.iota[r])).append('\n');
            }
        }
        sb.append("Estado tras Keccak-f[1600]:\n").append(grid(b.after)).append('\n');
    }

    /** Estado como 5 filas (y) de 5 palabras (x). */
    private static String grid(long[] state) {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < 5; y++) {
            sb.append("  y=").append(y).append(": ");
            for (int x = 0; x < 5; x++) {
                sb.append(TraceText.hex(state[x + 5 * y], 64)).append(x < 4 ? " " : "");
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
