package org.crayon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * The always-on-top navigation window. In compact mode it's borderless and translucent:
 * drag anywhere (except the buttons) to move it, right-click for options.
 */
final class NavWindow {

    // Padded titles set the minimum column widths
    private static final String[] TITLES = {" ID ", "         Target         ", " Distance ", "           Angle           "};
    private static final int NORMAL_WIDTH = 420;
    private static final int NORMAL_HEIGHT = 140;
    private static final int COMPACT_MIN_WIDTH = 404;

    private final JFrame frame;
    private final boolean compact;

    private final JLabel idValue;
    private final JLabel targetValue;
    private final JLabel distanceValue;
    private final JLabel angleValue;
    private final JLabel noteLabel;
    private final JLabel pageLabel;
    private final JButton prevBtn;
    private final JButton nextBtn;

    NavWindow(Main app, boolean compact, float opacity, Point location) {
        this.compact = compact;

        frame = new JFrame("CrayonNavAssist");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setAlwaysOnTop(true);
        frame.setIconImages(Icons.windowIcons());
        frame.setUndecorated(compact);

        JPanel root = new JPanel(new BorderLayout());
        frame.setContentPane(root);

        //=====================================================
        // Main info grid
        //=====================================================
        JPanel card = new JPanel(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        Font font = new Font("SansSerif", Font.PLAIN, 16);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 4, 2, 4);
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel[] values = new JLabel[TITLES.length];
        for (int col = 0; col < TITLES.length; col++) {
            gbc.gridx = col;

            gbc.gridy = 0;
            card.add(label(TITLES[col], font, Color.WHITE), gbc);

            gbc.gridy = 1;
            values[col] = label("-", font, Color.WHITE);
            card.add(values[col], gbc);
        }
        idValue = values[0];
        targetValue = values[1];
        distanceValue = values[2];
        angleValue = values[3];

        //=====================================================
        // Note
        //=====================================================
        noteLabel = label("", new Font("SansSerif", Font.PLAIN, 13), Color.YELLOW);
        // Reserve one line so notes still fit when the compact window is sized to its contents
        noteLabel.setPreferredSize(new Dimension(0, noteLabel.getFontMetrics(noteLabel.getFont()).getHeight()));

        //=====================================================
        // List controls
        //=====================================================
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 4));

        prevBtn = new JButton("◄");
        nextBtn = new JButton("►");
        pageLabel = new JLabel("0 / 0", SwingConstants.CENTER);
        pageLabel.setFont(new Font("SansSerif", Font.BOLD, 11));

        for (JButton btn : new JButton[]{prevBtn, nextBtn}) {
            btn.setFocusPainted(false);
            btn.setFont(new Font("SansSerif", Font.BOLD, 11));
            btn.setPreferredSize(new Dimension(100, 22));
            btn.setCursor(Cursor.getDefaultCursor());
        }
        prevBtn.addActionListener(e -> app.prevItem());
        nextBtn.addActionListener(e -> app.nextItem());

        navPanel.add(prevBtn);
        navPanel.add(pageLabel);
        navPanel.add(nextBtn);

        root.add(card, BorderLayout.NORTH);
        root.add(noteLabel, BorderLayout.CENTER);
        root.add(navPanel, BorderLayout.SOUTH);

        //=====================================================
        // Window mode
        //=====================================================
        if (compact) {
            root.setBorder(BorderFactory.createLineBorder(new Color(90, 90, 90)));
            root.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            frame.pack();
            frame.setSize(Math.max(frame.getWidth(), COMPACT_MIN_WIDTH), frame.getHeight());
            setOpacity(opacity);
        } else {
            frame.setSize(NORMAL_WIDTH, NORMAL_HEIGHT);
        }
        installMouseHandling(root, app);
        WindowPlacement.place(frame, location);

        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                app.navWindowMoved(frame.getLocation());
            }
        });
    }

    private static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    /** Right-click menu in both modes; drag-to-move in compact mode (there's no title bar to grab). */
    private void installMouseHandling(JPanel root, Main app) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem first = new JMenuItem("Go to first target");
        first.addActionListener(e -> app.firstItem());
        JMenuItem close = new JMenuItem("Close overlay");
        close.addActionListener(e -> frame.dispose());
        menu.add(first);
        menu.addSeparator();
        menu.add(close);

        MouseAdapter mouse = new MouseAdapter() {
            private Point grabOffset;

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    menu.show(e.getComponent(), e.getX(), e.getY());
                    return;
                }
                if (compact && SwingUtilities.isLeftMouseButton(e)) {
                    Point screen = e.getLocationOnScreen();
                    Point window = frame.getLocation();
                    grabOffset = new Point(screen.x - window.x, screen.y - window.y);
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (grabOffset == null) return;
                Point screen = e.getLocationOnScreen();
                frame.setLocation(screen.x - grabOffset.x, screen.y - grabOffset.y);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                grabOffset = null;
                if (e.isPopupTrigger()) menu.show(e.getComponent(), e.getX(), e.getY());
            }
        };
        root.addMouseListener(mouse);
        root.addMouseMotionListener(mouse);
    }

    void show() {
        frame.setVisible(true);
    }

    void toFront() {
        frame.toFront();
    }

    boolean isOpen() {
        return frame.isDisplayable();
    }

    void dispose() {
        frame.dispose();
    }

    Point location() {
        return frame.getLocation();
    }

    /** Only applies in compact mode; Windows only allows translucency on borderless windows. */
    void setOpacity(float opacity) {
        if (!compact) return;
        GraphicsDevice device = frame.getGraphicsConfiguration().getDevice();
        if (device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT)) {
            frame.setOpacity(Settings.clampOpacity(opacity));
        }
    }

    //=========================================================
    // Rendering
    //=========================================================
    void render(Target target, int index, int count, PlayerPosition player, boolean targetChunks) {
        if (target == null) {
            pageLabel.setText("0 / 0");
            for (JLabel value : new JLabel[]{idValue, targetValue, distanceValue, angleValue}) {
                value.setText("-");
                value.setForeground(Color.WHITE);
            }
            noteLabel.setText("");
            prevBtn.setEnabled(false);
            nextBtn.setEnabled(false);
            return;
        }

        pageLabel.setText((index + 1) + " / " + count);
        prevBtn.setEnabled(index > 0);
        nextBtn.setEnabled(index < count - 1);

        double targetX = target.x();
        double targetZ = target.z();

        // Distance and angle need an F3+C reading
        if (player == null) {
            distanceValue.setText("-");
            distanceValue.setForeground(Color.WHITE);
            angleValue.setText("-");
            angleValue.setForeground(Color.WHITE);
        } else {
            targetX = NavMath.toDimension(targetX, player.dimension());
            targetZ = NavMath.toDimension(targetZ, player.dimension());

            double requiredYaw = NavMath.requiredYaw(player.x(), player.z(), targetX, targetZ);
            double turn = NavMath.turnAmount(player.yaw(), requiredYaw);
            long distance = Math.round(NavMath.distance(player.x(), player.z(), targetX, targetZ));

            distanceValue.setText(String.valueOf(distance));
            distanceValue.setForeground(valueToColor(distance, 100.0));

            String arrow = turn >= 0 ? "->" : "<-";
            angleValue.setText(String.format("%.2f° (%s %.1f°)", requiredYaw, arrow, Math.abs(turn)));
            angleValue.setForeground(valueToColor(turn, 180.0));
        }

        idValue.setText(String.valueOf(target.id()));
        idValue.setForeground(Color.WHITE);

        if (targetChunks) {
            targetValue.setText(String.format("(%d, %d)", NavMath.chunk(targetX), NavMath.chunk(targetZ)));
        } else {
            targetValue.setText(String.format("(%d, %d)", (int) targetX, (int) targetZ));
        }
        targetValue.setForeground(Color.WHITE);

        noteLabel.setText(target.note());
    }

    /** Green within 5, fading to red at maxRange. */
    private static Color valueToColor(double value, double maxRange) {
        double absVal = Math.abs(value);
        if (absVal <= 5.0) return Color.GREEN;
        if (absVal >= maxRange) return Color.RED;

        float t = (float) ((absVal - 5.0) / (maxRange - 5.0));
        return new Color(t, 1.0f - t, 0.0f);
    }
}
