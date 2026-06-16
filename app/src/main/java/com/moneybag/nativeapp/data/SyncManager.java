package com.moneybag.nativeapp.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SyncManager {
    private final MoneyBagRepository repository;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final SharedPreferences prefs;

    public SyncManager(Context context, MoneyBagRepository repository) {
        this.repository = repository;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.prefs = context.getSharedPreferences("MoneyBagPrefs", Context.MODE_PRIVATE);
    }

    public void sync(SyncCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("User not signed in");
            return;
        }

        String userId = user.getUid();
        long lastSync = prefs.getLong("last_sync_timestamp", 0);

        // We'll perform sync in steps: Accounts, then Categories, then Transactions
        syncAccounts(userId, lastSync, () -> {
            syncCategories(userId, lastSync, () -> {
                syncTransactions(userId, lastSync, () -> {
                    prefs.edit().putLong("last_sync_timestamp", System.currentTimeMillis()).apply();
                    callback.onSuccess();
                }, callback::onError);
            }, callback::onError);
        }, callback::onError);
    }

    private void syncAccounts(String userId, long lastSync, Runnable onDone, ErrorCallback onError) {
        db.collection("users").document(userId).collection("accounts")
            .whereGreaterThan("lastModified", lastSync)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<DocumentSnapshot> remoteChanges = queryDocumentSnapshots.getDocuments();
                
                // 1. Download and merge remote changes
                AtomicInteger counter = new AtomicInteger(remoteChanges.size());
                if (remoteChanges.isEmpty()) {
                    uploadAccounts(userId, lastSync, onDone, onError);
                } else {
                    for (DocumentSnapshot doc : remoteChanges) {
                        Account remoteAccount = doc.toObject(Account.class);
                        if (remoteAccount != null) {
                            repository.getAccountBySyncId(remoteAccount.syncId, localAccount -> {
                                if (localAccount == null || remoteAccount.lastModified > localAccount.lastModified) {
                                    if (localAccount != null) remoteAccount.id = localAccount.id;
                                    repository.updateAccount(remoteAccount, null);
                                }
                                if (counter.decrementAndGet() == 0) {
                                    uploadAccounts(userId, lastSync, onDone, onError);
                                }
                            });
                        } else if (counter.decrementAndGet() == 0) {
                            uploadAccounts(userId, lastSync, onDone, onError);
                        }
                    }
                }
            })
            .addOnFailureListener(e -> onError.onError(e.getMessage()));
    }

    private void uploadAccounts(String userId, long lastSync, Runnable onDone, ErrorCallback onError) {
        repository.getAllAccounts(accounts -> {
            WriteBatch batch = db.batch();
            boolean hasChanges = false;
            for (Account account : accounts) {
                if (account.lastModified > lastSync) {
                    batch.set(db.collection("users").document(userId).collection("accounts").document(account.syncId), account);
                    hasChanges = true;
                }
            }
            if (hasChanges) {
                batch.commit().addOnSuccessListener(aVoid -> onDone.run()).addOnFailureListener(e -> onError.onError(e.getMessage()));
            } else {
                onDone.run();
            }
        });
    }

    private void syncCategories(String userId, long lastSync, Runnable onDone, ErrorCallback onError) {
        db.collection("users").document(userId).collection("categories")
            .whereGreaterThan("lastModified", lastSync)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<DocumentSnapshot> remoteChanges = queryDocumentSnapshots.getDocuments();
                AtomicInteger counter = new AtomicInteger(remoteChanges.size());
                if (remoteChanges.isEmpty()) {
                    uploadCategories(userId, lastSync, onDone, onError);
                } else {
                    for (DocumentSnapshot doc : remoteChanges) {
                        Category remote = doc.toObject(Category.class);
                        if (remote != null) {
                            repository.getCategoryBySyncId(remote.syncId, local -> {
                                if (local == null || remote.lastModified > local.lastModified) {
                                    if (local != null) remote.id = local.id;
                                    repository.updateCategory(remote, null);
                                }
                                if (counter.decrementAndGet() == 0) {
                                    uploadCategories(userId, lastSync, onDone, onError);
                                }
                            });
                        } else if (counter.decrementAndGet() == 0) {
                            uploadCategories(userId, lastSync, onDone, onError);
                        }
                    }
                }
            })
            .addOnFailureListener(e -> onError.onError(e.getMessage()));
    }

    private void uploadCategories(String userId, long lastSync, Runnable onDone, ErrorCallback onError) {
        repository.getAllCategories(categories -> {
            WriteBatch batch = db.batch();
            boolean hasChanges = false;
            for (Category c : categories) {
                if (c.lastModified > lastSync) {
                    batch.set(db.collection("users").document(userId).collection("categories").document(c.syncId), c);
                    hasChanges = true;
                }
            }
            if (hasChanges) {
                batch.commit().addOnSuccessListener(aVoid -> onDone.run()).addOnFailureListener(e -> onError.onError(e.getMessage()));
            } else {
                onDone.run();
            }
        });
    }

    private void syncTransactions(String userId, long lastSync, Runnable onDone, ErrorCallback onError) {
        db.collection("users").document(userId).collection("transactions")
            .whereGreaterThan("lastModified", lastSync)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<DocumentSnapshot> remoteChanges = queryDocumentSnapshots.getDocuments();
                AtomicInteger counter = new AtomicInteger(remoteChanges.size());
                if (remoteChanges.isEmpty()) {
                    uploadTransactions(userId, lastSync, onDone, onError);
                } else {
                    for (DocumentSnapshot doc : remoteChanges) {
                        Transaction remote = doc.toObject(Transaction.class);
                        if (remote != null) {
                            repository.getTransactionBySyncId(remote.syncId, local -> {
                                if (local == null || remote.lastModified > local.lastModified) {
                                    if (local != null) remote.id = local.id;
                                    // Note: In real app, we should also update Account balance if transaction changes
                                    // For now, let's keep it simple.
                                    repository.insertTransaction(remote, null); 
                                }
                                if (counter.decrementAndGet() == 0) {
                                    uploadTransactions(userId, lastSync, onDone, onError);
                                }
                            });
                        } else if (counter.decrementAndGet() == 0) {
                            uploadTransactions(userId, lastSync, onDone, onError);
                        }
                    }
                }
            })
            .addOnFailureListener(e -> onError.onError(e.getMessage()));
    }

    private void uploadTransactions(String userId, long lastSync, Runnable onDone, ErrorCallback onError) {
        repository.getAllTransactions(transactions -> {
            WriteBatch batch = db.batch();
            boolean hasChanges = false;
            for (TransactionWithAccount t : transactions) {
                if (t.transaction.lastModified > lastSync) {
                    batch.set(db.collection("users").document(userId).collection("transactions").document(t.transaction.syncId), t.transaction);
                    hasChanges = true;
                }
            }
            if (hasChanges) {
                batch.commit().addOnSuccessListener(aVoid -> onDone.run()).addOnFailureListener(e -> onError.onError(e.getMessage()));
            } else {
                onDone.run();
            }
        });
    }

    public interface SyncCallback {
        void onSuccess();
        void onError(String message);
    }

    private interface ErrorCallback {
        void onError(String message);
    }
}
