package olofos.sdemu;

import java.util.Queue;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.LinkedList;

public class SdEmu {
    private enum State {
        IDLE,
        READY,
        READING_BLOCK,
        READING_BLOCKS,
        WRITING_BLOCK,
        WRITING_BLOCK_BUSY,
        WRITING_BLOCKS,
    };

    private static final int CMD_LEN = 6;
    private static final int SECTOR_SIZE = 512;
    private static final int CMD_MASK = 0b0011_1111;
    private static final int CMD_BIT = 0b0100_0000;

    private static final int R1_READY = 0x00;
    private static final int R1_IDLE = 0x01;
    // private static final int R1_ERASE_RESET = 0x02;
    private static final int R1_ILLEGAL_COMMAND = 0x04;
    // private static final int R1_COM_CRC_ERR = 0x08;
    // private static final int R1_ERASE_SEQ_ERR = 0x10;
    private static final int R1_ADDRESS_ERR = 0x20;
    private static final int R1_PARAM_ERR = 0x40;

    private static final int ACMD = 0x100;

    private static final int CMD0 = 0;
    private static final int CMD1 = 1;
    private static final int CMD8 = 8;
    private static final int CMD12 = 12;
    private static final int CMD17 = 17;
    private static final int CMD18 = 18;
    private static final int CMD24 = 24;
    private static final int CMD25 = 25;
    private static final int CMD55 = 55;
    private static final int CMD58 = 58;
    private static final int CMD59 = 59;

    private static final int CMD_ALL = 0xFF;

    private static final int ACMD23 = 23 | ACMD;
    private static final int ACMD41 = 41 | ACMD;

    private final Queue<Integer> output;
    private final LinkedList<Integer> cmdBuf;
    private State state;
    private int readAddress;
    private int writeAddress;
    private final LinkedList<Integer> writeBuffer;
    private boolean appCmd;
    private int blockEraseCount;
    private boolean checkCrc;

    private SdEmuDataInterface data;

    public SdEmu(SdEmuDataInterface data) {
        this.data = data;
        cmdBuf = new LinkedList<>();
        output = new ArrayDeque<>();
        writeBuffer = new LinkedList<>();

        state = State.IDLE;
        appCmd = false;
        blockEraseCount = 0;

        checkCrc = false;
    }

    // CRC calculations from
    // https://github.com/LonelyWolf/stm32/blob/master/stm32l-dosfs/sdcard.c

    static private int crc7Step(int crcIn, int value) {
        int g = 0x89;

        crcIn ^= value;
        for (int i = 0; i < 8; i++) {
            if ((crcIn & 0x80) != 0)
                crcIn ^= g;
            crcIn <<= 1;
        }

        return crcIn & 0xFF;
    }

    static private int crc16Step(int crcIn, int value) {
        crcIn = ((crcIn >> 8) & 0xFF) | (crcIn << 8);
        crcIn ^= value;
        crcIn ^= (int) (crcIn & 0xff) >> 4;
        crcIn ^= (crcIn << 8) << 4;
        crcIn ^= ((crcIn & 0xff) << 4) << 1;

        return crcIn & 0xFFFF;
    }

    private void outputSector(int readAddress) {
        int crc = 0;
        int word = 0;
        addOutput(0xFE);
        for (int i = 0; i < SECTOR_SIZE; i++) {
            int val = data.getValue(readAddress);
            addOutput(val);
            readAddress = (readAddress + 1) % data.getSize();
            if ((i % 2) == 0) {
                word = val << 8;
            } else {
                word |= val;
                crc = crc16Step(crc, word);
            }
        }
        addOutput((crc >> 8) & 0xFF, crc & 0xFF);
    }

    static private int calculateBufferCrc(LinkedList<Integer> buf) {
        int crc = 0;
        int word = 0;
        for (int i = 0; i < SECTOR_SIZE; i++) {
            int val = buf.get(i);
            if (i % 2 == 0) {
                word = val << 8;
            } else {
                word |= val;
                crc = crc16Step(crc, word);
            }
        }
        return crc;
    }

    private void writeSector() {
        System.out.println(String.format("Next block [%08X]", writeAddress));
        writeBuffer.removeFirst(); // 0xFE
        int crc0 = writeBuffer.removeLast();
        int crc1 = writeBuffer.removeLast();

        int foundCrc = (crc0 << 8) | crc1;
        int expectedCrc = calculateBufferCrc(writeBuffer);

        if (checkCrc) {
            if (foundCrc != expectedCrc) {
                System.out.println(String.format("CRC error. Expected %04X but found %04X", expectedCrc, foundCrc));
                addOutput(0x0B, 0x00, 0x00);
                return;
            }
        }

        if (writeAddress + SECTOR_SIZE > data.getSize()) {
            addOutput(0x0D, 0x00, 0x00);
            return;
        }

        for (int i = 0; i < SECTOR_SIZE; i++) {
            data.setValue(writeAddress, writeBuffer.removeFirst() & 0xFF);
            writeAddress = (writeAddress + 1) % data.getSize();
        }

        addOutput(0x05, 0x00, 0x00);
    }

    public int transferByte(int value) {
        int result = getOutput();
        switch (state) {
            case IDLE:
                handleCommand(value, CMD0, CMD1, CMD8, ACMD41, CMD55, CMD58, CMD59);
                break;
            case READY:
                handleCommand(value, CMD_ALL);
                break;
            case READING_BLOCK:
                if (output.isEmpty()) {
                    state = State.READY;
                }
                break;

            case READING_BLOCKS:
                if (output.isEmpty()) {
                    System.out.println(String.format("Next block [%08X]", readAddress));
                    addOutput(0xFF, 0xFF);
                    outputSector(readAddress);
                }

                if (cmdBuf.isEmpty() && (value != (CMD_BIT | CMD12))) {
                    break;
                }
                cmdBuf.add(value);
                if (cmdBuf.size() == CMD_LEN) {
                    System.out.println("STOP_TRANSMISSION");
                    output.clear();
                    state = State.READY;
                    addOutput(0xFF, 0xFF, 0xFF, R1());
                    cmdBuf.clear();
                }

                break;

            case WRITING_BLOCK:
                if (writeBuffer.isEmpty() && (value != 0xFE)) {
                    break;
                }
                writeBuffer.add(value);
                if (writeBuffer.size() == 1 + SECTOR_SIZE + 2) {
                    writeSector();
                    state = State.WRITING_BLOCK_BUSY;
                }
                break;

            case WRITING_BLOCK_BUSY:
                if (output.isEmpty()) {
                    state = State.READY;
                }
                break;

            case WRITING_BLOCKS:
                if (writeBuffer.isEmpty() && (value != 0xFC)) {
                    if (value == 0xFD) {
                        addOutput(0xFF, 0x00, 0x00);
                        state = State.WRITING_BLOCK_BUSY;
                    }
                    break;
                }

                writeBuffer.add(value);
                if (writeBuffer.size() == 1 + SECTOR_SIZE + 2) {
                    writeSector();
                }
                break;
        }

        return result;
    }

    private boolean isCommandAcceptedInState(int cmd, int... acceptedCommands) {
        if ((acceptedCommands.length == 1) && (acceptedCommands[0] == CMD_ALL)) { // Shortcut for all commands
            return true;
        }
        return Arrays.stream(acceptedCommands).anyMatch(x -> x == cmd);
    }

    private void handleStandardCommand(int cmd, int cmdArg, int[] cmdArgBytes, int[] acceptedCommands) {
        if (!isCommandAcceptedInState(cmd, acceptedCommands)) {
            System.out.println("Illegal command CMD" + cmd + " in state " + state);
            addOutput(R1(R1_ILLEGAL_COMMAND));
            return;
        }

        switch (cmd) {
            case CMD0:
                System.out.println("GO_IDLE_STATE");
                state = State.IDLE;
                addOutput(R1());
                break;

            case CMD8:
                System.out.println("SEND_IF_COND");
                if (cmdArgBytes[2] == 0x01) {
                    addOutput(R1(), 0x00, 0x00, 0x01, cmdArgBytes[3]);
                } else {
                    addOutput(R1(R1_PARAM_ERR));
                }
                break;

            case CMD17:
                System.out.println(String.format("READ_SINGLE_BLOCK [%08X]", cmdArg));
                readAddress = cmdArg;
                if (readAddress + SECTOR_SIZE > data.getSize()) {
                    addOutput(R1(R1_ADDRESS_ERR), 0xFF, 0xFF, 0x09, 0xFF);
                } else {
                    addOutput(R1(), 0xFF, 0xFF);
                    outputSector(readAddress);
                }
                state = State.READING_BLOCK;
                break;

            case CMD18:
                System.out.println(String.format("READ_MULTIPLE_BLOCKS [%08X]", cmdArg));
                readAddress = cmdArg;
                addOutput(R1(), 0xFF, 0xFF);
                state = State.READING_BLOCKS;
                break;

            case CMD24:
                System.out.println(String.format("WRITE_SINGLE_BLOCK [%08X]", cmdArg));
                writeAddress = cmdArg;
                if (writeAddress + SECTOR_SIZE > data.getSize()) {
                    addOutput(R1(R1_ADDRESS_ERR));
                } else {
                    state = State.WRITING_BLOCK;
                    addOutput(R1());
                }
                break;

            case CMD25:
                System.out.println(String.format("WRITE_MULTIPLE_BLOCKS [%08X]", cmdArg));
                if (writeAddress + SECTOR_SIZE >= data.getSize()) {
                    addOutput(R1(R1_ADDRESS_ERR));
                } else {
                    writeAddress = cmdArg;
                    state = State.WRITING_BLOCKS;
                    addOutput(R1());
                }
                break;

            case CMD55:
                System.out.println("APP_CMD");
                appCmd = true;
                addOutput(R1());
                break;

            case CMD58:
                System.out.println("READ_OCR");
                addOutput(R1(), 0x00, 0x01, 0xFF, 0x00);
                break;

            case CMD59:
                System.out.println("CRC_ON_OFF");
                checkCrc = (cmdArg & 1) == 1;
                addOutput(R1());
                break;

            default:
                System.out.println("Unknown command CMD" + cmd);
                addOutput(R1(R1_ILLEGAL_COMMAND));
                break;
        }
    }

    private void handleAppCommand(int cmd, int cmdArg, int[] cmdArgBytes, int[] acceptedCommands) {
        if (!isCommandAcceptedInState(cmd, acceptedCommands)) {
            System.out.println("Illegal command ACMD" + (cmd - ACMD) + " in state " + state);
            addOutput(R1(R1_ILLEGAL_COMMAND));
            return;
        }

        switch (cmd) {
            case ACMD23:
                blockEraseCount = cmdArg & 0x3FFFFF;
                System.out.println("SET_WR_BLOCK_ERASE_COUNT " + blockEraseCount);
                addOutput(R1());
                break;
            case ACMD41:
                System.out.println("APP_SEND_OP_COND");
                state = State.READY;
                addOutput(R1());
                break;

            default:
                System.out.println("Unknown command ACMD" + (cmd - ACMD));
                addOutput(R1(R1_ILLEGAL_COMMAND));
                break;
        }
    }

    private void handleCommand(int value, int... acceptedCommands) {
        if (cmdBuf.isEmpty() && ((value & ~CMD_MASK) != CMD_BIT)) {
            return;
        }

        cmdBuf.add(value);

        if (cmdBuf.size() < CMD_LEN) {
            return;
        }

        int cmdIn = cmdBuf.remove();
        int expectedCrc = crc7Step(0, cmdIn);
        int cmd = cmdIn & CMD_MASK;
        int[] cmdArgBytes = new int[4];
        int cmdArg = 0;

        for (int i = 0; i < 4; i++) {
            int val = cmdBuf.remove();
            cmdArgBytes[i] = val;
            cmdArg = (cmdArg << 8) | val;
            expectedCrc = crc7Step(expectedCrc, val);
        }
        expectedCrc |= 1;

        int cmdCrc = cmdBuf.remove();
        System.out.print(String.format("[0x%02X, 0x%02X, 0x%02X, 0x%02X, 0x%02X, 0x%02X]: ", cmd | CMD_BIT,
                cmdArgBytes[0], cmdArgBytes[1], cmdArgBytes[2], cmdArgBytes[3], cmdCrc));

        cmdBuf.clear();

        if (checkCrc || (!appCmd && ((cmd == CMD0) || (cmd == CMD8)))) {
            if (cmdCrc != expectedCrc) {
                System.out.println(String.format("CRC mismatch: expected %02X but found %02X", expectedCrc, cmdCrc));
            }
        }

        if (appCmd) {
            handleAppCommand(cmd | ACMD, cmdArg, cmdArgBytes, acceptedCommands);
            appCmd = false;
            return;
        }
        handleStandardCommand(cmd, cmdArg, cmdArgBytes, acceptedCommands);
    }

    private int R1(int... errors) {
        int errorCode = 0;
        for (int e : errors) {
            errorCode |= e;
        }

        int result = state == State.IDLE ? R1_IDLE : R1_READY;
        if (errorCode != 0) {
            result = errorCode;
        }
        return result;
    }

    public void clear() {
        output.clear();
        cmdBuf.clear();
    }

    private int getOutput() {
        if (output.isEmpty()) {
            return 0xFF;
        }
        return output.remove();
    }

    private void addOutput(int... out) {
        for (int val : out) {
            output.add(val);
        }
    }
}
