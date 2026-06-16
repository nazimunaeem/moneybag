package com.moneybag.nativeapp.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class Category {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String syncId;
    public long lastModified;
    public boolean isDeleted;

    public String name;
    public String icon; // Icon name or resource ID string
    public TransactionType type;
    public int position;

    public Category() {
        this.syncId = java.util.UUID.randomUUID().toString();
        this.lastModified = System.currentTimeMillis();
        this.isDeleted = false;
    }

    public Category(String name, TransactionType type) {
        this();
        this.name = name;
        this.type = type;
    }
}
