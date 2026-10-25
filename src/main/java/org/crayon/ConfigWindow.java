package org.crayon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** The configuration page: target list, Launch, display toggles and keybinds. */
final class ConfigWindow {

    private static final int CONTENT_WIDTH = 324;
    private static final int CONTENT_HEIGHT = 520;

    private final JFrame frame;

    ConfigWindow(Main app, boolean hotkeysAvailable) {
        frame = new JFrame("CrayonNavConfig");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setIconImages(Icons.windowIcons());

        JPanel panel = new JPanel(null);
        panel.setPreferredSize(new Dimension(CONTENT_WIDTH, CONTENT_HEIGHT));
        frame.setContentPane(panel);

        JLabel titleLabel = new JLabel("Crayon Nav Assist", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 22));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setIcon(Icons.logo(32));
        titleLabel.setIconTextGap(10);
        titleLabel.setBounds(10, 6, 304, 36);
        panel.add(titleLabel);

        JLabel subtitleLabel = new JLabel("Paste list below:");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        subtitleLabel.setForeground(Color.WHITE);
        subtitleLabel.setBounds(15, 45, 304, 25);
        panel.add(subtitleLabel);

        JTextArea textArea = new JTextArea(app.initialTargetsText());
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        textArea.setForeground(Color.WHITE);
        textArea.setCaretColor(Color.WHITE);
        textArea.setMargin(new Insets(8, 8, 8, 8));
        textArea.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBounds(15, 75, 294, 180);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scrollPane);

        JButton launchButton = new JButton("Launch");
        launchButton.setFont(new Font("SansSerif", Font.PLAIN, 18));
        launchButton.setBackground(new Color(33, 115, 176));
        launchButton.setForeground(Color.WHITE);
        launchButton.setFocusPainted(false);
        launchButton.setBounds(15, 265, 294, 35);
        launchButton.addActionListener(e -> app.launch(textArea.getText()));
        panel.add(launchButton);

        //=====================================================
        // Display toggles
        //=====================================================
        JCheckBox chunkCheckBox = checkBox("Target as Chunks", app.targetChunks(), panel);
        chunkCheckBox.setBounds(15, 305, 294, 25);
        chunkCheckBox.addActionListener(e -> app.setTargetChunks(chunkCheckBox.isSelected()));
        panel.add(chunkCheckBox);

        JCheckBox compactCheckBox = checkBox("Compact Overlay", app.compactOverlay(), panel);
        compactCheckBox.setToolTipText("Borderless, see-through overlay. Drag it to move, right-click for options.");
        compactCheckBox.setBounds(15, 330, 294, 25);
        panel.add(compactCheckBox);

        int opacityPercent = Math.round(app.overlayOpacity() * 100);
        JLabel opacityLabel = new JLabel("Opacity " + opacityPercent + "%");
        opacityLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        opacityLabel.setForeground(Color.WHITE);
        opacityLabel.setBounds(40, 360, 100, 25);
        panel.add(opacityLabel);

        JSlider opacitySlider = new JSlider(Math.round(Settings.MIN_OPACITY * 100), 100, opacityPercent);
        opacitySlider.setBounds(140, 360, 169, 25);
        opacitySlider.addChangeListener(e -> {
            opacityLabel.setText("Opacity " + opacitySlider.getValue() + "%");
            app.setOverlayOpacity(opacitySlider.getValue() / 100f);
        });
        panel.add(opacitySlider);

        Runnable syncOpacityEnabled = () -> {
            opacitySlider.setEnabled(compactCheckBox.isSelected());
            opacityLabel.setEnabled(compactCheckBox.isSelected());
        };
        syncOpacityEnabled.run();
        compactCheckBox.addActionListener(e -> {
            syncOpacityEnabled.run();
            app.setCompactOverlay(compactCheckBox.isSelected());
        });

        //=====================================================
        // Rebind controls
        //=====================================================
        JPanel rebindPanel = new JPanel(null);
        rebindPanel.setBackground(new Color(50, 52, 54));
        rebindPanel.setBounds(15, 395, 294, 110);

        JLabel rebindTitle = new JLabel("Rebind Controls:", SwingConstants.LEFT);
        rebindTitle.setFont(new Font("SansSerif", Font.PLAIN, 15));
        rebindTitle.setForeground(Color.WHITE);
        rebindTitle.setBounds(10, 5, 274, 25);
        rebindPanel.add(rebindTitle);

        KeyBindField prevField = new KeyBindField(app.prevBind(), hotkeysAvailable,
                bind -> bind.equals(app.nextBind()), app::setPrevBind);
        KeyBindField nextField = new KeyBindField(app.nextBind(), hotkeysAvailable,
                bind -> bind.equals(app.prevBind()), app::setNextBind);

        addBindRow(rebindPanel, "Prev Item:", prevField, 40);
        addBindRow(rebindPanel, "Next Item:", nextField, 75);
        panel.add(rebindPanel);

        //=====================================================
        // Window
        //=====================================================
        frame.pack();
        WindowPlacement.place(frame, app.savedConfigWindowLocation());

        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                app.configWindowMoved(frame.getLocation());
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                app.exiting();
            }
        });
    }

    private static JCheckBox checkBox(String text, boolean selected, JPanel panel) {
        JCheckBox checkBox = new JCheckBox(text, selected);
        checkBox.setFont(new Font("SansSerif", Font.PLAIN, 16));
        checkBox.setBackground(panel.getBackground());
        checkBox.setForeground(Color.WHITE);
        checkBox.setFocusPainted(false);
        return checkBox;
    }

    private static void addBindRow(JPanel rebindPanel, String text, KeyBindField field, int y) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));
        label.setForeground(Color.WHITE);
        label.setBounds(10, y, 70, 25);
        rebindPanel.add(label);

        field.setBounds(85, y, 195, 25);
        rebindPanel.add(field);
    }

    JFrame frame() {
        return frame;
    }

    void show() {
        frame.setVisible(true);
    }
}
