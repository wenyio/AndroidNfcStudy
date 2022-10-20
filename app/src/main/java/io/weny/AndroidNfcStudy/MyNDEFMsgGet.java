package io.weny.AndroidNfcStudy;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.util.Log;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class MyNDEFMsgGet {

    /**
     * 封装成NdefMessage
     *
     * @param data
     * @param identifierCode
     * @return
     */
    public static NdefMessage getNdefMsg_RTD_URI(String data, byte identifierCode) {
        Log.i("MWY", "data = " + data);
        byte[] uriField = data.getBytes(StandardCharsets.US_ASCII);
        byte[] payload = new byte[uriField.length + 1];
        payload[0] = identifierCode; // 0x01 = http://www.
        System.arraycopy(uriField, 0, payload, 1, uriField.length);

        NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.RTD_URI, new byte[0], payload);
        return new NdefMessage(new NdefRecord[]{record});
    }

    public static NdefMessage getNdefMsg_RTD_TEXT(String data, boolean encodeInUTF8) {
        Log.i("MWY", "data = " + data);
        Locale locale = new Locale("en", "US");
        byte[] langBytes = locale.getLanguage().getBytes(StandardCharsets.US_ASCII);
        Charset utfEncoding = encodeInUTF8 ? StandardCharsets.UTF_8 : StandardCharsets.UTF_16;
        int utfBit = encodeInUTF8 ? 0 : (1 << 7); // 最高位 1 << 7 = 128
        char status = (char) (utfBit + langBytes.length);
        byte[] textBytes = data.getBytes(utfEncoding);
        byte[] payload = new byte[langBytes.length + textBytes.length + 1];
        payload[0] = (byte) status;
        System.arraycopy(langBytes, 0, payload, 1, langBytes.length); // 复制语言码
        System.arraycopy(textBytes, 0, payload, 1 + langBytes.length, textBytes.length); // 复制实际文本数据
        NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.RTD_TEXT, new byte[0], payload);
        return new NdefMessage(new NdefRecord[]{record});
    }

    public static NdefMessage getNdefMsg_Absolute_URI(String data) {
        Log.i("MWY", "data = " + data);
        byte[] payload = data.getBytes(StandardCharsets.US_ASCII);
        NdefRecord record = new NdefRecord(NdefRecord.TNF_ABSOLUTE_URI,
                new byte[0], new byte[0], payload);
        return new NdefMessage(new NdefRecord[]{record});
    }
}
