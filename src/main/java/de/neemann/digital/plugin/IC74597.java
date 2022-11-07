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

public class IC74597 extends Node implements Element {
    public static final ElementTypeDescription DESCRIPTION = new ElementTypeDescription("74597", IC74597.class,
            input("~MR"),
            input("SHCP").setClock(),
            input("DS"),
            input("STCP"),
            input("~PL"),
            input("D7"),
            input("D6"),
            input("D5"),
            input("D4"),
            input("D3"),
            input("D2"),
            input("D1"),
            input("D0")) {
        @Override
        public String getDescription(ElementAttributes elementAttributes) {
            return "74xx597";
        }
    }.addAttribute(Keys.ROTATE);

    private final ObservableValue Q7;
    private final ObservableValue Val;

    private ObservableValue mrIn;
    private ObservableValue shcpIn;
    private ObservableValue dsIn;
    private ObservableValue stcpIn;
    private ObservableValue plIn;
    private ObservableValue d7In;
    private ObservableValue d6In;
    private ObservableValue d5In;
    private ObservableValue d4In;
    private ObservableValue d3In;
    private ObservableValue d2In;
    private ObservableValue d1In;
    private ObservableValue d0In;

    private boolean lastShcp;
    private boolean lastStcp;

    private long shiftValue;
    private long latchValue;

    public IC74597(ElementAttributes attr) {
        Q7 = new ObservableValue("Q7", 1);
        Val = new ObservableValue("Val", 8);
    }

    @Override
    public void readInputs() {
        boolean mr = mrIn.getBool();
        boolean shcp = shcpIn.getBool();
        boolean ds = dsIn.getBool();
        boolean stcp = stcpIn.getBool();
        boolean pl = plIn.getBool();
        boolean d7 = d7In.getBool();
        boolean d6 = d6In.getBool();
        boolean d5 = d5In.getBool();
        boolean d4 = d4In.getBool();
        boolean d3 = d3In.getBool();
        boolean d2 = d2In.getBool();
        boolean d1 = d1In.getBool();
        boolean d0 = d0In.getBool();

        if (stcp && !lastStcp) {
            latchValue = (d0 ? 0x01 : 0) | (d1 ? 0x02 : 0) | (d2 ? 0x04 : 0) | (d3 ? 0x08 : 0) | (d4 ? 0x10 : 0)
                    | (d5 ? 0x20 : 0) | (d6 ? 0x40 : 0) | (d7 ? 0x80 : 0);
        }

        if (!mr) {
            shiftValue = 0;
        } else if (!pl) {
            shiftValue = latchValue;
        }

        if (shcp && !lastShcp) {
            shiftValue = (shiftValue << 1) | (ds ? 1 : 0);
        }

        lastShcp = shcp;
        lastStcp = stcp;
    }

    @Override
    public void writeOutputs() {
        Val.setValue(shiftValue);
        Q7.setValue((shiftValue >> 7) & 1);
    }

    @Override
    public void setInputs(ObservableValues inputs) throws NodeException {
        mrIn = inputs.get(0).addObserverToValue(this).checkBits(1, this);
        shcpIn = inputs.get(1).addObserverToValue(this).checkBits(1, this);
        dsIn = inputs.get(2).addObserverToValue(this).checkBits(1, this);
        stcpIn = inputs.get(3).addObserverToValue(this).checkBits(1, this);
        plIn = inputs.get(4).addObserverToValue(this).checkBits(1, this);
        d7In = inputs.get(5).addObserverToValue(this).checkBits(1, this);
        d6In = inputs.get(6).addObserverToValue(this).checkBits(1, this);
        d5In = inputs.get(7).addObserverToValue(this).checkBits(1, this);
        d4In = inputs.get(8).addObserverToValue(this).checkBits(1, this);
        d3In = inputs.get(9).addObserverToValue(this).checkBits(1, this);
        d2In = inputs.get(10).addObserverToValue(this).checkBits(1, this);
        d1In = inputs.get(11).addObserverToValue(this).checkBits(1, this);
        d0In = inputs.get(12).addObserverToValue(this).checkBits(1, this);
    }

    @Override
    public ObservableValues getOutputs() {
        return ovs(Q7, Val);
    }
}
