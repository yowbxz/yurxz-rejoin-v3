package com.yurxz.rejoin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class InstanceAdapter extends RecyclerView.Adapter<InstanceAdapter.VH> {

    public interface Listener {
        void onDelete(int pos);
        void onManualRejoin(int pos);
        void onClone(int pos);
    }

    private final List<RobloxInstance> items;
    private final Listener listener;

    public InstanceAdapter(List<RobloxInstance> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_instance, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        RobloxInstance inst = items.get(pos);
        h.tvName.setText(inst.name);
        h.tvPackage.setText(inst.packageName);
        h.tvPsLink.setText(inst.psLink != null ? inst.psLink : "-");

        // Status color
        String status = inst.status != null ? inst.status : "Idle";
        h.tvStatus.setText(status);
        int color;
        if (status.contains("Running") || status.contains("✅"))
            color = 0xFF22C55E;
        else if (status.contains("Frozen") || status.contains("Rejoining") || status.contains("🔄"))
            color = 0xFFEAB308;
        else if (status.contains("❌") || status.contains("Error"))
            color = 0xFFEF4444;
        else
            color = 0xFF6B6888;
        h.tvStatus.setTextColor(color);

        // Avatar initial
        String initial = inst.name != null && !inst.name.isEmpty()
            ? String.valueOf(inst.name.charAt(0)).toUpperCase() : "?";
        h.tvAvatar.setText(initial);

        h.btnDelete.setOnClickListener(v -> listener.onDelete(h.getAdapterPosition()));
        h.btnManualRejoin.setOnClickListener(v -> listener.onManualRejoin(h.getAdapterPosition()));
        h.btnClone.setOnClickListener(v -> listener.onClone(h.getAdapterPosition()));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvPackage, tvPsLink, tvStatus;
        ImageButton btnDelete;
        MaterialButton btnManualRejoin, btnClone;

        VH(View v) {
            super(v);
            tvAvatar = v.findViewById(R.id.tvAvatar);
            tvName = v.findViewById(R.id.tvName);
            tvPackage = v.findViewById(R.id.tvPackage);
            tvPsLink = v.findViewById(R.id.tvPsLink);
            tvStatus = v.findViewById(R.id.tvStatus);
            btnDelete = v.findViewById(R.id.btnDelete);
            btnManualRejoin = v.findViewById(R.id.btnManualRejoin);
            btnClone = v.findViewById(R.id.btnClone);
        }
    }
}
