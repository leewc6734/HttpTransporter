package com.goldtek.httptester;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

/**
 * Created by ljason on 2018/4/12.
 */

public class HttpDataSender extends AsyncTask<String, Void, String> {

    private static final String TAG;

    interface ResponseCallback {
        void onHttpResponse(String response);
        void onHttpCodeResponse(String responseCode);
    }

    ResponseCallback callback;

    public static final int SENDER_METHOD_GET;
    public static final int SENDER_METHOD_POST;

    public static final String PROTOCOL_HTTP;
    public static final String PROTOCOL_HTTPS;
    public static final String METHOD_POST;
    public static final String METHOD_GET;

    static {
        TAG = "HttpDataSender -> ";
        SENDER_METHOD_GET = 0;
        SENDER_METHOD_POST = 1;

        PROTOCOL_HTTP = "http://";
        PROTOCOL_HTTPS = "https://";

        METHOD_POST = "POST";
        METHOD_GET = "GET";
    }

    private String strServerIP;
    private String strActionName;
    private HashMap<String, String> hmapParams;
    private String strParamsName;
    private String strParamsValue;
    private String strProtocol;

    // Default sending method.
    private int senderMethod = SENDER_METHOD_GET;

    //TODO: serverURL => protocol, server-ip, method
    InputStream sendByMethod(String serverURL) {

        InputStream dataInputStream = null;
        int readTimeout = 5000;
        int connectionTimeout = 5000;

        // POST parameters
        String postParam = getParamsName() + "=" + getParamsValue();
//        String postParam = "first_name=android&amp;last_name=pala";

        // =================== TESTING ===================
        Log.d(TAG, "Server URL: " + serverURL);
        Log.d(TAG, "postParam: " + postParam);


        // Preparing
        try {

            URL url = new URL(serverURL);
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();

            // Set timeout for reading InputStream
            httpConnection.setReadTimeout(readTimeout);

            // Set timeout for connection
            httpConnection.setConnectTimeout(connectionTimeout);

            Log.d(TAG, "Request Method => " + senderMethod);

            // Set HTTP method to POST
            if (senderMethod == SENDER_METHOD_POST) {
                httpConnection.setRequestMethod(METHOD_POST);
            } else {
                httpConnection.setRequestMethod(METHOD_GET);
            }

            // Set it to true as we are connecting for input
            httpConnection.setDoInput(true);

            // Opens the communication link
            httpConnection.connect();

            if (senderMethod == SENDER_METHOD_POST) {

                // Write data (byte) to the data output stream
                DataOutputStream dataOutputStream = new DataOutputStream(httpConnection.getOutputStream());
                dataOutputStream.writeBytes(postParam);

                // Flush data output stream
                dataOutputStream.flush();
                dataOutputStream.close();
            }

            // Getting HTTP response code
            int responseCode = httpConnection.getResponseCode();

            // If responseCode is 200 / OK the read InputStream
            // HttpURLConnection.HTTP_OK is equal to 200
            if (responseCode == HttpURLConnection.HTTP_OK) {
                dataInputStream = httpConnection.getInputStream();
            } else {

                // 若與 HTTP Server 連線成功但出現錯誤時，
                // 將 HTTP Error Code 回報使用者
                Log.d(TAG, "Response Code => [" + responseCode + "]");

                // 當非 HTTP_OK 情況發生時，將 HTTP Code 回傳 Main UI
                if (callback != null) {
                    callback.onHttpResponse(String.valueOf(responseCode));
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in get Data", e);
        }

        return dataInputStream;
    }

    String convertStreamToString(InputStream inputStream) {

        InputStreamReader isReader = new InputStreamReader(inputStream);
        BufferedReader bfReader = new BufferedReader(isReader);
        StringBuilder sbResponse = new StringBuilder();

        String line = null;

        try {
            while ((line = bfReader.readLine()) != null) {
                sbResponse.append(line);
            }

        } catch (IOException e) {
            Log.e(TAG, "Error in ConvertStreamToString", e);

        } finally {

            try {
                inputStream.close();

            } catch (IOException e) {
                Log.e(TAG, "Error in ConvertStreamToString", e);

            }
        }

        return sbResponse.toString();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        Log.v(TAG, "Please wait...");
    }

    @Override
    protected String doInBackground(String... strings) {

        InputStream is = null;
        String url = strings[0];
        Log.d(TAG, "URL = " + url);
        String res = "";

        is = sendByMethod(url);

        if (is != null) {
            res = convertStreamToString(is);

        } else {
            Log.d(TAG, "Something went wrong!");
        }

        return res;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);

        // Return HTTP connection result.
        callback.onHttpResponse(s);

        Log.d(TAG, "Result = " + s);
    }

    public void setOnResponseListener(ResponseCallback callback) {
        this.callback = callback;
    }

    public void setServerIP(String serverIP) {
        this.strServerIP = serverIP;
    }

    public void setSenderMethod(int method) {
        this.senderMethod = method;
    }

    public String getSenderMethod() {
        return (this.senderMethod == 0) ? METHOD_GET : METHOD_POST;
    }

    public void setParamsName(String paramsName) {

        if (paramsName.isEmpty()) {
            throw new IllegalStateException ("The empty parameter name is not allow! Please check and try again!");

        } else {
            this.strParamsName = paramsName;
        }
    }

    public void setParamsValue(String paramsValue) {
        this.strParamsValue = paramsValue;
    }

    public String getServerIP() {
        return strServerIP;
    }

    public String getParamsName() {
        return strParamsName;
    }

    public String getParamsValue() {
        return strParamsValue;
    }

    public void setParams(HashMap<String, String> parameters) {
        this.hmapParams = parameters;
    }

    public void setActionName(String actionName) {
        this.strActionName = actionName;
    }

    public String getActionName() {
        return strActionName;
    }
}
