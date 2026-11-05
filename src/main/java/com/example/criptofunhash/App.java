package com.example.criptofunhash;

import com.example.criptofunhash.ui.MainWindow;

import javax.swing.SwingUtilities;

public final class App {

    private App() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}
