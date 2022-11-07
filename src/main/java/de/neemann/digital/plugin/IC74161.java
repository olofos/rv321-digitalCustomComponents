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

public class IC74161 extends Node implements Element {
    public static final ElementTypeDescription DESCRIPTION = new ElementTypeDescription("74161", IC74161.class,
            input("~LD"),
            input("~CLR"),
            input("CLK").setClock(),
            input("ENT"),
            input("ENP"),
            input("A"),
            input("B"),
            input("C"),
            input("D")) {
        @Override
        public String getDescription(ElementAttributes elementAttributes) {
            return "74xx161";
        }
    }.addAttribute(Keys.ROTATE);

    private final ObservableValue QA;
    private final ObservableValue QB;
    private final ObservableValue QC;
    private final ObservableValue QD;
    private final ObservableValue RCO;

    private ObservableValue ldIn;
    private ObservableValue clrIn;
    private ObservableValue clkIn;
    private ObservableValue entIn;
    private ObservableValue enpIn;
    private ObservableValue aIn;
    private ObservableValue bIn;
    private ObservableValue cIn;
    private ObservableValue dIn;

    private boolean lastClk;

    private long value;
    private boolean rco;

    public IC74161(ElementAttributes attr) {
        QA = new ObservableValue("QA", 1);
        QB = new ObservableValue("QB", 1);
        QC = new ObservableValue("QC", 1);
        QD = new ObservableValue("QD", 1);
        RCO = new ObservableValue("RCO", 1);
    }

    @Override
    public void readInputs() {
        boolean ld = ldIn.getBool();
        boolean clr = clrIn.getBool();
        boolean clk = clkIn.getBool();
        boolean ent = entIn.getBool();
        boolean enp = enpIn.getBool();
        boolean a = aIn.getBool();
        boolean b = bIn.getBool();
        boolean c = cIn.getBool();
        boolean d = dIn.getBool();

        if (!clr) {
            value = 0;
        } else {
            if (clk && !lastClk) {
                if (!ld) {
                    value = (d ? 0x8 : 0) | (c ? 0x4 : 0) | (b ? 0x2 : 0) | (a ? 0x1 : 0);
                } else if (ent && enp) {
                    value = (value + 1) & 0xF;
                }
            }
        }

        rco = ent && (value == 0xF);

        lastClk = clk;
    }

    @Override
    public void writeOutputs() {
        QA.setValue((value >> 0) & 1);
        QB.setValue((value >> 1) & 1);
        QC.setValue((value >> 2) & 1);
        QD.setValue((value >> 3) & 1);

        RCO.setBool(rco);
    }

    @Override
    public void setInputs(ObservableValues inputs) throws NodeException {
        ldIn = inputs.get(0).addObserverToValue(this).checkBits(1, this);
        clrIn = inputs.get(1).addObserverToValue(this).checkBits(1, this);
        clkIn = inputs.get(2).addObserverToValue(this).checkBits(1, this);
        entIn = inputs.get(3).addObserverToValue(this).checkBits(1, this);
        enpIn = inputs.get(4).addObserverToValue(this).checkBits(1, this);
        aIn = inputs.get(5).addObserverToValue(this).checkBits(1, this);
        bIn = inputs.get(6).addObserverToValue(this).checkBits(1, this);
        cIn = inputs.get(7).addObserverToValue(this).checkBits(1, this);
        dIn = inputs.get(8).addObserverToValue(this).checkBits(1, this);
    }

    @Override
    public ObservableValues getOutputs() {
        return ovs(RCO, QA, QB, QC, QD);
    }
}
