package com.example.criptofunhash.ui;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;

public final class HelpDialog extends JDialog {

    private static final String TEXT = "<html><body style='font-family: sans-serif; padding: 12px;'>"
            + "<h2>¿Cómo funciona CriptoFunHash?</h2>"
            + "<p>El programa sigue el esquema de CriptoRES: una ventana principal con ventanas internas "
            + "que se abren desde los menús o desde la barra de botones.</p>"
            + "<ul>"
            + "<li><b>Funciones hash</b> (MD5, SHA-1, SHA-2, SHA-3): ventana sencilla. Elige el algoritmo, escribe una "
            + "cadena de texto (con su codificación) o un fichero y pulsa <i>Transformar</i>.</li>"
            + "<li><b>Seguimientos</b> (MD5, SHA-1, SHA-2, SHA-3): igual, pero además muestra el cálculo. "
            + "<i>A nivel de bloques</i> enseña los resultados de cada bloque; <i>a nivel de pasos</i> detalla todos "
            + "los pasos del primer bloque (relleno, expansión W y rondas a…h en SHA-2; θ, ρ, π, χ, ι en SHA-3). "
            + "La pestaña <i>Seguimiento del Algoritmo</i> contiene el texto (se puede guardar o imprimir) y la "
            + "pestaña <i>Vista gráfica</i> lo muestra con colores, tablas y el estado 5×5 de Keccak.</li>"
            + "<li>MD5 y SHA-1 están <b>obsoletos</b> (colisiones reales desde 2004 y 2017): se incluyen para compararlos con SHA-2 y SHA-3. "
            + "Su seguimiento es solo en texto.</li>"
            + "<li><b>Análisis</b>: resumen con todos los algoritmos y efecto de la codificación (UTF-8, Latin-1…), "
            + "comparación Merkle–Damgård vs. esponja y efecto avalancha (cuántos bits del resumen cambian al "
            + "modificar un bit; lo deseable es cerca del 50 %).</li>"
            + "<li><b>Ataques</b>: simulación de la paradoja del cumpleaños (365 días) y ataque de cumpleaños real sobre el "
            + "resumen truncado a n bits de cualquier algoritmo: cuesta ≈ 2<sup>n/2</sup> intentos, no 2<sup>n</sup>.</li>"
            + "</ul>"
            + "<p>Los resúmenes del paso a paso se comprueban contra <code>java.security.MessageDigest</code> "
            + "cuando está disponible. La vista gráfica solo se genera para mensajes de hasta 4096 bytes.</p>"
            + "</body></html>";

    public HelpDialog(Window owner) {
        super(owner, "Cómo funciona CriptoFunHash", ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout());
        JEditorPane editorPane = new JEditorPane("text/html", TEXT);
        editorPane.setEditable(false);
        add(new JScrollPane(editorPane), BorderLayout.CENTER);
        setSize(new Dimension(600, 500));
        setLocationRelativeTo(owner);
    }
}
