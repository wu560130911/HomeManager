package com.wms.homemanager.utils;

import android.content.Context;

import com.wms.homemanager.model.LoginInfo;
import com.google.gson.Gson;

public class LoginInfoUtils {
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String KEY_CURRENT_LOGIN_INFO = "currentLoginInfo";
    private static final Gson gson = new Gson();

    /**
     * 从 SharedPreferences 中获取当前登录信息
     * @param context 上下文
     * @return LoginInfo 对象，如果没有登录信息则返回 null
     */
    public static LoginInfo getCurrentLoginInfo(Context context) {
        String loginInfoJson = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CURRENT_LOGIN_INFO, "");
        
        if (!loginInfoJson.isEmpty()) {
            return gson.fromJson(loginInfoJson, LoginInfo.class);
        }
        return null;
    }

    /**
     * 保存当前登录信息到 SharedPreferences
     * @param context 上下文
     * @param loginInfo 登录信息对象
     */
    public static void saveCurrentLoginInfo(Context context, LoginInfo loginInfo) {
        String loginInfoJson = gson.toJson(loginInfo);
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CURRENT_LOGIN_INFO, loginInfoJson)
                .apply();
    }

    /**
     * 清除当前登录信息
     * @param context 上下文
     */
    public static void clearCurrentLoginInfo(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_CURRENT_LOGIN_INFO)
                .apply();
    }

    private static final String KEY_LOGIN_INFO_LIST = "loginInfoList";

    /**
     * 从 SharedPreferences 中获取登录信息列表
     * @param context 上下文
     * @return 登录信息列表，如果没有则返回空列表
     */
    public static java.util.List<LoginInfo> getLoginInfoList(Context context) {
        String json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LOGIN_INFO_LIST, "");
        
        if (!json.isEmpty()) {
            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.List<LoginInfo>>() {}.getType();
            return gson.fromJson(json, type);
        }
        return new java.util.ArrayList<>();
    }

    /**
     * 保存登录信息列表到 SharedPreferences
     * @param context 上下文
     * @param loginInfoList 登录信息列表
     */
    public static void saveLoginInfoList(Context context, java.util.List<LoginInfo> loginInfoList) {
        String json = gson.toJson(loginInfoList);
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LOGIN_INFO_LIST, json)
                .apply();
    }

    /**
     * 清除登录信息列表
     * @param context 上下文
     */
    public static void clearLoginInfoList(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_LOGIN_INFO_LIST)
                .apply();
    }
}
