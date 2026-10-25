package org.crayon;

import org.junit.jupiter.api.Test;

import static com.github.kwhat.jnativehook.NativeInputEvent.CAPS_LOCK_MASK;
import static com.github.kwhat.jnativehook.NativeInputEvent.CTRL_L_MASK;
import static com.github.kwhat.jnativehook.NativeInputEvent.CTRL_MASK;
import static com.github.kwhat.jnativehook.NativeInputEvent.CTRL_R_MASK;
import static com.github.kwhat.jnativehook.NativeInputEvent.NUM_LOCK_MASK;
import static com.github.kwhat.jnativehook.NativeInputEvent.SHIFT_L_MASK;
import static com.github.kwhat.jnativehook.NativeInputEvent.SHIFT_MASK;
import static com.github.kwhat.jnativehook.keyboard.NativeKeyEvent.VC_CONTROL;
import static com.github.kwhat.jnativehook.keyboard.NativeKeyEvent.VC_ESCAPE;
import static com.github.kwhat.jnativehook.keyboard.NativeKeyEvent.VC_F7;
import static com.github.kwhat.jnativehook.keyboard.NativeKeyEvent.VC_LEFT;
import static com.github.kwhat.jnativehook.keyboard.NativeKeyEvent.VC_P;
import static com.github.kwhat.jnativehook.keyboard.NativeKeyEvent.VC_RIGHT;
import static com.github.kwhat.jnativehook.keyboard.NativeKeyEvent.VC_UP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyBindTest {

    @Test
    void anyKeyCanBeBoundNotJustArrows() {
        KeyBind p = new KeyBind(VC_P, 0);
        assertTrue(p.matches(VC_P, 0));
        assertEquals("P", p.label());
        assertEquals("CTRL + F7", new KeyBind(VC_F7, CTRL_MASK).label());
    }

    @Test
    void lockKeysAndLeftRightDontBreakMatching() {
        KeyBind ctrlLeft = new KeyBind(VC_LEFT, CTRL_MASK);
        assertTrue(ctrlLeft.matches(VC_LEFT, CTRL_L_MASK | NUM_LOCK_MASK | CAPS_LOCK_MASK));
        assertTrue(ctrlLeft.matches(VC_LEFT, CTRL_R_MASK));
        assertFalse(ctrlLeft.matches(VC_LEFT, NUM_LOCK_MASK));
        assertFalse(ctrlLeft.matches(VC_RIGHT, CTRL_L_MASK));
    }

    @Test
    void eachBindKeepsItsOwnModifiers() {
        // Old bug: binding Next as plain RIGHT also stopped Prev from needing Ctrl
        KeyBind prev = new KeyBind(VC_LEFT, CTRL_MASK);
        KeyBind next = new KeyBind(VC_RIGHT, 0);
        assertEquals(-1, KeyBind.resolve(VC_LEFT, 0, prev, next));
        assertEquals(0, KeyBind.resolve(VC_LEFT, CTRL_L_MASK, prev, next));
        assertEquals(1, KeyBind.resolve(VC_RIGHT, 0, prev, next));
    }

    @Test
    void extraModifiersStillTrigger() {
        // e.g. holding Ctrl to sprint while pressing a plain RIGHT bind
        KeyBind next = new KeyBind(VC_RIGHT, 0);
        assertTrue(next.matches(VC_RIGHT, CTRL_L_MASK));
        assertTrue(next.matches(VC_RIGHT, SHIFT_L_MASK | CTRL_L_MASK));
    }

    @Test
    void mostSpecificBindWins() {
        KeyBind prev = new KeyBind(VC_LEFT, CTRL_MASK);
        KeyBind next = new KeyBind(VC_LEFT, 0);
        assertEquals(0, KeyBind.resolve(VC_LEFT, CTRL_L_MASK, prev, next));
        assertEquals(1, KeyBind.resolve(VC_LEFT, 0, prev, next));
        assertEquals(-1, KeyBind.resolve(VC_UP, 0, prev, next));
    }

    @Test
    void bindsAreStoredNormalised() {
        assertEquals(new KeyBind(VC_LEFT, CTRL_L_MASK), new KeyBind(VC_LEFT, CTRL_R_MASK | NUM_LOCK_MASK));
        assertEquals("CTRL + SHIFT + LEFT", new KeyBind(VC_LEFT, CTRL_MASK | SHIFT_MASK).label());
    }

    @Test
    void modifierAndEscapeKeysAreSpecial() {
        assertTrue(KeyBind.isModifierKey(VC_CONTROL));
        assertFalse(KeyBind.isModifierKey(VC_LEFT));
        assertTrue(new KeyBind(VC_ESCAPE, 0).isEscape());
        assertFalse(new KeyBind(VC_ESCAPE, CTRL_MASK).isEscape());
    }

    @Test
    void defaultsAreCtrlArrows() {
        assertEquals("CTRL + LEFT", KeyBind.DEFAULT_PREV.label());
        assertEquals("CTRL + RIGHT", KeyBind.DEFAULT_NEXT.label());
    }
}
