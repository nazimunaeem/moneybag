package com.moneybag.nativeapp.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "accounts")
public class Account {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String syncId;
    public long lastModified;
    public boolean isDeleted;

    public String name;
    public double balance;
    public AccountType type;
    public String currency; // Default e.g. "USD"
    public String iconUri;

    public Account() {
        this.syncId = java.util.UUID.randomUUID().toString();
        this.lastModified = System.currentTimeMillis();
        this.isDeleted = false;
    }

    public Account(String name, double balance, AccountType type, String currency) {
        this();
        this.name = name;
        this.balance = balance;
        this.type = type;
        this.currency = currency;
    }
}
