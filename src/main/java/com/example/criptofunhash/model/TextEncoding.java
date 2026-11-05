package com.example.criptofunhash.model;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;

/** Cómo se convierte el texto escrito por el usuario en los bytes que entran a la función hash. */
public enum TextEncoding {
    UTF_8("UTF-8", StandardCharsets.UTF_8),
    LATIN_1("ISO-8859-1 (Latin-1)", StandardCharsets.ISO_8859_1),
    UTF_16BE("UTF-16 (big-endian)", StandardCharsets.UTF_16BE),
    US_ASCII("US-ASCII", StandardCharsets.US_ASCII),
    HEX("Hexadecimal (bytes)", null);

    private final String label;
    private final Charset charset;

    TextEncoding(String label, Charset charset) {
        this.label = label;
        this.charset = charset;
    }

    public boolean isHex() {
        return charset == null;
    }

    /** @throws IllegalArgumentException en modo hexadecimal si el texto no es hexadecimal válido */
    public byte[] encode(String text) {
        if (isHex()) {
            return Hex.parse(text);
        }
        return text.getBytes(charset);
    }

    /** ¿Se puede representar todo el texto sin perder caracteres (sustituidos por '?')? */
    public boolean canEncode(String text) {
        if (isHex()) {
            return true;
        }
        CharsetEncoder encoder = charset.newEncoder();
        return encoder.canEncode(text);
    }

    @Override
    public String toString() {
        return label;
    }
}
