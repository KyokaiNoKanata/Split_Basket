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

public class StatusLogAdapter extends ListAdapter<EventLogManager.LogEntry, StatusLogAdapter.LogViewHolder> {

    private final Context context;

    private static final DiffUtil.ItemCallback<EventLogManager.LogEntry> DIFF_CALLBACK = new DiffUtil.ItemCallback<EventLogManager.LogEntry>() {
        @Override
        public boolean areItemsTheSame(@NonNull EventLogManager.LogEntry oldItem, @NonNull EventLogManager.LogEntry newItem) {
            return oldItem.getTimestamp() == newItem.getTimestamp()
                    && oldItem.getActionType().equals(newItem.getActionType())
                    && oldItem.getItemName().equals(newItem.getItemName());
        }

        @Override
        public boolean areContentsTheSame(@NonNull EventLogManager.LogEntry oldItem, @NonNull EventLogManager.LogEntry newItem) {
            return oldItem.getDescription().equals(newItem.getDescription())
                    && oldItem.getTimestamp() == newItem.getTimestamp();
        }
    };

    public StatusLogAdapter(@NonNull Context context) {
        super(DIFF_CALLBACK);
        this.context = context;
    }

    @Nullable
    public EventLogManager.LogEntry getItemAt(int position) {
        if (position < 0 || position >= getItemCount()) return null;
        return getItem(position);
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        EventLogManager.LogEntry logEntry = getItem(position);
        holder.bind(logEntry);
    }

    class LogViewHolder extends RecyclerView.ViewHolder {
        private final TextView textLogContent;
        private final TextView textLogTime;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            textLogContent = itemView.findViewById(R.id.textLogContent);
            textLogTime = itemView.findViewById(R.id.textLogTime);
        }

        void bind(EventLogManager.LogEntry logEntry) {
            // 使用LogEntry的description直接展示
            textLogContent.setText(logEntry.getDescription());
            // 格式化时间
            String formattedTime = EventLogManager.formatTimeAgo(logEntry.getTimestamp());
            textLogTime.setText(formattedTime);
        }
    }
}