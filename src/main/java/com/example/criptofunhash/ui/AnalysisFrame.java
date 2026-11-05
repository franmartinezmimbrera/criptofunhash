package com.example.criptofunhash.ui;

import com.example.criptofunhash.controller.InputModel;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import java.awt.BorderLayout;
import java.util.function.Function;

/** Ventana de análisis (resumen, comparación, avalancha) con su propio mensaje de entrada. */
final class AnalysisFrame extends JInternalFrame {

    AnalysisFrame(String title, Function<InputModel, JComponent> content) {
        super(title, true, true, true, true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        InputModel model = new InputModel();
        setLayout(new BorderLayout());
        add(new InputPanel(model), BorderLayout.NORTH);
        add(content.apply(model), BorderLayout.CENTER);
        setSize(1000, 700);
    }
}
