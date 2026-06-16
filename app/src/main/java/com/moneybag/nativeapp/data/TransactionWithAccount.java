package com.moneybag.nativeapp.data;

import androidx.room.Embedded;
import androidx.room.Relation;

public class TransactionWithAccount {
    @Embedded
    public Transaction transaction;

    @Relation(
        parentColumn = "accountId",
        entityColumn = "id"
    )
    public Account account;

    @Relation(
        parentColumn = "toAccountId",
        entityColumn = "id"
    )
    public Account toAccount;
}
