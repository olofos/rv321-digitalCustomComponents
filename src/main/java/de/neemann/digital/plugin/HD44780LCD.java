package de.neemann.digital.plugin;

import de.neemann.digital.core.Node;
import de.neemann.digital.core.NodeException;
import de.neemann.digital.core.ObservableValue;
import de.neemann.digital.core.ObservableValues;
import de.neemann.digital.core.element.Element;
import de.neemann.digital.core.element.ElementAttributes;
import de.neemann.digital.core.element.ElementTypeDescription;
import de.neemann.digital.core.element.Keys;
import de.neemann.digital.core.element.Key;

import static de.neemann.digital.core.element.PinInfo.input;

import javax.swing.*;

import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import olofos.hd44780.HD44780Emu;

public class HD44780LCD extends Node implements Element {

    static final Key<Integer> COLS = new Key.KeyInteger("cols", 16)
            .setMin(8)
            .setMax(20)
            .setComboBoxValues(8, 16, 20)
            .setName("Number of rows")
            .setDescription("Sets the number of rows.");

    static final Key<Integer> ROWS = new Key.KeyInteger("rows", 2)
            .setMin(1)
            .setMax(4)
            .setComboBoxValues(1, 2, 4)
            .setName("Number of columns")
            .setDescription("Sets the number of columns.");

    static final Color[][] colorSchemes = {
            {
                    new Color(0x1f, 0x1f, 0xff),
                    new Color(0xf0, 0xf0, 0xff),
                    new Color(0x00, 0x00, 0xe0)
            },
            {
                    new Color(0x5C, 0xAA, 0xEA),
                    new Color(0x00, 0x09, 0x41),
                    new Color(0x51, 0x8B, 0xCA)
            },
            {
                    new Color(0x7D, 0xBE, 0x00),
                    new Color(0x00, 0x00, 0x00),
                    new Color(0x6F, 0xB9, 0x00)
            },
            {
                    new Color(0x21, 0x22, 0x25),
                    new Color(0xFB, 0x33, 0x49),
                    new Color(0x20, 0x27, 0x29)
            },
            {
                    new Color(0x21, 0x22, 0x25),
                    new Color(0xB0, 0xF7, 0xFE),
                    new Color(0x20, 0x27, 0x29)
            }
    };

    static final Key<Integer> COLOR_SCHEME = new Key.KeyInteger("colorScheme", 2)
            .setMin(1)
            .setMax(colorSchemes.length)
            .setComboBoxValues(IntStream.rangeClosed(1, colorSchemes.length).toArray())
            .setName("Color scheme")
            .setDescription("Sets the color scheme.");

    public static final ElementTypeDescription DESCRIPTION = new ElementTypeDescription(HD44780LCD.class,
            input("rs", "rs"),
            input("rw", "rw"),
            input("en", "en"),
            input("d", "d")) {
        @Override
        public String getDescription(ElementAttributes elementAttributes) {
            return "HD44780 LCD";
        }
    }
            .addAttribute(Keys.ROTATE)
            .addAttribute(ROWS)
            .addAttribute(COLS)
            .addAttribute(COLOR_SCHEME);

    private ObservableValue rs;
    private ObservableValue rw;
    private ObservableValue en;
    private ObservableValue d;

    private final Color onColor;
    private final Color offColor;
    private final Color bgColor;
    private final String label;
    private final int rows;
    private final int cols;

    private boolean lastEN;

    private HD44780Emu lcdEmu;
    private HD44780LCDDialog lcdDialog;

    public HD44780LCD(ElementAttributes attr) {
        rows = attr.get(ROWS);
        cols = attr.get(COLS);

        lcdEmu = new HD44780Emu(cols, rows, HD44780Emu.Rom.A02);
        label = attr.getLabel();

        int colorScheme = attr.get(COLOR_SCHEME) - 1;
        bgColor = colorSchemes[colorScheme][0];
        onColor = colorSchemes[colorScheme][1];
        offColor = colorSchemes[colorScheme][2];

        lastEN = true;
    }

    @Override
    public void readInputs() {
        boolean valueRS = rs.getBool();
        boolean valueRW = rw.getBool();
        boolean valueEN = en.getBool();
        int valueD = (int) d.getValue();

        if (!valueRW) {
            if (lastEN && !valueEN) {
                if (valueRS) {
                    lcdEmu.writeByte(valueD);
                } else {
                    lcdEmu.sendCommand(valueD);
                }
                updateDisplay();
            }
            lastEN = valueEN;
        } else {
            // TODO: support read operations
        }
    }

    @Override
    public void writeOutputs() {
    }

    @Override
    public void setInputs(ObservableValues inputs) throws NodeException {
        rs = inputs.get(0).addObserverToValue(this).checkBits(1, this);
        rw = inputs.get(1).addObserverToValue(this).checkBits(1, this);
        en = inputs.get(2).addObserverToValue(this).checkBits(1, this);
        d = inputs.get(3).addObserverToValue(this).checkBits(8, this);
    }

    @Override
    public ObservableValues getOutputs() {
        return ObservableValues.EMPTY_LIST;
    }

    private final AtomicBoolean paintPending = new AtomicBoolean();

    private void updateDisplay() {
        if (paintPending.compareAndSet(false, true)) {
            SwingUtilities.invokeLater(() -> {
                if (lcdDialog == null || !lcdDialog.isVisible()) {
                    lcdDialog = new HD44780LCDDialog(getModel().getWindowPosManager().getMainFrame(), lcdEmu.getWidth(),
                            lcdEmu.getHeight(), onColor, offColor, bgColor);
                    getModel().getWindowPosManager().register("lcd_" + label, lcdDialog);
                }
                paintPending.set(false);
                lcdDialog.updateGraphic(lcdEmu.getPixels());
            });
        }
    }
}
