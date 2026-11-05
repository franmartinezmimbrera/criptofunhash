package com.example.criptofunhash.ui;

import com.example.criptofunhash.model.HashAlgorithm;

import javax.swing.JComponent;

/** Vista gráfica de una familia de algoritmos que puede cambiar de variante desde fuera. */
interface VariantPanel {

    JComponent component();

    void setVariant(HashAlgorithm variant);
}
