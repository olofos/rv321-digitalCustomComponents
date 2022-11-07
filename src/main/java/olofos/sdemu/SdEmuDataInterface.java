package olofos.sdemu;

public interface SdEmuDataInterface {
    int getValue(int address);

    void setValue(int address, int value);

    int getSize();
}
