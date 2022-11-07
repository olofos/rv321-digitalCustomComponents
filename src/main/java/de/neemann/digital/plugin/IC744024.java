package de.neemann.digital.plugin;

import de.neemann.digital.core.Node;
import de.neemann.digital.core.NodeException;
import de.neemann.digital.core.ObservableValue;
import de.neemann.digital.core.ObservableValues;
import de.neemann.digital.core.element.Element;
import de.neemann.digital.core.element.ElementAttributes;
import de.neemann.digital.core.element.ElementTypeDescription;
import de.neemann.digital.core.element.Keys;

import static de.neemann.digital.core.element.PinInfo.input;
import static de.neemann.digital.core.ObservableValues.ovs;

public class IC744024 extends Node implements Element {
    public static final ElementTypeDescription DESCRIPTION = new ElementTypeDescription("744024", IC744024.class,
            input("CP").setClock(),
            input("MR")) {
        @Override
        public String getDescription(ElementAttributes elementAttributes) {
            return "74xx4024";
        }
    }.addAttribute(Keys.ROTATE);

    private final ObservableValue Q0;
    private final ObservableValue Q1;
    private final ObservableValue Q2;
    private final ObservableValue Q3;
    private final ObservableValue Q4;
    private final ObservableValue Q5;
    private final ObservableValue Q6;;

    private ObservableValue clkIn;
    private ObservableValue mrIn;

    private boolean lastClk;

    private long value;

    public IC744024(ElementAttributes attr) {
        Q0 = new ObservableValue("Q0", 1);
        Q1 = new ObservableValue("Q1", 1);
        Q2 = new ObservableValue("Q2", 1);
        Q3 = new ObservableValue("Q3", 1);
        Q4 = new ObservableValue("Q4", 1);
        Q5 = new ObservableValue("Q5", 1);
        Q6 = new ObservableValue("Q6", 1);
    }

    @Override
    public void readInputs() {
        boolean clk = clkIn.getBool();
        boolean mr = mrIn.getBool();

        if (mr) {
            value = 0;
        } else {
            if (!clk && lastClk) {
                value = (value + 1) % (1 << 7);
            }
        }

        lastClk = clk;
    }

    @Override
    public void writeOutputs() {
        Q0.setValue((value >> 0) & 1);
        Q1.setValue((value >> 1) & 1);
        Q2.setValue((value >> 2) & 1);
        Q3.setValue((value >> 3) & 1);
        Q4.setValue((value >> 4) & 1);
        Q5.setValue((value >> 5) & 1);
        Q6.setValue((value >> 6) & 1);
    }

    @Override
    public void setInputs(ObservableValues inputs) throws NodeException {
        clkIn = inputs.get(0).addObserverToValue(this).checkBits(1, this);
        mrIn = inputs.get(1).addObserverToValue(this).checkBits(1, this);
    }

    @Override
    public ObservableValues getOutputs() {
        return ovs(Q0, Q1, Q2, Q3, Q4, Q5, Q6);
    }
}
