package com.example.kimdongun.paintchat.server;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import com.example.kimdongun.paintchat.DebugHandler;
import com.example.kimdongun.paintchat.MultipartUtility;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostDB {
    // http://dna.daum.net/apis/local
    public static final String SERVER_ROOT_URL = "http://211.110.229.53/%s";
    public boolean progressBar; //프로그래스바 실행 여부
    public boolean full_url; //주소를 다 적을 것인가
    private int delay; //시작 딜레이
    private Context mContext;
    private String loadingMessage; //로딩 메세지
    private HashMap<String, String> postData = new HashMap<String, String>(); //보낼 데이타(Post)
    private String uploadFilePath; //업로드 파일 경로
    private String uploadFileName; //업로드 파일 저장 할 이름
    private String downloadFilePath; //다운로드 할 파일 경로
    private String saveFilePath; //다운로드 파일 저장 경로
    private String saveFileName; //다운로드 파일 저장 이름

    OnFinishDBListener onFinishDBListener;

    PostFileDataTask postFileDataTask; //파일 업로드 task
    DownloadFileTask downloadFileTask; //파일 다운로드 task

    public PostDB() {
        this.loadingMessage = "Loading...";
        this.delay = 0;
        progressBar = false;
        full_url = false;
    }

    public PostDB(Context mContext) {
        this.mContext = mContext;
        this.loadingMessage = "Loading...";
        this.delay = 0;
        progressBar = true;
        full_url = false;
    }

    public PostDB(Context mContext, String loadingMessage) {
        this.mContext = mContext;
        this.loadingMessage = loadingMessage;
        this.delay = 0;
        progressBar = true;
        full_url = false;
    }

    public PostDB(Context mContext, String loadingMessage, int delay) {
        this.mContext = mContext;
        this.loadingMessage = loadingMessage;
        this.delay = delay;
        progressBar = true;
        full_url = false;
    }

    private String buildPostDBUrlString(String php) {
        String phpName = "";
        try {
            phpName = URLEncoder.encode(php, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return String.format(SERVER_ROOT_URL, phpName);
    }

    //실행 할 php이름, postData, 업로드 할 파일, 업로드 파일 이름
    public void putFileData(String php, HashMap<String, String> postData, String uploadFilePath, String uploadFileName, OnFinishDBListener onFinishDBListener) {
        this.onFinishDBListener = onFinishDBListener;

        if (postFileDataTask != null) {
            postFileDataTask.cancel(true);
            postFileDataTask = null;
        }

        String url;
        if (full_url) { //url 주소 다 적는 경우
            url = php;
        } else {
            url = buildPostDBUrlString(php);
        }

        this.postData = postData;
        this.uploadFilePath = uploadFilePath;
        this.uploadFileName = uploadFileName;
        postFileDataTask = new PostFileDataTask();
        postFileDataTask.execute(url);
    }

    private class PostFileDataTask extends AsyncTask<String, String, String> {
        private ProgressDialog progressDialog;
        private PowerManager.WakeLock mWakeLock;

        @Override
        protected void onPreExecute() {
            //사용자가 업로드 중 파워 버튼을 누르더라도 CPU가 잠들지 않도록 해서
            //다시 파워버튼 누르면 그동안 업로드를 진행되고 있게 됩니다.
            if(mContext != null) {
                PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
                mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
                mWakeLock.acquire();
            }

            if (progressBar) {
                progressDialog = new ProgressDialog(mContext);
                progressDialog.setMessage(loadingMessage);
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.setCancelable(false);
                progressDialog.show();
            }
            super.onPreExecute();
        }//onPreExecute

        @Override
        protected void onPostExecute(String result) {
            //업로드 완료 이후 프로그레스 바 없애기
            if (progressBar) {
                progressDialog.dismiss();
            }
            if (onFinishDBListener != null) {
                result.trim();
                onFinishDBListener.onSuccess(result);
            }
        }

        @Override
        protected String doInBackground(String... urls) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String url = urls[0];
            DebugHandler.log(getClass().getName(), "url: " + url);
            String result = invokePostFileData(url);
            return result;
        }

        @Override
        protected void onProgressUpdate(String... values) {
        }
    }

    private String invokePostFileData(String requestURL) {
        DebugHandler.log("requestURL", requestURL);
        StringBuilder result = new StringBuilder();

        String charset = "UTF-8";
        try {
            MultipartUtility multipart = new MultipartUtility(requestURL, charset);

            if (postData != null) {
                for (Map.Entry<String, String> entry : postData.entrySet()) {
                    String key = entry.getKey();//URLEncoder.encode(entry.getKey(), "UTF-8");
                    String value = entry.getValue();//URLEncoder.encode(entry.getValue(), "UTF-8");
                    DebugHandler.log("postData", "key: " + key + " value: " + value.trim());
                    multipart.addFormField(key.trim(), value.trim());
                }
            }
            if (uploadFilePath != null)
                multipart.addFilePart("uploaded_file", new File(uploadFilePath), uploadFileName);

            List<String> responseList = multipart.finish();
            for (String line : responseList) {
                DebugHandler.log("PHP Request", line);
                result.append(line);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return result.toString();
    }//invokePost

    //파일 다운로드
    public void downloadFile(String downloadFilePath, String saveFilePath, String saveFileName, OnFinishDBListener onFinishDBListener){
        this.onFinishDBListener = onFinishDBListener;

        if (downloadFileTask != null) {
            downloadFileTask.cancel(true);
            downloadFileTask = null;
        }

        this.downloadFilePath = downloadFilePath;
        this.saveFilePath = saveFilePath;
        this.saveFileName = saveFileName;
        downloadFileTask = new DownloadFileTask();
        downloadFileTask.execute(downloadFilePath);
    }

    public class DownloadFileTask extends AsyncTask<String, String, Long>{
        private PowerManager.WakeLock mWakeLock;
        private ProgressDialog progressDialog;

        //파일 다운로드를 시작하기 전에 프로그레스바를 화면에 보여줍니다.
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //사용자가 다운로드 중 파워 버튼을 누르더라도 CPU가 잠들지 않도록 해서
            //다시 파워버튼 누르면 그동안 다운로드가 진행되고 있게 됩니다.
            if(mContext != null) {
                PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
                mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
                mWakeLock.acquire();
            }

            //프로그레스 바 설정
            if (progressBar) {
                progressDialog = new ProgressDialog(mContext);
                progressDialog.setMessage(loadingMessage);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setIndeterminate(true);
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.setCancelable(false);
//                progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "취소",
//                        new DialogInterface.OnClickListener(){
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//
//                            }
//                        });
//                progressDialog.setButton(DialogInterface.BUTTON_POSITIVE, "확인",
//                        new DialogInterface.OnClickListener(){
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//
//                            }
//                        });

                progressDialog.show();
            }
        }//onPreExecute

        //파일 다운로드를 진행합니다.
        @Override
        protected Long doInBackground(String... string_url) { //3
            int count;
            long FileSize = -1;
            InputStream input = null;
            OutputStream output = null;
            URLConnection connection = null;

            try {
                //Url 연결 시도
                URL url = new URL(string_url[0]);
                connection = url.openConnection();
                connection.connect();

                //파일 크기를 가져옴
                FileSize = connection.getContentLength();

                //URL 주소로부터 파일다운로드하기 위한 input stream 8192 = 넉넉한 사이즈
                input = new BufferedInputStream(url.openStream(), 8192);

                File outputFile; //파일명까지 포함한 경로
                outputFile= new File(saveFilePath, saveFileName); //파일명까지 포함함 경로의 File 객체 생성

                //저장소에 저장하기 위한 Output stream
                output = new FileOutputStream(outputFile);

                byte data[] = new byte[1024];
                long downloadedSize = 0;
                while ((count = input.read(data)) != -1) {
                    //사용자가 BACK 버튼 누르면 취소가능
                    if (isCancelled()) {
                        input.close();
                        return Long.valueOf(-1);
                    }
                    downloadedSize += count;

                    if (FileSize > 0) {
                        float per = ((float)downloadedSize/FileSize) * 100;
                        String str = "다운로드 중...";//downloadedSize + "KB / " + FileSize + "KB (" + (int)per + "%)";
                        publishProgress("" + (int) ((downloadedSize * 100) / FileSize), str);
                    }

                    //파일에 데이터를 기록합니다.
                    output.write(data, 0, count);
                }
                // Flush output
                output.flush();

                // Close streams
                output.close();
                input.close();


            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if(mWakeLock != null)
                    mWakeLock.release();
            }
            return FileSize;
        }


        //다운로드 중 프로그레스바 업데이트
        @Override
        protected void onProgressUpdate(String... progress) { //4
            super.onProgressUpdate(progress);
            if (progressBar) {
                // if we get here, length is known, now set indeterminate to false
                progressDialog.setIndeterminate(false);
                progressDialog.setMax(100);
                progressDialog.setProgress(Integer.parseInt(progress[0]));
                progressDialog.setMessage(progress[1]);
            }
        }

        //파일 다운로드 완료 후
        @Override
        protected void onPostExecute(Long size) { //5
            super.onPostExecute(size);
            //다운로드 완료 이후 프로그레스 바 없애기
            if (progressBar) {
                progressDialog.dismiss();
            }

            if (size> 0) {
//                Toast.makeText(mContext, "다운로드 완료되었습니다. 파일 크기=" + size.toString(), Toast.LENGTH_LONG).show();
                if (onFinishDBListener != null) {
                    onFinishDBListener.onSuccess("success");
                }
//                Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//                mediaScanIntent.setData(Uri.fromFile(outputFile));
//                sendBroadcast(mediaScanIntent);
//
//                playVideo(outputFile.getPath());
            }
            else {
                Toast.makeText(mContext, "다운로드를 실패하였습니다. 파일 크기=" + size.toString(), Toast.LENGTH_LONG).show();
                if (onFinishDBListener != null) {
                    onFinishDBListener.onSuccess("fail");
                }
            }
        }
    }
}