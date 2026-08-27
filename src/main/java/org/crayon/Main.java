package org.crayon;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main implements NativeKeyListener {

    // Regex Patterns
    private static final Pattern F3C_REGEX = Pattern.compile("/execute in minecraft:(\\w+) run tp @s ([\\-\\d.]+) ([\\-\\d.]+) ([\\-\\d.]+) ([\\-\\d.]+) ([\\-\\d.]+)");
    private static final Pattern TARGET_REGEX = Pattern.compile("\\[\\s*([+-]?\\d+)\\s*,\\s*([+-]?\\d+(?:\\.\\d+)?)\\s*,\\s*([+-]?\\d+(?:\\.\\d+)?)\\s*]");

    // Player State
    private static String playerDimension = null;
    private static Double playerX = null;
    private static Double playerZ = null;
    private static Double playerYaw = null;

    // Target State
    private static final List<double[]> targets = new ArrayList<>();
    private static int targetIndex = -1;
    private static String lastClip = "";

    // Global Key Code Binds (Native Key Codes)
    private static int prevNativeKey = NativeKeyEvent.VC_LEFT;
    private static int nextNativeKey = NativeKeyEvent.VC_RIGHT;
    private static boolean requiresCtrl = true;

    // Main UI components
    private static JLabel f3cStatusLabel;

    // Viewer Window components
    private static JFrame navFrame;
    private static JLabel idLabel;
    private static JLabel targetXLabel;
    private static JLabel targetZLabel;
    private static JLabel distanceXLabel;
    private static JLabel distanceZLabel;
    private static JLabel requiredAngleLabel;
    private static JLabel angleChangeLabel;
    private static JLabel pageLabel;
    private static JButton prevBtn;
    private static JButton nextBtn;

    public static void main(String[] args) {
        // Disable verbose JNativeHook logging
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
        logger.setUseParentHandlers(false);

        // Register Global Keyboard Hook
        try
        {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(new Main());
        }
        catch (NativeHookException ex)
        {
            System.err.println("Failed to register global key hook: " + ex.getMessage());
        }

        // Highlight on focus
        Color normalBg = new Color(45, 45, 45);
        Color focusBg = new Color(70, 70, 70); // Slightly lighter gray

        JFrame frame = new JFrame("CrayonNavConfig");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(340, 560);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(new Color(24, 24, 24));
        frame.add(panel);

        JLabel titleLabel = new JLabel("All Portals Navigator", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 22));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBounds(10, 10, 304, 30);
        panel.add(titleLabel);

        JLabel subtitleLabel = new JLabel("Paste list below:");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        subtitleLabel.setForeground(new Color(220, 220, 220));
        subtitleLabel.setBounds(15, 45, 304, 25);
        panel.add(subtitleLabel);

        JTextArea textArea = new JTextArea(
                """
                [1, 2048, 0]
                [2, 5120, 0]
                [3, 8192, 0]
                [4, 11264, 0]
                [5, 14336, 0]
                [6, 17408, 0]
                [7, 20480, 0]
                [8, 23552, 0]
                """
        );
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        textArea.setBackground(new Color(32, 32, 32));
        textArea.setForeground(Color.WHITE);
        textArea.setCaretColor(Color.WHITE);
        textArea.setMargin(new Insets(8, 8, 8, 8));

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
        launchButton.addActionListener(e -> Launch(textArea.getText()));
        panel.add(launchButton);

        // Status Panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        statusPanel.setBackground(new Color(35, 35, 35));
        statusPanel.setBounds(15, 310, 294, 35);

        JLabel statusTitle = new JLabel("F3+C:");
        statusTitle.setForeground(Color.WHITE);
        statusTitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        f3cStatusLabel = new JLabel("Not loaded");
        f3cStatusLabel.setForeground(Color.RED);
        f3cStatusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

        statusPanel.add(statusTitle);
        statusPanel.add(f3cStatusLabel);
        panel.add(statusPanel);

        // Rebind Controls Panel
        JPanel rebindPanel = new JPanel();
        rebindPanel.setLayout(null);
        rebindPanel.setBackground(new Color(35, 35, 35));
        rebindPanel.setBounds(15, 355, 294, 135);

        JLabel rebindTitle = new JLabel("Rebind Controls:", SwingConstants.LEFT);
        rebindTitle.setFont(new Font("SansSerif", Font.PLAIN, 15));
        rebindTitle.setForeground(Color.WHITE);
        rebindTitle.setBounds(10, 5, 274, 25);
        rebindPanel.add(rebindTitle);

        JLabel prevLabel = new JLabel("Prev Item:");
        prevLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        prevLabel.setForeground(Color.WHITE);
        prevLabel.setBounds(10, 40, 70, 25);
        rebindPanel.add(prevLabel);

        JTextField prevField = new JTextField("CTRL + LEFT");
        prevField.setEditable(false);
        prevField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        prevField.setBackground(new Color(45, 45, 45));
        prevField.setForeground(Color.WHITE);
        prevField.setBounds(85, 40, 195, 25);

        // On Focus
        prevField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                prevField.setBackground(focusBg);
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                prevField.setBackground(normalBg);
            }
        });

        // For Rebinding
        prevField.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_CONTROL || e.getKeyCode() == KeyEvent.VK_SHIFT || e.getKeyCode() == KeyEvent.VK_ALT || e.getKeyCode() == KeyEvent.VK_META)
                {
                    return;
                }

                // Get Key
                prevNativeKey = switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT -> NativeKeyEvent.VC_LEFT;
                    case KeyEvent.VK_RIGHT -> NativeKeyEvent.VC_RIGHT;
                    case KeyEvent.VK_UP -> NativeKeyEvent.VC_UP;
                    case KeyEvent.VK_DOWN -> NativeKeyEvent.VC_DOWN;
                    default -> NativeKeyEvent.VC_UNDEFINED;
                };

                // String Builder for display
                StringBuilder keyString = new StringBuilder();
                if (e.isControlDown())
                {
                    requiresCtrl = true;
                    keyString.append("CTRL + ");
                }
                else {
                    requiresCtrl = false;
                }
                keyString.append(KeyEvent.getKeyText(e.getKeyCode()).toUpperCase());

                prevField.setText(keyString.toString());
            }
        });
        rebindPanel.add(prevField);

        JLabel nextLabel = new JLabel("Next Item:");
        nextLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        nextLabel.setForeground(Color.WHITE);
        nextLabel.setBounds(10, 75, 70, 25);
        rebindPanel.add(nextLabel);

        JTextField nextField = new JTextField("CTRL + RIGHT");
        nextField.setEditable(false);
        nextField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        nextField.setBackground(new Color(45, 45, 45));
        nextField.setForeground(Color.WHITE);
        nextField.setBounds(85, 75, 195, 25);

        // On Focus
        nextField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                nextField.setBackground(focusBg);
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                nextField.setBackground(normalBg);
            }
        });

        // For Rebinding
        nextField.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_CONTROL || e.getKeyCode() == KeyEvent.VK_SHIFT || e.getKeyCode() == KeyEvent.VK_ALT || e.getKeyCode() == KeyEvent.VK_META)
                {
                    return;
                }

                nextNativeKey = switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT -> NativeKeyEvent.VC_LEFT;
                    case KeyEvent.VK_RIGHT -> NativeKeyEvent.VC_RIGHT;
                    case KeyEvent.VK_UP -> NativeKeyEvent.VC_UP;
                    case KeyEvent.VK_DOWN -> NativeKeyEvent.VC_DOWN;
                    default -> NativeKeyEvent.VC_UNDEFINED;
                };

                // String Builder for display
                StringBuilder keyString = new StringBuilder();
                if (e.isControlDown())
                {
                    requiresCtrl = true;
                    keyString.append("CTRL + ");
                }
                else {
                    requiresCtrl = false;
                }
                keyString.append(KeyEvent.getKeyText(e.getKeyCode()).toUpperCase());

                nextField.setText(keyString.toString());
            }
        });
        rebindPanel.add(nextField);

        panel.add(rebindPanel);
        frame.setVisible(true);

        // Start Clipboard Monitoring Timer (250ms interval)
        Timer timer = new Timer(250, e -> CheckClipboard());
        timer.start();
    }

    //=========================================================
    // Global Key Event Listener
    //=========================================================
    @Override
    public void nativeKeyPressed(NativeKeyEvent e)
    {
        boolean ctrlPressed = (e.getModifiers() & NativeKeyEvent.CTRL_MASK) != 0;

        if (requiresCtrl && !ctrlPressed) return;

        if (e.getKeyCode() == prevNativeKey) {
            SwingUtilities.invokeLater(Main::PrevItem);
        } else if (e.getKeyCode() == nextNativeKey) {
            SwingUtilities.invokeLater(Main::NextItem);
        }
    }

    @Override public void nativeKeyReleased(NativeKeyEvent e) {}
    @Override public void nativeKeyTyped(NativeKeyEvent e) {}

    //=========================================================
    // MATH HELPERS
    //=========================================================
    private static Color ValueToColor(double value, double maxRange)
    {
        double absVal = Math.abs(value);
        if (absVal <= 5.0) return Color.GREEN;
        if (absVal >= maxRange) return Color.RED;

        float t = (float) ((absVal - 5.0) / (maxRange - 5.0));
        return new Color(t, 1.0f - t, 0.0f);
    }

    //=========================================================
    // Clipboard Polling
    //=========================================================
    private static void CheckClipboard() {
        try
        {
            String text = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);

            if (text != null && !text.equals(lastClip))
            {
                lastClip = text;
                ParseF3C(text);
            }
        }
        catch (Exception ignored) {}
    }

    private static void ParseF3C(String text)
    {
        Matcher matcher = F3C_REGEX.matcher(text);
        if (!matcher.find()) return;

        try
        {
            playerDimension = matcher.group(1);
            playerX = Double.parseDouble(matcher.group(2));
            Double playerY = Double.parseDouble(matcher.group(3));
            playerZ = Double.parseDouble(matcher.group(4));
            playerYaw = Double.parseDouble(matcher.group(5));
            // Double playerPitch = Double.parseDouble(matcher.group(6));

            f3cStatusLabel.setText(String.format("Loaded (%s) at [%.0f, %.0f, %.0f]", playerDimension, playerX, playerY, playerZ));
            f3cStatusLabel.setForeground(Color.GREEN);
        }
        catch (Exception e)
        {
            f3cStatusLabel.setText("Parse failed");
            f3cStatusLabel.setForeground(Color.RED);
        }

        RefreshNavWindow();
    }

    //=========================================================
    // Nav Window Related
    //=========================================================
    private static void Launch(String text)
    {
        targets.clear();
        Matcher matcher = TARGET_REGEX.matcher(text);
        while (matcher.find())
        {
            targets.add(new double[]{
                    Double.parseDouble(matcher.group(1)),
                    Double.parseDouble(matcher.group(2)),
                    Double.parseDouble(matcher.group(3))
            });
        }

        if (targets.isEmpty())
        {
            JOptionPane.showMessageDialog(null, "No target lines were found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        targetIndex = 0;
        StartNavWindow();
    }

    public static void StartNavWindow()
    {
        if (navFrame != null && navFrame.isDisplayable())
        {
            navFrame.toFront();
            RefreshNavWindow();
            return;
        }

        navFrame = new JFrame("CrayonNavAssist");
        navFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        navFrame.setSize(400, 360);
        navFrame.setAlwaysOnTop(true);
        navFrame.setLayout(new BorderLayout(10, 10));

        Color bgColor = new Color(33, 33, 33);
        Color panelColor = new Color(45, 45, 45);
        Color textColor = Color.WHITE;
        Color buttonColor = new Color(30, 115, 190);

        navFrame.getContentPane().setBackground(bgColor);

        // Main info grid
        JPanel card = new JPanel(new GridLayout(5, 3, 5, 8));
        card.setBackground(panelColor);
        card.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        Font labelFont = new Font("SansSerif", Font.PLAIN, 14);
        Font valFont = new Font("SansSerif", Font.PLAIN, 18);

        idLabel = new JLabel("-", SwingConstants.CENTER);
        idLabel.setFont(valFont);
        idLabel.setForeground(textColor);
        card.add(new JLabel("")); card.add(idLabel); card.add(new JLabel(""));

        JLabel targetLabel = new JLabel("Target (X, Z):", SwingConstants.CENTER);
        targetLabel.setFont(labelFont);
        targetLabel.setForeground(textColor);
        card.add(targetLabel);

        targetXLabel = new JLabel("-", SwingConstants.CENTER);
        targetXLabel.setFont(valFont);
        targetXLabel.setForeground(textColor);
        card.add(targetXLabel);

        targetZLabel = new JLabel("-", SwingConstants.CENTER);
        targetZLabel.setFont(valFont);
        targetZLabel.setForeground(textColor);
        card.add(targetZLabel);

        JLabel distanceLabel = new JLabel("Distance (X, Z):", SwingConstants.CENTER);
        distanceLabel.setFont(labelFont);
        distanceLabel.setForeground(textColor);
        card.add(distanceLabel);

        distanceXLabel = new JLabel("-", SwingConstants.CENTER);
        distanceXLabel.setFont(valFont);
        distanceXLabel.setForeground(textColor);
        card.add(distanceXLabel);

        distanceZLabel = new JLabel("-", SwingConstants.CENTER);
        distanceZLabel.setFont(valFont);
        distanceZLabel.setForeground(textColor);
        card.add(distanceZLabel);

        JLabel reqAngleLabel = new JLabel("Required Angle:", SwingConstants.CENTER);
        reqAngleLabel.setFont(labelFont);
        reqAngleLabel.setForeground(textColor);
        card.add(reqAngleLabel);

        requiredAngleLabel = new JLabel("-", SwingConstants.CENTER);
        requiredAngleLabel.setFont(valFont);
        requiredAngleLabel.setForeground(textColor);
        card.add(requiredAngleLabel); card.add(new JLabel(""));

        JLabel turnAmountLabel = new JLabel("Turn Amount:", SwingConstants.CENTER);
        turnAmountLabel.setFont(labelFont);
        turnAmountLabel.setForeground(textColor);
        card.add(turnAmountLabel);

        angleChangeLabel = new JLabel("-", SwingConstants.CENTER);
        angleChangeLabel.setFont(valFont);
        angleChangeLabel.setForeground(textColor);
        card.add(angleChangeLabel); card.add(new JLabel(""));

        JPanel cardWrapper = new JPanel(new BorderLayout());
        cardWrapper.setBackground(bgColor);
        cardWrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        cardWrapper.add(card, BorderLayout.CENTER);

        // Bottom Controls
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        navPanel.setBackground(panelColor);

        prevBtn = new JButton("◄ Prev");
        nextBtn = new JButton("Next ►");
        pageLabel = new JLabel("0 / 0", SwingConstants.CENTER);
        pageLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        pageLabel.setForeground(textColor);

        for (JButton btn : new JButton[]{prevBtn, nextBtn})
        {
            btn.setBackground(buttonColor);
            btn.setForeground(textColor);
            btn.setFocusPainted(false);
            btn.setFont(new Font("SansSerif", Font.BOLD, 12));
            btn.setPreferredSize(new Dimension(100, 30));
        }

        prevBtn.addActionListener(e -> PrevItem());
        nextBtn.addActionListener(e -> NextItem());

        navPanel.add(prevBtn); navPanel.add(pageLabel); navPanel.add(nextBtn);

        JPanel navWrapper = new JPanel(new BorderLayout());
        navWrapper.setBackground(bgColor);
        navWrapper.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        navWrapper.add(navPanel, BorderLayout.CENTER);

        navFrame.add(cardWrapper, BorderLayout.CENTER);
        navFrame.add(navWrapper, BorderLayout.SOUTH);

        navFrame.setLocationRelativeTo(null);
        navFrame.setVisible(true);

        RefreshNavWindow();
    }

    //=========================================================

    private static void PrevItem()
    {
        if (targets.isEmpty()) return;
        targetIndex = Math.max(0, targetIndex - 1);
        RefreshNavWindow();
    }

    private static void NextItem()
    {
        if (targets.isEmpty()) return;
        targetIndex = Math.min(targets.size() - 1, targetIndex + 1);
        RefreshNavWindow();
    }

    //=========================================================

    // Refresh target for Nav
    private static void RefreshNavWindow()
    {
        if (navFrame == null || !navFrame.isDisplayable()) return;

        if (targets.isEmpty() || targetIndex < 0)
        {
            pageLabel.setText("0 / 0");
            idLabel.setText("-");
            targetXLabel.setText("-"); targetZLabel.setText("-");
            distanceXLabel.setText("-"); distanceZLabel.setText("-");
            requiredAngleLabel.setText("-"); angleChangeLabel.setText("-");
            prevBtn.setEnabled(false); nextBtn.setEnabled(false);
            return;
        }

        pageLabel.setText((targetIndex + 1) + " / " + targets.size());

        if (targets.size() <= 1) {
            prevBtn.setEnabled(false);
            nextBtn.setEnabled(false);
        } else {
            prevBtn.setEnabled(targetIndex > 0);
            nextBtn.setEnabled(targetIndex < targets.size() - 1);
        }

        double[] currentTarget = targets.get(targetIndex);
        int targetId = (int) currentTarget[0];
        double targetX = currentTarget[1];
        double targetZ = currentTarget[2];

        // Perform F3+C calculation if available
        if (playerX == null || playerZ == null || playerYaw == null || playerDimension == null)
        {
            distanceXLabel.setText("-"); distanceXLabel.setForeground(Color.WHITE);
            distanceZLabel.setText("-"); distanceZLabel.setForeground(Color.WHITE);
            requiredAngleLabel.setText("-");
            angleChangeLabel.setText("-"); angleChangeLabel.setForeground(Color.WHITE);
        }
        else
        {
            if (playerDimension.contains("nether"))
            {
                targetX /= 8.0;
                targetZ /= 8.0;
            }

            double reqAngle = Math.toDegrees(Math.atan2(playerX - targetX, targetZ - playerZ));
            double angleChange = (reqAngle - playerYaw) - 360.0 * Math.floor(((reqAngle - playerYaw) + 180.0) / 360.0);
            Color angleColor = ValueToColor(angleChange, 180.0);

            int distX = (int) (playerX - targetX);
            int distZ = (int) (playerZ - targetZ);

            Color distXColor = ValueToColor(distX, 100.0);
            Color distZColor = ValueToColor(distZ, 100.0);

            distanceXLabel.setText(String.valueOf(distX));
            distanceXLabel.setForeground(distXColor);

            distanceZLabel.setText(String.valueOf(distZ));
            distanceZLabel.setForeground(distZColor);

            requiredAngleLabel.setText(String.format("%.1f°", reqAngle));

            angleChangeLabel.setText(String.format("%+.1f°", angleChange));
            angleChangeLabel.setForeground(angleColor);
        }

        idLabel.setText(String.valueOf(targetId));
        targetXLabel.setText(String.valueOf((int) targetX));
        targetZLabel.setText(String.valueOf((int) targetZ));
    }
}