package com.example.criptofunhash.ui;

import com.example.criptofunhash.model.Algorithms;
import com.example.criptofunhash.model.Birthday;
import com.example.criptofunhash.model.HashAlgorithm;
import com.example.criptofunhash.model.Hex;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutionException;

/** Simulación de la paradoja del cumpleaños y del ataque de cumpleaños sobre un hash truncado. */
final class BirthdayFrame extends JInternalFrame {

    private static final String DAYS = "Paradoja del cumpleaños (365 días)";
    private static final String HASH = "Colisión en un resumen truncado";

    private final JComboBox<String> modeBox = new JComboBox<>(new String[]{DAYS, HASH});
    private final JComboBox<HashAlgorithm> algorithmBox = new JComboBox<>(Algorithms.all().toArray(new HashAlgorithm[0]));
    private final JSpinner bitsSpinner = new JSpinner(new SpinnerNumberModel(24, 4, 40, 1));
    private final JSpinner iterationsSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 5000, 1));
    private final JButton start = new JButton("Simular");
    private final JButton stop = new JButton("Detener");
    private final JCheckBox viewIterations = new JCheckBox("Ver cada iteración");
    private final JCheckBox viewBirthdays = new JCheckBox("Ver cada cumpleaños");
    private final JButton next = new JButton("Siguiente");
    private final JButton finish = new JButton("Finalizar");
    private final JLabel message = new JLabel(" ");
    private final CalendarPanel calendar = new CalendarPanel();
    private final JTextArea detail = Ui.monoArea(6);
    private final BarsPanel bars = new BarsPanel();
    private final JLabel stats = new JLabel(" ");
    private final CardLayout cards = new CardLayout();
    private final JPanel top = new JPanel(cards);
    private final List<Integer> attempts = new ArrayList<>();
    private SwingWorker<Void, Birthday.Round> worker;

    // Estado del modo paso a paso (solo cumpleaños)
    private boolean stepping;
    private int stepTotal;
    private double stepExpected;
    private Random stepRandom;
    private Birthday.Round stepRound;
    private int stepShown;

    BirthdayFrame() {
        super("Simulación de la paradoja del cumpleaños", true, true, true, true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            public void internalFrameClosed(InternalFrameEvent e) {
                stepping = false;
                if (worker != null) {
                    worker.cancel(true);
                }
            }
        });

        algorithmBox.setSelectedItem(Algorithms.all().get(Algorithms.defaultIndex()));
        modeBox.addActionListener(e -> modeChanged());
        start.addActionListener(e -> simulate());
        stop.addActionListener(e -> {
            if (worker != null) {
                worker.cancel(true);
            }
        });
        stop.addActionListener(e -> endStepping("Simulación detenida."));
        viewIterations.addActionListener(e -> modeChanged());
        next.addActionListener(e -> advance());
        finish.addActionListener(e -> finishAll());
        stop.setEnabled(false);
        next.setEnabled(false);
        finish.setEnabled(false);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        row1.add(new JLabel("Simulación:"));
        row1.add(modeBox);
        row1.add(new JLabel("Algoritmo:"));
        row1.add(algorithmBox);
        row1.add(new JLabel("Bits comparados:"));
        row1.add(bitsSpinner);
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        row2.add(new JLabel("Iteraciones (1–5000):"));
        row2.add(iterationsSpinner);
        row2.add(viewIterations);
        row2.add(viewBirthdays);
        row2.add(start);
        row2.add(next);
        row2.add(finish);
        row2.add(stop);
        JPanel controls = new JPanel(new java.awt.GridLayout(2, 1));
        controls.add(row1);
        controls.add(row2);

        detail.setLineWrap(true);
        JPanel calendarCard = new JPanel(new BorderLayout());
        calendarCard.add(calendar, BorderLayout.CENTER);
        message.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        message.setFont(Theme.TITLE_FONT);
        calendarCard.add(message, BorderLayout.SOUTH);
        top.add(calendarCard, DAYS);
        top.add(new JScrollPane(detail), HASH);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bars);
        split.setResizeWeight(0.5);

        JButton close = new JButton("Cerrar");
        close.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        bottom.add(stats, BorderLayout.CENTER);
        JPanel closeRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        closeRow.add(close);
        bottom.add(closeRow, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(controls, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        setSize(900, 700);
        modeChanged();
    }

    private boolean hashMode() {
        return HASH.equals(modeBox.getSelectedItem());
    }

    private void modeChanged() {
        boolean hash = hashMode();
        algorithmBox.setEnabled(hash);
        bitsSpinner.setEnabled(hash);
        viewIterations.setEnabled(!hash);
        viewBirthdays.setEnabled(!hash && viewIterations.isSelected());
        cards.show(top, hash ? HASH : DAYS);
        iterationsSpinner.setValue(10);
        reset();
        stats.setText(hash
                ? "Se generan mensajes distintos hasta que dos resúmenes coinciden en sus primeros n bits. "
                + "Se esperan ≈ 1,25·2^(n/2) intentos."
                : "Se generan cumpleaños aleatorios hasta que dos personas coinciden. Con 23 personas ya hay un 50,7 % "
                + "de probabilidad; se esperan ≈ 24,6 intentos.");
    }

    private void reset() {
        attempts.clear();
        calendar.setRound(null);
        detail.setText("");
        message.setText(" ");
        bars.setData(attempts, 0, ((Number) iterationsSpinner.getValue()).intValue());
    }

    private void simulate() {
        final boolean hash = hashMode();
        final int iterations = ((Number) iterationsSpinner.getValue()).intValue();
        final int bits = ((Number) bitsSpinner.getValue()).intValue();
        final HashAlgorithm algorithm = (HashAlgorithm) algorithmBox.getSelectedItem();
        if (hash && bits > algorithm.digestBits()) {
            JOptionPane.showMessageDialog(this, "El resumen de " + algorithm.displayName() + " solo tiene "
                    + algorithm.digestBits() + " bits.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        reset();
        final double space = hash ? Math.pow(2, bits) : 365;
        final double expected = Birthday.expectedAttempts(space);
        if (!hash && viewIterations.isSelected()) {
            startStepping(iterations, expected);
            return;
        }
        start.setEnabled(false);
        stop.setEnabled(true);
        worker = new SwingWorker<Void, Birthday.Round>() {
            @Override
            protected Void doInBackground() {
                Random random = new Random();
                for (int i = 0; i < iterations && !isCancelled(); i++) {
                    publish(hash ? Birthday.hash(algorithm, bits, random) : Birthday.days(random, 365));
                }
                return null;
            }

            @Override
            protected void process(List<Birthday.Round> rounds) {
                for (Birthday.Round r : rounds) {
                    attempts.add(r.attempts);
                    if (hash) {
                        showCollision(algorithm, bits, attempts.size(), r);
                    } else {
                        calendar.setRound(r);
                    }
                }
                bars.setData(attempts, expected, iterations);
                showStats(expected, iterations);
            }

            @Override
            protected void done() {
                start.setEnabled(true);
                stop.setEnabled(false);
                try {
                    get();
                } catch (ExecutionException e) {
                    JOptionPane.showMessageDialog(BirthdayFrame.this, String.valueOf(e.getCause()), "Error",
                            JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException | java.util.concurrent.CancellationException e) {
                    // simulación detenida por el usuario
                }
            }
        };
        worker.execute();
    }

    // ---------- modo paso a paso (como «¿Desea ver cada una de las N iteraciones?» de CriptoRES) ----------

    private void startStepping(int iterations, double expected) {
        stepping = true;
        stepTotal = iterations;
        stepExpected = expected;
        stepRandom = new Random();
        stepRound = null;
        start.setEnabled(false);
        stop.setEnabled(true);
        next.setEnabled(true);
        finish.setEnabled(true);
        message.setText("Pulse «Siguiente» para empezar la iteración 1 de " + iterations + ".");
    }

    private void advance() {
        if (!stepping) {
            return;
        }
        if (stepRound == null || stepShown >= stepRound.days.length) {
            stepRound = Birthday.days(stepRandom, 365);
            stepShown = viewBirthdays.isSelected() ? 1 : stepRound.days.length;
        } else {
            stepShown++;
        }
        calendar.setRound(stepRound, stepShown);
        int iteration = attempts.size() + 1;
        int[] md = CalendarPanel.monthAndDay(stepRound.days[stepShown - 1]);
        if (stepShown < stepRound.days.length) {
            message.setText("Iteración " + iteration + " de " + stepTotal + " · Anotado cumpleaños (Mes-Día): "
                    + (md[0] + 1) + "-" + md[1] + "  (persona " + stepShown + ")");
            return;
        }
        attempts.add(stepRound.attempts);
        bars.setData(attempts, stepExpected, stepTotal);
        showStats(stepExpected, stepTotal);
        String found = "Iteración " + iteration + " de " + stepTotal + " · ¡Coincidencia! Las personas "
                + (stepRound.first + 1) + " y " + (stepRound.second + 1) + " cumplen el " + (md[0] + 1) + "-" + md[1]
                + " (Mes-Día) tras " + stepRound.attempts + " intentos.";
        if (attempts.size() >= stepTotal) {
            endStepping(found + " Fin de la simulación.");
        } else {
            message.setText(found + " «Siguiente» para continuar.");
        }
    }

    /** Completa de golpe las iteraciones que faltan. */
    private void finishAll() {
        if (!stepping) {
            return;
        }
        Birthday.Round last = stepRound;
        while (attempts.size() < stepTotal) {
            if (stepRound != null && stepShown < stepRound.days.length) {
                attempts.add(stepRound.attempts);   // la iteración que se estaba viendo
                last = stepRound;
                stepRound = null;
                continue;
            }
            last = Birthday.days(stepRandom, 365);
            attempts.add(last.attempts);
            stepRound = null;
        }
        calendar.setRound(last);
        bars.setData(attempts, stepExpected, stepTotal);
        showStats(stepExpected, stepTotal);
        endStepping("Simulación completada: " + stepTotal + " iteraciones.");
    }

    private void endStepping(String text) {
        if (worker != null) {
            worker.cancel(true);
        }
        boolean wasStepping = stepping;
        stepping = false;
        stepRound = null;
        start.setEnabled(true);
        stop.setEnabled(false);
        next.setEnabled(false);
        finish.setEnabled(false);
        if (wasStepping || !text.startsWith("Simulación detenida")) {
            message.setText(text);
        }
    }

    private void showStats(double expected, int iterations) {
        double sum = 0;
        int min = Integer.MAX_VALUE;
        int max = 0;
        for (int a : attempts) {
            sum += a;
            min = Math.min(min, a);
            max = Math.max(max, a);
        }
        stats.setText(String.format(Locale.getDefault(),
                "Iteraciones: %d de %d   ·   Media de intentos: %.2f   ·   Teórico ≈ %.2f   ·   Mín: %d   ·   Máx: %d",
                attempts.size(), iterations, sum / attempts.size(), expected, min, max));
    }

    private void showCollision(HashAlgorithm a, int bits, int iteration, Birthday.Round r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Iteración ").append(iteration).append(": ");
        if (r.aborted) {
            sb.append("se alcanzó el límite de intentos sin encontrar colisión.");
        } else {
            sb.append("colisión de ").append(bits).append(" bits de ").append(a.displayName()).append(" tras ")
                    .append(r.attempts).append(" mensajes.\n\n");
            sb.append("Mensaje A (nº ").append(r.first + 1).append("): ").append(Hex.of(r.messageA)).append('\n');
            sb.append("Mensaje B (nº ").append(r.second + 1).append("): ").append(Hex.of(r.messageB)).append("\n\n");
            sb.append("Resumen A: ").append(Hex.of(r.digestA)).append('\n');
            sb.append("Resumen B: ").append(Hex.of(r.digestB)).append("\n\n");
            sb.append("Coinciden los primeros ").append(bits).append(" bits del resumen (").append(Long.toHexString(r.key))
                    .append(" en hexadecimal), pero los mensajes son distintos: es una colisión parcial.");
        }
        detail.setText(sb.toString());
        detail.setCaretPosition(0);
    }
}
