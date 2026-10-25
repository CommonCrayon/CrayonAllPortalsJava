package org.crayon;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Read-only box showing a key bind. Click it, then press the new key combination (Esc cancels).
 * Keys are captured through the global hook rather than Swing, so the stored key code is exactly
 * what the hook reports later. Any key works, not just the arrows.
 */
final class KeyBindField extends JTextField {

    private static final Color NORMAL_BG = new Color(45, 45, 45);
    private static final Color CAPTURE_BG = new Color(70, 70, 70);

    /** The box waiting for a key, if any. Written on the EDT, read on the hook thread. */
    private static volatile KeyBindField capturing;

    private final Predicate<KeyBind> inUse;
    private final Consumer<KeyBind> onChange;
    private KeyBind bind;

    KeyBindField(KeyBind initial, boolean hotkeysAvailable, Predicate<KeyBind> inUse, Consumer<KeyBind> onChange) {
        super(initial.label());
        this.bind = initial;
        this.inUse = inUse;
        this.onChange = onChange;

        setEditable(false);
        setFocusTraversalKeysEnabled(false); // lets Tab be bound instead of moving focus
        setFont(new Font("Monospaced", Font.PLAIN, 12));
        setBackground(NORMAL_BG);
        setForeground(Color.WHITE);

        if (!hotkeysAvailable) {
            setEnabled(false);
            setToolTipText("Global hotkeys are unavailable");
            return;
        }
        setToolTipText("Click, then press the new key combination. Esc cancels.");

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (capturing != KeyBindField.this) startCapture();
            }
        });
        // Stop listening when the box (or the whole window) loses focus, so keys pressed in-game are never captured
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (capturing == KeyBindField.this) finishCapture();
            }
        });
    }

    /**
     * Called on the hook thread for every key press.
     * Returns true if a box was waiting for a key, in which case the press shouldn't also trigger a hotkey.
     */
    static boolean offerNativeKey(NativeKeyEvent e) {
        KeyBindField field = capturing;
        if (field == null) return false;
        if (!KeyBind.isModifierKey(e.getKeyCode())) {
            KeyBind pressed = KeyBind.of(e);
            SwingUtilities.invokeLater(() -> field.accept(pressed));
        }
        return true;
    }

    private void startCapture() {
        KeyBindField other = capturing;
        if (other != null) other.finishCapture();

        requestFocusInWindow();
        capturing = this;
        setBackground(CAPTURE_BG);
        setText("PRESS A KEY...");
    }

    private void finishCapture() {
        if (capturing == this) capturing = null;
        setBackground(NORMAL_BG);
        setText(bind.label());
    }

    private void accept(KeyBind pressed) {
        if (capturing != this) return;
        if (pressed.isEscape()) {
            finishCapture();
            return;
        }
        if (!pressed.equals(bind) && inUse.test(pressed)) {
            setText("IN USE - TRY ANOTHER");
            return;
        }
        bind = pressed;
        onChange.accept(pressed);
        finishCapture();
    }
}
