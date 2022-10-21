package io.weny.AndroidNfcStudy;

import static io.weny.AndroidNfcStudy.MyHostApduService.HexStringToByteArray;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.cardemulation.CardEmulation;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity6 extends AppCompatActivity {

    private static final List<String> AIDS = new ArrayList<>();
    private static int flag = 0; // 1 读 2 模拟

    private Context mContext;
    private EditText mEditText;
    private Button mSimulateBtn;
    private Button mReadBtn;
    private AlertDialog mAlertDialog;

    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;

    public static String mPayload;

    static {
        AIDS.add("F222222222"); // 卡ID对应上才可以读
    }

    private CardEmulation mCardEmulation;
    private ComponentName mService;

    private static final String SAMPLE_LOYALTY_CARD_AID = "F222222222";
    // ISO-DEP command HEADER for selecting an AID.
    // Format: [Class | Instruction | Parameter 1 | Parameter 2]
    private static final String SELECT_APDU_HEADER = "00A40400";
    // "OK" status word sent in response to SELECT AID command (0x9000)
    private static final byte[] SELECT_OK_SW = {(byte) 0x90, (byte) 0x00};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main6);

        mContext = this;

        initView();

        nfcCheck();

        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE);

        mCardEmulation = CardEmulation.getInstance(mNfcAdapter);
        mService = new ComponentName(this, MyHostApduService.class);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (flag == 1) {
            enableForegroundDispatch();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.i("MWY", "flag = " + flag);
        if (flag == 1) {
            resolveReadIntent(intent);
        }
    }

    private void initView() {
        mEditText = findViewById(R.id.et);
        mPayload = mEditText.getText().toString();
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mPayload = s.toString();
                Log.i("MWY", "mPayload = " + mPayload);
            }
        });

        mSimulateBtn = findViewById(R.id.simulate);
        mSimulateBtn.setOnClickListener(v -> {
            if (mPayload == null || "".equals(mPayload)) {
                Toast.makeText(this, "数据不能为空！", Toast.LENGTH_LONG).show();
                return;
            }
            flag = 2;
            registerAidsForService();
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle("模拟卡")
                    .setMessage("我现在是一张卡")
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            flag = 0;
                            unRegisterAidsForService();
                        }
                    });
            mAlertDialog = builder.create();
            mAlertDialog.setCanceledOnTouchOutside(false);
            mAlertDialog.show();
        });
        mReadBtn = findViewById(R.id.read);
        mReadBtn.setOnClickListener(v -> {
            flag = 1;
            enableForegroundDispatch();
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle("触碰标签读取")
                    .setMessage("将标签靠近设备")
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            flag = 0;
                            disableForegroundDispatch();
                        }
                    });
            mAlertDialog = builder.create();
            mAlertDialog.setCanceledOnTouchOutside(false);
            mAlertDialog.show();
        });
    }

    private void resolveReadIntent(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) {
            mAlertDialog.setMessage("tag = null");
            return;
        }
        IsoDep isoDep = IsoDep.get(tag);
        try {
            isoDep.connect();
            byte[] command = HexStringToByteArray(SELECT_APDU_HEADER + String.format("%02X", SAMPLE_LOYALTY_CARD_AID.length() / 2) + SAMPLE_LOYALTY_CARD_AID);
            byte[] result = isoDep.transceive(command);
            int resultLength = result.length;
            byte[] statusWord = {result[resultLength - 2], result[resultLength - 1]};
            byte[] payload = Arrays.copyOf(result, resultLength - 2);
            if (Arrays.equals(SELECT_OK_SW, statusWord)) {
                mAlertDialog.setMessage(new String(payload, "UTF-8"));
            } else {
                mAlertDialog.setMessage("Select failed");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                isoDep.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void nfcCheck() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, "设备不支持NFC功能", Toast.LENGTH_LONG).show();
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

    private void registerAidsForService() {
        mCardEmulation.setPreferredService(this, mService);
        mCardEmulation.registerAidsForService(mService, "other", AIDS);
    }

    private void unRegisterAidsForService() {
        mCardEmulation.removeAidsForService(mService, "other");
        mCardEmulation.unsetPreferredService(this);
    }
}