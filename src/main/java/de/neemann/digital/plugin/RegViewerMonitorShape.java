package de.neemann.digital.plugin;

import de.neemann.digital.core.element.ElementAttributes;
import de.neemann.digital.core.element.PinDescriptions;
import de.neemann.digital.draw.elements.IOState;
import de.neemann.digital.draw.elements.Pin;
import de.neemann.digital.draw.elements.Pins;
import de.neemann.digital.draw.graphics.Graphic;
import de.neemann.digital.draw.graphics.Orientation;
import de.neemann.digital.draw.graphics.Polygon;
import de.neemann.digital.draw.graphics.Style;
import de.neemann.digital.draw.shapes.Interactor;
import de.neemann.digital.draw.shapes.Shape;

import static de.neemann.digital.draw.graphics.Vector.vec;
import static de.neemann.digital.draw.shapes.GenericShape.SIZE;
import static de.neemann.digital.draw.shapes.GenericShape.SIZE2;

import java.awt.*;

public class RegViewerMonitorShape implements Shape {
    private final PinDescriptions inputs;
    private IOState ioState;

    private long[] values;
    private int rsd;

    public RegViewerMonitorShape(ElementAttributes elementAttributes, PinDescriptions inputs, PinDescriptions outputs) {
        this.inputs = inputs;
        values = new long[32];
    }

    @Override
    public Pins getPins() {
        Pins pins = new Pins().add(new Pin(vec(0, 0), inputs.get(0)));
        for (int i = 0; i < 32; i++) {
            pins.add(new Pin(vec(0, SIZE * (i + 1)), inputs.get(i + 1)));
        }
        return pins;
    }

    @Override
    public Interactor applyStateMonitor(IOState ioState) {
        this.ioState = ioState;
        return null;
    }

    @Override
    public void readObservableValues() {
        if (ioState != null) {
            rsd = (int) ioState.getInput(0).getValue();
            for (int i = 0; i < 32; i++) {
                values[i] = ioState.getInput(i + 1).getValue();
            }
        }
    }

    @Override
    public void drawTo(Graphic graphic, Style highLight) {

        Polygon polygon = new Polygon(true)
                .add(1, -SIZE2)
                .add(SIZE * 7 - 1, -SIZE2)
                .add(SIZE * 7 - 1, 32 * SIZE + SIZE2)
                .add(1, 32 * SIZE + SIZE2);

        graphic.drawPolygon(polygon, Style.NORMAL);
        if (ioState != null) {
            for (int i = 0; i < 32; i++) {
                graphic.drawText(vec(6 * SIZE + SIZE / 2, SIZE * i + SIZE / 2), vec(1, SIZE * i + SIZE / 2),
                        String.format("%08X", values[i]),
                        Orientation.LEFTCENTER, i == rsd ? Style.THICK.deriveColor(Color.RED) : Style.THICK);
            }
        }
    }
}
