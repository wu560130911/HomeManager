package com.wms.homemanager.utils;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wms.homemanager.model.LoginInfo;
import com.wms.homemanager.model.PortForwardingConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortForwardingUtils {
    private static final String PREFS_NAME = "PortForwardingPrefs";
    private static final String KEY_PORT_FORWARDING_CONFIGS = "portForwardingConfigs";
    private static final Gson gson = new Gson();

    /**
     * 从 SharedPreferences 中获取指定用户的端口转发配置列表
     * @param context 上下文
     * @param loginInfo 用户登录信息
     * @return 端口转发配置列表，如果没有则返回空列表
     */
    public static List<PortForwardingConfig> getPortForwardingConfigs(Context context, LoginInfo loginInfo) {
        if (loginInfo == null) {
            return new ArrayList<>();
        }

        Map<String, List<PortForwardingConfig>> allConfigs = getAllConfigs(context);
        String key = generateKey(loginInfo);

        if (allConfigs.containsKey(key)) {
            return allConfigs.get(key);
        }

        return new ArrayList<>();
    }

    /**
     * 保存指定用户的端口转发配置列表到 SharedPreferences
     * @param context 上下文
     * @param loginInfo 用户登录信息
     * @param configs 端口转发配置列表
     */
    public static void savePortForwardingConfigs(Context context, LoginInfo loginInfo, List<PortForwardingConfig> configs) {
        if (loginInfo == null) {
            return;
        }

        Map<String, List<PortForwardingConfig>> allConfigs = getAllConfigs(context);
        String key = generateKey(loginInfo);

        allConfigs.put(key, configs);
        saveAllConfigs(context, allConfigs);
    }

    /**
     * 从 SharedPreferences 中获取所有用户的端口转发配置
     * @param context 上下文
     * @return 所有用户的端口转发配置映射
     */
    private static Map<String, List<PortForwardingConfig>> getAllConfigs(Context context) {
        String json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_PORT_FORWARDING_CONFIGS, "");

        if (!json.isEmpty()) {
            TypeToken<Map<String, List<PortForwardingConfig>>> typeToken = new TypeToken<Map<String, List<PortForwardingConfig>>>() {};
            return gson.fromJson(json, typeToken.getType());
        }

        return new HashMap<>();
    }

    /**
     * 保存所有用户的端口转发配置到 SharedPreferences
     * @param context 上下文
     * @param allConfigs 所有用户的端口转发配置映射
     */
    private static void saveAllConfigs(Context context, Map<String, List<PortForwardingConfig>> allConfigs) {
        String json = gson.toJson(allConfigs);
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PORT_FORWARDING_CONFIGS, json)
                .apply();
    }

    /**
     * 生成用户配置的唯一键
     * @param loginInfo 用户登录信息
     * @return 唯一键
     */
    private static String generateKey(LoginInfo loginInfo) {
        return loginInfo.getUsername() + "@" + loginInfo.getHost() + ":" + loginInfo.getPort();
    }

    /**
     * 清除指定用户的端口转发配置
     * @param context 上下文
     * @param loginInfo 用户登录信息
     */
    public static void clearPortForwardingConfigs(Context context, LoginInfo loginInfo) {
        if (loginInfo == null) {
            return;
        }

        Map<String, List<PortForwardingConfig>> allConfigs = getAllConfigs(context);
        String key = generateKey(loginInfo);

        allConfigs.remove(key);
        saveAllConfigs(context, allConfigs);
    }

    /**
     * 清除所有用户的端口转发配置
     * @param context 上下文
     */
    public static void clearAllPortForwardingConfigs(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_PORT_FORWARDING_CONFIGS)
                .apply();
    }
}
