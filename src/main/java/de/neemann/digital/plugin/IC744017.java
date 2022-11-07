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

public class IC744017 extends Node implements Element {
    public static final ElementTypeDescription DESCRIPTION = new ElementTypeDescription("744017", IC744017.class,
            input("CLK").setClock(),
            input("~EN"),
            input("MR")) {
        @Override
        public String getDescription(ElementAttributes elementAttributes) {
            return "74xx4017";
        }
    }.addAttribute(Keys.ROTATE);

    private final ObservableValue Q0;
    private final ObservableValue Q1;
    private final ObservableValue Q2;
    private final ObservableValue Q3;
    private final ObservableValue Q4;
    private final ObservableValue Q5;
    private final ObservableValue Q6;
    private final ObservableValue Q7;
    private final ObservableValue Q8;
    private final ObservableValue Q9;
    private final ObservableValue Q59;

    private ObservableValue clkIn;
    private ObservableValue enIn;
    private ObservableValue mrIn;

    private boolean lastClk;

    private long value;

    public IC744017(ElementAttributes attr) {
        Q0 = new ObservableValue("Q0", 1);
        Q1 = new ObservableValue("Q1", 1);
        Q2 = new ObservableValue("Q2", 1);
        Q3 = new ObservableValue("Q3", 1);
        Q4 = new ObservableValue("Q4", 1);
        Q5 = new ObservableValue("Q5", 1);
        Q6 = new ObservableValue("Q6", 1);
        Q7 = new ObservableValue("Q7", 1);
        Q8 = new ObservableValue("Q8", 1);
        Q9 = new ObservableValue("Q9", 1);
        Q59 = new ObservableValue("~Q59", 1);
    }

    @Override
    public void readInputs() {
        boolean clk = clkIn.getBool() && !enIn.getBool();
        boolean mr = mrIn.getBool();

        if (mr) {
            value = 0;
        } else {
            if (clk && !lastClk) {
                value = (value + 1) % 10;
            }
        }

        lastClk = clk;
    }

    @Override
    public void writeOutputs() {
        Q0.setBool(value == 0);
        Q1.setBool(value == 1);
        Q2.setBool(value == 2);
        Q3.setBool(value == 3);
        Q4.setBool(value == 4);
        Q5.setBool(value == 5);
        Q6.setBool(value == 6);
        Q7.setBool(value == 7);
        Q8.setBool(value == 8);
        Q9.setBool(value == 9);

        Q59.setBool(value < 5);
    }

    @Override
    public void setInputs(ObservableValues inputs) throws NodeException {
        clkIn = inputs.get(0).addObserverToValue(this).checkBits(1, this);
        enIn = inputs.get(1).addObserverToValue(this).checkBits(1, this);
        mrIn = inputs.get(2).addObserverToValue(this).checkBits(1, this);
    }

    @Override
    public ObservableValues getOutputs() {
        return ovs(Q0, Q1, Q2, Q3, Q4, Q5, Q6, Q7, Q8, Q9, Q59);
    }
}
