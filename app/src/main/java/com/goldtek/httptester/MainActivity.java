package com.goldtek.httptester;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private static final String TAG;

    static {
        TAG = "MainActivity -> ";
    }

    EditText edtDeviceIP;
    TextView edtAction;
    EditText edtParamsName;
    EditText edtParamsValue;
    RadioButton rbtMethodPost;
    RadioButton rbtMethodGet;
    TextView tvResponseResult;
    Button btnSend;
    int intSendMethod;

    Thread connectThread;
    HttpConnectionRunnable connRunnable;

    // 為了解決 Handler 使用 Thread 時可能發生的記憶體洩漏問題
    private static class WeakResponseHandler extends Handler {

        private final WeakReference<MainActivity> mActivity;

        WeakResponseHandler(MainActivity activity) {
            this.mActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {

            MainActivity mActivity = this.mActivity.get();

            if (mActivity != null) {

                Bundle mBundle;
                int responseCode = 0;
                String responseData = "";

                if (msg.what == 1) {
                    mBundle = msg.getData();
                    responseCode = mBundle.getInt("httpCode");
                    responseData = mBundle.getString("httpData");

                    if (mActivity.tvResponseResult == null) {
                        mActivity.tvResponseResult = mActivity.findViewById(R.id.tv_response_result);
                        Log.d(TAG, "TextView[Result] was not found!");
                    }

                    // 顯示遠端回應內容
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        mActivity.tvResponseResult.setTextColor(mActivity.getResources().getColor(R.color.default_black));
                        mActivity.tvResponseResult.setText(responseData);

                    } else {
                        Log.d(TAG, "(M)responseCode => " + String.valueOf(responseCode));

                        // 顯示回傳結果
                        String httpCode = "http_" + String.valueOf(responseCode);
                        mActivity.tvResponseResult.setTextColor(mActivity.getResources().getColor(R.color.errorRed));
                        mActivity.tvResponseResult.setText(mActivity.getResources().getIdentifier(httpCode, "string", mActivity.getPackageName()));
                    }
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtDeviceIP = findViewById(R.id.edt_device_ip);
        edtAction = findViewById(R.id.edt_action_name);
        edtParamsName = findViewById(R.id.edt_params_name);
        edtParamsValue = findViewById(R.id.edt_params_value);
        rbtMethodPost = findViewById(R.id.rbt_post);
        rbtMethodGet = findViewById(R.id.rbt_get);
        tvResponseResult = findViewById(R.id.tv_response_result);
        btnSend = findViewById(R.id.btn_send);

        // 避免開啟 Activity 時 focus 在 EditText 物件上
        // 強制指定先行 focus 在 TextView
        tvResponseResult.requestFocus();
        tvResponseResult.setMovementMethod(new ScrollingMovementMethod());

        // 為了 Callback 能夠正常操作 View 物件，故必須確認傳送了正確的 Activity
        // 若是在 BtnEventListener 物件中才設定 setOnHttpResponseListener 時將會發生
        // 找不到 View Widget 的 NullPointer Exception
        connRunnable = new HttpConnectionRunnable(new WeakResponseHandler(this));

        rbtMethodGet.setOnCheckedChangeListener(new RbtnEventListener());
        rbtMethodPost.setOnCheckedChangeListener(new RbtnEventListener());
        btnSend.setOnClickListener(new BtnEventListener());
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (connectThread != null) {

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.d(TAG, "Thread was interrupted, Failed to complete operation.");
                e.printStackTrace();
            }
        }
    }

    private boolean isIPv4Address(String ipAddress) {
        String pattern = "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b";
        return ipAddress.matches(pattern);
    }

    private class BtnEventListener implements View.OnClickListener {

        private final String TAG = "(MA)BtnEvent -> ";

        String url = "";
        private String ipAddress = "";
        private String actionName = "";
        private String paramsName = "";
        private String paramsValue = "";
        private HashMap<String, String> params = new HashMap<>();

        @Override
        public void onClick(View v) {

            Log.d(TAG, "Send button was click!");

            StringBuilder sbErrMsg = new StringBuilder();
            boolean errorFlag = false;
            tvResponseResult.setText("");
            tvResponseResult.setTextColor(getResources().getColor(R.color.default_black));

            switch (v.getId()) {
                case R.id.btn_send:

                    ipAddress = edtDeviceIP.getText().toString();
                    actionName = edtAction.getText().toString();
                    paramsName = edtParamsName.getText().toString();
                    paramsValue = edtParamsValue.getText().toString();

                    if (ipAddress.isEmpty() || !isIPv4Address(ipAddress)) {
                        errorFlag = true;
                        sbErrMsg.append("The IP address is not legal!\n");
                    }

                    if (actionName.isEmpty() || paramsName.isEmpty()) {
                        errorFlag = true;
                        sbErrMsg.append("Empty Action Name or Parameter are not allowed!\n");
                    }

                    if (!paramsName.isEmpty() && paramsValue.isEmpty()) {
                        errorFlag = true;
                        sbErrMsg.append("No any values are enter!\n");
                    }

                    if (!errorFlag) {

                        Log.d(TAG, "IP Address => " + ipAddress);
                        Log.d(TAG, "HTTP Method => " + intSendMethod);
                        Log.d(TAG, "Action => " + actionName);
                        Log.d(TAG, "parameter name => " + paramsName);
                        Log.d(TAG, "parameter value => " + paramsValue);

                        // 當沒有錯誤後才進行 connRunnable 物件資料設定
                        if (connRunnable != null) {
                            connRunnable.setServerIP(ipAddress);
                            connRunnable.setHttpMethod(intSendMethod);
                            connRunnable.setActionName(actionName);
                            connRunnable.setParamsName(paramsName);
                            connRunnable.setParamsValue(paramsValue);

                            connectThread = new Thread(connRunnable, "connThread");
                            connectThread.start();

                        } else {
                            Log.d(TAG, "Thread initial failed....");
                        }

                    } else {
                        sbErrMsg.append("Please check and try again!");

                        tvResponseResult.setTextColor(getResources().getColor(R.color.errorRed));
                        tvResponseResult.setText(sbErrMsg.toString());
                        Log.d(TAG, "Something went wrong, please try again!");
                    }

                    break;

                default:
            }
        }
    }

    private class RbtnEventListener implements CompoundButton.OnCheckedChangeListener {

        private final String TAG = "(MA)RbtnEvent -> ";

        String methodName = "";
        private HashMap<String, String> params = new HashMap<>();

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            if (isChecked) {

                switch (buttonView.getId()) {
                    case R.id.rbt_get:
                        intSendMethod = AppConfiguration.METHOD_HTTP_GET;
                        methodName = AppConfiguration.METHOD_NAME_GET;

                        break;

                    case R.id.rbt_post:
                        intSendMethod = AppConfiguration.METHOD_HTTP_POST;
                        methodName = AppConfiguration.METHOD_NAME_POST;

                        break;

                    default:
                }

                Log.d(TAG, "Radiobox \"Method\" was changed - " + methodName);
            }
        }
    }
}
