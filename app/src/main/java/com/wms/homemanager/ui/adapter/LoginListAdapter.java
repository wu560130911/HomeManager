package com.wms.homemanager.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wms.homemanager.databinding.ItemLoginInfoBinding;
import com.wms.homemanager.model.LoginInfo;

import java.util.List;

public class LoginListAdapter extends RecyclerView.Adapter<LoginListAdapter.ViewHolder> {

    private final List<LoginInfo> loginInfoList;
    private final OnLoginInfoClickListener listener;
    private boolean enabled = true;

    public interface OnLoginInfoClickListener {
        void onLoginInfoClick(LoginInfo loginInfo);
        void onLoginInfoEdit(LoginInfo loginInfo);
        void onLoginInfoDelete(LoginInfo loginInfo);
    }

    public LoginListAdapter(Context context, List<LoginInfo> loginInfoList, OnLoginInfoClickListener listener) {
        this.loginInfoList = loginInfoList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLoginInfoBinding binding = ItemLoginInfoBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LoginInfo loginInfo = loginInfoList.get(position);
        holder.bind(loginInfo, listener, enabled);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public int getItemCount() {
        return loginInfoList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemLoginInfoBinding binding;

        ViewHolder(ItemLoginInfoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(LoginInfo loginInfo, OnLoginInfoClickListener listener, boolean enabled) {
            binding.tvHost.setText(loginInfo.toString());

            binding.getRoot().setOnClickListener(v -> {
                if (enabled && listener != null) {
                    listener.onLoginInfoClick(loginInfo);
                }
            });

            binding.tvEdit.setOnClickListener(v -> {
                if (enabled && listener != null) {
                    listener.onLoginInfoEdit(loginInfo);
                }
            });

            binding.tvDelete.setOnClickListener(v -> {
                if (enabled && listener != null) {
                    listener.onLoginInfoDelete(loginInfo);
                }
            });
        }
    }
}
