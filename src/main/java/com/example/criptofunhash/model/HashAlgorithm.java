package com.example.criptofunhash.model;

/**
 * Una función hash disponible en la herramienta. Para añadir una nueva basta con implementar
 * esta interfaz y registrarla en {@link Algorithms}.
 */
public interface HashAlgorithm {

    String displayName();

    /** Familia: "SHA-2", "SHA-3"... */
    String family();

    int digestBits();

    byte[] digest(byte[] message);

    /** Bits de mensaje que procesa cada invocación de la función interna (bloque o "rate"). */
    int blockBits();

    /** Tamaño del estado interno en bits. */
    int stateBits();

    int wordBits();

    int rounds();

    /** Nombre que usa {@link java.security.MessageDigest} para este algoritmo. */
    String javaName();
}
