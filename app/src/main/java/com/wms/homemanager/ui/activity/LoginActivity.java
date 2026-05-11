package com.wms.homemanager.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.wms.homemanager.R;
import com.wms.homemanager.databinding.ActivityLoginBinding;
import com.wms.homemanager.model.LoginInfo;
import com.wms.homemanager.service.SshService;
import com.wms.homemanager.ui.adapter.LoginListAdapter;
import com.wms.homemanager.utils.FileUtils;
import com.wms.homemanager.utils.LoginInfoUtils;

import java.util.List;

public class LoginActivity extends AppCompatActivity implements LoginListAdapter.OnLoginInfoClickListener {

    private ActivityLoginBinding binding;
    private LoginListAdapter loginListAdapter;
    private List<LoginInfo> loginInfoList;
    private LoginInfo currentLoginInfo;
    private static final int FILE_SELECT_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 加载登录信息
        loadLoginInfoList();

        // 初始化RecyclerView
        initRecyclerView();

        // 设置点击事件
        setClickListeners();

        // 如果登录信息列表为空，直接显示登录表单
        if (loginInfoList.isEmpty()) {
            showLoginForm(null, false);
        }
    }

    private void initRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.rvLoginList.setLayoutManager(layoutManager);
        loginListAdapter = new LoginListAdapter(this, loginInfoList, this);
        binding.rvLoginList.setAdapter(loginListAdapter);
    }

    private void setClickListeners() {
        binding.btnSelectCert.setOnClickListener(v -> showFileChooser());
        binding.btnLogin.setOnClickListener(v -> login());
        binding.btnAddLogin.setOnClickListener(v -> showLoginForm(null, false));
        binding.btnCancel.setOnClickListener(v -> showLoginList());
    }

    private void showFileChooser() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(Intent.createChooser(intent, "选择证书文件"), FILE_SELECT_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开文件选择器", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK && data != null) {
            try {
                android.net.Uri uri = data.getData();
                if (uri != null) {
                    String internalFilePath = FileUtils.copyFileToInternalStorage(this, uri);
                    if (internalFilePath != null) {
                        binding.etCertPath.setText(internalFilePath);
                        Toast.makeText(this, "文件选择成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "文件复制失败", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception e) {
                Toast.makeText(this, "文件选择失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadLoginInfoList() {
        loginInfoList = LoginInfoUtils.getLoginInfoList(this);
    }

    private void saveLoginInfoList() {
        LoginInfoUtils.saveLoginInfoList(this, loginInfoList);
    }

    private void showLoginForm(LoginInfo loginInfo, boolean isAutoLogin) {
        currentLoginInfo = loginInfo;
        if (loginInfo != null) {
            binding.etHost.setText(loginInfo.getHost());
            binding.etPort.setText(String.valueOf(loginInfo.getPort()));
            binding.etUsername.setText(loginInfo.getUsername());
            binding.etCertPath.setText(loginInfo.getCertPath());
            binding.etCertPass.setText(loginInfo.getCertPass());
        } else {
            clearForm();
        }
        binding.cvLoginList.setVisibility(isAutoLogin ? View.VISIBLE : View.GONE);
        binding.cvLoginForm.setVisibility(isAutoLogin ? View.GONE : View.VISIBLE);
    }

    private void showLoginList() {
        currentLoginInfo = null;
        clearForm();
        binding.cvLoginList.setVisibility(View.VISIBLE);
        binding.cvLoginForm.setVisibility(View.GONE);
    }

    private void clearForm() {
        binding.etHost.setText("");
        binding.etPort.setText("22");
        binding.etUsername.setText("wms");
        binding.etCertPath.setText("");
        binding.etCertPass.setText("");
    }

    private void login() {
        String host = binding.etHost.getText().toString().trim();
        String portStr = binding.etPort.getText().toString().trim();
        String username = binding.etUsername.getText().toString().trim();
        String certPath = binding.etCertPath.getText().toString().trim();
        String certPass = binding.etCertPass.getText().toString().trim();

        if (host.isEmpty() || portStr.isEmpty() || username.isEmpty() || certPath.isEmpty()) {
            Toast.makeText(this, R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        int port = Integer.parseInt(portStr);

        LoginInfo loginInfo;
        if (currentLoginInfo != null) {
            currentLoginInfo.setHost(host);
            currentLoginInfo.setPort(port);
            currentLoginInfo.setUsername(username);
            currentLoginInfo.setCertPath(certPath);
            currentLoginInfo.setCertPass(certPass);
            loginInfo = currentLoginInfo;
        } else {
            loginInfo = new LoginInfo(host, port, username, certPath, certPass);
        }

        showLoading(true);

        SshService.getInstance(this).login(loginInfo, new SshService.LoginCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                    if (currentLoginInfo == null) {
                        loginInfoList.add(loginInfo);
                    }
                    saveLoginInfoList();
                    LoginInfoUtils.saveCurrentLoginInfo(LoginActivity.this, loginInfo);

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showErrorDialog("登录失败", error);
                });
            }
        });
    }

    private void showLoading(boolean show) {
        if (show) {
            binding.llLoading.setVisibility(View.VISIBLE);
            binding.btnLogin.setEnabled(false);
            binding.btnSelectCert.setEnabled(false);
            binding.btnCancel.setEnabled(false);
            binding.btnAddLogin.setEnabled(false);
            loginListAdapter.setEnabled(false);
        } else {
            binding.llLoading.setVisibility(View.GONE);
            binding.btnLogin.setEnabled(true);
            binding.btnSelectCert.setEnabled(true);
            binding.btnCancel.setEnabled(true);
            binding.btnAddLogin.setEnabled(true);
            loginListAdapter.setEnabled(true);
        }
    }

    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
    }

    @Override
    public void onLoginInfoClick(LoginInfo loginInfo) {
        showLoginForm(loginInfo, true);
        login();
    }

    @Override
    public void onLoginInfoEdit(LoginInfo loginInfo) {
        showLoginForm(loginInfo, false);
    }

    @Override
    public void onLoginInfoDelete(LoginInfo loginInfo) {
        loginInfoList.remove(loginInfo);
        saveLoginInfoList();
        loginListAdapter.notifyDataSetChanged();
        Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
