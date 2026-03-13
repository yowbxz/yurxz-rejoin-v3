package com.yurxz.rejoin;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AppConfig {
    private static final String PREFS = "yurxz_prefs";
    private static final Gson gson = new Gson();

    public static List<RobloxInstance> getInstances(Context ctx) {
        String json = ctx.getSharedPreferences(PREFS, 0).getString("instances", "[]");
        Type type = new TypeToken<List<RobloxInstance>>(){}.getType();
        List<RobloxInstance> list = gson.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    public static void saveInstances(Context ctx, List<RobloxInstance> list) {
        ctx.getSharedPreferences(PREFS, 0).edit()
            .putString("instances", gson.toJson(list)).apply();
    }

    public static String getWebhook(Context ctx) {
        return ctx.getSharedPreferences(PREFS, 0).getString("webhook", "");
    }
    public static void setWebhook(Context ctx, String url) {
        ctx.getSharedPreferences(PREFS, 0).edit().putString("webhook", url).apply();
    }

    public static int getInterval(Context ctx) {
        return ctx.getSharedPreferences(PREFS, 0).getInt("interval", 30);
    }
    public static void setInterval(Context ctx, int sec) {
        ctx.getSharedPreferences(PREFS, 0).edit().putInt("interval", sec).apply();
    }

    public static boolean getAutoMute(Context ctx) {
        return ctx.getSharedPreferences(PREFS, 0).getBoolean("auto_mute", true);
    }
    public static void setAutoMute(Context ctx, boolean v) {
        ctx.getSharedPreferences(PREFS, 0).edit().putBoolean("auto_mute", v).apply();
    }

    public static boolean getAutoLowRes(Context ctx) {
        return ctx.getSharedPreferences(PREFS, 0).getBoolean("auto_low_res", true);
    }
    public static void setAutoLowRes(Context ctx, boolean v) {
        ctx.getSharedPreferences(PREFS, 0).edit().putBoolean("auto_low_res", v).apply();
    }

    public static boolean getFloatingWindow(Context ctx) {
        return ctx.getSharedPreferences(PREFS, 0).getBoolean("floating_window", false);
    }
    public static void setFloatingWindow(Context ctx, boolean v) {
        ctx.getSharedPreferences(PREFS, 0).edit().putBoolean("floating_window", v).apply();
    }

    public static boolean getAutoScreenshot(Context ctx) {
        return ctx.getSharedPreferences(PREFS, 0).getBoolean("auto_screenshot", false);
    }
    public static void setAutoScreenshot(Context ctx, boolean v) {
        ctx.getSharedPreferences(PREFS, 0).edit().putBoolean("auto_screenshot", v).apply();
    }

    public static String getAdbIp(Context ctx) {
        return ctx.getSharedPreferences(PREFS, 0).getString("adb_ip", "");
    }
    public static void setAdbIp(Context ctx, String ip) {
        ctx.getSharedPreferences(PREFS, 0).edit().putString("adb_ip", ip).apply();
    }

    public static int getAdbPort(Context ctx) {
        return ctx.getSharedPreferences(PREFS, 0).getInt("adb_port", 5555);
    }
    public static void setAdbPort(Context ctx, int port) {
        ctx.getSharedPreferences(PREFS, 0).edit().putInt("adb_port", port).apply();
    }
}
