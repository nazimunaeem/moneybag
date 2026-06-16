package com.moneybag.nativeapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.moneybag.nativeapp.data.Category;
import com.moneybag.nativeapp.data.MoneyBagRepository;
import com.moneybag.nativeapp.data.SimpleGridAdapter;
import com.moneybag.nativeapp.data.TransactionType;
import com.moneybag.nativeapp.databinding.FragmentCategoriesBinding;
import com.moneybag.nativeapp.databinding.DialogAddCategoryBinding;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CategoriesFragment extends Fragment {
    private FragmentCategoriesBinding binding;
    private MoneyBagRepository repository;
    private CategoryAdapter incomeAdapter;
    private CategoryAdapter expenseAdapter;
    private TransactionType selectedType = TransactionType.EXPENSE;

    private static final String[] INCOME_EMOJIS = {"💰", "🧧", "🎁", "📈", "🏦", "💵", "💎", "💹", "🎰", "🏆"};
    private static final String[] EXPENSE_EMOJIS = {"🍔", "🚗", "🛍️", "🏠", "📑", "🎮", "🍿", "🏥", "⛽", "🎓", "✈️", "👗", "🔌", "🧴"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCategoriesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new MoneyBagRepository(requireActivity().getApplication());
        
        incomeAdapter = createAdapter();
        expenseAdapter = createAdapter();
        
        binding.incomeRecyclerView.setAdapter(incomeAdapter);
        binding.expenseRecyclerView.setAdapter(expenseAdapter);
        
        setupDragDrop(binding.incomeRecyclerView, incomeAdapter);
        setupDragDrop(binding.expenseRecyclerView, expenseAdapter);

        binding.fabAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        loadCategories();
    }

    private void setupDragDrop(RecyclerView rv, CategoryAdapter adapter) {
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPos = viewHolder.getAdapterPosition();
                int toPos = target.getAdapterPosition();
                adapter.moveItem(fromPos, toPos);
                saveOrder(adapter.categories);
                return true;
            }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
        });
        helper.attachToRecyclerView(rv);
    }

    private void saveOrder(List<Category> list) {
        for (int i = 0; i < list.size(); i++) {
            list.get(i).position = i;
            repository.updateCategory(list.get(i));
        }
    }

    private CategoryAdapter createAdapter() {
        return new CategoryAdapter(category -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.delete))
                    .setMessage(getString(R.string.confirm_delete_category, category.name))
                    .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                        repository.deleteCategory(category);
                        loadCategories();
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        });
    }

    private void loadCategories() {
        repository.getAllCategories(categories -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    List<Category> income = categories.stream()
                            .filter(c -> c.type == TransactionType.INCOME)
                            .sorted((c1, c2) -> Integer.compare(c1.position, c2.position))
                            .collect(Collectors.toList());
                    List<Category> expense = categories.stream()
                            .filter(c -> c.type == TransactionType.EXPENSE)
                            .sorted((c1, c2) -> Integer.compare(c1.position, c2.position))
                            .collect(Collectors.toList());
                    
                    incomeAdapter.setCategories(income);
                    expenseAdapter.setCategories(expense);
                });
            }
        });
    }

    private void showAddCategoryDialog() {
        DialogAddCategoryBinding dialogBinding = DialogAddCategoryBinding.inflate(getLayoutInflater());
        selectedType = TransactionType.EXPENSE;

        // Emoji Grid
        SimpleGridAdapter<String> emojiAdapter = new SimpleGridAdapter<>(
                e -> e,
                emoji -> dialogBinding.editCategoryEmoji.setText(emoji)
        );
        dialogBinding.emojiGridRecyclerView.setAdapter(emojiAdapter);
        emojiAdapter.setItems(Arrays.asList(EXPENSE_EMOJIS));

        // Type Grid
        SimpleGridAdapter<TransactionType> typeAdapter = new SimpleGridAdapter<>(
                TransactionType::name,
                type -> {
                    selectedType = type;
                    emojiAdapter.setItems(Arrays.asList(type == TransactionType.INCOME ? INCOME_EMOJIS : EXPENSE_EMOJIS));
                }
        );
        dialogBinding.typeGridRecyclerView.setAdapter(typeAdapter);
        List<TransactionType> types = Arrays.asList(TransactionType.INCOME, TransactionType.EXPENSE, TransactionType.TRANSFER);
        typeAdapter.setItems(types);

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.add))
                .setView(dialogBinding.getRoot())
                .setPositiveButton(getString(R.string.add), (dialog, which) -> {
                    String emoji = dialogBinding.editCategoryEmoji.getText() != null ? dialogBinding.editCategoryEmoji.getText().toString() : "";
                    String name = dialogBinding.editCategoryName.getText() != null ? dialogBinding.editCategoryName.getText().toString() : "";
                    if (!name.isEmpty()) {
                        String fullName = emoji.isEmpty() ? name : emoji + " " + name;
                        repository.insertCategory(new Category(fullName, selectedType));
                        loadCategories();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private static class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
        List<Category> categories = new ArrayList<>();
        private OnCategoryClickListener listener;

        interface OnCategoryClickListener {
            void onCategoryClick(Category category);
        }

        CategoryAdapter(OnCategoryClickListener listener) {
            this.listener = listener;
        }

        void setCategories(List<Category> categories) {
            this.categories = categories;
            notifyDataSetChanged();
        }

        void moveItem(int from, int to) {
            Category item = categories.remove(from);
            categories.add(to, item);
            notifyItemMoved(from, to);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_config, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Category category = categories.get(position);
            holder.text.setText(category.name);
            holder.itemView.setOnClickListener(v -> listener.onCategoryClick(category));
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            android.widget.TextView text;
            ViewHolder(View itemView) {
                super(itemView);
                text = itemView.findViewById(R.id.categoryName);
            }
        }
    }
}
