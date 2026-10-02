package com.example.aks;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.*;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {

    private Context context;
    private List<HashMap<String, String>> groupList;
    private List<HashMap<String, String>> originalList;
    private OnGroupClickListener listener;

    public interface OnGroupClickListener {
        void onGroupClick(HashMap<String, String> group);
    }

    public GroupAdapter(Context context, List<HashMap<String, String>> groupList, OnGroupClickListener listener) {
        this.context = context;
        this.groupList = new ArrayList<>(groupList);
        this.originalList = new ArrayList<>(groupList);
        this.listener = listener;
    }

    public void updateData(List<HashMap<String, String>> updatedList) {
        this.groupList.clear();
        this.originalList.clear();
        this.groupList.addAll(updatedList);
        this.originalList.addAll(updatedList);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        groupList.clear();
        if (query.isEmpty()) {
            groupList.addAll(originalList);
        } else {
            query = query.toLowerCase();
            for (HashMap<String, String> group : originalList) {
                String groupName = group.get("groupName");
                if (groupName != null && groupName.toLowerCase().contains(query)) {
                    groupList.add(group);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        HashMap<String, String> group = groupList.get(position);
        String groupName = group.get("groupName");

        holder.textGroupName.setText(groupName);

        // Initials
        if (groupName != null) {
            String[] words = groupName.split( " ");
            String initials = "";
            for (int i = 0; i < words.length && i < 2; i++) {
                if (!words[i].isEmpty()) {
                    initials += Character.toUpperCase(words[i].charAt(0));
                }
            }
            holder.groupInitials.setText(initials);
        }

        // Arrow click
        holder.iconArrow.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGroupClick(group);
            }
        });

        // Random color from a fixed set
        int[] colors = {
                Color.parseColor("#FF6F61"),
                Color.parseColor("#6A5ACD"),
                Color.parseColor("#00BFA6"),
                Color.parseColor("#FF9800"),
                Color.parseColor("#009688"),
                Color.parseColor("#9C27B0")
        };

        int color = colors[position % colors.length]; // or random

        GradientDrawable bgShape = new GradientDrawable();
        bgShape.setShape(GradientDrawable.OVAL);
        bgShape.setColor(color);

        holder.groupInitials.setBackground(bgShape);
    }


    @Override
    public int getItemCount() {
        return groupList.size();
    }

    public static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView textGroupName, groupInitials;
        ImageView iconArrow;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            textGroupName = itemView.findViewById(R.id.textGroupName);
            groupInitials = itemView.findViewById(R.id.groupInitials);
            iconArrow = itemView.findViewById(R.id.iconArrow);
        }
    }
}
