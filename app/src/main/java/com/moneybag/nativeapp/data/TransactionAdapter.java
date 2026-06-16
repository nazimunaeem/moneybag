package com.moneybag.nativeapp.data;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.moneybag.nativeapp.R;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class TransactionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private List<Object> displayItems = new ArrayList<>();
    private OnTransactionClickListener listener;

    public interface OnTransactionClickListener {
        void onTransactionClick(TransactionWithAccount item);
    }

    public void setOnTransactionClickListener(OnTransactionClickListener listener) {
        this.listener = listener;
    }
    private final SimpleDateFormat headerDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public void setTransactions(List<TransactionWithAccount> transactions) {
        displayItems.clear();
        if (transactions.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        // Group by date
        Map<String, List<TransactionWithAccount>> grouped = new TreeMap<>((o1, o2) -> o2.compareTo(o1)); // Reverse date order
        SimpleDateFormat groupKeyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (TransactionWithAccount t : transactions) {
            String key = groupKeyFormat.format(new Date(t.transaction.timestamp));
            if (!grouped.containsKey(key)) {
                grouped.put(key, new ArrayList<>());
            }
            grouped.get(key).add(t);
        }

        for (Map.Entry<String, List<TransactionWithAccount>> entry : grouped.entrySet()) {
            List<TransactionWithAccount> dayTransactions = entry.getValue();
            double dayIncome = 0;
            double dayExpense = 0;
            for (TransactionWithAccount t : dayTransactions) {
                if (t.transaction.type == TransactionType.INCOME) dayIncome += t.transaction.amount;
                else if (t.transaction.type == TransactionType.EXPENSE) dayExpense += t.transaction.amount;
            }

            // Add Header
            displayItems.add(new HeaderItem(dayTransactions.get(0).transaction.timestamp, dayIncome, dayExpense));
            // Add Items
            displayItems.addAll(dayTransactions);
        }
        notifyDataSetChanged();
    }

    public Object getItem(int position) {
        return displayItems.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        return displayItems.get(position) instanceof HeaderItem ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            HeaderItem header = (HeaderItem) displayItems.get(position);
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            
            Date date = new Date(header.timestamp);
            headerHolder.date.setText(headerDateFormat.format(date));
            headerHolder.summary.setText(String.format(Locale.getDefault(), "Inc: ৳%.2f  Exp: ৳%.2f", header.income, header.expense));
            
            // Set Weekday
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            String weekday = "";
            
            switch (dayOfWeek) {
                case Calendar.MONDAY: weekday = "MONDAY"; break;
                case Calendar.TUESDAY: weekday = "TUESDAY"; break;
                case Calendar.WEDNESDAY: weekday = "WEDNESDAY"; break;
                case Calendar.THURSDAY: weekday = "THURSDAY"; break;
                case Calendar.FRIDAY: weekday = "FRIDAY"; break;
                case Calendar.SATURDAY: weekday = "SATURDAY"; break;
                case Calendar.SUNDAY: weekday = "SUNDAY"; break;
            }
            
            headerHolder.weekday.setText(weekday);
            // Use primary color for weekday
            headerHolder.weekday.setTextColor(holder.itemView.getContext().getColor(R.color.primary));
            
        } else {
            TransactionWithAccount item = (TransactionWithAccount) displayItems.get(position);
            ItemViewHolder itemHolder = (ItemViewHolder) holder;
            Transaction transaction = item.transaction;
            Context context = holder.itemView.getContext();
            
            itemHolder.title.setText(transaction.category != null ? transaction.category : "-");
            
            // Subtitle: Account | Note or Date
            String subtitle;
            String accountName = item.account != null ? item.account.name : "Unknown";
            if (transaction.type == TransactionType.TRANSFER) {
                String toAccountName = item.toAccount != null ? item.toAccount.name : "...";
                subtitle = accountName + " → " + toAccountName;
            } else if (transaction.note != null && !transaction.note.isEmpty()) {
                subtitle = accountName + " • " + transaction.note;
            } else {
                subtitle = accountName;
            }
            itemHolder.subTitle.setText(subtitle);

            // Amount with semantic coloring
            int amountColor;
            String amountText;
            String currencySymbol = "BDT".equals(transaction.currency) ? "৳" : "$";
            
            if (transaction.type == TransactionType.EXPENSE) {
                amountColor = context.getColor(R.color.expense);
                amountText = String.format(Locale.getDefault(), "-%s%.2f", currencySymbol, transaction.amount);
            } else if (transaction.type == TransactionType.INCOME) {
                amountColor = context.getColor(R.color.income);
                amountText = String.format(Locale.getDefault(), "+%s%.2f", currencySymbol, transaction.amount);
            } else {
                amountColor = context.getColor(R.color.onSurface);
                amountText = String.format(Locale.getDefault(), "%s%.2f", currencySymbol, transaction.amount);
            }
            
            itemHolder.amount.setText(amountText);
            itemHolder.amount.setTextColor(amountColor);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTransactionClick(item);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return displayItems.size();
    }

    static class HeaderItem {
        long timestamp;
        double income;
        double expense;

        HeaderItem(long timestamp, double income, double expense) {
            this.timestamp = timestamp;
            this.income = income;
            this.expense = expense;
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView date, summary, weekday;
        HeaderViewHolder(View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.headerDate);
            summary = itemView.findViewById(R.id.headerSummary);
            weekday = itemView.findViewById(R.id.headerWeekday);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView title, amount, subTitle;
        android.widget.ImageView icon;
        ItemViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.transactionTitle);
            amount = itemView.findViewById(R.id.transactionAmount);
            subTitle = itemView.findViewById(R.id.transactionSubTitle);
            icon = itemView.findViewById(R.id.categoryIcon);
        }
    }
}
