package io.weny.AndroidNfcStudy;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.util.Arrays;

public class MainActivity4 extends AppCompatActivity {

    private Context mContext;
    private Button mButton;
    private EditText mEditText;
    private AlertDialog mAlertDialog;

    private NfcAdapter mNfcAdapter;
    private NdefMessage mNdefMessage;
    private PendingIntent mPendingIntent;
    private String mPayload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main4);

        mContext = this;

        // init UI
        initUI();

        // check NFC
        nfcCheck();

        // init NFC
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // enableForegroundDispatch();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // disableForegroundDispatch();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        resolveIntent(intent);
    }

    private void initUI() {
        mButton = findViewById(R.id.write);
        mEditText = findViewById(R.id.et);
        mButton.setOnClickListener((view) -> {
            enableForegroundDispatch();
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle("Touch tag to write")
                    .setMessage("Bring the Tag close to the device")
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            disableForegroundDispatch();
                        }
                    });
            mAlertDialog = builder.create();
            mAlertDialog.setCanceledOnTouchOutside(false);
            mAlertDialog.show();
        });
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
    }

    private void resolveIntent(Intent intent) {
        String action = intent.getAction();
        Log.i("MWY", "action = " + action);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (supportedTechs(tag.getTechList())) {
            mNdefMessage = MyNDEFMsgGet.getNdefMsg_RTD_URI(mPayload, (byte) 0x01);
            new WriteTask(this, mNdefMessage, tag).execute();
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

    static class WriteTask extends AsyncTask<Void, Void, Void> {

        Activity activity;
        NdefMessage msg;
        Tag tag;
        String text;

        WriteTask(Activity host, NdefMessage msg, Tag tag) {
            this.activity = host;
            this.msg = msg;
            this.tag = tag;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            if (text != null) {
                Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
            }
            activity.finish();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            int size = msg.toByteArray().length; // 消息长度
            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                NdefFormatable formatable = NdefFormatable.get(tag);
                if (formatable != null) {
                    try {
                        formatable.connect();
                        formatable.format(msg);
                    } catch (IOException e) {
                        text = "Failed to connect Tag!";
                        Log.i("MWY", "Failed to connect Tag!");
                        e.printStackTrace();
                    } catch (FormatException e) {
                        text = "Failed to format Tag!";
                        Log.i("MWY", "Failed to format Tag!");
                        e.printStackTrace();
                    } finally {
                        try {
                            formatable.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    text = "NDEF not support you tag!";
                    Log.i("MWY", "NDEF not support you tag!");
                }
            } else {
                try {
                    ndef.connect();
                    if (!ndef.isWritable()) {
                        text = "Tag read only!";
                        Log.i("MWY", "Tag read only!");
                    } else if (ndef.getMaxSize() < size) {
                        text = "Tag is too small!";
                        Log.i("MWY", "Tag is too small!");
                    } else {
                        ndef.writeNdefMessage(msg);
                    }
                } catch (IOException e) {
                    text = "Failed to connect Tag!";
                    Log.i("MWY", "Failed to connect Tag!");
                    e.printStackTrace();
                } catch (FormatException e) {
                    text = "Failed to writeNdefMessage Tag!";
                    Log.i("MWY", "Failed to writeNdefMessage Tag!");
                    e.printStackTrace();
                } finally {
                    try {
                        ndef.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
    }
}