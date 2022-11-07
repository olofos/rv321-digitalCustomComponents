package olofos.sdemu;

public class SdEmuSpi {
    private SdEmu sd;

    private int lastClk;
    private int clkCount;
    private int input;
    private int output;

    private int initCount;

    public SdEmuSpi(SdEmuDataInterface data) {
        sd = new SdEmu(data);
        initCount = 76;
        clkCount = 0;
        output = 0xFF;
    }

    public int write(int clk, int di, int cs) {
        int out;
        if (initCount > 0) {
            if ((clk == 1) && (lastClk == 0)) {
                initCount -= 1;
            }
            out = 1;
            sd.clear();
        } else if (cs != 0) {
            clkCount = 0;
            output = 0xFF;
            out = 1;
        } else {
            out = (output >> 7) & 1;
            if ((clk == 1) && (lastClk == 0)) {
                output = (output << 1) & 0xFF;
                input = (input << 1) | di;
                clkCount += 1;
                if (clkCount == 8) {
                    clkCount = 0;
                    output = sd.transferByte(input);
                    input = 0;
                }
            }
        }
        lastClk = clk;
        return out;
    }
}
