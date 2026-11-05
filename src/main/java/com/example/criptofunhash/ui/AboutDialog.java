package com.example.criptofunhash.ui;

import com.example.criptofunhash.AppInfo;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Window;
import java.net.URL;

public final class AboutDialog extends JDialog {

    private static final String[] DEVELOPERS = {
            "Francisco J. Martínez Mimbrera",
            "Manuel J. Lucena López",
            "Luis Gonzaga Pérez Cordón",
            "Cristóbal J. Carmona del Jesus"
    };

    public AboutDialog(Window owner) {
        super(owner, "Acerca de", ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(Color.WHITE);
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // 1. Nombre y versión de la aplicación
        JLabel title = new JLabel(AppInfo.NAME_AND_VERSION, SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setBackground(Color.WHITE);

        // 2. Escudo de la Universidad de Jaén
        JLabel logo = loadLogo(130);
        if (logo != null) {
            center.add(logo, BorderLayout.NORTH);
        }

        // 3. Asignatura y responsables
        JPanel body = new JPanel(new GridLayout(0, 1, 4, 4));
        body.setBackground(Color.WHITE);
        body.add(new JLabel("Asignatura: Seguridad en Tecnologías de la Información", SwingConstants.CENTER));
        JLabel university = new JLabel("Universidad de Jaén", SwingConstants.CENTER);
        university.setFont(university.getFont().deriveFont(Font.BOLD));
        body.add(university);
        body.add(new JLabel(" "));

        JLabel developersTitle = new JLabel("Desarrollado por:", SwingConstants.CENTER);
        developersTitle.setFont(developersTitle.getFont().deriveFont(Font.BOLD));
        body.add(developersTitle);
        for (String developer : DEVELOPERS) {
            body.add(new JLabel(developer, SwingConstants.CENTER));
        }
        center.add(body, BorderLayout.CENTER);

        // 4. Aviso de uso
        JLabel notice = new JLabel("<html><div style='text-align:center; width:340px'>"
                + "<b>Software libre de uso educativo.</b> Puede utilizarse, copiarse y distribuirse sin permiso "
                + "de los autores para fines exclusivamente educativos y sin ánimo de lucro. "
                + "Se ofrece «tal cual», sin garantía de ningún tipo.<br><br>"
                + "<span style='font-size:90%'>El escudo de la Universidad de Jaén es propiedad de la Universidad "
                + "de Jaén y no está incluido en esta cesión de uso.</span></div></html>", SwingConstants.CENTER);
        notice.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        center.add(notice, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /** Escudo de la Universidad de Jaén (recurso del jar), escalado a la altura dada; null si no está. */
    private static JLabel loadLogo(int height) {
        URL url = AboutDialog.class.getResource("/images/logo-uja.png");
        if (url == null) {
            return null;
        }
        ImageIcon icon = new ImageIcon(url);
        if (icon.getIconHeight() <= 0) {
            return null;
        }
        int width = icon.getIconWidth() * height / icon.getIconHeight();
        Image scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new JLabel(new ImageIcon(scaled), SwingConstants.CENTER);
    }
}
