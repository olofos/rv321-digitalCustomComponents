/*
 * Copyright (c) 2017 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digital.plugin;

import de.neemann.digital.gui.components.graphics.*;

import javax.swing.*;
import java.awt.*;

public class HD44780LCDDialog extends JDialog {

    private final HD44780LCDComponent lcdComponent;

    public HD44780LCDDialog(JFrame parent, int width, int height, Color onColor, Color offColor, Color bgColor) {
        super(parent, "LCD", false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        lcdComponent = new HD44780LCDComponent(width, height, onColor, offColor, bgColor);
        getContentPane().add(lcdComponent);
        pack();

        setLocationRelativeTo(null);
        setVisible(true);

        MoveFocusTo.addListener(this, parent);
    }

    public void updateGraphic(int[][] pixels) {
        lcdComponent.updateGraphic(pixels);
    }
}