package io.weny.AndroidNfcStudy;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.Arrays;

/**
 * Android 要求仅使用 Nfc-A (ISO/IEC 14443-3 Type A) 技术模拟 ISO-DEP。也可以支持 Nfc-B (ISO/IEC 14443-4 Type B) 技术。
 */
public class MyHostApduService extends HostApduService {

    private static final String TAG = "CardEmulation";
    // AID for our loyalty card service.
    private static final String SAMPLE_LOYALTY_CARD_AID = "F222222222";
    // ISO-DEP command HEADER for selecting an AID.
    // Format: [Class | Instruction | Parameter 1 | Parameter 2]
    private static final String SELECT_APDU_HEADER = "00A40400";
    private static final String UPDATE_APDU_HEADER = "00B40400";
    // Format: [Class | Instruction | Parameter 1 | Parameter 2]
    private static final String GET_DATA_APDU_HEADER = "00CA0000";
    // "OK" status word sent in response to SELECT AID command (0x9000)
    private static final byte[] SELECT_OK_SW = HexStringToByteArray("9000");
    // "UNKNOWN" status word sent in response to invalid APDU command (0x0000)
    private static final byte[] UNKNOWN_CMD_SW = HexStringToByteArray("0000");

    private static final String WRITE_DATA_APDU_HEADER = "00DA0000";
    private static final String READ_DATA_APDU_HEADER = "00EA0000";
    private static String dataStr = null;

    /*File IO Stuffs*/
    File sdcard = Environment.getExternalStorageDirectory();
    File file = new File(sdcard, "file.txt");
    StringBuilder text = new StringBuilder();
    int pointer;

    /**
     * 只要 NFC 读取器向您的服务发送应用协议数据单元 (APDU)，系统就会调用 processCommandApdu()
     * APDU 也在 ISO/IEC 7816-4 规范中定义。APDU 是在 NFC 读取器和您的 HCE 服务之间进行交换的应用级数据包。
     * 该应用级协议为半双工：NFC 读取器会向您发送命令 APDU，反之它会等待您发送响应 APDU。
     *
     * @param commandApdu
     * @param extras
     * @return
     */
    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        Log.i(TAG, "Received APDU: " + ByteArrayToHexString(commandApdu));
        if (commandApdu.length < 6) {
            return UNKNOWN_CMD_SW;
        }
        StringBuffer buffer = new StringBuffer();
//        byte[] header = new byte[4];
//        int pos = 0;
//        System.arraycopy(commandApdu, pos, header, 0, header.length);
//        buffer.append("header:").append(ByteArrayToHexString(header));
//        pos += header.length;
//        if (commandApdu.length == 6) {
//            return SELECT_OK_SW;
//        }
//        int dataLen = Integer.parseInt(ByteArrayToHexString(new byte[]{commandApdu[pos++]}));
//        buffer.append("\ndataLen:").append(dataLen);
//        if (commandApdu.length < pos + dataLen) {
//            return ConcatArrays(buffer.toString().getBytes(), SELECT_OK_SW);
//        }
//        byte[] data = new byte[dataLen];
//        System.arraycopy(commandApdu, pos, data, 0, dataLen);
//        buffer.append("\ndata:").append(ByteArrayToHexString(data));
//        Log.d("MWY", String.valueOf(ConcatArrays(buffer.toString().getBytes(), SELECT_OK_SW)));
        buffer.append(MainActivity6.mPayload);
        return ConcatArrays(buffer.toString().getBytes(), SELECT_OK_SW);
    }

    /**
     * onDeactivated() 卡片移走或断开连接时调用
     *
     * @param reason
     */
    @Override
    public void onDeactivated(int reason) {
    }

    /**
     * Build APDU for SELECT AID command. This command indicates which service a reader is
     * interested in communicating with. See ISO 7816-4.
     *
     * @param aid Application ID (AID) to select
     * @return APDU for SELECT AID command
     */
    public static byte[] BuildSelectApdu(String aid) {
        // Format: [CLASS | INSTRUCTION | PARAMETER 1 | PARAMETER 2 | LENGTH | DATA]
        return HexStringToByteArray(SELECT_APDU_HEADER + String.format("%02X", aid.length() / 2) + aid);
    }


    /**
     * Utility method to convert a byte array to a hexadecimal string.
     *
     * @param bytes Bytes to convert
     * @return String, containing hexadecimal representation.
     */
    public static String ByteArrayToHexString(byte[] bytes) {
        final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] hexChars = new char[bytes.length * 2]; // Each byte has two hex characters (nibbles)
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF; // Cast bytes[j] to int, treating as unsigned value
            hexChars[j * 2] = hexArray[v >>> 4]; // Select hex character from upper nibble
            hexChars[j * 2 + 1] = hexArray[v & 0x0F]; // Select hex character from lower nibble
        }
        return new String(hexChars);
    }

    /**
     * Utility method to convert a hexadecimal string to a byte string.
     *
     * <p>Behavior with input strings containing non-hexadecimal characters is undefined.
     *
     * @param s String containing hexadecimal characters to convert
     * @return Byte array generated from input
     * @throws java.lang.IllegalArgumentException if input length is incorrect
     */
    public static byte[] HexStringToByteArray(String s) throws IllegalArgumentException {
        int len = s.length();
        if (len % 2 == 1) {
            throw new IllegalArgumentException("Hex string must have even number of characters");
        }
        byte[] data = new byte[len / 2]; // Allocate 1 byte per 2 hex characters
        for (int i = 0; i < len; i += 2) {
            // Convert each character into a integer (base-16), then bit-shift into place
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Utility method to concatenate two byte arrays.
     *
     * @param first First array
     * @param rest  Any remaining arrays
     * @return Concatenated copy of input arrays
     */
    public static byte[] ConcatArrays(byte[] first, byte[]... rest) {
        int totalLength = first.length;
        for (byte[] array : rest) {
            totalLength += array.length;
        }
        byte[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (byte[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

    private void readFromFile() {
//        try {
//            BufferedReader br = new BufferedReader(new FileReader(file));
//            String line;
//
//            while ((line = br.readLine()) != null) {
//                text.append(line);
//                text.append('\n');
//            }
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//        }
        text.append("some string random data some string random data some string random data some string random data some string random data \n");
        text.append("some string random data some string random data some string random data some string random data some string random data \n");
        text.append("some string random data some string random data some string random data some string random data some string random data \n");
        text.append("some string random data some string random data some string random data some string random data some string random data \n");
        text.append("some string random data some string random data some string random data some string random data some string random data \n");
        text.append("some string random data some string random data some string random data some string random data some string random data \n");
    }
}