package com.wms.homemanager.ui.adapter;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.wms.homemanager.R;
import com.wms.homemanager.model.PortForwardingConfig;

public class PortForwardingDialog extends Dialog {

    private EditText etName;
    private EditText etSourceIp;
    private EditText etSourcePort;
    private EditText etTargetIp;
    private EditText etTargetPort;
    private RadioGroup rgForwardType;
    private RadioButton rbTargetToLocal;
    private RadioButton rbLocalToTarget;
    private Button btnSave;
    private Button btnCancel;

    private PortForwardingConfig config;
    private PortForwardingAdapter adapter;

    public PortForwardingDialog(Context context, PortForwardingConfig config, PortForwardingAdapter adapter) {
        super(context);
        this.config = config;
        this.adapter = adapter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_port_forwarding);

        // 设置对话框样式
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        getWindow().setLayout(
                (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.95),
                (int) (getContext().getResources().getDisplayMetrics().heightPixels * 0.85)
        );

        etName = findViewById(R.id.et_name);
        etSourceIp = findViewById(R.id.et_source_ip);
        etSourcePort = findViewById(R.id.et_source_port);
        etTargetIp = findViewById(R.id.et_target_ip);
        etTargetPort = findViewById(R.id.et_target_port);
        rgForwardType = findViewById(R.id.rg_forward_type);
        rbTargetToLocal = findViewById(R.id.rb_target_to_local);
        rbLocalToTarget = findViewById(R.id.rb_local_to_target);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);

        // 设置对话框标题
        setTitle(config == null ? "添加端口转发配置" : "编辑端口转发配置");
        //getWindow().setTitleColor(getContext().getResources().getColor(android.R.color.black));

        // 如果是编辑模式，填充现有配置
        if (config != null) {
            etName.setText(config.getName());
            etSourceIp.setText(config.getSourceIp());
            etSourcePort.setText(String.valueOf(config.getSourcePort()));
            etTargetIp.setText(config.getTargetIp());
            etTargetPort.setText(String.valueOf(config.getTargetPort()));
            if (config.getForwardType() == PortForwardingConfig.ForwardType.TARGET_TO_LOCAL) {
                rbTargetToLocal.setChecked(true);
            } else {
                rbLocalToTarget.setChecked(true);
            }
        } else {
            // 默认选择目标转发到本地
            rbTargetToLocal.setChecked(true);
            // 默认源IP为127.0.0.1
            etSourceIp.setText(R.string._127_0_0_1);
            etTargetIp.setText(R.string._127_0_0_1);
        }

        // 转发类型选择监听
        rgForwardType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_target_to_local) {
                // 目标转发到本地时，源IP默认为127.0.0.1
                etSourceIp.setText("127.0.0.1");
                etSourceIp.setEnabled(false);
            } else {
                // 本地转发到目标时，源IP可编辑
                etSourceIp.setEnabled(true);
            }
        });

        // 保存按钮点击事件
        btnSave.setOnClickListener(v -> saveConfig());

        // 取消按钮点击事件
        btnCancel.setOnClickListener(v -> dismiss());
    }

    // 保存配置
    private void saveConfig() {
        String name = etName.getText().toString().trim();
        String sourceIp = etSourceIp.getText().toString().trim();
        String sourcePortStr = etSourcePort.getText().toString().trim();
        String targetIp = etTargetIp.getText().toString().trim();
        String targetPortStr = etTargetPort.getText().toString().trim();

        // 验证输入
        if (name.isEmpty()) {
            Toast.makeText(getContext(), "请输入配置名称", Toast.LENGTH_SHORT).show();
            return;
        }

        if (sourceIp.isEmpty()) {
            Toast.makeText(getContext(), "请输入源IP", Toast.LENGTH_SHORT).show();
            return;
        }

        if (sourcePortStr.isEmpty()) {
            Toast.makeText(getContext(), "请输入源端口", Toast.LENGTH_SHORT).show();
            return;
        }

        if (targetIp.isEmpty()) {
            Toast.makeText(getContext(), "请输入目标IP", Toast.LENGTH_SHORT).show();
            return;
        }

        if (targetPortStr.isEmpty()) {
            Toast.makeText(getContext(), "请输入目标端口", Toast.LENGTH_SHORT).show();
            return;
        }

        int sourcePort;
        int targetPort;

        try {
            sourcePort = Integer.parseInt(sourcePortStr);
            targetPort = Integer.parseInt(targetPortStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "端口号必须为数字", Toast.LENGTH_SHORT).show();
            return;
        }

        // 验证端口范围
        if (sourcePort < 1 || sourcePort > 65535) {
            Toast.makeText(getContext(), "源端口必须在1-65535之间", Toast.LENGTH_SHORT).show();
            return;
        }

        if (targetPort < 1 || targetPort > 65535) {
            Toast.makeText(getContext(), "目标端口必须在1-65535之间", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取转发类型
        PortForwardingConfig.ForwardType forwardType;
        if (rbTargetToLocal.isChecked()) {
            forwardType = PortForwardingConfig.ForwardType.TARGET_TO_LOCAL;
        } else {
            forwardType = PortForwardingConfig.ForwardType.LOCAL_TO_TARGET;
        }

        // 创建或更新配置
        PortForwardingConfig newConfig = new PortForwardingConfig(
                name,
                sourceIp,
                sourcePort,
                targetIp,
                targetPort,
                forwardType
        );

        if (config == null) {
            // 添加新配置
            adapter.addConfig(newConfig);
            Toast.makeText(getContext(), "配置已添加", Toast.LENGTH_SHORT).show();
        } else {
            // 更新现有配置
            adapter.updateConfig(config, newConfig);
            Toast.makeText(getContext(), "配置已更新", Toast.LENGTH_SHORT).show();
        }

        dismiss();
    }
}
