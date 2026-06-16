package com.moneybag.nativeapp.data;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.moneybag.nativeapp.R;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarMonthAdapter extends RecyclerView.Adapter<CalendarMonthAdapter.ViewHolder> {

    private final List<TransactionWithAccount> allTransactions = new ArrayList<>();
    private final Calendar baseCalendar = Calendar.getInstance();
    private final SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private final CalendarAdapter.OnDayClickListener onDayClickListener;

    public CalendarMonthAdapter(CalendarAdapter.OnDayClickListener listener) {
        this.onDayClickListener = listener;
        baseCalendar.set(Calendar.DAY_OF_MONTH, 1);
    }

    public void setTransactions(List<TransactionWithAccount> transactions) {
        this.allTransactions.clear();
        this.allTransactions.addAll(transactions);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_month, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Calendar cal = (Calendar) baseCalendar.clone();
        cal.add(Calendar.MONTH, position - 120); // 10 years back, 10 years forward (total 240 months)
        
        holder.monthName.setText(monthFormat.format(cal.getTime()));
        
        CalendarAdapter dayAdapter = new CalendarAdapter();
        dayAdapter.setOnDayClickListener(onDayClickListener);
        holder.dayGrid.setAdapter(dayAdapter);
        
        List<CalendarAdapter.CalendarDay> days = calculateDays(cal);
        dayAdapter.setDays(days);
    }

    @Override
    public int getItemCount() {
        return 240; // 20 years
    }

    private List<CalendarAdapter.CalendarDay> calculateDays(Calendar monthCal) {
        List<CalendarAdapter.CalendarDay> days = new ArrayList<>();
        Calendar cal = (Calendar) monthCal.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        
        for (int i = 0; i < firstDayOfWeek; i++) {
            days.add(null);
        }
        
        Calendar today = Calendar.getInstance();
        double[] dailyIncome = new double[daysInMonth + 1];
        double[] dailyExpense = new double[daysInMonth + 1];

        int currentMonth = cal.get(Calendar.MONTH);
        int currentYear = cal.get(Calendar.YEAR);

        for (TransactionWithAccount t : allTransactions) {
            Calendar tCal = Calendar.getInstance();
            tCal.setTimeInMillis(t.transaction.timestamp);
            if (tCal.get(Calendar.MONTH) == currentMonth && tCal.get(Calendar.YEAR) == currentYear) {
                int day = tCal.get(Calendar.DAY_OF_MONTH);
                if (t.transaction.type == TransactionType.INCOME) {
                    dailyIncome[day] += t.transaction.amount;
                } else if (t.transaction.type == TransactionType.EXPENSE) {
                    dailyExpense[day] += t.transaction.amount;
                }
            }
        }

        for (int day = 1; day <= daysInMonth; day++) {
            boolean isToday = today.get(Calendar.DAY_OF_MONTH) == day && 
                             today.get(Calendar.MONTH) == currentMonth && 
                             today.get(Calendar.YEAR) == currentYear;
            
            cal.set(Calendar.DAY_OF_MONTH, day);
            days.add(new CalendarAdapter.CalendarDay(day, dailyIncome[day], dailyExpense[day], isToday, cal.getTimeInMillis()));
        }
        return days;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView monthName;
        RecyclerView dayGrid;
        ViewHolder(View itemView) {
            super(itemView);
            monthName = itemView.findViewById(R.id.textMonthName);
            dayGrid = itemView.findViewById(R.id.monthDayGrid);
            dayGrid.setLayoutManager(new GridLayoutManager(itemView.getContext(), 7));
            dayGrid.setNestedScrollingEnabled(false);
        }
    }
}
