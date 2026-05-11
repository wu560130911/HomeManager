package com.wms.homemanager.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.wms.homemanager.databinding.FragmentRemoteBinding;
import com.wms.homemanager.model.PortForwardingConfig;
import com.wms.homemanager.ui.adapter.PortForwardingAdapter;
import com.wms.homemanager.ui.adapter.PortForwardingDialog;

public class RemoteFragment extends Fragment implements PortForwardingAdapter.OnConfigActionListener {

    private FragmentRemoteBinding binding;
    private PortForwardingAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRemoteBinding.inflate(inflater, container, false);

        // 初始化RecyclerView
        initRecyclerView();

        // 添加配置按钮点击事件
        binding.btnAddConfig.setOnClickListener(v -> showAddConfigDialog());

        // 生效所有配置按钮点击事件
        binding.btnApplyAll.setOnClickListener(v -> applyAllConfigs());

        return binding.getRoot();
    }

    private void initRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.rvPortForwarding.setLayoutManager(layoutManager);
        adapter = new PortForwardingAdapter(requireContext(), this);
        binding.rvPortForwarding.setAdapter(adapter);
    }

    // 生效所有配置
    private void applyAllConfigs() {
        if (adapter != null) {
            adapter.applyAllConfigs();
        }
    }

    // 显示添加配置对话框
    private void showAddConfigDialog() {
        PortForwardingDialog dialog = new PortForwardingDialog(requireContext(), null, adapter);
        dialog.show();
    }

    @Override
    public void onEdit(PortForwardingConfig config) {
        showEditConfigDialog(config);
    }

    @Override
    public void onDelete(PortForwardingConfig config) {
        if (adapter != null) {
            adapter.deleteConfig(config);
        }
    }

    @Override
    public void onApply(PortForwardingConfig config) {
        if (adapter != null) {
            adapter.applyConfig(config);
        }
    }

    private void showEditConfigDialog(PortForwardingConfig config) {
        PortForwardingDialog dialog = new PortForwardingDialog(requireContext(), config, adapter);
        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
