package com.yurxz.rejoin;

public class RobloxInstance {
    public String name;
    public String psLink;
    public String packageName;
    public String status; // "Running", "Frozen", "Rejoining", "Idle"

    public RobloxInstance(String name, String psLink, String packageName) {
        this.name = name;
        this.psLink = psLink;
        this.packageName = packageName != null ? packageName : "com.roblox.client";
        this.status = "Idle";
    }
}
