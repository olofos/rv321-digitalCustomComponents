package de.neemann.digital.plugin;

import javax.swing.*;
import java.awt.*;

public class HD44780LCDComponent extends JComponent {
    private final int width;
    private final int height;
    private final Color onColor;
    private final Color offColor;
    private final Color bgColor;
    private int[][] pixels;

    public HD44780LCDComponent(int width, int height, Color onColor, Color offColor, Color bgColor) {
        this.width = width;
        this.height = height;
        this.onColor = onColor;
        this.offColor = offColor;
        this.bgColor = bgColor;

        int pw = 320 / width;
        if (pw < 2)
            pw = 2;
        int ph = 200 / height;
        if (ph < 2)
            ph = 2;
        int ledSize = (pw + ph) / 2;

        Dimension size = new Dimension((width + 2) * ledSize, (height + 2) * ledSize);
        setPreferredSize(size);
        setOpaque(false);
    }

    public void updateGraphic(int[][] pixels) {
        this.pixels = pixels;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(bgColor);
        g.fillRect(0, 0, getWidth(), getHeight());
        for (int x = 0; x < width; x++) {
            int xPos = (x + 1) * getWidth() / (width + 2);
            int dx = (x + 2) * getWidth() / (width + 2) - xPos;
            ;
            for (int y = 0; y < height; y++) {

                if (pixels[x][y] == 1) {
                    g.setColor(onColor);
                } else if (pixels[x][y] == 0) {
                    g.setColor(offColor);
                } else {
                    g.setColor(bgColor);
                }

                int ypos = (y + 1) * getHeight() / (height + 2);
                int dy = (y + 2) * getHeight() / (height + 2) - ypos;

                g.fillRect(xPos + 1, ypos + 1, dx - 2, dy - 2);
            }
        }
    }

}