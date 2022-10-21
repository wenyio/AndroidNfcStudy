package io.weny.AndroidNfcStudy;

import static io.weny.AndroidNfcStudy.MyHostApduService.HexStringToByteArray;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;

import java.io.IOException;
import java.util.Arrays;

/**
 * 读卡DEMO
 */
public class MainActivity extends AppCompatActivity {

    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private IntentFilter[] mIntentFilter;
    private String[][] mTechList;

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = findViewById(R.id.tv);
        mTextView.setText("Scan a TAG!");

        nfcCheck();

        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),  PendingIntent.FLAG_MUTABLE);

        IntentFilter intentFilter1 = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            intentFilter1.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            e.printStackTrace();
        }
        IntentFilter intentFilter2 = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        // ...
        mIntentFilter = new IntentFilter[]{intentFilter1, intentFilter2};

        mTechList = null;

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mTextView.setText("Scan a Tag: " + intent.getParcelableExtra(NfcAdapter.EXTRA_TAG));
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) {
            mTextView.setText("tag = null");
            return;
        }
        String parse = parse(tag);
        mTextView.setText("parse: \n" + parse);
    }

    // ================ parse IsoDep begin =================
    private static final String SAMPLE_LOYALTY_CARD_AID = "F222222222";
    // ISO-DEP command HEADER for selecting an AID.
    // Format: [Class | Instruction | Parameter 1 | Parameter 2]
    private static final String SELECT_APDU_HEADER = "00A40400";
    // "OK" status word sent in response to SELECT AID command (0x9000)
    private static final byte[] SELECT_OK_SW = {(byte) 0x90, (byte) 0x00};


    public String parse(Tag tag) {
        IsoDep isoDep = IsoDep.get(tag);
        try {
            isoDep.connect();
            byte[] command = BuildSelectApdu(SAMPLE_LOYALTY_CARD_AID);
            byte[] result = isoDep.transceive(command);
            int resultLength = result.length;
            byte[] statusWord = {result[resultLength - 2], result[resultLength - 1]};
            byte[] payload = Arrays.copyOf(result, resultLength - 2);
            if (Arrays.equals(SELECT_OK_SW, statusWord)) {
                return new String(payload, "UTF-8");
            }
            return "Select failed";
        } catch (IOException e) {
            e.printStackTrace();
            return e.getMessage();
        } finally {
            try {
                isoDep.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    // ================ parse IsoDep end =================


    /**
     * Build APDU for SELECT AID command. This command indicates which service a reader is
     * interested in communicating with. See ISO 7816-4.
     *
     * @param aid Application ID (AID) to select
     * @return APDU for SELECT AID command
     */
    private byte[] BuildSelectApdu(String aid) {
        // Format: [CLASS | INSTRUCTION | PARAMETER 1 | PARAMETER 2 | LENGTH | DATA]
        return HexStringToByteArray(SELECT_APDU_HEADER + String.format("%02X", aid.length() / 2) + aid);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, mIntentFilter, mTechList);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNfcAdapter.disableForegroundDispatch(this);
    }

    private void nfcCheck() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            // TODO
            return;
        } else {
            if (!mNfcAdapter.isEnabled()) {
                Intent setNfc = new Intent(Settings.ACTION_NFC_SETTINGS);
                startActivity(setNfc);
            }
        }
    }
}