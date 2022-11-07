package de.neemann.digital.plugin;

import de.neemann.digital.core.*;
import de.neemann.digital.core.element.Element;
import de.neemann.digital.core.element.ElementAttributes;
import de.neemann.digital.core.element.ElementTypeDescription;
import de.neemann.digital.core.element.Key;
import de.neemann.digital.core.element.Keys;
import de.neemann.digital.core.memory.rom.ROMInterface;
import de.neemann.digital.core.memory.RAMInterface;
import de.neemann.digital.core.memory.DataField;
import de.neemann.digital.core.ValueFormatter;
import de.neemann.digital.draw.elements.VisualElement;
import de.neemann.digital.gui.components.CircuitModifier;
import de.neemann.digital.gui.components.modification.ModifyAttribute;

import olofos.sdemu.SdEmuDataInterface;
import olofos.sdemu.SdEmuSpi;

import static de.neemann.digital.core.element.PinInfo.input;

class SdCardMemory implements SdEmuDataInterface {
    private DataField dataField;

    SdCardMemory(DataField dataField) {
        this.dataField = dataField;
    }

    @Override
    public int getValue(int address) {
        return (int) dataField.getDataWord(address);
    }

    @Override
    public void setValue(int address, int value) {
        dataField.setData(address, value);
    }

    @Override
    public int getSize() {
        return dataField.hgsArraySize();
    }
}

public class SdCard extends Node implements Element, RAMInterface, ROMInterface {
    public static final Key<Boolean> SD_INIT = new Key<>("sdInit", false);

    public static final ElementTypeDescription DESCRIPTION = new ElementTypeDescription(SdCard.class,
            input("CLK").setClock(),
            input("MOSI"),
            input("CS"))
            .addAttribute(Keys.ROTATE)
            .addAttribute(Keys.ADDR_BITS)
            .addAttribute(Keys.BITS)
            .addAttribute(Keys.LABEL)
            .addAttribute(Keys.DATA)
            .addAttribute(Keys.INT_FORMAT)
            .addAttribute(SD_INIT);

    private final ElementAttributes attr;
    private final ValueFormatter formatter;

    private DataField memory;

    private final String label;

    private ObservableValue clkIn;
    private ObservableValue mosiIn;
    private ObservableValue csIn;
    private ObservableValue misoOut;

    private int miso;
    private boolean cs;

    private SdEmuSpi spi;

    int din;
    int dout;

    public SdCard(ElementAttributes attr) {
        super(true);
        this.attr = attr;
        label = attr.getLabel();
        formatter = attr.getValueFormatter();
        memory = new DataField(attr.get(Keys.DATA));
        misoOut = new ObservableValue("MISO", 1)
                .setToHighZ()
                .setPinDescription(DESCRIPTION);
        miso = 1;

        din = 0xFF;
        dout = 0xFF;

        spi = new SdEmuSpi(new SdCardMemory(memory));
    }

    @Override
    public void setInputs(ObservableValues inputs) throws NodeException {
        clkIn = inputs.get(0).checkBits(1, this).addObserverToValue(this);
        mosiIn = inputs.get(1).checkBits(1, this).addObserverToValue(this);
        csIn = inputs.get(2).checkBits(1, this).addObserverToValue(this);
    }

    @Override
    public void readInputs() throws NodeException {
        cs = csIn.getBool();
        miso = spi.write((int) clkIn.getValue(), (int) mosiIn.getValue(), (int) csIn.getValue());
    }

    @Override
    public void writeOutputs() throws NodeException {
        if (cs) {
            misoOut.setToHighZ();
        } else {
            misoOut.setValue(miso);
        }
    }

    @Override
    public void enableCircuitModification(VisualElement visualElement, CircuitModifier circuitModifier) {
        getModel().addObserver(event -> {
            if (event.getType() == ModelEventType.CLOSED) {
                DataField orig = attr.get(Keys.DATA);
                memory.trim();
                if (!orig.equals(memory))
                    circuitModifier.modify(new ModifyAttribute<>(visualElement, Keys.DATA, memory));
            }
        }, ModelEventType.CLOSED);
    }

    @Override
    public ObservableValues getOutputs() {
        return misoOut.asList();
    }

    @Override
    public DataField getMemory() {
        return memory;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public int getAddrBits() {
        return 12;
    }

    @Override
    public int getSize() {
        return 1 << 12;
    }

    @Override
    public int getDataBits() {
        return 8;
    }

    public void setData(DataField data) {
        memory = data;
    }

    @Override
    public boolean isProgramMemory() {
        return false;
    }

    @Override
    public void setProgramMemory(DataField dataField) {
        memory.setDataFrom(dataField);
    }

    @Override
    public ValueFormatter getValueFormatter() {
        return formatter;
    }
}
