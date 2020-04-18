package com.knaanstudio.coronawise.internet;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;

import com.knaanstudio.coronawise.CoronaHandler;
import com.knaanstudio.coronawise.Exposure;
import com.knaanstudio.coronawise.MainActivity;
import com.knaanstudio.coronawise.database.ContactTrace;

import java.io.DataOutputStream;
import android.os.Handler;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Timestamp;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class DownloadThreadManager {

    public interface MessageConstants{
        int MESSAGE_UPLOAD_DONE = 0;
        int MESSAGE_DOWNLOAD_DONE = 1;
        int MESSAGE_ERROR = 2;
    }

    public static final String TAG = "downloadManager";
    DownloadThread mDownloadThread;
    String mURL;
    Handler mHandler;

    public DownloadThreadManager(String URL, Handler handler){
        mURL = URL;
        mHandler = handler;
    }

    public boolean startDownload(String params, String requestType){
        boolean isDone;
        if(mDownloadThread != null) {
             isDone = mDownloadThread.checkIfDone();
        }else{ isDone=true;}
        if(isDone) {
            mDownloadThread = new DownloadThread(params, requestType);
            mDownloadThread.start();
            return true;
        }else{
            return false;
        }
    }

    class DownloadThread extends Thread{
        String mmParams;
        String mmRequestType;
        public boolean isDone;
        public DownloadThread(String params, String requestType){
            mmParams = params;
            mmRequestType = requestType;
            isDone = false;
        }
        public void run(){
            try {
                String result = downloadUrl(mURL, mmParams, mmRequestType);
                if(mmRequestType.equals("GET")){
                    mHandler.obtainMessage(MessageConstants.MESSAGE_DOWNLOAD_DONE, result).sendToTarget();
                }else{
                    mHandler.obtainMessage(MessageConstants.MESSAGE_UPLOAD_DONE).sendToTarget();
                }
            }catch (Exception e){
                e.printStackTrace();
                mHandler.obtainMessage(MessageConstants.MESSAGE_ERROR).sendToTarget();
            }
            isDone = true;
        }

        private String downloadUrl(String urlString, String URLparams, String requestType) throws Exception {
            URL url = new URL(urlString);
            //Passing urlParameters will make the connection run through POST and send them. Otherwise, connection ru
            InputStream stream = null;
            HttpURLConnection connection = null; // TODO: use Https
            String result = null;
            Exception storedException=null;
            try {
                connection = (HttpURLConnection) url.openConnection(); // TODO: use https
                // Timeout for reading InputStream arbitrarily set to 3000ms.
                connection.setReadTimeout(3000);
                // Timeout for connection.connect() arbitrarily set to 3000ms.
                connection.setConnectTimeout(3000);
                connection.setRequestMethod(requestType);
                connection.setDoInput(true);
                // Add urlParamaters to the request
                if(!URLparams.equals("")) {
                    byte[] postData = URLparams.getBytes(StandardCharsets.UTF_8);
                    int postDataLength = postData.length;
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setRequestProperty("charset", "utf-8");
                    connection.setRequestProperty("Content-Length", Integer.toString(postDataLength));
                    try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                        wr.write(postData);
                    }catch (Exception e){
                        e.printStackTrace();
                        throw e;
                    }
                }

                // Open communications link (network traffic occurs here).
                connection.connect();
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpsURLConnection.HTTP_OK) {
                    throw new IOException("HTTP error code: " + responseCode);
                }
                // Retrieve the response body as an InputStream.
                stream = connection.getInputStream();
                if(requestType == DownloadCallback.RequestTypes.GET) {
                    if (stream != null) {
                        // Converts Stream to String with max length of 500.
                        result = readStream(stream);
                    }
                }else{
                    result = "Successfully updated website";
                }
            }catch (Exception e){
                storedException = e;
            } finally {
                // Close Stream and disconnect HTTPS connection.
                if (stream != null) {
                    stream.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
            if(storedException != null){
                Log.i(TAG, "there's an error");
                throw storedException;
            }
            Log.i(TAG, result);
            return result;
        }


        private String readStream(InputStream stream)
                throws IOException {
            Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
            int currentChar;
            StringBuilder messageBuilder = new StringBuilder();
            while(true) {//repeats until reaching currentChar = -1, which indicates the end of the file
                StringBuilder signatureStringBuilder = new StringBuilder(); // builds the sugnature
                do{
                    currentChar = reader.read();
                    if (currentChar == (int)','){
                        throw new IOException("String received from server was not properly formatted. Unexpected ','");
                    }
                    if((currentChar != (int) ' ') && (currentChar != (int) '.') && (currentChar != -1)){ //if the current char is a space, it's a php problem so we exclude it. if it's a ., that indicates the end of the section, so we don't use it.
                        signatureStringBuilder.append((char) currentChar);
                    }
                }while ((currentChar != (int) '.') && (currentChar != -1)); // when there is a '.', that indicates the end of the signature, so we move on. When there is a -1, that means the file is over.

                if(currentChar == -1){break;}// -1 indicates the end of the file
                String signatureString = signatureStringBuilder.toString();
                byte[] signature = Base64.decode(signatureString, android.util.Base64.DEFAULT);

                StringBuilder timeStampStringBuilder = new StringBuilder();
                do {
                    currentChar = reader.read();
                    if (currentChar == (int)'.'){
                        throw new IOException("String received from server was not properly formatted. Unexpected '.'");
                    }
                    if((currentChar != (int) ' ') && (currentChar != (int) ','  && (currentChar != -1))){ //if the current char is a space, it's a php problem so we exclude it. if it's a ., that indicates the end of the section, so we don't use it.
                        timeStampStringBuilder.append((char) currentChar);
                    }
                }while ((currentChar != (int) ',') && (currentChar != -1) );
                if(currentChar == -1){break;}// -1 indicates the end of the file
                String timeStampString = timeStampStringBuilder.toString();
                Timestamp verifiedExposureTime  = new Timestamp(Long.parseLong(timeStampString));

                Exposure exposure = checkIfSignatureMatchesDB(signature,verifiedExposureTime);
                if(exposure != null){
                    messageBuilder.append(exposure.getMessage())
                            .append("\n");
                }

            }

            String message = messageBuilder.toString();
            if (!message.equals("")){
                return message;
            }else{
                return "You have not been exposed.";
            }
        }



        @Nullable
        private Exposure checkIfSignatureMatchesDB(byte[] signature, Timestamp verifiedExposureTime){
            //returns an exposure if there was an exposure, otherwise returns null.
            List<ContactTrace> contactTraces = CoronaHandler.readContactTrace(MainActivity.getLocalContext());
            for (ContactTrace trace : contactTraces){
                try {
                    Log.i(TAG, "Key: " + CoronaHandler.KeyToString(trace.getKey().getEncoded()));
                    Log.i(TAG, "Signature: " + CoronaHandler.KeyToString(signature));

                    if (CoronaHandler.verifySignature(trace.getKey(), signature)){
                        return new Exposure(trace.getTimestamp(), verifiedExposureTime);
                    }
                }catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | InvalidKeySpecException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        synchronized boolean checkIfDone(){
            return isDone;
        }
    }
}
