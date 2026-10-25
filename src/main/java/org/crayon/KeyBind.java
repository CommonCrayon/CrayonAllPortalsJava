package org.crayon;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

import java.util.Locale;

import static com.github.kwhat.jnativehook.NativeInputEvent.ALT_MASK;
import static com.github.kwhat.jnativehook.NativeInputEvent.CTRL_MASK;
import static com.github.kwhat.jnativehook.NativeInputEvent.META_MASK;
import static com.github.kwhat.jnativehook.NativeInputEvent.SHIFT_MASK;

/**
 * A global hotkey: a native key code plus the modifiers that must be held.
 * Modifiers are stored normalised (left/right merged, lock keys and mouse buttons dropped).
 */
record KeyBind(int keyCode, int modifiers) {

    static final KeyBind DEFAULT_PREV = new KeyBind(NativeKeyEvent.VC_LEFT, CTRL_MASK);
    static final KeyBind DEFAULT_NEXT = new KeyBind(NativeKeyEvent.VC_RIGHT, CTRL_MASK);

    KeyBind {
        modifiers = normalize(modifiers);
    }

    static KeyBind of(NativeKeyEvent e) {
        return new KeyBind(e.getKeyCode(), e.getModifiers());
    }

    /**
     * True if the key is pressed with at least this bind's modifiers held.
     * Extra modifiers are allowed, so holding Ctrl to sprint doesn't block a plain RIGHT bind.
     */
    boolean matches(int pressedKeyCode, int rawModifiers) {
        return pressedKeyCode == keyCode && (normalize(rawModifiers) & modifiers) == modifiers;
    }

    boolean matches(NativeKeyEvent e) {
        return matches(e.getKeyCode(), e.getModifiers());
    }

    /**
     * Which bind a key press triggers, or -1 for none.
     * If several match, the one with the most modifiers wins (so CTRL + LEFT beats LEFT).
     */
    static int resolve(int pressedKeyCode, int rawModifiers, KeyBind... binds) {
        int best = -1;
        for (int i = 0; i < binds.length; i++) {
            if (!binds[i].matches(pressedKeyCode, rawModifiers)) continue;
            if (best < 0 || Integer.bitCount(binds[i].modifiers) > Integer.bitCount(binds[best].modifiers)) {
                best = i;
            }
        }
        return best;
    }

    /** Merges left/right modifier flags and drops Num/Caps/Scroll Lock and mouse button flags. */
    static int normalize(int rawModifiers) {
        int out = 0;
        if ((rawModifiers & CTRL_MASK) != 0) out |= CTRL_MASK;
        if ((rawModifiers & SHIFT_MASK) != 0) out |= SHIFT_MASK;
        if ((rawModifiers & ALT_MASK) != 0) out |= ALT_MASK;
        if ((rawModifiers & META_MASK) != 0) out |= META_MASK;
        return out;
    }

    /** Modifier keys on their own can't be a bind; they're only part of one. */
    static boolean isModifierKey(int keyCode) {
        return switch (keyCode) {
            case NativeKeyEvent.VC_CONTROL, NativeKeyEvent.VC_SHIFT, NativeKeyEvent.VC_ALT, NativeKeyEvent.VC_META,
                 0x0E1D, 0x0036, 0x0E38, 0x0E5C -> true; // right-hand variants some platforms report separately
            default -> false;
        };
    }

    boolean isEscape() {
        return keyCode == NativeKeyEvent.VC_ESCAPE && modifiers == 0;
    }

    /** Display text, e.g. "CTRL + LEFT". */
    String label() {
        StringBuilder text = new StringBuilder();
        if ((modifiers & CTRL_MASK) != 0) text.append("CTRL + ");
        if ((modifiers & SHIFT_MASK) != 0) text.append("SHIFT + ");
        if ((modifiers & ALT_MASK) != 0) text.append("ALT + ");
        if ((modifiers & META_MASK) != 0) text.append("WIN + ");

        String key = NativeKeyEvent.getKeyText(keyCode);
        if (key.startsWith("Unknown")) key = String.format("KEY 0x%X", keyCode);
        return text.append(key.toUpperCase(Locale.ROOT)).toString();
    }
}
