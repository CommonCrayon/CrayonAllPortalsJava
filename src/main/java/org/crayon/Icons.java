package org.crayon;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** The 16x16 pixel-art logo, scaled up with hard pixel edges (no smoothing). */
final class Icons {

    private static final int[] WINDOW_SIZES = {16, 20, 24, 32, 40, 48, 64, 128, 256};
    private static final BufferedImage BASE = load();

    private Icons() {}

    private static BufferedImage load() {
        try (InputStream in = Icons.class.getResourceAsStream("logo.png")) {
            return in == null ? null : ImageIO.read(in);
        } catch (IOException e) {
            return null;
        }
    }

    /** Every size Windows might want for the title bar, taskbar and Alt+Tab. */
    static List<Image> windowIcons() {
        List<Image> icons = new ArrayList<>();
        if (BASE == null) return icons;
        for (int size : WINDOW_SIZES) icons.add(scaled(size));
        return icons;
    }

    static Icon logo(int size) {
        return BASE == null ? null : new ImageIcon(scaled(size));
    }

    private static BufferedImage scaled(int size) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(BASE, 0, 0, size, size, null);
        g.dispose();
        return out;
    }
}
