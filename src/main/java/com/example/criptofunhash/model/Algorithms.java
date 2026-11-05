package com.example.criptofunhash.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Catálogo de algoritmos disponibles. */
public final class Algorithms {

    private static final List<HashAlgorithm> ALL;

    static {
        List<HashAlgorithm> list = new ArrayList<>();
        list.add(Md5.INSTANCE);
        list.add(Sha1.INSTANCE);
        Collections.addAll(list, Sha2Variant.values());
        Collections.addAll(list, Sha3Variant.values());
        ALL = Collections.unmodifiableList(list);
    }

    private Algorithms() {
    }

    /** Posición de SHA-256 en el catálogo: algoritmo seleccionado por defecto en las tablas. */
    public static int defaultIndex() {
        return ALL.indexOf(Sha2Variant.SHA_256);
    }

    public static List<HashAlgorithm> all() {
        return ALL;
    }

    /**
     * Calcula el resumen con la implementación de {@link MessageDigest} del JDK.
     *
     * @return el resumen, o {@code null} si esta versión de Java no ofrece el algoritmo
     *         (p. ej. SHA-3 no existe en Java 8)
     */
    public static byte[] referenceDigest(HashAlgorithm algorithm, byte[] message) {
        try {
            return MessageDigest.getInstance(algorithm.javaName()).digest(message);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
