package com.moneybag.nativeapp.data;

import android.content.Context;
import android.net.Uri;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CsvManager {
    private final Context context;
    private final MoneyBagRepository repository;
    private final SimpleDateFormat csvDateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault());

    public CsvManager(Context context, MoneyBagRepository repository) {
        this.context = context;
        this.repository = repository;
    }

    public void importCsv(Uri uri, ImportCallback callback) {
        new Thread(() -> {
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                boolean isHeader = true;
                
                Map<String, Integer> accountMap = new HashMap<>();
                
                while ((line = reader.readLine()) != null) {
                    if (isHeader) {
                        isHeader = false;
                        continue;
                    }
                    
                    String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                    if (parts.length < 10) continue;

                    String dateStr = parts[0];
                    String accountName = parts[1];
                    String category = parts[2];
                    String subcategory = parts[3];
                    String note = parts[4];
                    String typeStr = parts[6];
                    String description = parts[7];
                    double amount = Double.parseDouble(parts[8]);
                    String currency = parts[9];

                    long timestamp = csvDateFormat.parse(dateStr).getTime();
                    int accId = getOrCreateAccountId(accountName, accountMap);

                    TransactionType type;
                    if (typeStr.equalsIgnoreCase("Income")) type = TransactionType.INCOME;
                    else if (typeStr.equalsIgnoreCase("Expense")) type = TransactionType.EXPENSE;
                    else type = TransactionType.TRANSFER;

                    Transaction transaction = new Transaction(description.isEmpty() ? category : description, amount, category, timestamp, type, accId, currency);
                    transaction.subcategory = subcategory;
                    transaction.note = note;
                    transaction.description = description;
                    
                    repository.insertTransaction(transaction);
                }
                reader.close();
                callback.onSuccess();
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void exportCsv(Uri uri, ExportCallback callback) {
        repository.getAllTransactions(transactions -> {
            new Thread(() -> {
                try {
                    OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
                    
                    // Header
                    writer.write("Date,Account,Category,Subcategory,Note,BDT,Income/Expense,Description,Amount,Currency\n");
                    
                    for (TransactionWithAccount t : transactions) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(csvDateFormat.format(new Date(t.transaction.timestamp))).append(",");
                        sb.append(escape(t.account != null ? t.account.name : "Unknown")).append(",");
                        sb.append(escape(t.transaction.category)).append(",");
                        sb.append(escape(t.transaction.subcategory)).append(",");
                        sb.append(escape(t.transaction.note)).append(",");
                        sb.append("0,"); // Placeholder for BDT column
                        sb.append(t.transaction.type.name()).append(",");
                        sb.append(escape(t.transaction.description)).append(",");
                        sb.append(t.transaction.amount).append(",");
                        sb.append(t.transaction.currency).append("\n");
                        writer.write(sb.toString());
                    }
                    
                    writer.close();
                    callback.onSuccess();
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }).start();
        });
    }

    private String escape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private int getOrCreateAccountId(String name, Map<String, Integer> map) {
        if (!map.containsKey(name)) {
            int id = map.size() + 1;
            map.put(name, id);
            repository.insertAccount(new Account(name, 0, AccountType.OTHER, "BDT"));
            return id;
        }
        return map.get(name);
    }

    public interface ImportCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface ExportCallback {
        void onSuccess();
        void onError(String message);
    }
}
