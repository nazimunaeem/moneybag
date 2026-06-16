package com.moneybag.nativeapp.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.util.concurrent.Executors;

@Database(entities = {Account.class, Transaction.class, Category.class}, version = 7, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract MoneyBagDao moneyBagDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "moneybag_database")
                            .fallbackToDestructiveMigration()
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        // Use a local reference to the database instance to avoid recursion
                                        AppDatabase instance = getDatabase(context);
                                        MoneyBagDao dao = instance.moneyBagDao();
                                        // Default Accounts
                                        dao.insertAccount(new Account("Cash 💵", 0, AccountType.CASH, "BDT"));
                                        dao.insertAccount(new Account("bKash 📱", 0, AccountType.CASH, "BDT"));
                                        dao.insertAccount(new Account("Nagad 📱", 0, AccountType.CASH, "BDT"));
                                        dao.insertAccount(new Account("Islami Bank 🏦", 0, AccountType.SAVINGS, "BDT"));
                                        dao.insertAccount(new Account("Dutch Bangla Bank 🏦", 0, AccountType.SAVINGS, "BDT"));
                                        dao.insertAccount(new Account("Pubali DPS 📈", 0, AccountType.SAVINGS, "BDT"));
                                        dao.insertAccount(new Account("IBBL FDR 💰", 0, AccountType.SAVINGS, "BDT"));
                                        
                                        // Default Categories
                                        dao.insertCategory(new Category("Salary 💰", TransactionType.INCOME));
                                        dao.insertCategory(new Category("Business 🏢", TransactionType.INCOME));
                                        dao.insertCategory(new Category("Interest 📈", TransactionType.INCOME));
                                        dao.insertCategory(new Category("Bonus 🧧", TransactionType.INCOME));
                                        dao.insertCategory(new Category("Gift 🎁", TransactionType.INCOME));

                                        dao.insertCategory(new Category("Food 🍔", TransactionType.EXPENSE));
                                        dao.insertCategory(new Category("Transport 🚗", TransactionType.EXPENSE));
                                        dao.insertCategory(new Category("Shopping 🛍️", TransactionType.EXPENSE));
                                        dao.insertCategory(new Category("Rent 🏠", TransactionType.EXPENSE));
                                        dao.insertCategory(new Category("Bills 📑", TransactionType.EXPENSE));
                                        dao.insertCategory(new Category("Education 🎓", TransactionType.EXPENSE));
                                        dao.insertCategory(new Category("Health 💊", TransactionType.EXPENSE));
                                        dao.insertCategory(new Category("Entertainment 🎮", TransactionType.EXPENSE));
                                        
                                        dao.insertCategory(new Category("Transfer 🔄", TransactionType.TRANSFER));
                                    });
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
