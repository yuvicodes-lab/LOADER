package com.arcx.adapter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arcx.R;
import com.mundo.MundoCore;

import java.util.List;

public class GamePagerAdapter extends RecyclerView.Adapter<GamePagerAdapter.GameViewHolder> {

    public static class GameItem {
        public String title;
        public String packageName;

        public GameItem(String title, String packageName) {
            this.title = title;
            this.packageName = packageName;
        }
    }

    public interface OnGameActionListener {
        void onGameAction(GameItem item, boolean isInstalled);
    }

    private final Context context;
    private final List<GameItem> gameList;
    private final OnGameActionListener listener;
    private static final int USER_ID = 0;

    public GamePagerAdapter(Context context, List<GameItem> gameList, OnGameActionListener listener) {
        this.context = context;
        this.gameList = gameList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_game_card, parent, false);
        return new GameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        GameItem item = gameList.get(position);
        holder.titleText.setText(item.title);
        holder.pkgText.setText(item.packageName);

        // Try to fetch real icon from device package manager
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(item.packageName, 0);
            Drawable icon = pm.getApplicationIcon(ai);
            holder.iconView.setImageDrawable(icon);
        } catch (PackageManager.NameNotFoundException e) {
            holder.iconView.setImageResource(R.drawable.ic_game_premium);
        }

        // Check virtual installation state using com.mundo.MundoCore
        boolean isInstalled = false;
        try {
            if (MundoCore.get() != null) {
                isInstalled = MundoCore.get().isInstalled(item.packageName, USER_ID);
            }
        } catch (Throwable ignored) {}

        if (isInstalled) {
            holder.actionButton.setText("LAUNCH GAME");
            holder.actionButton.setBackgroundResource(R.drawable.bg_clay_button);
        } else {
            holder.actionButton.setText("INSTALL GAME");
            holder.actionButton.setBackgroundResource(R.drawable.bg_clay_secondary);
        }

        final boolean finalInstalled = isInstalled;
        holder.actionButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGameAction(item, finalInstalled);
            }
        });
    }

    @Override
    public int getItemCount() {
        return gameList != null ? gameList.size() : 0;
    }

    static class GameViewHolder extends RecyclerView.ViewHolder {
        ImageView iconView;
        TextView titleText;
        TextView pkgText;
        Button actionButton;

        GameViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.cardGameIcon);
            titleText = itemView.findViewById(R.id.cardGameTitle);
            pkgText = itemView.findViewById(R.id.cardGamePkg);
            actionButton = itemView.findViewById(R.id.cardActionButton);
        }
    }
}