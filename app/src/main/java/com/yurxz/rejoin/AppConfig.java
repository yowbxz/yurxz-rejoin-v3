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
        String json = prefs(ctx).getString("instances", "[]");
        Type type = new TypeToken<List<RobloxInstance>>(){}.getType();
        List<RobloxInstance> list = gson.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }
    public static void saveInstances(Context ctx, List<RobloxInstance> list) {
        prefs(ctx).edit().putString("instances", gson.toJson(list)).apply();
    }

    public static String getWebhook(Context ctx) { return prefs(ctx).getString("webhook", ""); }
    public static void setWebhook(Context ctx, String v) { prefs(ctx).edit().putString("webhook", v).apply(); }

    public static int getInterval(Context ctx) { return prefs(ctx).getInt("interval", 30); }
    public static void setInterval(Context ctx, int v) { prefs(ctx).edit().putInt("interval", v).apply(); }

    public static int getSsInterval(Context ctx) { return prefs(ctx).getInt("ss_interval", 60); }
    public static void setSsInterval(Context ctx, int v) { prefs(ctx).edit().putInt("ss_interval", v).apply(); }

    public static boolean getAutoLowRes(Context ctx) { return prefs(ctx).getBoolean("auto_low_res", false); }
    public static void setAutoLowRes(Context ctx, boolean v) { prefs(ctx).edit().putBoolean("auto_low_res", v).apply(); }

    public static int getResWidth(Context ctx) { return prefs(ctx).getInt("res_width", 540); }
    public static void setResWidth(Context ctx, int v) { prefs(ctx).edit().putInt("res_width", v).apply(); }

    public static int getResHeight(Context ctx) { return prefs(ctx).getInt("res_height", 960); }
    public static void setResHeight(Context ctx, int v) { prefs(ctx).edit().putInt("res_height", v).apply(); }

    public static int getResDensity(Context ctx) { return prefs(ctx).getInt("res_density", 120); }
    public static void setResDensity(Context ctx, int v) { prefs(ctx).edit().putInt("res_density", v).apply(); }

    public static boolean getFloatingWindow(Context ctx) { return prefs(ctx).getBoolean("floating_window", false); }
    public static void setFloatingWindow(Context ctx, boolean v) { prefs(ctx).edit().putBoolean("floating_window", v).apply(); }

    public static boolean getAutoScreenshot(Context ctx) { return prefs(ctx).getBoolean("auto_screenshot", false); }
    public static void setAutoScreenshot(Context ctx, boolean v) { prefs(ctx).edit().putBoolean("auto_screenshot", v).apply(); }

    public static String getAdbIp(Context ctx) { return prefs(ctx).getString("adb_ip", ""); }
    public static void setAdbIp(Context ctx, String v) { prefs(ctx).edit().putString("adb_ip", v).apply(); }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, 0);
    }
}
