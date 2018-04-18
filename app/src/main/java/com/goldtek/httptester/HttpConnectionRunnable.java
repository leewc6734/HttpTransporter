package com.goldtek.httptester;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
 * Created by ljason on 2018/4/17.
 */

public class HttpConnectionRunnable implements Runnable {

    private static final String TAG;

    static {
        TAG = "HttpConnectionRunnable -> ";
    }

    private String strServerIP;
    private String strActionName;
    private HashMap<String, String> hmapParams;
    private String strParamsName;
    private String strParamsValue;
    private String strURL;
    private int httpResponseCode = 0;


    // Default settings.
    private int httpMethod = AppConfiguration.METHOD_HTTP_GET;
    private Handler mHandler;

    public HttpConnectionRunnable(Handler handler) {
        setHandler(handler);
    }

    public HttpConnectionRunnable(String strServerIP, String strActionName, int method,
                                  String strParamsName, String strParamsValue) {

        setServerIP(strServerIP);
        setActionName(strActionName);
        setParamsName(strParamsName);
        setParamsValue(strParamsValue);
        setHttpMethod(method);
    }

    @Override
    public void run() {

        Log.v(TAG, "Please wait...");

        InputStream is = null;
        String defaultParams = "post=1";
        String postParams = "";
        String res = "";

        switch (httpMethod) {

            case AppConfiguration.METHOD_HTTP_POST:

                Log.d(TAG, "HTTP data sending by -> [" + AppConfiguration.METHOD_NAME_POST + "]");
                strURL = AppConfiguration.PROTOCOL_HTTP + strServerIP + strActionName;

                postParams = defaultParams + "&" + strParamsName + "=" + strParamsValue;
                /*String postParams = strParamsName1 + "=" + strParamsValue1 + "&amp;" +
                        strParamsName2 + "=" + strParamsValue2;*/

                Log.d(TAG, "URL = " + strURL);
                Log.d(TAG, "Parameters => " + postParams);

                is = sendByPostMethod(strURL, postParams);

                break;

            default:

                Log.d(TAG, "HTTP data sending by -> [" + AppConfiguration.METHOD_NAME_GET + "]");
                postParams = strParamsName + "=" + strParamsValue;
                strURL = AppConfiguration.PROTOCOL_HTTP + strServerIP + strActionName + "?" + postParams;

                Log.d(TAG, "URL = " + strURL);
                is = sendByGetMethod(strURL);

        }

        if (is != null) {
            res = convertStreamToString(is);
            Log.d(TAG, "res = " + res);

        } else {
            Log.d(TAG, "Something went wrong!");
        }

        // 回傳 HTTP 結果
        //httpResponseCallback.onHttpResponse(httpResponseCode, res);

        Bundle mBundle = new Bundle();
        mBundle.putInt("httpCode", httpResponseCode);
        mBundle.putString("httpData", res);

        Message msg = mHandler.obtainMessage();
        msg.what = 1;
        msg.setData(mBundle);
        mHandler.sendMessage(msg);
    }

    //TODO: serverURL => protocol, server-ip, method
    private InputStream sendByGetMethod(String serverURL) {

        InputStream dataInputStream = null;
        int readTimeout = 5000;
        int connectionTimeout = 5000;

        // Preparing
        try {

            URL url = new URL(serverURL);
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();

            // Set timeout for reading InputStream
            httpConnection.setReadTimeout(readTimeout);

            // Set timeout for connection
            httpConnection.setConnectTimeout(connectionTimeout);

            // Set HTTP method to POST
            httpConnection.setRequestMethod(AppConfiguration.METHOD_NAME_GET);

            // Set it to true as we are connecting for input
            httpConnection.setDoInput(true);

            // Getting HTTP response code
            int responseCode = httpConnection.getResponseCode();
            setHttpResponseCode(responseCode);

            // If responseCode is 200 / OK the read InputStream
            // HttpURLConnection.HTTP_OK is equal to 200
            if (responseCode == HttpURLConnection.HTTP_OK) {
                dataInputStream = httpConnection.getInputStream();

            } else {

                // 若與 HTTP Server 連線成功但出現錯誤時，
                // 將 HTTP Error Code 回報使用者
                Log.d(TAG, "Response Code => [" + responseCode + "]");

                // 當非 HTTP_OK 情況發生時，將 HTTP Code 回傳 Main UI
//                if (callback != null) {
//                    callback.onHttpResponse(String.valueOf(responseCode));
//                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in get Data", e);
        }

        return dataInputStream;
    }

    private InputStream sendByPostMethod(String serverURL, String params) {

        InputStream dataInputStream = null;
        int readTimeout = 5000;
        int connectionTimeout = 5000;

        // Preparing
        try {

            URL url = new URL(serverURL);
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();

            // Set timeout for reading InputStream
            httpConnection.setReadTimeout(readTimeout);

            // Set timeout for connection
            httpConnection.setConnectTimeout(connectionTimeout);

            httpConnection.setRequestMethod(AppConfiguration.METHOD_NAME_POST);

            // Set it to true as we are connecting for input
            httpConnection.setDoInput(true);

            // Opens the communication link
            httpConnection.connect();

            // Write data (byte) to the data output stream
            DataOutputStream dataOutputStream = new DataOutputStream(httpConnection.getOutputStream());
            dataOutputStream.writeBytes(params);

            // Flush data output stream
            dataOutputStream.flush();
            dataOutputStream.close();

            // Getting HTTP response code
            int responseCode = httpConnection.getResponseCode();
            setHttpResponseCode(responseCode);

            // If responseCode is 200 / OK the read InputStream
            // HttpURLConnection.HTTP_OK is equal to 200
            if (responseCode == HttpURLConnection.HTTP_OK) {
                dataInputStream = httpConnection.getInputStream();

            } else {

                // 若與 HTTP Server 連線成功但出現錯誤時，
                // 將 HTTP Error Code 回報使用者
                Log.d(TAG, "Response Code => [" + responseCode + "]");

                // 當非 HTTP_OK 情況發生時，將 HTTP Code 回傳 Main UI
//                if (callback != null) {
//                    callback.onHttpResponse(String.valueOf(responseCode));
//                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in get Data", e);
        }

        return dataInputStream;
    }

    private String convertStreamToString(InputStream inputStream) {

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

    public void setHandler(Handler handler) {
        this.mHandler = handler;
    }

    public void setServerIP(String serverIP) {
        this.strServerIP = serverIP;
    }

    public void setHttpMethod(int method) {
        this.httpMethod = method;
    }

    public String getHttpMethod() {
        return (this.httpMethod == 0) ? AppConfiguration.METHOD_NAME_GET : AppConfiguration.METHOD_NAME_POST;
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

    public void setHttpResponseCode(int httpResponseCode) {
        this.httpResponseCode = httpResponseCode;
    }
}
