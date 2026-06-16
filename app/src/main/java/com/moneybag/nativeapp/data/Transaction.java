package com.moneybag.nativeapp.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
    tableName = "transactions",
    foreignKeys = {
        @ForeignKey(entity = Account.class, parentColumns = "id", childColumns = "accountId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Account.class, parentColumns = "id", childColumns = "toAccountId", onDelete = ForeignKey.SET_NULL)
    },
    indices = {@Index("accountId"), @Index("toAccountId")}
)
public class Transaction {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String syncId;
    public long lastModified;
    public boolean isDeleted;

    public String title;
    public double amount;
    public String category;
    public String subcategory;
    public String note;
    public String description;
    public String currency;
    public long timestamp;
    public TransactionType type;
    public double exchangeRate = 1.0;
    
    public int accountId;       // From account (or main account for income/expense)
    public Integer toAccountId; // Target account for transfers

    public Transaction() {
        this.syncId = java.util.UUID.randomUUID().toString();
        this.lastModified = System.currentTimeMillis();
        this.isDeleted = false;
    }

    public Transaction(String title, double amount, String category, long timestamp, TransactionType type, int accountId, String currency) {
        this();
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.timestamp = timestamp;
        this.type = type;
        this.accountId = accountId;
        this.currency = currency;
    }
}
