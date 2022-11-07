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

public class ICHEF4557B extends Node implements Element {
    public static final ElementTypeDescription DESCRIPTION = new ElementTypeDescription("HEF4557B", ICHEF4557B.class,
            input("DA"),
            input("DB"),
            input("A/~B"),
            input("~CP1"),
            input("CP0"),
            input("MR"),
            input("L32"),
            input("L16"),
            input("L8"),
            input("L4"),
            input("L2"),
            input("L1")) {
        @Override
        public String getDescription(ElementAttributes elementAttributes) {
            return "HEF457B";
        }
    }.addAttribute(Keys.ROTATE);

    private final ObservableValue Q;
    private final ObservableValue Qb;

    private ObservableValue daIn;
    private ObservableValue dbIn;
    private ObservableValue abIn;
    private ObservableValue cp1In;
    private ObservableValue cp0In;
    private ObservableValue mrIn;
    private ObservableValue l32In;
    private ObservableValue l16In;
    private ObservableValue l8In;
    private ObservableValue l4In;
    private ObservableValue l2In;
    private ObservableValue l1In;

    private boolean lastClk;
    private boolean out;

    private long ff32;
    private long ff16;
    private long ff8;
    private long ff4;
    private long ff2;
    private long ff1;

    public ICHEF4557B(ElementAttributes attr) {
        Q = new ObservableValue("Q", 1);
        Qb = new ObservableValue("~Q", 1);
    }

    @Override
    public void readInputs() {
        boolean da = daIn.getBool();
        boolean db = dbIn.getBool();
        boolean ab = abIn.getBool();
        boolean cp1 = cp1In.getBool();
        boolean cp0 = cp0In.getBool();
        boolean mr = mrIn.getBool();
        boolean l32 = l32In.getBool();
        boolean l16 = l16In.getBool();
        boolean l8 = l8In.getBool();
        boolean l4 = l4In.getBool();
        boolean l2 = l2In.getBool();
        boolean l1 = l1In.getBool();

        boolean clk = cp0 && !cp1;
        boolean in = ab ? da : db;

        if (mr) {
            ff32 = 0;
            ff16 = 0;
            ff8 = 0;
            ff4 = 0;
            ff2 = 0;
            ff1 = 0;
            out = false;
        } else {
            if (clk && !lastClk) {
                boolean d = in;
                boolean q;
                q = (ff32 & 1) != 0;
                ff32 = (ff32 >> 1) | (d ? (1L << 31) : 0);
                if (l32) {
                    d = q;
                }
                q = (ff16 & 1) != 0;
                ff16 = (ff16 >> 1) | (d ? (1 << 15) : 0);
                if (l16) {
                    d = q;
                }
                q = (ff8 & 1) != 0;
                ff8 = (ff8 >> 1) | (d ? (1 << 7) : 0);
                if (l8) {
                    d = q;
                }
                q = (ff4 & 1) != 0;
                ff4 = (ff4 >> 1) | (d ? (1 << 3) : 0);
                if (l4) {
                    d = q;
                }
                q = (ff2 & 1) != 0;
                ff2 = (ff2 >> 1) | (d ? (1 << 1) : 0);
                if (l2) {
                    d = q;
                }
                q = (ff1 & 1) != 0;
                ff1 = (ff1 >> 1) | (d ? (1 << 0) : 0);
                if (l1) {
                    d = q;
                }
                out = d;
            }
        }

        lastClk = clk;
    }

    @Override
    public void writeOutputs() {
        Q.setBool(out);
        Qb.setBool(!out);
    }

    @Override
    public void setInputs(ObservableValues inputs) throws NodeException {
        daIn = inputs.get(0).addObserverToValue(this).checkBits(1, this);
        dbIn = inputs.get(1).addObserverToValue(this).checkBits(1, this);
        abIn = inputs.get(2).addObserverToValue(this).checkBits(1, this);
        cp1In = inputs.get(3).addObserverToValue(this).checkBits(1, this);
        cp0In = inputs.get(4).addObserverToValue(this).checkBits(1, this);
        mrIn = inputs.get(5).addObserverToValue(this).checkBits(1, this);
        l32In = inputs.get(6).addObserverToValue(this).checkBits(1, this);
        l16In = inputs.get(7).addObserverToValue(this).checkBits(1, this);
        l8In = inputs.get(8).addObserverToValue(this).checkBits(1, this);
        l4In = inputs.get(9).addObserverToValue(this).checkBits(1, this);
        l2In = inputs.get(10).addObserverToValue(this).checkBits(1, this);
        l1In = inputs.get(11).addObserverToValue(this).checkBits(1, this);
    }

    @Override
    public ObservableValues getOutputs() {
        return ovs(Q, Qb);
    }
}
