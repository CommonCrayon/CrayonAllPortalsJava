package org.crayon;

import javax.swing.Timer;
import java.awt.Point;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * Everything that survives a restart: the launched list, which target you were on, keybinds,
 * toggles and window positions. Stored as a plain properties file in %APPDATA%\CrayonNavAssist.
 */
final class Settings {

    static final float MIN_OPACITY = 0.3f;

    private static final String TARGETS_TEXT = "targets.text";
    private static final String CURRENT_TARGET = "targets.current";
    private static final String TARGET_CHUNKS = "display.chunks";
    private static final String COMPACT = "overlay.compact";
    private static final String OPACITY = "overlay.opacity";
    private static final String PREV_BIND = "bind.prev";
    private static final String NEXT_BIND = "bind.next";
    private static final String CONFIG_WINDOW = "window.config";
    private static final String NAV_WINDOW = "window.nav";

    private final Path file;
    private final Properties props = new Properties();
    private Timer saveTimer;

    Settings(Path file) {
        this.file = file;
    }

    static Settings loadDefault() {
        Settings settings = new Settings(defaultFile());
        settings.load();
        return settings;
    }

    static Path defaultFile() {
        String appData = System.getenv("APPDATA");
        Path dir = (appData != null && !appData.isBlank())
                ? Path.of(appData, "CrayonNavAssist")
                : Path.of(System.getProperty("user.home"), ".crayon-nav-assist");
        return dir.resolve("settings.properties");
    }

    Path file() {
        return file;
    }

    void load() {
        if (!Files.exists(file)) return;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Couldn't read settings from " + file + ": " + e.getMessage());
        }
    }

    /** Writes to a temp file then swaps it in, so a crash mid-write can't corrupt the settings. */
    synchronized void save() {
        if (saveTimer != null) saveTimer.stop();
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                props.store(writer, "Crayon Nav Assist settings");
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("Couldn't save settings to " + file + ": " + e.getMessage());
        }
    }

    /** Saves half a second after the last change, so dragging or key spam writes once. Call on the EDT. */
    void saveSoon() {
        if (saveTimer == null) {
            saveTimer = new Timer(500, e -> save());
            saveTimer.setRepeats(false);
        }
        saveTimer.restart();
    }

    // ---------------------------------------------------------------- Targets

    String targetsText() {
        return props.getProperty(TARGETS_TEXT);
    }

    void setTargetsText(String text) {
        put(TARGETS_TEXT, text);
    }

    String currentTargetKey() {
        return props.getProperty(CURRENT_TARGET);
    }

    void setCurrentTargetKey(String key) {
        put(CURRENT_TARGET, key);
    }

    // ---------------------------------------------------------------- Toggles

    boolean targetChunks() {
        return Boolean.parseBoolean(props.getProperty(TARGET_CHUNKS, "false"));
    }

    void setTargetChunks(boolean value) {
        put(TARGET_CHUNKS, String.valueOf(value));
    }

    boolean compactOverlay() {
        return Boolean.parseBoolean(props.getProperty(COMPACT, "false"));
    }

    void setCompactOverlay(boolean value) {
        put(COMPACT, String.valueOf(value));
    }

    float overlayOpacity() {
        try {
            return clampOpacity(Float.parseFloat(props.getProperty(OPACITY, "0.85")));
        } catch (NumberFormatException e) {
            return 0.85f;
        }
    }

    void setOverlayOpacity(float value) {
        put(OPACITY, String.valueOf(clampOpacity(value)));
    }

    static float clampOpacity(float value) {
        if (Float.isNaN(value)) return 1f;
        return Math.max(MIN_OPACITY, Math.min(1f, value));
    }

    // ---------------------------------------------------------------- Keybinds

    KeyBind prevBind() {
        return getBind(PREV_BIND, KeyBind.DEFAULT_PREV);
    }

    void setPrevBind(KeyBind bind) {
        putBind(PREV_BIND, bind);
    }

    KeyBind nextBind() {
        return getBind(NEXT_BIND, KeyBind.DEFAULT_NEXT);
    }

    void setNextBind(KeyBind bind) {
        putBind(NEXT_BIND, bind);
    }

    private KeyBind getBind(String prefix, KeyBind fallback) {
        Integer key = getInt(prefix + ".key");
        Integer mods = getInt(prefix + ".modifiers");
        if (key == null || mods == null || key <= 0) return fallback;
        return new KeyBind(key, mods);
    }

    private void putBind(String prefix, KeyBind bind) {
        put(prefix + ".key", String.valueOf(bind.keyCode()));
        put(prefix + ".modifiers", String.valueOf(bind.modifiers()));
    }

    // ---------------------------------------------------------------- Window positions

    Point configWindowLocation() {
        return getPoint(CONFIG_WINDOW);
    }

    void setConfigWindowLocation(Point p) {
        putPoint(CONFIG_WINDOW, p);
    }

    Point navWindowLocation() {
        return getPoint(NAV_WINDOW);
    }

    void setNavWindowLocation(Point p) {
        putPoint(NAV_WINDOW, p);
    }

    private Point getPoint(String prefix) {
        Integer x = getInt(prefix + ".x");
        Integer y = getInt(prefix + ".y");
        return (x == null || y == null) ? null : new Point(x, y);
    }

    private void putPoint(String prefix, Point p) {
        put(prefix + ".x", String.valueOf(p.x));
        put(prefix + ".y", String.valueOf(p.y));
    }

    // ---------------------------------------------------------------- Helpers

    private Integer getInt(String key) {
        String value = props.getProperty(key);
        if (value == null) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void put(String key, String value) {
        if (value == null) props.remove(key);
        else props.setProperty(key, value);
    }
}
