package com.example.aks;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private final ArrayList<String> members;
    private final Map<String, Integer> memberColorMap;
    private final Random random = new Random();

    public MemberAdapter(ArrayList<String> members) {
        this.members = members;
        this.memberColorMap = new HashMap<>();
    }

    @Override
    public MemberViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MemberViewHolder holder, int position) {
        String memberInfo = members.get(position);

        if (memberInfo != null && !memberInfo.isEmpty()) {
            String name = memberInfo;
            String role = "";

            if (memberInfo.contains("(") && memberInfo.contains(")")) {
                int start = memberInfo.indexOf('(');
                int end = memberInfo.indexOf(')');
                name = memberInfo.substring(0, start).trim();
                role = memberInfo.substring(start + 1, end).trim();
            }

            holder.memberName.setText(name);
            holder.memberRole.setText(role.isEmpty() ? "Member" : role);

            // Set initial
            char initial = name.charAt(0);
            holder.memberInitial.setText(String.valueOf(initial).toUpperCase());

            // Assign random color — each person gets fixed color
            int color;
            if (memberColorMap.containsKey(name)) {
                color = memberColorMap.get(name);
            } else {
                color = generateRandomColor();
                memberColorMap.put(name, color);
            }

            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(color);
            holder.memberInitial.setBackground(drawable);
        }
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    private int generateRandomColor() {
        // Colors between 100-255 (not too light or too dark)
        int red = random.nextInt(156) + 100;
        int green = random.nextInt(156) + 100;
        int blue = random.nextInt(156) + 100;
        return Color.rgb(red, green, blue);
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView memberName, memberRole, memberInitial;

        public MemberViewHolder(View itemView) {
            super(itemView);
            memberName = itemView.findViewById(R.id.textMemberName);
            memberRole = itemView.findViewById(R.id.textMemberRole);
            memberInitial = itemView.findViewById(R.id.textMemberInitial);
        }
    }
}
