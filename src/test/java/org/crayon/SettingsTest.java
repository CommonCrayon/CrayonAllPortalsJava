package org.crayon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Point;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.kwhat.jnativehook.NativeInputEvent.CTRL_MASK;
import static com.github.kwhat.jnativehook.NativeInputEvent.SHIFT_MASK;
import static com.github.kwhat.jnativehook.keyboard.NativeKeyEvent.VC_F7;
import static com.github.kwhat.jnativehook.keyboard.NativeKeyEvent.VC_P;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsTest {

    @Test
    void roundTripsEverything(@TempDir Path dir) {
        Path file = dir.resolve("nested").resolve("settings.properties");
        String list = "\"0:Steve\"\n[3,2024,310] \"note with ° and ◄\"\n[21,10085,5017]";

        Settings saved = new Settings(file);
        saved.setTargetsText(list);
        saved.setCurrentTargetKey(new Target(21, 10085, 5017, "").key());
        saved.setPrevBind(new KeyBind(VC_P, CTRL_MASK | SHIFT_MASK));
        saved.setNextBind(new KeyBind(VC_F7, 0));
        saved.setTargetChunks(true);
        saved.setCompactOverlay(true);
        saved.setOverlayOpacity(0.6f);
        saved.setNavWindowLocation(new Point(-1500, 200));
        saved.setConfigWindowLocation(new Point(10, 20));
        saved.save();

        Settings loaded = new Settings(file);
        loaded.load();
        assertEquals(list, loaded.targetsText());
        assertEquals("21,10085.0,5017.0", loaded.currentTargetKey());
        assertEquals(new KeyBind(VC_P, CTRL_MASK | SHIFT_MASK), loaded.prevBind());
        assertEquals(new KeyBind(VC_F7, 0), loaded.nextBind());
        assertTrue(loaded.targetChunks());
        assertTrue(loaded.compactOverlay());
        assertEquals(0.6f, loaded.overlayOpacity(), 1e-6);
        assertEquals(new Point(-1500, 200), loaded.navWindowLocation());
        assertEquals(new Point(10, 20), loaded.configWindowLocation());
    }

    @Test
    void missingFileGivesDefaults(@TempDir Path dir) {
        Settings settings = new Settings(dir.resolve("settings.properties"));
        settings.load();
        assertNull(settings.targetsText());
        assertNull(settings.currentTargetKey());
        assertEquals(KeyBind.DEFAULT_PREV, settings.prevBind());
        assertEquals(KeyBind.DEFAULT_NEXT, settings.nextBind());
        assertFalse(settings.targetChunks());
        assertFalse(settings.compactOverlay());
        assertNull(settings.navWindowLocation());
    }

    @Test
    void badValuesFallBackToDefaults(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("settings.properties");
        Files.writeString(file, "bind.prev.key=abc\nbind.prev.modifiers=2\noverlay.opacity=0.01\nwindow.nav.x=5\n");
        Settings settings = new Settings(file);
        settings.load();
        assertEquals(KeyBind.DEFAULT_PREV, settings.prevBind());
        assertEquals(Settings.MIN_OPACITY, settings.overlayOpacity(), 1e-6);
        assertNull(settings.navWindowLocation());
    }
}
