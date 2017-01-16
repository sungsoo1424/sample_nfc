package ttoview.ubigate.com.nakayosi.test_nfc;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;

import ttoview.ubigate.com.nakayosi.test_nfc.NFC.*;

import com.example.simplenfc.R;
import com.google.common.base.Charsets;
import com.google.common.primitives.Bytes;

import android.nfc.tech.MifareUltralight;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;

public class WriteActivity extends ActionBarActivity {

	private NfcAdapter nfcAdapter;
	private PendingIntent pendingIntent;

	public static final int TYPE_TEXT = 1;
	public static final int TYPE_URI = 2;

	EditText writeText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_write);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}

		writeText = (EditText) findViewById(R.id.writeText);

		// NFC ���� ��ü ����
		nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		Intent intent = new Intent(this, getClass())
				.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.write, menu);
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
			View rootView = inflater.inflate(R.layout.fragment_write,
					container, false);
			return rootView;
		}
	}

	/************************************
	 * ���⼭���� NFC ���� �޼ҵ�
	 ************************************/
	@Override
	protected void onPause() {
		if (nfcAdapter != null) {
			nfcAdapter.disableForegroundDispatch(this);
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (nfcAdapter != null) {
			nfcAdapter
					.enableForegroundDispatch(this, pendingIntent, null, null);
		}
	}

	// NFC �±� ��ĵ�� ȣ��Ǵ� �޼ҵ�
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		if (intent != null) {

			processTag(intent); // processTag �޼ҵ� ȣ��
		}
	}

	// onNewIntent �޼ҵ� ���� �� ȣ��Ǵ� �޼ҵ�
	private void processTag(Intent intent){
		String s = writeText.getText().toString();
		try {
			// ������ �±׸� ����Ű�� ��ü
			Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

			MifareUltralight mifare = MifareUltralight.get(detectedTag);
			mifare.connect();
			byte[] pwd = new byte[] { (byte)0x70, (byte)0x61, (byte)0x73, (byte)0x73 };
			byte[] pack = new byte[] { (byte)0x98, (byte)0x76 };

// write PACK:
			byte[] result = mifare.transceive(new byte[] {
					(byte)0xA2,  /* CMD = WRITE */
					(byte)0x2C,  /* PAGE = 44 */
					pack[0], pack[1], 0, 0
			});
			Log.d("test2","pack입력 결과:"+result);

// write PWD:
			result = mifare.transceive(new byte[] {
					(byte)0xA2,  /* CMD = WRITE */
					(byte)0x2B,  /* PAGE = 43 */
					pwd[0], pwd[1], pwd[2], pwd[3]
			});
			Log.d("test2","비번 입력 결과:"+result);

		}
		catch (Exception e){
			Log.v("test2",e.toString());
		}


	/*
		if (s.equals("")) {
			Toast.makeText(getApplicationContext(), "내용이 비어있습니다.", Toast.LENGTH_SHORT).show();
		}

		else {
			NdefMessage message = createTagMessage(s, TYPE_TEXT);
			writeTag(message, detectedTag);
		}
		*/
	}

	// ������ �±׿� NdefMessage�� ���� �޼ҵ�
	public boolean writeTag(NdefMessage message, Tag tag) {
		int size = message.toByteArray().length;
		try {
			Ndef ndef = Ndef.get(tag);
			if (ndef != null) {
				ndef.connect();
				if (!ndef.isWritable()) {
					return false;
				}

				if (ndef.getMaxSize() < size) {
					return false;
				}

				ndef.writeNdefMessage(message);
				Toast.makeText(getApplicationContext(), "���� ����!", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this, "이 메세지의 정체는 모름(ndf타입이 아니다?).",
						Toast.LENGTH_SHORT).show();

				NdefFormatable formatable = NdefFormatable.get(tag);
				if (formatable != null) {
					try {
						formatable.connect();
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

	/**
	 * Create a new tag message
	 * 
	 * @param msg
	 * @param type
	 * @return
	 */
	private NdefMessage createTagMessage(String msg, int type) {
		NdefRecord[] records = new NdefRecord[1];

		if (type == TYPE_TEXT) {
			records[0] = createTextRecord(msg, Locale.KOREAN, true);
		} else if (type == TYPE_URI) {
			records[0] = createUriRecord(msg.getBytes());
		}

		NdefMessage mMessage = new NdefMessage(records);

		return mMessage;
	}

	private NdefRecord createTextRecord(String text, Locale locale,
			boolean encodeInUtf8) {
		final byte[] langBytes = locale.getLanguage().getBytes(
				Charsets.US_ASCII);
		final Charset utfEncoding = encodeInUtf8 ? Charsets.UTF_8 : Charset
				.forName("UTF-16");
		final byte[] textBytes = text.getBytes(utfEncoding);
		final int utfBit = encodeInUtf8 ? 0 : (1 << 7);
		final char status = (char) (utfBit + langBytes.length);
		final byte[] data = Bytes.concat(new byte[] { (byte) status },
				langBytes, textBytes);
		return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT,
				new byte[0], data);
	}

	private NdefRecord createUriRecord(byte[] data) {
		return new NdefRecord(NdefRecord.TNF_ABSOLUTE_URI, NdefRecord.RTD_URI,
				new byte[0], data);
	}

}
