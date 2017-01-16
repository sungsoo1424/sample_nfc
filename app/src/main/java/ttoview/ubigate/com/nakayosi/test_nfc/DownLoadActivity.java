package ttoview.ubigate.com.nakayosi.test_nfc;

import com.example.simplenfc.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import ttoview.ubigate.com.nakayosi.test_nfc.manager.VersionManager;


public class DownLoadActivity extends Activity implements View.OnClickListener {
    private EditText downloadUrl;
    private Button addToQueueButton;
    private Button cancelLatestButton;
    private Button viewDownloadsButton;
    private long latestId = -1;

    private DownloadManager downloadManager;
    private DownloadManager.Request request;
    private Uri urlToDownload;
    String basePath;
    String folderPath;
    String realFolderPath;

    private static final int BUFFER_SIZE = 1024 * 4;

    private BroadcastReceiver completeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                unzip(realFolderPath + File.separator + "rs.zip", realFolderPath, false);
            }catch (Exception e){
               Log.d("test2", e.toString());
            }

            Toast.makeText(context, "다운로드가 완료 되었습니다.", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        downloadUrl = (EditText) findViewById(R.id.downloadUrl);
        addToQueueButton = (Button) findViewById(R.id.addQueueButton);
        cancelLatestButton = (Button) findViewById(R.id.cancelDownloadButton);
        viewDownloadsButton = (Button) findViewById(R.id.viewDownloadsButton);

        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        addToQueueButton.setOnClickListener(this);
        cancelLatestButton.setOnClickListener(this);
        viewDownloadsButton.setOnClickListener(this);

        basePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        // 폴더명 및 파일명
        folderPath = "missionfarm" + File.separator + "bukchon";

        realFolderPath = basePath + File.separator + folderPath;
        Log.d("test2", "basePath : " + basePath);
        Log.d("test2", "folderPath : " + folderPath);

        File fileFolderPath = new File(realFolderPath);
        if (!fileFolderPath.exists()) {
            fileFolderPath.mkdirs();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter completeFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(completeReceiver, completeFilter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.addQueueButton:
                if(rsVersionChk()) {
                    removeFiles(realFolderPath); //기존파일 삭제
                    urlToDownload = Uri.parse(downloadUrl.getText().toString());
                    List<String> pathSegments = urlToDownload.getPathSegments();
                    request = new DownloadManager.Request(urlToDownload);
                    request.setTitle("미션팜 다운로드");
                    request.setDescription("미션팜의 데이터를 다운로드합니다.");
                    request.setDestinationInExternalPublicDir(folderPath, pathSegments.get(pathSegments.size() - 1));
                    //Environment.getExternalStoragePublicDirectory(folderPath).mkdirs();
                    latestId = downloadManager.enqueue(request);
                    downloadUrl.setText("");
                }
                break;

            case R.id.cancelDownloadButton:
                downloadManager.remove(latestId);
                break;

            case R.id.viewDownloadsButton:
                startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(completeReceiver);
    }

    public void removeFiles(String path)
    {
        File file = new File(path);
        File[] childFileList = file.listFiles();

        Log.d("test2", "파일이 존재하는가? : " + file.exists() + ", " + file.length());
        Log.d("test2", "파일경로 : " + file.getAbsolutePath());

        Log.d("test2,", "자식 사이즈 : " + childFileList.length);
        for (File childFile : childFileList) {
            if (childFile.isDirectory()) {
                removeFiles(childFile.getAbsolutePath());     //하위 디렉토리 루프
            } else {
                childFile.delete();    //하위 파일삭제
            }
        }
        file.delete();    //root 삭제
    }

    public static void unzip(String zipFile, String targetDir, boolean fileNameToLowerCase) throws Exception {
        FileInputStream fis = null;
        ZipInputStream zis = null;
        ZipEntry zentry = null;

        try {
            fis = new FileInputStream(zipFile); // FileInputStream
            zis = new ZipInputStream(fis); // ZipInputStream

            while ((zentry = zis.getNextEntry()) != null) {
                String fileNameToUnzip = zentry.getName();
                if (fileNameToLowerCase) { // fileName toLowerCase
                    fileNameToUnzip = fileNameToUnzip.toLowerCase();
                }

                File targetFile = new File(targetDir, fileNameToUnzip);

                if (zentry.isDirectory()) {// Directory 인 경우
                    //FileUtils.makeDir(targetFile.getAbsolutePath()); // 디렉토리 생성
                    File path = new File(targetFile.getAbsolutePath());
                    path.mkdirs();
                } else { // File 인 경우
                    // parent Directory 생성
                    //FileUtils.makeDir(targetFile.getParent());
                    File path = new File(targetFile.getParent());
                    path.mkdirs();
                    unzipEntry(zis, targetFile);
                }
            }
        } finally {
            if (zis != null) {
                zis.close();
            }
            if (fis != null) {
                fis.close();
            }
            File zipedFile = new File(zipFile);
            zipedFile.delete();
        }
    }

    protected static File unzipEntry(ZipInputStream zis, File targetFile) throws Exception {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(targetFile);

            byte[] buffer = new byte[BUFFER_SIZE];
            int len = 0;
            while ((len = zis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
        return targetFile;
    }

    private String getDownLoadFileVersion(String path){
        String version = null;
        try {
            File file = new File(path);
            if(file.exists()){
            FileInputStream fis = new FileInputStream(file);
            Reader in = new InputStreamReader(fis);
            int size = fis.available();
            char[] buffer = new char[size];
            in.read(buffer);
            in.close();
            version = new String(buffer);
                return version;

            }
            else return "-2";
        } catch (Exception e) {
            return "-2";
        }
    }

    private boolean rsVersionChk(){
        //다운로드 버전정보
        String rV;
        String sV;
        rV = getDownLoadFileVersion(realFolderPath + File.separator +"version");
        //서버의 리소스 버전정보
        VersionManager versionManager = new VersionManager(DownLoadActivity.this);
        try {
            sV = versionManager.execute("1").get();
        }
        catch (Exception e){
            sV = "-1";
        }
        if(sV.equals("-1")) { Log.d("test2", "서버오류."); return true;}

        Log.d("test2", "기기버전 :"+rV + " 서버버전 : "+sV);

        if (rV.compareTo(sV) < 0 || rV.equals("-2")) {
            Log.d("test2", "현재 버전은 구버전입니다 업데이트 내역이 존재합니다.");
            return true;
        }
        else{
            Log.d("test2", "현재 버전은 최신버전 입니다.");
            return false;
        }
    }
}