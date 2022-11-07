package de.neemann.digital.plugin;

import de.neemann.digital.core.Node;
import de.neemann.digital.core.NodeException;
import de.neemann.digital.core.ObservableValue;
import de.neemann.digital.core.ObservableValues;
import de.neemann.digital.core.element.*;

import static de.neemann.digital.core.element.PinInfo.input;

public class RegViewer extends Node implements Element {
    public static final ElementTypeDescription DESCRIPTION = new ElementTypeDescription(RegViewer.class,
            input("BUS-CLK", "Clock"),
            input("~{REG-IN-EN}", "Enable"),
            input("REG-IN", "Input"),
            input("RSD", "Address"))
            .addAttribute(Keys.ROTATE);

    private ObservableValue busClkIn;
    private ObservableValue regInEnIn;
    private ObservableValue regInIn;
    private ObservableValue rsdIn;

    private ObservableValue lastWriteOut;
    private ObservableValue[] regOut;

    private long[] values;

    private long reg;
    private int rsd;
    private int lastWrite;

    private boolean lastBusClk;
    private boolean lastRegInEn;

    public RegViewer(ElementAttributes attr) {
        super(true);
        values = new long[32];
        regOut = new ObservableValue[values.length];

        lastWriteOut = new ObservableValue("RSD", 5);
        for (int i = 0; i < values.length; i++) {
            regOut[i] = new ObservableValue("Reg" + i, 32);
        }
    }

    @Override
    public void readInputs() {
        boolean busClk = busClkIn.getBool();
        boolean regInEn = regInEnIn.getBool();
        boolean regIn = regInIn.getBool();

        if (busClk && !lastBusClk) {
            reg = (reg >> 1) | (regIn ? 0x80000000L : 0);
        }

        if (!regInEn && lastRegInEn) {
            rsd = (int) rsdIn.getValue() % values.length;
        }

        if (regInEn && !lastRegInEn) {
            if (rsd != 0) {
                values[rsd] = reg;
            }
            lastWrite = rsd;
        }

        lastBusClk = busClk;
        lastRegInEn = regInEn;
    }

    @Override
    public void writeOutputs() {
        for (int i = 0; i < values.length; i++) {
            regOut[i].setValue(values[i]);
        }
        lastWriteOut.setValue(lastWrite);
    }

    @Override
    public void setInputs(ObservableValues inputs) throws NodeException {

        busClkIn = inputs.get(0).addObserverToValue(this).checkBits(1, this);
        regInEnIn = inputs.get(1).addObserverToValue(this).checkBits(1, this);
        regInIn = inputs.get(2).addObserverToValue(this).checkBits(1, this);
        rsdIn = inputs.get(3).addObserverToValue(this).checkBits(5, this);
    }

    @Override
    public ObservableValues getOutputs() {
        ObservableValues.Builder ov = new ObservableValues.Builder();
        ov.add(lastWriteOut);
        ov.add(regOut);
        return ov.build();
    }
}
