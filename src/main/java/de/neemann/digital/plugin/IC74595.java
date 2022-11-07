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

public class IC74595 extends Node implements Element {
    public static final ElementTypeDescription DESCRIPTION = new ElementTypeDescription("74595", IC74595.class,
            input("SER"),
            input("SRCLK").setClock(),
            input("~SRCLR"),
            input("RCLK"),
            input("~OE")) {
        @Override
        public String getDescription(ElementAttributes elementAttributes) {
            return "74xx595";
        }
    }.addAttribute(Keys.ROTATE);

    private final ObservableValue QA;
    private final ObservableValue QB;
    private final ObservableValue QC;
    private final ObservableValue QD;
    private final ObservableValue QE;
    private final ObservableValue QF;
    private final ObservableValue QG;
    private final ObservableValue QH;
    private final ObservableValue QHp;

    private ObservableValue serIn;
    private ObservableValue srclkIn;
    private ObservableValue srclrIn;
    private ObservableValue rclkIn;
    private ObservableValue oeIn;

    private boolean lastSrClk;
    private boolean lastRClk;

    private boolean oe;

    private long shiftValue;
    private long latchValue;

    public IC74595(ElementAttributes attr) {
        QA = new ObservableValue("QA", 1);
        QB = new ObservableValue("QB", 1);
        QC = new ObservableValue("QC", 1);
        QD = new ObservableValue("QD", 1);
        QE = new ObservableValue("QE", 1);
        QF = new ObservableValue("QF", 1);
        QG = new ObservableValue("QG", 1);
        QH = new ObservableValue("QH", 1);
        QHp = new ObservableValue("QHp", 1);
    }

    @Override
    public void readInputs() {
        oe = oeIn.getBool();
        boolean ser = serIn.getBool();
        boolean srclk = srclkIn.getBool();
        boolean rclk = rclkIn.getBool();
        boolean srclr = srclrIn.getBool();

        if (srclr) {
            if (rclk && !lastRClk) {
                latchValue = shiftValue;
            }

            if (srclk && !lastSrClk) {
                shiftValue = (shiftValue << 1) | (ser ? 1 : 0);
            }
        } else {
            shiftValue = 0;
            latchValue = 0;
        }

        lastRClk = rclk;
        lastSrClk = srclk;
    }

    @Override
    public void writeOutputs() {
        if (oe) {
            QA.setToHighZ();
            QB.setToHighZ();
            QC.setToHighZ();
            QD.setToHighZ();
            QE.setToHighZ();
            QF.setToHighZ();
            QG.setToHighZ();
            QH.setToHighZ();
        } else {
            QA.setValue((latchValue >> 0) & 1);
            QB.setValue((latchValue >> 1) & 1);
            QC.setValue((latchValue >> 2) & 1);
            QD.setValue((latchValue >> 3) & 1);
            QE.setValue((latchValue >> 4) & 1);
            QF.setValue((latchValue >> 5) & 1);
            QG.setValue((latchValue >> 6) & 1);
            QH.setValue((latchValue >> 7) & 1);
        }
        QHp.setValue((shiftValue >> 7) & 1);
    }

    @Override
    public void setInputs(ObservableValues inputs) throws NodeException {
        serIn = inputs.get(0).addObserverToValue(this).checkBits(1, this);
        srclkIn = inputs.get(1).addObserverToValue(this).checkBits(1, this);
        srclrIn = inputs.get(2).addObserverToValue(this).checkBits(1, this);
        rclkIn = inputs.get(3).addObserverToValue(this).checkBits(1, this);
        oeIn = inputs.get(4).addObserverToValue(this).checkBits(1, this);
    }

    @Override
    public ObservableValues getOutputs() {
        return ovs(QH, QG, QF, QE, QD, QC, QB, QA, QHp);
    }
}
