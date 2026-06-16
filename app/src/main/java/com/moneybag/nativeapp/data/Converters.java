package com.moneybag.nativeapp.data;

import androidx.room.TypeConverter;

public class Converters {
    @TypeConverter
    public static String fromAccountType(AccountType value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static AccountType toAccountType(String value) {
        return value == null ? null : AccountType.valueOf(value);
    }

    @TypeConverter
    public static String fromTransactionType(TransactionType value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static TransactionType toTransactionType(String value) {
        return value == null ? null : TransactionType.valueOf(value);
    }
}
