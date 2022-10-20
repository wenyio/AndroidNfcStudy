package io.weny.AndroidNfcStudy;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.primitives.Bytes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MainActivity5 extends AppCompatActivity {

    private static int flag = 0; // 1 读 2 写

    private Context mContext;

    private EditText mEditText;
    private Button mWriteBtn;
    private Button mReadBtn;
    private AlertDialog mAlertDialog;

    private NfcAdapter mNfcAdapter;
    private NdefMessage mNdefMessage;
    private PendingIntent mPendingIntent;
    private String mPayload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main5);

        mContext = this;

        initView();

        nfcCheck();

        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (flag != 0) {
            enableForegroundDispatch();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.i("MWY", "flag = " + flag);
        if (flag == 1) {
            resolveReadIntent(intent);
        } else if (flag == 2) {
            resolveWriteIntent(intent);
        }
    }

    private void initView() {
        mEditText = findViewById(R.id.et);
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
        mWriteBtn = findViewById(R.id.write);
        mWriteBtn.setOnClickListener(v -> {
            flag = 2;
            enableForegroundDispatch();
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle("触碰标签写入")
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
        String action = intent.getAction();
        Log.d("MWY", "action = " + action);
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            NdefMessage[] messages = null;
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                messages = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    messages[i] = (NdefMessage) rawMsgs[i];
                }
            } else {
                byte[] empty = new byte[]{};
                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
                NdefMessage msg = new NdefMessage(new NdefRecord[]{record});
                messages = new NdefMessage[]{msg};
            }
            processNDEFMsg(messages);
        } else if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {

        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

        } else {
            mAlertDialog.setMessage("???? ");
        }
    }


    private void processNDEFMsg(NdefMessage[] messages) {
        if (messages == null || messages.length == 0) {
            Toast.makeText(this, "TAG 内容为空", Toast.LENGTH_LONG).show();
            return;
        }
        for (int i = 0; i < messages.length; i++) {
            int length = messages[i].getRecords().length;
            NdefRecord[] records = messages[i].getRecords();
            for (int j = 0; j < length; j++) {
                for (NdefRecord record : records) {
                    parseRTDUriRecord(record);
                }
            }
        }
    }

    /**
     * 处理RTD-URI类型的记录
     * @param record
     */
    private void parseRTDUriRecord(NdefRecord record) {
        Preconditions.checkArgument(Arrays.equals(record.getType(), NdefRecord.RTD_URI));
        byte[] payload = record.getPayload();
        String prefix = URI_PREFIX_MAP.get(payload[0]);
        byte[] fullUri = Bytes.concat(prefix.getBytes(StandardCharsets.UTF_8),
                Arrays.copyOfRange(payload, 1, payload.length));
        Uri uri = Uri.parse(new String(fullUri, StandardCharsets.UTF_8));
        mAlertDialog.setMessage(uri.toString());
    }

    private void resolveWriteIntent(Intent intent) {
        String action = intent.getAction();
        Log.i("MWY", "action = " + action);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (supportedTechs(tag.getTechList())) {
            mNdefMessage = MyNDEFMsgGet.getNdefMsg_RTD_URI(mPayload, (byte) 0x01);
            new WriteTask(this, mAlertDialog, mNdefMessage, tag).execute();
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

    static class WriteTask extends AsyncTask<Void, Void, Void> {

        Activity activity;
        AlertDialog mAlertDialog;
        NdefMessage msg;
        Tag tag;
        String text;

        WriteTask(Activity host, AlertDialog mAlertDialog, NdefMessage msg, Tag tag) {
            this.activity = host;
            this.mAlertDialog = mAlertDialog;
            this.msg = msg;
            this.tag = tag;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            if (text != null) {
                Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
            }
            mAlertDialog.cancel();
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

    private static final BiMap<Byte, String> URI_PREFIX_MAP = ImmutableBiMap.<Byte, String>builder()
            .put((byte) 0x00, "").put((byte) 0x01, "http://www.").put((byte) 0x02, "https://www.")
            .put((byte) 0x03, "http://").put((byte) 0x04, "https://").put((byte) 0x05, "tel:")
            .put((byte) 0x06, "mailto:").put((byte) 0x07, "ftp://anonvmous:anonvmous@").put((byte) 0x08, "ftp://ftp.")
            .put((byte) 0x09, "ftps://").put((byte) 0x0A, "sftp://").put((byte) 0x0B, "smb://")
            .put((byte) 0x0C, "nfs://").put((byte) 0x0D, "ftp://").put((byte) 0x0E, "dav://").put((byte) 0x0F, "news:")
            .put((byte) 0x10, "telnet://").put((byte) 0x11, "imap:").put((byte) 0x12, "rtsp://")
            .put((byte) 0x13, "urn:").put((byte) 0x14, "pop:").put((byte) 0x15, "sip:").put((byte) 0x16, "sips:")
            .put((byte) 0x17, "tftp:").put((byte) 0x18, "btspp://").put((byte) 0x19, "btl2cap://")
            .put((byte) 0x1A, "btgoep://").put((byte) 0x1B, "tcpobex://").put((byte) 0x1C, "irdaobex://")
            .put((byte) 0x1D, "file://").put((byte) 0x1E, "urn:epc:id:").put((byte) 0x1F, "urn:epc:tag:")
            .put((byte) 0x20, "urn:epc:pat:").put((byte) 0x21, "urn:epc:raw:").put((byte) 0x22, "urn:epc:")
            .put((byte) 0x23, "urn:nfc:").build();
}