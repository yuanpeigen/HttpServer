package com.guo.duoduo.httpserver.utils;


import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.lang.reflect.Method;


public class Network {

    public static String getLocalIp(Context context) {
        WifiManager wifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);//获取WifiManager

        //检查wifi是否开启
        if (!wifiManager.isWifiEnabled() && !isWifiApEnabled(context)) {
            return null;
        }

        WifiInfo wifiinfo = wifiManager.getConnectionInfo();

        String ip = intToIp(wifiinfo.getIpAddress());

        return ip;
    }

    public static boolean isWifiApEnabled(Context context) {
        WifiManager wifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);//获取WifiManager
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


    private static String intToIp(int paramInt) {
        return (paramInt & 0xFF) + "." + (0xFF & paramInt >> 8) + "."
                + (0xFF & paramInt >> 16) + "." + (0xFF & paramInt >> 24);
    }


}
