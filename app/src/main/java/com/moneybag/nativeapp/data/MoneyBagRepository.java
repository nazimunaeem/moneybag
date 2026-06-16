package com.moneybag.nativeapp.data;

import android.app.Application;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MoneyBagRepository {
    private MoneyBagDao moneyBagDao;
    private ExecutorService executorService;

    public MoneyBagRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        moneyBagDao = db.moneyBagDao();
        executorService = Executors.newFixedThreadPool(4);
    }

    public void getAllAccounts(Callback<List<Account>> callback) {
        executorService.execute(() -> callback.onResult(moneyBagDao.getAllAccounts()));
    }

    public void insertAccount(Account account) {
        insertAccount(account, null);
    }

    public void insertAccount(Account account, Runnable onDone) {
        account.lastModified = System.currentTimeMillis();
        executorService.execute(() -> {
            moneyBagDao.insertAccount(account);
            if (onDone != null) onDone.run();
        });
    }

    public void updateAccount(Account account) {
        updateAccount(account, null);
    }

    public void updateAccount(Account account, Runnable onDone) {
        account.lastModified = System.currentTimeMillis();
        executorService.execute(() -> {
            moneyBagDao.updateAccount(account);
            if (onDone != null) onDone.run();
        });
    }

    public void deleteAccount(Account account) {
        deleteAccount(account, null);
    }

    public void deleteAccount(Account account, Runnable onDone) {
        account.isDeleted = true;
        account.lastModified = System.currentTimeMillis();
        executorService.execute(() -> {
            moneyBagDao.updateAccount(account);
            if (onDone != null) onDone.run();
        });
    }

    public void getAllTransactions(Callback<List<TransactionWithAccount>> callback) {
        executorService.execute(() -> callback.onResult(moneyBagDao.getAllTransactionsWithAccount()));
    }

    public void insertTransaction(Transaction transaction) {
        insertTransaction(transaction, null);
    }

    public void insertTransaction(Transaction transaction, Runnable onDone) {
        transaction.lastModified = System.currentTimeMillis();
        executorService.execute(() -> {
            moneyBagDao.insertTransactionWithBalanceUpdate(transaction);
            if (onDone != null) onDone.run();
        });
    }

    public void deleteTransaction(Transaction transaction) {
        deleteTransaction(transaction, null);
    }

    public void deleteTransaction(Transaction transaction, Runnable onDone) {
        transaction.isDeleted = true;
        transaction.lastModified = System.currentTimeMillis();
        executorService.execute(() -> {
            moneyBagDao.insertTransactionWithBalanceUpdate(transaction);
            if (onDone != null) onDone.run();
        });
    }

    public void getCategories(TransactionType type, Callback<List<Category>> callback) {
        executorService.execute(() -> callback.onResult(moneyBagDao.getCategoriesByType(type)));
    }

    public void getAllCategories(Callback<List<Category>> callback) {
        executorService.execute(() -> callback.onResult(moneyBagDao.getAllCategories()));
    }

    public void insertCategory(Category category) {
        insertCategory(category, null);
    }

    public void insertCategory(Category category, Runnable onDone) {
        category.lastModified = System.currentTimeMillis();
        executorService.execute(() -> {
            moneyBagDao.insertCategory(category);
            if (onDone != null) onDone.run();
        });
    }

    public void updateCategory(Category category) {
        updateCategory(category, null);
    }

    public void updateCategory(Category category, Runnable onDone) {
        category.lastModified = System.currentTimeMillis();
        executorService.execute(() -> {
            moneyBagDao.updateCategory(category);
            if (onDone != null) onDone.run();
        });
    }

    public void deleteCategory(Category category) {
        deleteCategory(category, null);
    }

    public void deleteCategory(Category category, Runnable onDone) {
        category.isDeleted = true;
        category.lastModified = System.currentTimeMillis();
        executorService.execute(() -> {
            moneyBagDao.updateCategory(category);
            if (onDone != null) onDone.run();
        });
    }

    public void getAccountBySyncId(String syncId, Callback<Account> callback) {
        executorService.execute(() -> callback.onResult(moneyBagDao.getAccountBySyncId(syncId)));
    }

    public void getTransactionBySyncId(String syncId, Callback<Transaction> callback) {
        executorService.execute(() -> callback.onResult(moneyBagDao.getTransactionBySyncId(syncId)));
    }

    public void getCategoryBySyncId(String syncId, Callback<Category> callback) {
        executorService.execute(() -> callback.onResult(moneyBagDao.getCategoryBySyncId(syncId)));
    }

    public void clearAllData(Runnable onDone) {
        executorService.execute(() -> {
            moneyBagDao.clearAllData();
            if (onDone != null) onDone.run();
        });
    }

    public interface Callback<T> {
        void onResult(T result);
    }
}
