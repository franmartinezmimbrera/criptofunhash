package com.example.criptofunhash.model;

import java.util.Locale;

/** Informes de texto del seguimiento de MD5 y SHA-1, al estilo de CriptoRES. */
public final class ClassicReport {

    private ClassicReport() {
    }

    public static String md5(byte[] message, boolean steps) {
        return build(true, message, steps);
    }

    public static String sha1(byte[] message, boolean steps) {
        return build(false, message, steps);
    }

    private static String build(final boolean md5, byte[] message, final boolean steps) {
        final StringBuilder sb = new StringBuilder();
        final String name = md5 ? "MD5" : "SHA-1";
        final int n = message.length;
        final int total = md5 ? Md5.blockCount(n) : Sha1.blockCount(n);
        final long[] iv = md5 ? Md5.initialHash() : Sha1.initialHash();
        final String[] regNames = md5 ? new String[]{"A", "B", "C", "D"} : new String[]{"A", "B", "C", "D", "E"};

        sb.append("Seguimiento del algoritmo ").append(name).append(steps ? " (a nivel de pasos)" : " (a nivel de bloques)")
                .append('\n');
        sb.append("Palabras de 32 bits, bloques de 512 bits, ").append(md5 ? Md5.STEPS : Sha1.STEPS).append(" pasos, resumen de ")
                .append(md5 ? 128 : 160).append(" bits.\n").append(name).append(" está OBSOLETA: no debe usarse para seguridad.\n\n");

        if (md5) {
            sb.append("Valor inicial  :");
            for (long v : iv) {
                sb.append(' ').append(littleEndianView(v));
            }
            sb.append("\n\n  Donde los bytes menos significativos de cada palabra (A,B,C,D) están a la izquierda. "
                    + "Si los representamos en el orden natural\n(bytes menos significativos a la derecha), se obtiene:\n\n");
        }
        sb.append(md5 ? "Valor inicial´ :" : "Valor inicial  :");
        for (long v : iv) {
            sb.append(' ').append(TraceText.hex(v, 32));
        }
        sb.append("\n\n");

        sb.append(TraceText.line('=', 20)).append(" Relleno ").append(TraceText.line('=', 20)).append('\n');
        int zeros = Math.floorMod(-(n + 1 + 8), 64);
        sb.append("Longitud del mensaje: ").append(n).append(" bytes = ").append(n * 8L).append(" bits\n");
        sb.append("Se añade un bit 1 (byte 80), ").append(zeros * 8).append(" bits a cero (").append(zeros)
                .append(" bytes)\ny la longitud del mensaje en 64 bits (")
                .append(md5 ? "little-endian" : "big-endian").append(").\n");
        sb.append("Longitud con relleno: ").append((long) total * 64).append(" bytes = ").append(total).append(" bloque(s) de 512 bits.\n");
        byte[] tail = md5 ? Md5.padTail(message) : Sha1.padTail(message);
        if (tail.length <= 128) {
            sb.append("Mensaje con relleno").append(n - n % 64 > 0 ? " (a partir del byte " + (n - n % 64) + ")" : "").append(":\n");
            TraceText.hexDump(sb, tail, 0, tail.length);
        }
        sb.append('\n');

        final int[] last = {-1};
        java.util.function.IntPredicate traced = i -> steps ? i == 0 : (total <= Sha2Report.ALL_BLOCKS_LIMIT
                || i < Sha2Report.HEAD_BLOCKS || i >= total - Sha2Report.TAIL_BLOCKS);
        java.util.function.Consumer<ClassicBlock> visitor = b -> {
            if (b.index > last[0] + 1) {
                sb.append("   [ ... se omiten los bloques ").append(last[0] + 2).append(" a ").append(b.index)
                        .append(" ... ]\n\n");
            }
            last[0] = b.index;
            block(sb, md5, b, total, steps, regNames);
        };
        long[] h = md5 ? Md5.walk(message, traced, visitor) : Sha1.walk(message, traced, visitor);
        if (steps && total > 1) {
            sb.append("[ En el seguimiento a nivel de pasos solo se detalla el primer bloque; el resumen final\n"
                    + "  incluye los ").append(total).append(" bloques. ]\n\n");
        }

        sb.append(TraceText.line('=', 20)).append(" Resultado ").append(TraceText.line('=', 20)).append('\n');
        byte[] digest = md5 ? Md5.digestOf(h) : Sha1.digestOf(h);
        sb.append("Registros finales: ").append(regs(h, regNames)).append('\n');
        sb.append(md5 ? "El resumen se obtiene escribiendo A, B, C, D con el byte menos significativo primero:\n"
                : "El resumen es A‖B‖C‖D‖E:\n");
        sb.append(Hex.of(digest).toUpperCase(Locale.ROOT)).append('\n');
        return sb.toString();
    }

    /** Palabra con el byte menos significativo a la izquierda (como aparece en memoria). */
    private static String littleEndianView(long v) {
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < 4; j++) {
            sb.append(String.format("%02X", (v >>> (8 * j)) & 0xFF));
        }
        return sb.toString();
    }

    private static void block(StringBuilder sb, boolean md5, ClassicBlock b, int total, boolean steps, String[] reg) {
        int n = md5 ? Md5.STEPS : Sha1.STEPS;
        sb.append(TraceText.line('=', 12)).append(" Procesamiento del Bloque ").append(b.index + 1).append(" (de ")
                .append(total).append(") ").append(TraceText.line('=', 12)).append('\n');
        sb.append("Las palabras del bloque ").append(b.index + 1).append(" del mensaje son:\n");
        sb.append(md5 ? "(Los bytes menos significativos a la derecha)\n" : "(Cada palabra son 4 bytes en orden big-endian)\n");
        for (int t = 0; t < 16; t++) {
            sb.append(String.format("Palabra %2d  %s = %s%n", t + 1, TraceText.binary(b.w[t], 32), TraceText.hex(b.w[t], 32)));
        }
        sb.append('\n');
        if (steps) {
            if (!md5) {
                sb.append("--- Expansión del mensaje: W[t] = ROTL1(W[t-3] ⊕ W[t-8] ⊕ W[t-14] ⊕ W[t-16]) ---\n");
                for (int t = 16; t < n; t++) {
                    sb.append(String.format("W[%2d] = ROTL1(%s ⊕ %s ⊕ %s ⊕ %s) = %s%n", t, TraceText.hex(b.w[t - 3], 32),
                            TraceText.hex(b.w[t - 8], 32), TraceText.hex(b.w[t - 14], 32), TraceText.hex(b.w[t - 16], 32),
                            TraceText.hex(b.w[t], 32)));
                }
                sb.append('\n');
            }
            sb.append("--- Pasos ---\nRegistros iniciales:  ").append(regs(b.regs[0], reg)).append("\n\n");
            for (int i = 0; i < n; i++) {
                if (md5) {
                    int s = Md5.shift(i);
                    sb.append(String.format("Paso %2d  (función %s, palabra M[%d], rotación %d)%n", i + 1,
                            i < 16 ? "F" : i < 32 ? "G" : i < 48 ? "H" : "I", b.g[i], s));
                    sb.append("   f = ").append(TraceText.hex(b.f[i], 32)).append("   K = ").append(TraceText.hex(b.k[i], 32))
                            .append("   A + f + K + M = ").append(TraceText.hex(b.tmp[i], 32)).append('\n');
                    sb.append("   B nuevo = B + ROTL").append(s).append("(suma) = ").append(TraceText.hex(b.regs[i + 1][1], 32)).append('\n');
                } else {
                    sb.append(String.format("Paso %2d  (función %s)%n", i, i < 20 ? "Ch" : i < 40 ? "Paridad" : i < 60 ? "Maj" : "Paridad"));
                    sb.append("   f = ").append(TraceText.hex(b.f[i], 32)).append("   K = ").append(TraceText.hex(b.k[i], 32))
                            .append("   W[").append(i).append("] = ").append(TraceText.hex(b.w[i], 32)).append('\n');
                    sb.append("   temp = ROTL5(A) + f + E + K + W = ").append(TraceText.hex(b.tmp[i], 32)).append('\n');
                }
                sb.append("   ").append(regs(b.regs[i + 1], reg)).append("\n\n");
            }
        } else {
            sb.append("Registros tras los ").append(n).append(" pasos:\n").append(regs(b.regs[n], reg)).append("\n\n");
        }
        sb.append("--- Suma con el valor anterior (mod 2^32) ---\n");
        for (int i = 0; i < reg.length; i++) {
            sb.append(reg[i]).append(" = ").append(TraceText.hex(b.hIn[i], 32)).append(" + ").append(TraceText.hex(b.regs[n][i], 32))
                    .append(" = ").append(TraceText.hex(b.hOut[i], 32)).append('\n');
        }
        sb.append('\n');
    }

    private static String regs(long[] r, String[] names) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.length; i++) {
            sb.append(names[i]).append('=').append(TraceText.hex(r[i], 32)).append(i < names.length - 1 ? " " : "");
        }
        return sb.toString();
    }
}
