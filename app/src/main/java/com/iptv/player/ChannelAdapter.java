package com.iptv.player;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ViewHolder> {
    private List<Channel> channels;
    private OnChannelClickListener listener;
    private int selectedPosition = -1;

    public interface OnChannelClickListener {
        void onChannelClick(Channel channel, int position);
    }

    public ChannelAdapter(List<Channel> channels, OnChannelClickListener listener) {
        this.channels = channels;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_channel, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Channel channel = channels.get(position);
        holder.number.setText(String.valueOf(channel.getNumber()));
        holder.name.setText(channel.getName());
        holder.epg.setText(channel.getEpg() != null ? channel.getEpg() : "");

        if (channel.getLogo() != null && !channel.getLogo().isEmpty()) {
            // 使用简单的占位，实际项目中可使用 Glide
            // Glide.with(holder.itemView).load(channel.getLogo()).into(holder.logo);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChannelClick(channel, position);
            }
        });

        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                selectedPosition = position;
                notifyDataSetChanged();
            }
        });

        holder.itemView.setSelected(position == selectedPosition);
    }

    @Override
    public int getItemCount() {
        return channels != null ? channels.size() : 0;
    }

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView number, name, epg;
        ImageView logo;

        ViewHolder(View itemView) {
            super(itemView);
            number = itemView.findViewById(R.id.channel_number);
            name = itemView.findViewById(R.id.channel_name_small);
            epg = itemView.findViewById(R.id.channel_epg_small);
            logo = itemView.findViewById(R.id.channel_logo_small);
        }
    }
}
