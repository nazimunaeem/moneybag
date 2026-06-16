package com.moneybag.nativeapp.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import java.util.List;

@Dao
public abstract class MoneyBagDao {
    // Accounts
    @Query("SELECT * FROM accounts")
    public abstract List<Account> getAllAccounts();

    @Query("SELECT * FROM accounts WHERE id = :id")
    public abstract Account getAccountById(int id);

    @Query("SELECT * FROM accounts WHERE syncId = :syncId")
    public abstract Account getAccountBySyncId(String syncId);

    @Insert
    public abstract void insertAccount(Account account);

    @Update
    public abstract void updateAccount(Account account);

    @Delete
    public abstract void deleteAccount(Account account);

    // Transactions
    @Transaction
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    public abstract List<TransactionWithAccount> getAllTransactionsWithAccount();

    @Query("SELECT * FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId")
    public abstract List<com.moneybag.nativeapp.data.Transaction> getTransactionsForAccount(int accountId);

    @Insert
    public abstract long _insertTransaction(com.moneybag.nativeapp.data.Transaction transaction);

    @Delete
    public abstract void _deleteTransaction(com.moneybag.nativeapp.data.Transaction transaction);

    @Query("SELECT * FROM transactions WHERE syncId = :syncId")
    public abstract com.moneybag.nativeapp.data.Transaction getTransactionBySyncId(String syncId);

    @Transaction
    public void insertTransactionWithBalanceUpdate(com.moneybag.nativeapp.data.Transaction transaction) {
        _insertTransaction(transaction);
        
        // Update Source Account
        Account account = getAccountById(transaction.accountId);
        if (account != null) {
            double convertedAmount = transaction.amount;
            if (transaction.exchangeRate != 0 && !"BDT".equals(account.currency)) {
                convertedAmount = transaction.amount / transaction.exchangeRate;
            }

            if (transaction.type == TransactionType.INCOME) {
                account.balance += convertedAmount;
            } else if (transaction.type == TransactionType.EXPENSE) {
                account.balance -= convertedAmount;
            } else if (transaction.type == TransactionType.TRANSFER) {
                account.balance -= convertedAmount;
                
                // Update Destination Account for Transfer
                if (transaction.toAccountId != null) {
                    Account toAccount = getAccountById(transaction.toAccountId);
                    if (toAccount != null) {
                        double toConvertedAmount = transaction.amount;
                        // For transfer, we might need a separate exchange rate for the 'toAccount' 
                        // if both are non-primary. But let's assume transaction.exchangeRate 
                        // applies to the non-primary account in the pair.
                        if (transaction.exchangeRate != 0 && !"BDT".equals(toAccount.currency)) {
                            toConvertedAmount = transaction.amount / transaction.exchangeRate;
                        }
                        toAccount.balance += toConvertedAmount;
                        updateAccount(toAccount);
                    }
                }
            } else if (transaction.type == TransactionType.MODIFIED_BALANCE) {
                account.balance = transaction.amount;
            }
            updateAccount(account);
        }
    }

    @Transaction
    public void deleteTransactionWithBalanceUpdate(com.moneybag.nativeapp.data.Transaction transaction) {
        _deleteTransaction(transaction);
        
        Account account = getAccountById(transaction.accountId);
        if (account != null) {
            if (transaction.type == TransactionType.INCOME) {
                account.balance -= transaction.amount;
            } else if (transaction.type == TransactionType.EXPENSE) {
                account.balance += transaction.amount;
            } else if (transaction.type == TransactionType.TRANSFER) {
                account.balance += transaction.amount;
                
                if (transaction.toAccountId != null) {
                    Account toAccount = getAccountById(transaction.toAccountId);
                    if (toAccount != null) {
                        toAccount.balance -= (transaction.amount * transaction.exchangeRate);
                        updateAccount(toAccount);
                    }
                }
            }
            updateAccount(account);
        }
    }

    // Basic Analytics
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME'")
    public abstract double getTotalIncome();

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE'")
    public abstract double getTotalExpense();

    // Categories
    @Query("SELECT * FROM categories")
    public abstract List<Category> getAllCategories();

    @Query("SELECT * FROM categories WHERE syncId = :syncId")
    public abstract Category getCategoryBySyncId(String syncId);

    @Query("SELECT * FROM categories WHERE type = :type")
    public abstract List<Category> getCategoriesByType(TransactionType type);

    @Insert
    public abstract void insertCategory(Category category);

    @Update
    public abstract void updateCategory(Category category);

    @Delete
    public abstract void deleteCategory(Category category);

    @Query("DELETE FROM categories")
    public abstract void deleteAllCategories();

    @Query("DELETE FROM accounts")
    public abstract void deleteAllAccounts();

    @Query("DELETE FROM transactions")
    public abstract void deleteAllTransactions();

    @Transaction
    public void clearAllData() {
        deleteAllTransactions();
        deleteAllAccounts();
        deleteAllCategories();
    }
}
