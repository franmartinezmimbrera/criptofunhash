package com.example.criptofunhash.controller;

import com.example.criptofunhash.model.TextEncoding;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Mensaje de entrada compartido por todas las pestañas: un texto con su codificación, o el
 * contenido de un fichero. Avisa a los observadores cuando cambia.
 */
public final class InputModel {

    /** Por encima de este tamaño no se genera el paso a paso (solo el resumen). */
    public static final int MAX_TRACE_BYTES = 4096;
    private static final long MAX_FILE_BYTES = 512L * 1024 * 1024;

    private final List<Runnable> listeners = new ArrayList<>();
    private String text = "Hola mundo";
    private TextEncoding encoding = TextEncoding.UTF_8;
    private byte[] fileBytes;
    private String fileName;
    private byte[] bytes = new byte[0];
    private String error;

    public InputModel() {
        recompute();
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public String getText() {
        return text;
    }

    public TextEncoding getEncoding() {
        return encoding;
    }

    public byte[] getBytes() {
        return bytes;
    }

    /** Mensaje de error (p. ej. hexadecimal mal formado) o {@code null}. */
    public String getError() {
        return error;
    }

    public boolean isFile() {
        return fileBytes != null;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isTraceable() {
        return error == null && bytes.length <= MAX_TRACE_BYTES;
    }

    /** ¿Se perderían caracteres al codificar el texto (se sustituirían por '?')? */
    public boolean isLossy() {
        return !isFile() && !encoding.canEncode(text);
    }

    public void setText(String text) {
        this.text = text;
        recompute();
    }

    public void setEncoding(TextEncoding encoding) {
        this.encoding = encoding;
        recompute();
    }

    public void loadFile(File file) throws IOException {
        if (file.length() > MAX_FILE_BYTES) {
            throw new IOException("El fichero es demasiado grande (máximo " + (MAX_FILE_BYTES >> 20) + " MB)");
        }
        this.fileBytes = Files.readAllBytes(file.toPath());
        this.fileName = file.getName();
        recompute();
    }

    /** Usa unos bytes ya leídos de un fichero (evita leerlo otra vez). */
    public void loadBytes(String name, byte[] data) {
        this.fileBytes = data;
        this.fileName = name;
        recompute();
    }

    /** Cambia texto y codificación con una sola notificación; sale del modo fichero. */
    public void setTextAndEncoding(String text, TextEncoding encoding) {
        this.text = text;
        this.encoding = encoding;
        this.fileBytes = null;
        this.fileName = null;
        recompute();
    }

    public void clearFile() {
        this.fileBytes = null;
        this.fileName = null;
        recompute();
    }

    private void recompute() {
        error = null;
        if (fileBytes != null) {
            bytes = fileBytes;
        } else {
            try {
                bytes = encoding.encode(text);
            } catch (IllegalArgumentException e) {
                bytes = new byte[0];
                error = e.getMessage();
            }
        }
        for (Runnable listener : new ArrayList<>(listeners)) {
            listener.run();
        }
    }
}
