package ttoview.ubigate.com.nakayosi.test_nfc;


import java.util.List;

import com.example.simplenfc.R;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;

import ttoview.ubigate.com.nakayosi.test_nfc.NFC.NdefMessageParser;
import ttoview.ubigate.com.nakayosi.test_nfc.NFC.ParsedRecord;
import ttoview.ubigate.com.nakayosi.test_nfc.NFC.TextRecord;
import ttoview.ubigate.com.nakayosi.test_nfc.NFC.UriRecord;
import ttoview.ubigate.com.nakayosi.test_nfc.manager.CardManager;
import ttoview.ubigate.com.nakayosi.test_nfc.model.HttpRequestResult;

public class ReadActivity extends ActionBarActivity {

    TextView readResult;

    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;
    private IntentFilter[] mFilters;
    private String[][] mTechLists;
    private int scanCount;


    private String connectUrl;
    private static final String baseUrl = "http://ttoview.com";

    //테스트
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment()).commit();
        }

        readResult = (TextView) findViewById(R.id.readResult);

        // NFC ���� ��ü ����
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        Intent targetIntent = new Intent(this, ReadActivity.class);
        targetIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mPendingIntent = PendingIntent.getActivity(this, 0, targetIntent, 0);

        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter mifare = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        try {
            ndef.addDataType("*/*");
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }

        mFilters = new IntentFilter[]{mifare};

        mTechLists = new String[][]{new String[]{NfcF.class.getName()}};

        Intent passedIntent = getIntent();
        if (passedIntent != null) {
            String action = passedIntent.getAction();
            if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
                processTag(passedIntent);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.read, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_read, container,
                    false);
            return rootView;
        }
    }

    /************************************
     * ���⼭���� NFC ���� �޼ҵ�
     ************************************/
    public void onResume() {
        super.onResume();

        if (mAdapter != null) {
            mAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters,
                    mTechLists);
        }
    }

    public void onPause() {
        super.onPause();

        if (mAdapter != null) {
            mAdapter.disableForegroundDispatch(this);
        }
    }

    // NFC �±� ��ĵ�� ȣ��Ǵ� �޼ҵ�
    public void onNewIntent(Intent passedIntent) {
        // NFC �±�
        Tag tag = passedIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            byte[] tagId = tag.getId();
            readResult.setText("서버 카드 정보 : " + "\n");
            String uid = toHexString(tagId);
            Log.d("test2", uid);

            CardManager cardManager = new CardManager(this);
            connectUrl = "http://ttoview.com/card/uid/" + uid;
            Log.d("test2", connectUrl);
            try {
                HttpRequestResult result = cardManager.execute("GET", connectUrl).get();
                if (result.getResultJson() != null) {
                    readResult.append(result.getResultJson().toString(1) + "\n");
                } else {
                    readResult.append("서버에 등록안된 카드\n");
                }
                readResult.append("\n카드 내부 정보 : \n");
            } catch (Exception e) {
                Log.d("test2", e.toString());
            }


            readResult.append("NFC_UID : " + toHexString(tagId) + "\n"); // TextView�� �±� ID ������
        }

        if (passedIntent != null) {
            processTag(passedIntent); // processTag �޼ҵ� ȣ��
        }
    }

    // NFC �±� ID�� �����ϴ� �޼ҵ�
    public static final String CHARS = "0123456789ABCDEF";

    public static String toHexString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; ++i) {
            sb.append(CHARS.charAt((data[i] >> 4) & 0x0F)).append(
                    CHARS.charAt(data[i] & 0x0F));
        }
        return sb.toString();
    }

    // onNewIntent �޼ҵ� ���� �� ȣ��Ǵ� �޼ҵ�
    private void processTag(Intent passedIntent) {
        Parcelable[] rawMsgs = passedIntent
                .getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMsgs == null) {
            return;
        }

        // ����! rawMsgs.length : ��ĵ�� �±� ����
        Toast.makeText(getApplicationContext(), "��ĵ ����!", Toast.LENGTH_SHORT).show();

        NdefMessage[] msgs;
        if (rawMsgs != null) {
            msgs = new NdefMessage[rawMsgs.length];
            for (int i = 0; i < rawMsgs.length; i++) {
                msgs[i] = (NdefMessage) rawMsgs[i];
                showTag(msgs[i]); // showTag �޼ҵ� ȣ��
            }
        }
    }

    // NFC �±� ������ �о���̴� �޼ҵ�
    private int showTag(NdefMessage mMessage) {
        List<ParsedRecord> records = NdefMessageParser.parse(mMessage);
        final int size = records.size();
        for (int i = 0; i < size; i++) {
            ParsedRecord record = records.get(i);

            int recordType = record.getType();
            String recordStr = ""; // NFC �±׷κ��� �о���� �ؽ�Ʈ ��
            if (recordType == ParsedRecord.TYPE_TEXT) {
                recordStr = "TEXT : " + ((TextRecord) record).getText();
            } else if (recordType == ParsedRecord.TYPE_URI) {
                recordStr = "URI : " + ((UriRecord) record).getUri().toString();
            }
            readResult.append(recordStr + "\n");
            readResult.append("-----------------------------------------------------\n");
        }

        return size;
    }
}
