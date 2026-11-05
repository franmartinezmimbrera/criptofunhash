package com.example.criptofunhash.model;

/** Registro de un bloque de MD5 o SHA-1 (funciones hash de la familia MD4). */
public final class ClassicBlock {

    public final int index;
    public final byte[] data;
    /** Palabras del mensaje: 16 en MD5; las 80 palabras expandidas en SHA-1. */
    public long[] w;
    public long[] hIn;
    /** regs[i] = registros antes del paso i; regs[pasos] = tras el último (A..D en MD5, A..E en SHA-1). */
    public long[][] regs;
    /** Salida de la función no lineal de cada paso. */
    public long[] f;
    /** Constante de cada paso. */
    public long[] k;
    /** MD5: suma A+F+K+M antes de rotar. SHA-1: valor temporal que será el nuevo A. */
    public long[] tmp;
    /** MD5: índice de la palabra del mensaje usada en cada paso. */
    public int[] g;
    public long[] hOut;

    ClassicBlock(int index, byte[] data) {
        this.index = index;
        this.data = data;
    }
}
