package ttoview.ubigate.com.nakayosi.test_nfc.manager;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import ttoview.ubigate.com.nakayosi.test_nfc.model.HttpRequestResult;

/**
 * Created by sungs on 2016-11-23.
 */

public class CardManager extends AsyncTask<String, Void, HttpRequestResult> {

    Context mContext;
    ProgressDialog dialog;
    String method;

    public CardManager(Context context) {
        this.mContext = context;
    }


    @Override
    protected HttpRequestResult doInBackground(String... strings) {

        HttpRequestResult result = null;
        method = strings[0];
        result = registCard(strings[1]);
        return result;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog = ProgressDialog.show(mContext, "", "버전 확인중 입니다..", true);
    }

    @Override
    protected void onPostExecute(HttpRequestResult s) {
        super.onPostExecute(s);
        dialog.dismiss();
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }


    private HttpRequestResult registCard(String url) {
        BufferedReader bufferedReader = null;
        HttpURLConnection urlConnection = null;
        StringBuilder sb = new StringBuilder();
        HttpRequestResult result = new HttpRequestResult();
        String json;

        try {
            URL urlToRequest = new URL(url);
            Log.d("test2","URL : "+url);
            Log.d("test2","Method : "+method);

            urlConnection = (HttpURLConnection) urlToRequest.openConnection();
            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(15000);
            urlConnection.setRequestProperty("Accept", "application/json");
            switch (method){
                case "POST" :  urlConnection.setDoOutput(true); urlConnection.setDoInput(true); break;
                case "GET" :  urlConnection.setDoInput(true); break;
                case "PUT" : break;
            }
            urlConnection.setInstanceFollowRedirects(false);
            urlConnection.setRequestMethod(method);
            urlConnection.connect();



            switch (urlConnection.getResponseCode()){
                case 200 : result.setResultCode(200); break;
                case 226 : result.setResultCode(226);break;
                case 500 : result.setResultCode(500); break;
                default: result.setResultCode(urlConnection.getResponseCode()); break;

            }
            bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            while ((json = bufferedReader.readLine()) != null) {
                sb.append(json + "\n");
            }
            result.setResultJson(new JSONObject(sb.toString()));

        } catch (Exception e) {
            Log.d("test3", e.toString());
        }
        urlConnection.disconnect();
        return result;
    }
}
