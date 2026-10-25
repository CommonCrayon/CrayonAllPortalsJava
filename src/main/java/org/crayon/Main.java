package org.crayon;

import com.formdev.flatlaf.FlatDarkLaf;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wires the app together: owns the target list, the latest F3+C reading and the hotkeys,
 * and keeps the settings file up to date.
 */
public final class Main implements NativeKeyListener {

    private static final String DEFAULT_TARGETS = """
            [1, 2048, 0]
            [2, 5120, 0]
            [3, 8192, 0]
            [4, 11264, 0]
            [5, 14336, 0]
            [6, 17408, 0]
            [7, 20480, 0]
            [8, 23552, 0]""";

    // Held in a field: java.util.logging only keeps weak references, so a local could be
    // garbage collected and JNativeHook's verbose logging would come back
    private static final Logger HOOK_LOGGER = Logger.getLogger(GlobalScreen.class.getPackage().getName());

    private final Settings settings;

    // Target state (EDT only)
    private final List<Target> targets = new ArrayList<>();
    private int targetIndex = -1;
    private String currentTargetKey;

    // Player state from F3+C (EDT only)
    private PlayerPosition player;
    private String lastClip = "";

    // Read on the hook thread, written on the EDT
    private volatile KeyBind prevBind;
    private volatile KeyBind nextBind;

    private boolean targetChunks;
    private boolean compactOverlay;
    private float overlayOpacity;

    private ConfigWindow configWindow;
    private NavWindow navWindow;
    private String hookError;

    private Main(Settings settings) {
        this.settings = settings;
        this.currentTargetKey = settings.currentTargetKey();
        this.prevBind = settings.prevBind();
        this.nextBind = settings.nextBind();
        this.targetChunks = settings.targetChunks();
        this.compactOverlay = settings.compactOverlay();
        this.overlayOpacity = settings.overlayOpacity();
    }

    public static void main(String[] args) {
        // Disable verbose JNativeHook logging
        HOOK_LOGGER.setLevel(Level.OFF);
        HOOK_LOGGER.setUseParentHandlers(false);

        useNativeLibraryFolder();
        FlatDarkLaf.setup();
        Settings settings = Settings.loadDefault();
        SwingUtilities.invokeLater(() -> new Main(settings).start());
    }

    /**
     * JNativeHook extracts its DLL next to the jar by default. Put it in the app's settings folder
     * (%APPDATA%\CrayonNavAssist\native) instead so the jar's folder stays clean.
     */
    private static void useNativeLibraryFolder() {
        if (System.getProperty("jnativehook.lib.path") != null) return;
        Path dir = Settings.defaultFile().getParent().resolve("native");
        try {
            Files.createDirectories(dir);
            System.setProperty("jnativehook.lib.path", dir.toString());
        } catch (IOException e) {
            System.err.println("Couldn't create " + dir + ", using the default library location: " + e.getMessage());
        }
    }

    private void start() {
        boolean hotkeysAvailable = registerHotkeys();

        configWindow = new ConfigWindow(this, hotkeysAvailable);
        configWindow.show();

        if (!hotkeysAvailable) {
            JOptionPane.showMessageDialog(configWindow.frame(),
                    "Global hotkeys couldn't be started, so the keybinds won't work.\n"
                            + "You can still switch targets with the ◄ ► buttons.\n\n"
                            + "Reason: " + hookError,
                    "Hotkeys unavailable", JOptionPane.WARNING_MESSAGE);
        }

        // Clipboard polling for F3+C (50ms interval)
        new Timer(50, e -> checkClipboard()).start();
    }

    private boolean registerHotkeys() {
        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
            return true;
        } catch (NativeHookException | LinkageError ex) {
            hookError = ex.getMessage() != null ? ex.getMessage() : ex.toString();
            System.err.println("Failed to register global key hook: " + hookError);
            return false;
        }
    }

    //=========================================================
    // Global hotkeys (hook thread)
    //=========================================================
    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        // A rebind box waiting for a key takes the press instead
        if (KeyBindField.offerNativeKey(e)) return;

        switch (KeyBind.resolve(e.getKeyCode(), e.getModifiers(), prevBind, nextBind)) {
            case 0 -> SwingUtilities.invokeLater(this::prevItem);
            case 1 -> SwingUtilities.invokeLater(this::nextItem);
            default -> { }
        }
    }

    @Override public void nativeKeyReleased(NativeKeyEvent e) {}
    @Override public void nativeKeyTyped(NativeKeyEvent e) {}

    //=========================================================
    // Clipboard polling
    //=========================================================
    private void checkClipboard() {
        try {
            String text = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            if (text != null && !text.equals(lastClip)) {
                lastClip = text;
                PlayerPosition.parse(text).ifPresent(position -> {
                    player = position;
                    refreshNav();
                });
            }
        } catch (Exception ignored) {
            // Clipboard busy or not text
        }
    }

    //=========================================================
    // Targets
    //=========================================================
    void launch(String text) {
        List<Target> parsed = Target.parseAll(text);
        if (parsed.isEmpty()) {
            JOptionPane.showMessageDialog(configWindow.frame(), "No target lines were found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        targets.clear();
        targets.addAll(parsed);

        // Stay on the same target if it's still in the list (relaunch after a crash, or after fixing a line).
        // A new run has different coordinates, so it starts from the top.
        targetIndex = Math.max(0, indexOfTarget(currentTargetKey));
        rememberCurrentTarget();
        settings.setTargetsText(text);
        settings.save();

        openNavWindow();
    }

    private int indexOfTarget(String key) {
        if (key == null) return -1;
        for (int i = 0; i < targets.size(); i++) {
            if (targets.get(i).key().equals(key)) return i;
        }
        return -1;
    }

    void prevItem() {
        goTo(targetIndex - 1);
    }

    void nextItem() {
        goTo(targetIndex + 1);
    }

    void firstItem() {
        goTo(0);
    }

    private void goTo(int index) {
        if (targets.isEmpty()) return;
        targetIndex = Math.max(0, Math.min(targets.size() - 1, index));
        rememberCurrentTarget();
        settings.saveSoon();
        refreshNav();
    }

    private void rememberCurrentTarget() {
        currentTargetKey = targets.get(targetIndex).key();
        settings.setCurrentTargetKey(currentTargetKey);
    }

    //=========================================================
    // Nav window
    //=========================================================
    private void openNavWindow() {
        if (navWindow != null && navWindow.isOpen()) {
            navWindow.toFront();
            refreshNav();
            return;
        }
        navWindow = new NavWindow(this, compactOverlay, overlayOpacity, settings.navWindowLocation());
        render(navWindow);
        navWindow.show();
    }

    private void refreshNav() {
        if (navWindow != null && navWindow.isOpen()) render(navWindow);
    }

    private void render(NavWindow window) {
        boolean hasTarget = targetIndex >= 0 && targetIndex < targets.size();
        window.render(hasTarget ? targets.get(targetIndex) : null, targetIndex, targets.size(), player, targetChunks);
    }

    void navWindowMoved(Point location) {
        settings.setNavWindowLocation(location);
        settings.saveSoon();
    }

    //=========================================================
    // Config page
    //=========================================================
    String initialTargetsText() {
        String saved = settings.targetsText();
        return saved != null ? saved : DEFAULT_TARGETS;
    }

    boolean targetChunks() {
        return targetChunks;
    }

    void setTargetChunks(boolean value) {
        targetChunks = value;
        settings.setTargetChunks(value);
        settings.saveSoon();
        refreshNav();
    }

    boolean compactOverlay() {
        return compactOverlay;
    }

    void setCompactOverlay(boolean value) {
        compactOverlay = value;
        settings.setCompactOverlay(value);
        settings.saveSoon();

        // A window can't switch between borderless and normal while open, so rebuild it in place
        if (navWindow != null && navWindow.isOpen()) {
            Point location = navWindow.location();
            navWindow.dispose();
            navWindow = new NavWindow(this, value, overlayOpacity, location);
            render(navWindow);
            navWindow.show();
        }
    }

    float overlayOpacity() {
        return overlayOpacity;
    }

    void setOverlayOpacity(float value) {
        overlayOpacity = Settings.clampOpacity(value);
        settings.setOverlayOpacity(overlayOpacity);
        settings.saveSoon();
        if (navWindow != null && navWindow.isOpen()) navWindow.setOpacity(overlayOpacity);
    }

    KeyBind prevBind() {
        return prevBind;
    }

    void setPrevBind(KeyBind bind) {
        prevBind = bind;
        settings.setPrevBind(bind);
        settings.saveSoon();
    }

    KeyBind nextBind() {
        return nextBind;
    }

    void setNextBind(KeyBind bind) {
        nextBind = bind;
        settings.setNextBind(bind);
        settings.saveSoon();
    }

    Point savedConfigWindowLocation() {
        return settings.configWindowLocation();
    }

    void configWindowMoved(Point location) {
        settings.setConfigWindowLocation(location);
        settings.saveSoon();
    }

    /** Flushes anything still waiting to be saved. */
    void exiting() {
        settings.save();
    }
}
