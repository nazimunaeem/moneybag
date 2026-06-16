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

public class SimpleGridAdapter<T> extends RecyclerView.Adapter<SimpleGridAdapter.ViewHolder> {
    private List<T> items = new ArrayList<>();
    private int selectedPosition = -1;
    private OnItemClickListener<T> listener;
    private ItemFormatter<T> formatter;

    public interface OnItemClickListener<T> {
        void onItemClick(T item);
    }

    public interface ItemFormatter<T> {
        String getName(T item);
    }

    public SimpleGridAdapter(ItemFormatter<T> formatter, OnItemClickListener<T> listener) {
        this.formatter = formatter;
        this.listener = listener;
    }

    public void setItems(List<T> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_grid_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        T item = items.get(position);
        holder.text.setText(formatter.getName(item));
        
        holder.itemView.setSelected(selectedPosition == position);
        
        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        ViewHolder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.selectionButton);
        }
    }
}
