package de.neemann.digital.plugin;

import de.neemann.digital.core.Node;
import de.neemann.digital.core.NodeException;
import de.neemann.digital.core.ObservableValue;
import de.neemann.digital.core.ObservableValues;
import de.neemann.digital.core.element.*;

import static de.neemann.digital.core.element.PinInfo.input;

public class RegViewerMonitor extends Node implements Element {
    public static final ElementTypeDescription DESCRIPTION = new ElementTypeDescription(RegViewerMonitor.class) {
        @Override
        public PinDescriptions getInputDescription(ElementAttributes elementAttributes) {
            PinDescription[] names = new PinDescription[32 + 1];
            names[0] = input("RSD");
            for (int i = 0; i < 32; i++) {
                names[i + 1] = input("Reg" + i);
            }
            return new PinDescriptions(names);
        }
    }
            .addAttribute(Keys.ROTATE);

    private ObservableValue[] regIn;

    private long[] values;

    public RegViewerMonitor(ElementAttributes attr) {
        super(true);
        values = new long[32];
        regIn = new ObservableValue[values.length];
    }

    @Override
    public void readInputs() {
        for (int i = 0; i < 32; i++) {
            values[i] = regIn[i].getValue();
        }
    }

    @Override
    public void writeOutputs() {
    }

    @Override
    public void setInputs(ObservableValues inputs) throws NodeException {
        for (int i = 0; i < 32; i++) {
            regIn[i] = inputs.get(i + 1).addObserverToValue(this).checkBits(32, this);
        }
    }

    @Override
    public ObservableValues getOutputs() {
        return ObservableValues.EMPTY_LIST;
    }
}
