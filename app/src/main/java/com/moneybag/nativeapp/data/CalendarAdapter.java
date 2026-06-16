package com.moneybag.nativeapp.data;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.moneybag.nativeapp.R;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {
    private List<CalendarDay> days = new ArrayList<>();
    private OnDayClickListener listener;

    public interface OnDayClickListener {
        void onDayClick(CalendarDay day);
    }

    public void setOnDayClickListener(OnDayClickListener listener) {
        this.listener = listener;
    }

    public void setDays(List<CalendarDay> days) {
        this.days = days;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CalendarDay day = days.get(position);
        if (day == null) {
            holder.dayNumber.setText("");
            holder.income.setVisibility(View.INVISIBLE);
            holder.expense.setVisibility(View.INVISIBLE);
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
            holder.itemView.setOnClickListener(null);
        } else {
            holder.dayNumber.setText(String.valueOf(day.dayOfMonth));
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onDayClick(day);
            });
            if (day.income > 0) {
                holder.income.setVisibility(View.VISIBLE);
                holder.income.setText(String.format(Locale.getDefault(), "+%.0f", day.income));
            } else {
                holder.income.setVisibility(View.INVISIBLE);
            }

            if (day.expense > 0) {
                holder.expense.setVisibility(View.VISIBLE);
                holder.expense.setText(String.format(Locale.getDefault(), "-%.0f", day.expense));
            } else {
                holder.expense.setVisibility(View.INVISIBLE);
            }
            
            if (day.isToday) {
                holder.itemView.setBackgroundResource(android.R.drawable.editbox_dropdown_light_frame);
            } else {
                holder.itemView.setBackgroundColor(Color.WHITE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    public static class CalendarDay {
        public int dayOfMonth;
        public double income;
        public double expense;
        public boolean isToday;
        public long timestamp;

        public CalendarDay(int dayOfMonth, double income, double expense, boolean isToday, long timestamp) {
            this.dayOfMonth = dayOfMonth;
            this.income = income;
            this.expense = expense;
            this.isToday = isToday;
            this.timestamp = timestamp;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dayNumber, income, expense;
        ViewHolder(View itemView) {
            super(itemView);
            dayNumber = itemView.findViewById(R.id.textDayNumber);
            income = itemView.findViewById(R.id.textDayIncome);
            expense = itemView.findViewById(R.id.textDayExpense);
        }
    }
}
