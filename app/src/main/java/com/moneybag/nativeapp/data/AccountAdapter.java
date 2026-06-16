package com.moneybag.nativeapp.data;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.moneybag.nativeapp.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.ViewHolder> {
    private List<Account> accounts = new ArrayList<>();
    private OnAccountClickListener listener;

    public interface OnAccountClickListener {
        void onAccountClick(Account account);
    }

    public void setOnAccountClickListener(OnAccountClickListener listener) {
        this.listener = listener;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_account, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Account account = accounts.get(position);
        holder.name.setText(account.name);
        holder.type.setText(account.type.name());
        
        String symbol = "USD".equals(account.currency) ? "$" : "৳";
        holder.balance.setText(String.format(Locale.getDefault(), "%s%.2f", symbol, account.balance));

        if (account.iconUri != null && !account.iconUri.isEmpty()) {
            try {
                com.bumptech.glide.Glide.with(holder.itemView.getContext())
                        .load(android.net.Uri.parse(account.iconUri))
                        .placeholder(R.drawable.ic_nav_accounts)
                        .error(R.drawable.ic_nav_accounts)
                        .into(holder.icon);
            } catch (Exception e) {
                holder.icon.setImageResource(R.drawable.ic_nav_accounts);
            }
        } else {
            holder.icon.setImageResource(R.drawable.ic_nav_accounts);
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAccountClick(account);
            }
        });
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, type, balance;
        android.widget.ImageView icon;
        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.accountName);
            type = itemView.findViewById(R.id.accountType);
            balance = itemView.findViewById(R.id.accountBalance);
            icon = itemView.findViewById(R.id.accountIcon);
        }
    }
}
