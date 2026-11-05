package com.example.criptofunhash.ui;

import java.awt.Color;
import java.awt.Font;

public final class Theme {

    private Theme() {
    }

    public static final Color FRAME_BACKGROUND = new Color(30, 38, 50);
    public static final Color PANEL_BACKGROUND = new Color(20, 26, 36);
    public static final Color TEXT_LIGHT = new Color(222, 228, 238);
    public static final Color ACCENT = new Color(120, 200, 255);

    /** Colores de resaltado (fondo) para bytes y palabras. */
    public static final Color MESSAGE = new Color(190, 220, 255);
    public static final Color ONE_BIT = new Color(255, 200, 120);
    public static final Color ZEROS = new Color(224, 224, 224);
    public static final Color LENGTH = new Color(185, 235, 185);
    public static final Color PAD_END = new Color(240, 190, 230);
    public static final Color CHANGED = new Color(255, 232, 140);
    public static final Color RATE = new Color(205, 228, 255);
    public static final Color CAPACITY = new Color(255, 224, 205);
    public static final Color DIFF = new Color(255, 150, 150);

    public static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    public static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 13);
    public static final Font MONO_BOLD = new Font(Font.MONOSPACED, Font.BOLD, 13);
}
