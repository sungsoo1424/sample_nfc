package ttoview.ubigate.com.nakayosi.test_nfc.manager;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by sungs on 2016-11-23.
 */

public class VersionManager extends AsyncTask<String, Void, String> {

    Context mContext;
    ProgressDialog dialog;
    String version;

    @Override
    protected String doInBackground(String... strings) {

        version = getRsVersion(strings[0]);
        return version;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog = ProgressDialog.show(mContext, "","버전 확인중 입니다..", true);
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        dialog.dismiss();
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }

    public VersionManager(Context context){
        this.mContext = context;
    }



    public String getRsVersion(String pc) {
        URL url1 = null;
        HttpURLConnection con1 = null;
        String version = "-1";
        try {
            // 접속 URL 경로
            String surl = "http://ttoview.com/rsversionchk.do?pc=";
            surl += pc;
            Log.d("test2",surl);
            // URL생성
            url1 = new URL(surl);
            // 커넥션 생성
            con1 = (HttpURLConnection) url1.openConnection();
            // 파싱
            version = versionXmlParse(con1.getInputStream());
            // 닫기
            con1.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return version;
    }

    //버전정보xml parser
    private String versionXmlParse(InputStream in) throws Exception {
        Document doc = null;
        doc = parseXML(in);
        NodeList descNodes = doc.getElementsByTagName("result");
        String version = null;
        for (int i = 0; i < descNodes.getLength(); i++) {
            for (Node node = descNodes.item(i).getFirstChild(); node != null; node = node.getNextSibling()) {
                Log.d("test2",node.getNodeName());
                if (node.getNodeName().equals("version")) {
                    version = node.getTextContent();
                    Log.d("test2","버전찾음 :"+ version);
                }
            }
        }
        return version;
    }

    private static Document parseXML(InputStream stream) throws Exception {
        DocumentBuilderFactory objDocumentBuilderFactory = null;
        DocumentBuilder objDocumentBuilder = null;
        Document doc = null;
        try {
            objDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
            objDocumentBuilder = objDocumentBuilderFactory.newDocumentBuilder();
            doc = objDocumentBuilder.parse(stream);
        } catch (Exception ex) {
            throw ex;
        }
        return doc;
    }


}
