package com.moneybag.nativeapp.data;

public class SettingsItem {
    public enum Type { HEADER, ITEM, SWITCH, DIVIDER }
    
    public String id;
    public String title;
    public String subtitle;
    public int iconRes;
    public Type type;
    public boolean switchState;
    public String category;

    public SettingsItem(String id, String title, String subtitle, int iconRes, Type type, String category) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.iconRes = iconRes;
        this.type = type;
        this.category = category;
    }
}
