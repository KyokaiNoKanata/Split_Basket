package com.example.split_basket;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;

public class ShoppingListAdapter extends ListAdapter<ShoppingItem, ShoppingListAdapter.ItemViewHolder> {

    private static final DiffUtil.ItemCallback<ShoppingItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<ShoppingItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull ShoppingItem oldItem, @NonNull ShoppingItem newItem) {
            if (oldItem.getId() != 0 && newItem.getId() != 0) {
                return oldItem.getId() == newItem.getId();
            }
            return oldItem.getCreatedAt() == newItem.getCreatedAt()
                    && oldItem.getName().equals(newItem.getName());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ShoppingItem oldItem, @NonNull ShoppingItem newItem) {
            return oldItem.isPurchased() == newItem.isPurchased()
                    && oldItem.getQuantity() == newItem.getQuantity()
                    && oldItem.getName().equals(newItem.getName())
                    && oldItem.getAddedBy().equals(newItem.getAddedBy());
        }
    };
    private final ItemInteractionListener listener;
    private final Context context;

    public ShoppingListAdapter(@NonNull Context context, @NonNull ItemInteractionListener listener) {
        super(DIFF_CALLBACK);
        this.context = context.getApplicationContext();
        this.listener = listener;
        setHasStableIds(true);
    }

    @Nullable
    public ShoppingItem getItemAt(int position) {
        if (position < 0 || position >= getItemCount()) return null;
        return getItem(position);
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shopping, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        ShoppingItem item = getItem(position);
        holder.bind(item);
    }

    @Override
    public long getItemId(int position) {
        ShoppingItem item = getItem(position);
        return item != null ? item.getId() : RecyclerView.NO_ID;
    }

    public interface ItemInteractionListener {
        void onItemChecked(@NonNull ShoppingItem item, boolean isChecked, int position);

        void onItemLongPressed(@NonNull ShoppingItem item, int position);
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCheckBox checkPurchased;
        private final TextView textName;
        private final TextView textAddedBy;
        private final TextView textQuantity;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            checkPurchased = itemView.findViewById(R.id.checkPurchased);
            textName = itemView.findViewById(R.id.textItemName);
            textAddedBy = itemView.findViewById(R.id.textAddedBy);
            textQuantity = itemView.findViewById(R.id.textQuantity);
        }

        void bind(ShoppingItem item) {
            textName.setText(item.getName());
            textAddedBy.setText(context.getString(R.string.added_by_template, item.getAddedBy()));
            textQuantity.setText(String.valueOf(item.getQuantity()));

            checkPurchased.setOnCheckedChangeListener(null);
            checkPurchased.setChecked(item.isPurchased());
            checkPurchased.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (getBindingAdapterPosition() == RecyclerView.NO_POSITION) return;
                listener.onItemChecked(item, isChecked, getBindingAdapterPosition());
            });

            itemView.setOnLongClickListener(v -> {
                if (getBindingAdapterPosition() == RecyclerView.NO_POSITION) return false;
                listener.onItemLongPressed(item, getBindingAdapterPosition());
                return true;
            });
        }
    }
}
