package com.example.criptofunhash.ui;

import com.example.criptofunhash.AppInfo;
import com.example.criptofunhash.model.ClassicReport;
import com.example.criptofunhash.model.HashAlgorithm;
import com.example.criptofunhash.model.Md5;
import com.example.criptofunhash.model.Sha1;
import com.example.criptofunhash.model.Sha2Report;
import com.example.criptofunhash.model.Sha2Variant;
import com.example.criptofunhash.model.Sha3Report;
import com.example.criptofunhash.model.Sha3Variant;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.beans.PropertyVetoException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Ventana principal con ventanas internas, como CriptoRES. */
public final class MainWindow extends JFrame {

    private final JDesktopPane desktop = new JDesktopPane();
    private int opened;
    private Timer clockTimer;

    public MainWindow() {
        super(AppInfo.NAME_AND_VERSION);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                exit();
            }
        });
        setLayout(new BorderLayout());
        setIconImage(AppIcon.create(64));
        desktop.setBackground(new java.awt.Color(244, 244, 244));

        setJMenuBar(buildMenuBar());
        add(buildToolBar(), BorderLayout.NORTH);
        add(desktop, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        setMinimumSize(new Dimension(900, 600));
        setSize(1240, 880);
        setLocationRelativeTo(null);
    }

    // ---------- ventanas ----------

    private void openHash(String title, HashAlgorithm[] variants, HashAlgorithm initial) {
        show(new SimpleHashFrame(title, variants, initial));
    }

    private void openMd5() {
        openHash("MD5", new HashAlgorithm[]{Md5.INSTANCE}, Md5.INSTANCE);
    }

    private void openSha1() {
        openHash("SHA-1", new HashAlgorithm[]{Sha1.INSTANCE}, Sha1.INSTANCE);
    }

    private void openMd5Tracking() {
        show(new TrackingFrame("Seguimiento del algoritmo MD5", new HashAlgorithm[]{Md5.INSTANCE}, Md5.INSTANCE,
                (a, msg, steps) -> ClassicReport.md5(msg, steps), null));
    }

    private void openSha1Tracking() {
        show(new TrackingFrame("Seguimiento del algoritmo SHA-1", new HashAlgorithm[]{Sha1.INSTANCE}, Sha1.INSTANCE,
                (a, msg, steps) -> ClassicReport.sha1(msg, steps), null));
    }

    private void openSha2() {
        openHash("SHA-2", Sha2Variant.values(), Sha2Variant.SHA_256);
    }

    private void openSha3() {
        openHash("SHA-3", Sha3Variant.values(), Sha3Variant.SHA3_256);
    }

    private void openSha2Tracking() {
        show(new TrackingFrame("Seguimiento del algoritmo SHA-2", Sha2Variant.values(), Sha2Variant.SHA_256,
                (a, msg, steps) -> Sha2Report.build((Sha2Variant) a, msg, steps),
                m -> new Sha2Panel(m, false)));
    }

    private void openSha3Tracking() {
        show(new TrackingFrame("Seguimiento del algoritmo SHA-3", Sha3Variant.values(), Sha3Variant.SHA3_256,
                (a, msg, steps) -> Sha3Report.build((Sha3Variant) a, msg, steps),
                m -> new Sha3Panel(m, false)));
    }

    private void show(JInternalFrame frame) {
        int offset = 28 * (opened++ % 10);
        frame.setLocation(offset, offset);
        desktop.add(frame);
        frame.setVisible(true);
        try {
            frame.setSelected(true);
        } catch (PropertyVetoException ignored) {
            // la ventana no admite selección en este momento
        }
    }

    private List<JInternalFrame> frames() {
        List<JInternalFrame> list = new ArrayList<>();
        for (JInternalFrame f : desktop.getAllFrames()) {
            if (!f.isIcon()) {
                list.add(f);
            }
        }
        return list;
    }

    private void cascade() {
        int i = 0;
        for (JInternalFrame f : frames()) {
            f.setLocation(28 * i, 28 * i);
            i++;
            desktop.getDesktopManager().activateFrame(f);
        }
    }

    private void tile() {
        List<JInternalFrame> list = frames();
        if (list.isEmpty()) {
            return;
        }
        int cols = (int) Math.ceil(Math.sqrt(list.size()));
        int rows = (int) Math.ceil(list.size() / (double) cols);
        int w = desktop.getWidth() / cols;
        int h = desktop.getHeight() / rows;
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setBounds((i % cols) * w, (i / cols) * h, w, h);
        }
    }

    /**
     * Cierra la aplicación del todo: detiene el reloj, cierra las ventanas internas (cancelando sus cálculos)
     * y termina la JVM. {@code dispose()} a secas no basta: un temporizador o un cálculo en marcha mantienen
     * viva la JVM.
     */
    private void exit() {
        if (clockTimer != null) {
            clockTimer.stop();
        }
        closeAll();
        dispose();
        System.exit(0);
    }

    private void closeAll() {
        for (JInternalFrame f : desktop.getAllFrames()) {
            f.dispose();
        }
    }

    // ---------- menús, barra de botones y de estado ----------

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu archivo = menu("Archivo", KeyEvent.VK_A);
        archivo.add(item("Salir", this::exit));

        JMenu hash = menu("Funciones hash", KeyEvent.VK_F);
        hash.add(item("MD5...", this::openMd5));
        hash.add(item("SHA-1...", this::openSha1));
        hash.add(item("SHA-2 (224, 256, 384, 512)...", this::openSha2));
        hash.add(item("SHA-3 (224, 256, 384, 512)...", this::openSha3));

        JMenu seg = menu("Seguimientos", KeyEvent.VK_S);
        seg.add(item("MD5...", this::openMd5Tracking));
        seg.add(item("SHA-1...", this::openSha1Tracking));
        seg.add(item("SHA-2...", this::openSha2Tracking));
        seg.add(item("SHA-3...", this::openSha3Tracking));

        JMenu analisis = menu("Análisis", KeyEvent.VK_N);
        analisis.add(item("Resumen con todos los algoritmos...", this::openSummary));
        analisis.add(item("Merkle–Damgård vs. esponja...", this::openCompare));
        analisis.add(item("Efecto avalancha...", this::openAvalanche));

        JMenu ataques = menu("Ataques", KeyEvent.VK_Q);
        ataques.add(item("Paradoja del cumpleaños...", this::openBirthday));

        JMenu ventana = menu("Ventana", KeyEvent.VK_V);
        ventana.add(item("Cascada", this::cascade));
        ventana.add(item("Mosaico", this::tile));
        ventana.addSeparator();
        ventana.add(item("Cerrar todas", this::closeAll));

        JMenu ayuda = menu("Ayuda", KeyEvent.VK_Y);
        ayuda.add(item("Acerca de", () -> new AboutDialog(this).setVisible(true)));
        ayuda.add(item("Cómo funciona " + AppInfo.NAME, () -> new HelpDialog(this).setVisible(true)));

        bar.add(archivo);
        bar.add(hash);
        bar.add(seg);
        bar.add(analisis);
        bar.add(ataques);
        bar.add(ventana);
        bar.add(ayuda);
        return bar;
    }

    private void openBirthday() {
        show(new BirthdayFrame());
    }

    private void openSummary() {
        show(new AnalysisFrame("Resumen con todos los algoritmos", SummaryPanel::new));
    }

    private void openCompare() {
        show(new AnalysisFrame("Merkle–Damgård vs. esponja", ComparePanel::new));
    }

    private void openAvalanche() {
        show(new AnalysisFrame("Efecto avalancha", AvalanchePanel::new));
    }

    private JToolBar buildToolBar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.add(button("MD5", "Calcular el resumen con MD5 (obsoleto)", this::openMd5));
        bar.add(button("SHA-1", "Calcular el resumen con SHA-1 (obsoleto)", this::openSha1));
        bar.add(button("SHA-2", "Calcular el resumen con SHA-2", this::openSha2));
        bar.add(button("SHA-3", "Calcular el resumen con SHA-3", this::openSha3));
        bar.addSeparator(new Dimension(24, 0));
        bar.add(button("Seg. MD5", "Seguimiento paso a paso de MD5", this::openMd5Tracking));
        bar.add(button("Seg. SHA-1", "Seguimiento paso a paso de SHA-1", this::openSha1Tracking));
        bar.add(button("Seg. SHA-2", "Seguimiento paso a paso de SHA-2", this::openSha2Tracking));
        bar.add(button("Seg. SHA-3", "Seguimiento paso a paso de SHA-3", this::openSha3Tracking));
        bar.addSeparator(new Dimension(24, 0));
        bar.add(button("Resumen", "Resumen con todos los algoritmos y efecto de la codificación", this::openSummary));
        bar.add(button("MD vs. esponja", "Comparar Merkle–Damgård con la construcción de esponja", this::openCompare));
        bar.add(button("Avalancha", "Efecto avalancha al cambiar un bit", this::openAvalanche));
        bar.addSeparator(new Dimension(24, 0));
        bar.add(button("Cumpleaños", "Ataque de cumpleaños: simulación y colisiones en resúmenes truncados", this::openBirthday));
        return bar;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        bar.add(new JLabel("Bienvenido a " + AppInfo.NAME_AND_VERSION), BorderLayout.WEST);
        final JLabel clock = new JLabel();
        final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        clockTimer = new Timer(1000, e -> clock.setText(format.format(new Date())));
        clockTimer.setInitialDelay(0);
        clockTimer.start();
        bar.add(clock, BorderLayout.EAST);
        return bar;
    }

    private static JMenu menu(String text, int mnemonic) {
        JMenu menu = new JMenu(text);
        menu.setMnemonic(mnemonic);
        return menu;
    }

    private static JMenuItem item(String text, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(e -> action.run());
        return item;
    }

    private static JButton button(String text, String tooltip, Runnable action) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFont(button.getFont().deriveFont(java.awt.Font.BOLD, 12f));
        button.setFocusable(false);
        button.addActionListener(e -> action.run());
        return button;
    }
}
