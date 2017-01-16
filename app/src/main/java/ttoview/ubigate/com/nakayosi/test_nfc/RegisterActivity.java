package ttoview.ubigate.com.nakayosi.test_nfc;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import com.example.simplenfc.R;

import ttoview.ubigate.com.nakayosi.test_nfc.NFC.*;
import ttoview.ubigate.com.nakayosi.test_nfc.manager.CardManager;
import ttoview.ubigate.com.nakayosi.test_nfc.model.HttpRequestResult;

import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.app.PendingIntent;
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
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class RegisterActivity extends ActionBarActivity {

	TextView readResult;

	private NfcAdapter mAdapter;
	private PendingIntent mPendingIntent;
	private IntentFilter[] mFilters;
	private String[][] mTechLists;

	private String place;
	private String cardType;

	private String connectUrl;
	private boolean readOnlyFlag = true;
	private int scanCount;

	public static final int TYPE_TEXT = 1;
	public static final int TYPE_URI = 2;

	private static final String baseUrl = "http://ttoview.com";
	private String serial_num;
	CheckBox readOnlyChkBox;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}

		readResult = (TextView) findViewById(R.id.readResult);
		readOnlyChkBox = (CheckBox) findViewById(R.id.checkBox);



		Spinner s1 = (Spinner)findViewById(R.id.spinner1);
		s1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				switch (position){
					case 0: place="000"; break;
					case 1: place="001"; break;
					case 2: place="002";break;
					case 3: place="003";break;
				}
				readResult.setText("기록될 카드 타입 :"+place+"-"+cardType+"\n");
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});

		Spinner s2 = (Spinner)findViewById(R.id.spinner2);
		s2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				switch (position){
					case 0: cardType="0000"; break;
					case 1: cardType="0001"; break;
					case 2: cardType="0002"; break;
					case 3: cardType="0100"; break;
					case 4: cardType="0101"; break;
					case 5: cardType="0200"; break;
					case 6: cardType="FF00"; break;
				}
				readResult.setText("기록될 카드 타입 :"+place+"-"+cardType+"\n");
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});


		readOnlyChkBox.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (readOnlyChkBox.isChecked()) {
					readOnlyFlag = true;
				} else {
					readOnlyFlag = false;
				}

				Log.d("test2", "리드온리 플래그: " + readOnlyFlag);
			}
		});


		// NFC ���� ��ü ����
		mAdapter = NfcAdapter.getDefaultAdapter(this);
		Intent targetIntent = new Intent(this, RegisterActivity.class);
		targetIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		mPendingIntent = PendingIntent.getActivity(this, 0, targetIntent, 0);

		IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
		IntentFilter mifare = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
		try {
			ndef.addDataType("*/*");
		} catch (MalformedMimeTypeException e) {
			throw new RuntimeException("fail", e);
		}

		mFilters = new IntentFilter[] { mifare };

		mTechLists = new String[][] { new String[] { NfcF.class.getName() } };

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
        Parcelable[] rawMsgs = passedIntent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);


		if (tag != null) {
			byte[] tagId = tag.getId();
			String uid = toHexString(tagId);

			scanCount++;
			if (scanCount > 6) {
				scanCount = 0;
				readResult.setText("기록될 카드 타입 :" + place + "-" + cardType + "\n");
			}

			Ndef ndef = Ndef.get(tag);
			if (ndef != null) {
				if (!ndef.isWritable()) {
					Toast.makeText(getApplicationContext(), "이 카드는 리드 온리 입니다.", Toast.LENGTH_LONG).show();
					readResult.append(" 이 카드는 리드 온리 입니다.\n");
				} else {
					CardManager cardManager = new CardManager(this);
					try {
						connectUrl = baseUrl + "/card/" + uid + "/" + place + "/" + cardType;
                        Log.d("test2",connectUrl);
						HttpRequestResult result = cardManager.execute("POST",connectUrl).get();
						switch (result.getResultCode()) {
							case 200:
                                readResult.append(" 등록성공. ");
                                serial_num = result.getResultJson().getString("serial_num");
								processTag(passedIntent);
								break;
							case 226:
                                    readResult.append(" 등록된 카드. 카드 재기록시도\n");
                                    serial_num = result.getResultJson().getString("serial_num");
                                    processTag(passedIntent);
								break;
							case 500:
								readResult.append(" 등록실패(내부오류\n");
								break;
						}
					} catch (Exception e) {
						Log.d("test2", e.toString());
					}
				}
			}
		}else {
			readResult.append(" 카드가 손상되었습니다.카드를 포맷 해야 합니다.\n");
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

	private void processTag(Intent intent) {
		String s = baseUrl+"/"+serial_num;

		Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			NdefMessage message = createTagMessage(s, TYPE_URI);
			writeTag(message, detectedTag);
	}

	private NdefMessage createTagMessage(String msg, int type) {
		NdefRecord[] records = new NdefRecord[1];

			records[0] = createUriRecord(msg);
			Log.d("ddd", "URI" + records[0]);

		NdefMessage mMessage = new NdefMessage(records);

		return mMessage;
	}

	private NdefRecord createUriRecord(String msg) {
		Log.d("ddd", "URI2");
		byte[] uriField = msg.getBytes(Charset.forName("US-ASCII"));
		byte[] payload = new byte[uriField.length + 1]; // URI Prefix�� 1�� �߰���
		payload[0] = 0x00;
		System.arraycopy(uriField, 0, payload, 1, uriField.length); // payload��
		NdefRecord rtdUriRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI, new byte[0], payload);
		return rtdUriRecord;
	}

	public boolean writeTag(NdefMessage message, Tag tag) {
		int size = message.toByteArray().length;
		Log.d("dd", "size : " + size);
		try {
			Ndef ndef = Ndef.get(tag);
			if (ndef != null) {
				ndef.connect();
				if (!ndef.isWritable()) {
					Toast.makeText(getApplicationContext(), "이 카드는 리드 온리 입니다.", Toast.LENGTH_LONG).show();
					readResult.append(" 이 카드는 리드 온리 입니다.\n");
					return false;
				}
				if (ndef.getMaxSize() < size) {

					return false;
				}

				ndef.writeNdefMessage(message);
				if(readOnlyFlag){
					ndef.makeReadOnly();//리드온리
				}
				readResult.append("기록성공 : "+ serial_num+"\n");
                readResult.append("-----------------------------------------------------\n");
				Toast.makeText(getApplicationContext(), "기록성공", Toast.LENGTH_SHORT).show();

			} else {
				NdefFormatable formatable = NdefFormatable.get(tag);
				if (formatable != null) {
					try {
						formatable.connect();
						//리드온리
						if(readOnlyFlag){
							formatable.formatReadOnly(message);
						}
						formatable.format(message);
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
				return false;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
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
			readResult.append(recordStr + "\n"); // �о���� �ؽ�Ʈ ���� TextView�� ������
		}
		return size;
	}
}
