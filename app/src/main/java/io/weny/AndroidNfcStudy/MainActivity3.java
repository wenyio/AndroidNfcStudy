package io.weny.AndroidNfcStudy;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;

public class MainActivity3 extends AppCompatActivity {

    private Context mContext;

    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        mContext = this;

        // init UI

        // check NFC
        nfcCheck();

        // init NFC
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableForegroundDispatch();
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableForegroundDispatch();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    public static final byte[] KEY_A =
            {(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF};

    private void resolveIntent(Intent intent) {
        String action = intent.getAction();
        Log.d("MWY", "action = " + action);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (supportedTechs(tag.getTechList())) {
            MifareClassic mfc = MifareClassic.get(tag);
            if (mfc != null) {
                // readMifare(mfc);
                writeMifare(mfc);
            } else {
                Log.i("MWY", "Your Tag is not MifareClassic!");
            }
        }
    }

    private void writeMifare(MifareClassic mfc) {
        Boolean isAuth = false;
        try {
            mfc.connect();
            // 一共0-15 16个扇区，默认密码（可以自己定义）
            if (!mfc.authenticateSectorWithKeyA(1, MifareClassic.KEY_DEFAULT)) {
                // 假设使用其他密码验证
                if (mfc.authenticateSectorWithKeyA(1, MifareClassic.KEY_DEFAULT)) {
                    mfc.writeBlock(1, "0123456789123456".getBytes()); // 一共0-3 4个块，写入的数据必须是16 bytes（字节）
                }
            } else {
                Log.i("MWY", "Failed to write as the false KEY!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readMifare(MifareClassic mfc) {
        Boolean isAuth = false;
        try {
            mfc.connect();
            int sectorCount = mfc.getSectorCount();
            Log.i("MWY", "sectorCount = " + sectorCount);
            for (int i = 0; i < sectorCount; i++) {
                if (mfc.authenticateSectorWithKeyA(i, MifareClassic.KEY_DEFAULT)) {
                    isAuth = true;
                } else if (mfc.authenticateSectorWithKeyA(i, KEY_A)) {
                    isAuth = true;
                } else {
                    isAuth = false;
                }
                if (isAuth) {
                    int nBlock = mfc.getBlockCountInSector(i);
                    Log.i("MWY", "nBlock = " + nBlock);
                    for (int j = 0; j < nBlock; j++) {
                        byte[] data = mfc.readBlock(j);
                        Log.i("MWY", "data " + j + " = " + Arrays.toString(data));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean supportedTechs(String[] techList) {
        boolean isSupport = false;
        for (String s : techList) {
            Log.i("MWY", "All SupportedTechs: " + s);
        }
        for (String s : techList) {
            if (s.equals("android.nfc.tech.MifareClassic")) {
                isSupport = true;
            } else if (s.equals("android.nfc.tech.MifareUltralight")) {
                isSupport = true;
            } else if (s.equals("android.nfc.tech.Ndef")) {
                isSupport = true;
            } else if (s.equals("android.nfc.tech.NfcA")) {
                isSupport = true;
            } else if (s.equals("android.nfc.tech.NfcB")) {
                isSupport = true;
            } else {
                isSupport = false;
            }
        }
        return isSupport;
    }

    private void nfcCheck() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(mContext, "设备不支持NFC功能", Toast.LENGTH_LONG).show();
            finish();
        } else {
            if (!mNfcAdapter.isEnabled()) {
                Intent setNfc = new Intent(Settings.ACTION_NFC_SETTINGS);
                startActivity(setNfc);
            }
        }
    }

    private void enableForegroundDispatch() {
        if (mNfcAdapter != null) {
            mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
        }
    }

    private void disableForegroundDispatch() {
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);
        }
    }
}