package com.guo.duoduo.httpserver.ui;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.guo.duoduo.httpserver.R;
import com.guo.duoduo.httpserver.service.WebService;
import com.guo.duoduo.httpserver.utils.Constant;
import com.guo.duoduo.httpserver.utils.Network;
import com.guo.duoduo.httpserver.utils.WifiApControl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private MainHandler handler;
    private Button btn;
    private View view_share, v_parent;
    private PopupWindow mPopupWindow_share = null;
    private LayoutInflater inflate;
    private String ssid = "NexFi";
    private WifiManager wifiManager;
    private Handler mHandler, ex_handler;
    private AlertDialog mAlertDialog;
    private Thread thread;
    private boolean isExit, isWifiOpen;
    private WifiApControl wifiApControl;
    private String SSID, preSharedKey, userSSID, userPreShareedKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        if (wifiManager.isWifiEnabled()) {
            isWifiOpen = true;
        } else {
            isWifiOpen = false;
        }

        wifiApControl = WifiApControl.getApControl(wifiManager);
        WifiConfiguration wifiConfiguration = wifiApControl.getWifiApConfiguration();
        SSID = wifiConfiguration.SSID;
        preSharedKey = wifiConfiguration.preSharedKey;
        if (SSID != null && !SSID.equals(ssid)) {
            saveNetConfig(this, SSID, preSharedKey);
        }
        inflate = getLayoutInflater();
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                createFile(R.raw.nexfi_ble, "nexfi_ble");
                createWifiAccessPoint(ssid);
                startService(new Intent(getApplicationContext(), WebService.class));
            }
        });
        thread.start();
        handler = new MainHandler(this);
        mHandler = new Handler();
        ex_handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                isExit = false;
            }
        };
        initDialog();
        initView();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mAlertDialog.dismiss();
                Toast.makeText(MainActivity.this, "服务启动成功", Toast.LENGTH_SHORT).show();
                btn.setVisibility(View.VISIBLE);
            }
        }, 2000);
        initUserNetConfig();
        Log.e("SSID:" + SSID + "\n" + "preSharedKey:" + preSharedKey, "+++++++++++++++++++++++++++++++++++++++");
    }

    private void saveNetConfig(Context context, String SSID, String preSharedKey) {
        SharedPreferences sp = context.getSharedPreferences("netConfig", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("SSID", SSID);
        editor.putString("preSharedKey", preSharedKey);
        editor.commit();
    }

    private void initUserNetConfig() {
        userSSID = initSSID(this);
        userPreShareedKey = initPreSharedKey(this);
        Log.e("initUserNetConfig=\n", userSSID + "\n" + userPreShareedKey + "-----------------------");
    }

    private String initPreSharedKey(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("netConfig", Context.MODE_PRIVATE);
        preSharedKey = preferences.getString("preSharedKey", null);
        return preSharedKey;
    }

    private String initSSID(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("netConfig", Context.MODE_PRIVATE);
        SSID = preferences.getString("SSID", null);
        return SSID;
    }


    private void initDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View v = inflater.inflate(R.layout.dialog_loading, null);
        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0x00000000));
        mAlertDialog.show();
        mAlertDialog.getWindow().setContentView(v);
        mAlertDialog.setCancelable(false);
        Toast.makeText(MainActivity.this, "正在启动服务，请稍后", Toast.LENGTH_SHORT).show();
    }


    public boolean isWifiApEnabled() {
        try {
            Method method = wifiManager.getClass().getMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifiManager);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    private void createWifiAccessPoint(String ssid) {
        if (wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
        }
        if (isWifiApEnabled()) {
            closeWifiAp();
        }
        Method[] wmMethods = wifiManager.getClass().getDeclaredMethods();
        boolean methodFound = false;
        for (Method method : wmMethods) {
            if (method.getName().equals("setWifiApEnabled")) {
                methodFound = true;
                WifiConfiguration netConfig = new WifiConfiguration();
                netConfig.SSID = ssid;
                netConfig.allowedAuthAlgorithms.set(
                        WifiConfiguration.AuthAlgorithm.OPEN);
                try {
                    boolean apstatus = (Boolean) method.invoke(
                            wifiManager, netConfig, true);
                    for (Method isWifiApEnabledmethod : wmMethods) {
                        if (isWifiApEnabledmethod.getName().equals(
                                "isWifiApEnabled")) {
                            while (!(Boolean) isWifiApEnabledmethod.invoke(
                                    wifiManager)) {
                            }
                            for (Method method1 : wmMethods) {
                                if (method1.getName().equals(
                                        "getWifiApState")) {
                                    int apstate;
                                    apstate = (Integer) method1.invoke(
                                            wifiManager);
                                }
                            }
                        }
                    }
                    if (apstatus) {
                        Log.d("Splash Activity",
                                "Access Point created");
                    } else {
                        Log.d("Splash Activity",
                                "Access Point creation failed");
                    }

                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        if (!methodFound) {
            Log.d("Splash Activity",
                    "cannot configure an access point");
        }
    }

    private void restore(boolean flag) {
        closeWifiAp();
        try {
            Method enableWifi = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            String ssid = userSSID;
            String pass = userPreShareedKey;
            WifiConfiguration myConfig = new WifiConfiguration();
            myConfig.SSID = ssid;
            myConfig.preSharedKey = pass;
            myConfig.status = WifiConfiguration.Status.ENABLED;
            myConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            myConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            myConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            myConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            myConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            myConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            myConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            boolean result = (Boolean) enableWifi.invoke(wifiManager, myConfig, flag);
            Log.e("restore: ", result + "");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    private void setApEnabled() {
        try {
            Method enableWifi = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            WifiConfiguration wifiConfiguration = wifiApControl.getWifiApConfiguration();
            enableWifi.invoke(wifiManager, wifiConfiguration, false);
        } catch (Exception e) {
            Log.e("Failed", "===============");
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!isExit) {
                isExit = true;
                mHandler.sendEmptyMessageDelayed(0, 1500);
                Toast.makeText(this, "再按一次退出NexFi，关闭服务", Toast.LENGTH_SHORT).show();
                return false;
            } else {
                finish();
            }
        }
        return true;
    }


    private void closeWifiAp() {
        if (isWifiApEnabled()) {
            try {
                Method method = wifiManager.getClass().getMethod("getWifiApConfiguration");
                method.setAccessible(true);
                WifiConfiguration config = (WifiConfiguration) method.invoke(wifiManager);
                Method method2 = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                method2.invoke(wifiManager, config, false);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "热点已关闭", Toast.LENGTH_SHORT).show();
        }

    }


    private void initPopShare() {
        if (mPopupWindow_share == null) {
            mPopupWindow_share = new PopupWindow(view_share, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
            mPopupWindow_share.setBackgroundDrawable(new ColorDrawable(0x00000000));
        }
        mPopupWindow_share.showAtLocation(v_parent, Gravity.CENTER, 0, 0);
    }


    private void initView() {
        btn = (Button) findViewById(R.id.btn);
        v_parent = inflate.inflate(R.layout.activity_main, null);
        view_share = inflate.inflate(R.layout.layout_share, null);
        btn.setOnClickListener(this);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        initUserNetConfig();
        stopService(new Intent(getApplicationContext(), WebService.class));
        restore(true);
        setApEnabled();
        if (isWifiOpen) {
            wifiManager.setWifiEnabled(true);
        }
        Log.e("" + isWifiOpen, "onDestroy: ---------------");
    }

    public void createFile(int id, String name) {

        String file_name = name;//文件名
        File fileDir = null;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            //存在sd卡
            fileDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
            if (!fileDir.exists()) {
                fileDir.mkdirs();
            }
        }
        String rece_file_path = fileDir + "/" + file_name + ".apk";
        try {
            File file = new File(rece_file_path);
            if (!file.exists()) {// 文件不存在
                System.out.println("要打开的文件不存在");
                InputStream ins = getResources().openRawResource(
                        id);// 通过raw得到数据资源
                System.out.println("开始读入");
                FileOutputStream fos = new FileOutputStream(file);
                System.out.println("开始写出");
                byte[] buffer = new byte[8192];
                int count = 0;// 循环写出
                while ((count = ins.read(buffer)) > 0) {
                    fos.write(buffer, 0, count);
                }
                System.out.println("已经创建该文件");
                fos.close();// 关闭流
                ins.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn:
                String ip = Network.getLocalIp(getApplicationContext());
                if (TextUtils.isEmpty(ip)) {
                    Message msg = new Message();
                    msg.what = Constant.MSG.GET_NETWORK_ERROR;
                    handler.sendMessage(msg);
                } else {
                    initPopShare();
                    Log.e("ip:", ip + "..............------------------ ");
                }
                break;
        }
    }

    private class MainHandler extends Handler {
        private WeakReference<MainActivity> weakReference;

        public MainHandler(MainActivity activity) {
            weakReference = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final MainActivity activity = weakReference.get();
            if (activity == null)
                return;

            switch (msg.what) {
                case Constant.MSG.GET_NETWORK_ERROR:
                    Toast.makeText(MainActivity.this, "手机网络地址获取失败，即将退出程序", Toast.LENGTH_SHORT).show();
                    activity.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            activity.finish();
                            android.os.Process.killProcess(android.os.Process.myPid());
                        }
                    }, 2 * 1000);
                    break;
            }
        }
    }
}
