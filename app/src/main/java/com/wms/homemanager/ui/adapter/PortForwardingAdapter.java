package com.wms.homemanager.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wms.homemanager.databinding.ItemPortForwardingBinding;
import com.wms.homemanager.model.LoginInfo;
import com.wms.homemanager.model.PortForwardingConfig;
import com.wms.homemanager.service.SshService;
import com.wms.homemanager.service.SshForegroundService;
import com.wms.homemanager.utils.LoginInfoUtils;
import com.wms.homemanager.utils.PortForwardingUtils;

import java.util.ArrayList;
import java.util.List;

public class PortForwardingAdapter extends RecyclerView.Adapter<PortForwardingAdapter.ViewHolder> {

    private final Context context;
    private final List<PortForwardingConfig> configList;
    private final LoginInfo currentLoginInfo;
    private final Handler handler;
    private OnConfigActionListener listener;

    public interface OnConfigActionListener {
        void onEdit(PortForwardingConfig config);
        void onDelete(PortForwardingConfig config);
        void onApply(PortForwardingConfig config);
    }

    public PortForwardingAdapter(Context context, OnConfigActionListener listener) {
        this.context = context;
        this.listener = listener;
        this.currentLoginInfo = LoginInfoUtils.getCurrentLoginInfo(context);
        this.configList = new ArrayList<>(PortForwardingUtils.getPortForwardingConfigs(context, currentLoginInfo));
        this.handler = new Handler(Looper.getMainLooper());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPortForwardingBinding binding = ItemPortForwardingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PortForwardingConfig config = configList.get(position);
        holder.bind(config, listener);
    }

    @Override
    public int getItemCount() {
        return configList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemPortForwardingBinding binding;

        ViewHolder(ItemPortForwardingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(PortForwardingConfig config, OnConfigActionListener listener) {
            binding.tvName.setText(config.getName());
            binding.tvSourceIp.setText(config.getSourceIp());
            binding.tvSourcePort.setText(String.valueOf(config.getSourcePort()));
            binding.tvTargetIp.setText(config.getTargetIp());
            binding.tvTargetPort.setText(String.valueOf(config.getTargetPort()));

            // 根据转发类型设置箭头方向
            if (config.getForwardType() == PortForwardingConfig.ForwardType.TARGET_TO_LOCAL) {
                binding.tvArrow.setText("←");
            } else {
                binding.tvArrow.setText("→");
            }

            // 编辑按钮点击事件
            binding.btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEdit(config);
                }
            });

            // 删除按钮点击事件
            binding.btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDelete(config);
                }
            });
        }
    }

    // 添加配置
    public void addConfig(PortForwardingConfig config) {
        configList.add(config);
        saveConfigs();
        notifyItemInserted(configList.size() - 1);
    }

    // 更新配置
    public void updateConfig(PortForwardingConfig oldConfig, PortForwardingConfig newConfig) {
        int position = configList.indexOf(oldConfig);
        if (position != -1) {
            configList.set(position, newConfig);
            saveConfigs();
            notifyItemChanged(position);
        }
    }

    // 删除配置
    public void deleteConfig(PortForwardingConfig config) {
        int position = configList.indexOf(config);
        if (position != -1) {
            configList.remove(position);
            saveConfigs();
            notifyItemRemoved(position);
            Toast.makeText(context, "配置已删除", Toast.LENGTH_SHORT).show();
        }
    }

    // 应用单个配置
    public void applyConfig(PortForwardingConfig config) {
        if (currentLoginInfo == null) {
            Toast.makeText(context, "请先登录SSH服务器", Toast.LENGTH_SHORT).show();
            return;
        }

        SshService sshService = SshService.getInstance(context);
        List<PortForwardingConfig> singleConfig = new ArrayList<>();
        singleConfig.add(config);
        
        sshService.applyPortForwardingConfigs(currentLoginInfo, singleConfig, new SshService.ExecuteCallback() {
            @Override
            public void onSuccess(String result) {
                handler.post(() -> Toast.makeText(context, "端口转发配置已生效", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onFailure(String error) {
                handler.post(() -> Toast.makeText(context, "端口转发生效失败: " + error, Toast.LENGTH_LONG).show());
            }

            @Override
            public void onOutput(String output) {
                System.out.println("SSH命令输出: " + output);
            }
        });
    }

    // 保存配置到持久化存储
    private void saveConfigs() {
        PortForwardingUtils.savePortForwardingConfigs(context, currentLoginInfo, configList);
    }

    // 生效所有配置
    public void applyAllConfigs() {
        if (currentLoginInfo == null) {
            Toast.makeText(context, "请先登录SSH服务器", Toast.LENGTH_SHORT).show();
            return;
        }

        // 启动前台服务，确保即使在后台也能保持SSH会话活跃
        Intent serviceIntent = new Intent(context, SshForegroundService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        // 直接调用SshService应用所有配置
        SshService sshService = SshService.getInstance(context);
        sshService.applyPortForwardingConfigs(currentLoginInfo, configList, new SshService.ExecuteCallback() {
            @Override
            public void onSuccess(String result) {
                handler.post(() -> Toast.makeText(context, "所有端口转发配置已生效", Toast.LENGTH_LONG).show());
            }

            @Override
            public void onFailure(String error) {
                handler.post(() -> Toast.makeText(context, "端口转发生效失败: " + error, Toast.LENGTH_LONG).show());
            }

            @Override
            public void onOutput(String output) {
                System.out.println("SSH命令输出: " + output);
            }
        });
    }
}
