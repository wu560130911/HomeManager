package com.wms.homemanager.ui.fragment;

import android.app.AlertDialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wms.homemanager.R;
import com.wms.homemanager.databinding.FragmentWolBinding;
import com.wms.homemanager.model.LoginInfo;
import com.wms.homemanager.service.SshService;
import com.wms.homemanager.utils.LoginInfoUtils;

import java.util.ArrayList;
import java.util.List;

public class WolFragment extends Fragment {

    private FragmentWolBinding binding;
    private List<Device> devices;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWolBinding.inflate(inflater, container, false);

        devices = new ArrayList<>();

        loadDevices();
        displayDevices();

        binding.fabAddDevice.setOnClickListener(v -> showAddDeviceDialog());

        return binding.getRoot();
    }

    private void addDefaultDevices() {
        devices.add(new Device("NAS", "90:09:D0:13:6C:6F"));
        devices.add(new Device("FNOS", "a4:5d:36:73:b4:5c"));
        saveDevices();
    }

    private void saveDevices() {
        Gson gson = new Gson();
        String json = gson.toJson(devices);
        requireContext().getSharedPreferences("DevicePrefs", 0)
                .edit()
                .putString("devices", json)
                .apply();
    }

    private void loadDevices() {
        String json = requireContext().getSharedPreferences("DevicePrefs", 0)
                .getString("devices", null);
        if (json != null) {
            Gson gson = new Gson();
            devices = gson.fromJson(json, new TypeToken<List<Device>>(){}.getType());
        } else {
            addDefaultDevices();
        }
    }

    private void displayDevices() {
        binding.llDevices.removeAllViews();
        for (int i = 0; i < devices.size(); i++) {
            final int position = i;
            final Device device = devices.get(position);
            View deviceView = LayoutInflater.from(requireContext()).inflate(R.layout.device_item, null);
            TextView tvDeviceName = deviceView.findViewById(R.id.tv_device_name);
            TextView tvDeviceMac = deviceView.findViewById(R.id.tv_device_mac);
            Button btnWake = deviceView.findViewById(R.id.btn_wake);

            tvDeviceName.setText(device.getName());
            tvDeviceMac.setText(device.getMacAddress());

            btnWake.setOnClickListener(v -> wakeDevice(device.getMacAddress()));
            tvDeviceName.setOnClickListener(v -> showEditDeviceDialog(position, device));

            binding.llDevices.addView(deviceView);
        }
    }

    private void showAddDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.add_device_dialog, null);
        
        final EditText etDeviceName = view.findViewById(R.id.et_device_name);
        final EditText etDeviceMac = view.findViewById(R.id.et_device_mac);
        final Button btnCancel = view.findViewById(R.id.btn_cancel);
        final Button btnSave = view.findViewById(R.id.btn_save);

        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String deviceName = etDeviceName.getText().toString().trim();
            String deviceMac = etDeviceMac.getText().toString().trim();

            if (!deviceName.isEmpty() && !deviceMac.isEmpty()) {
                devices.add(new Device(deviceName, deviceMac));
                saveDevices();
                displayDevices();
                dialog.dismiss();
            } else {
                Toast.makeText(requireContext(), R.string.please_fill_device_info, Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showEditDeviceDialog(final int position, final Device device) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.add_device_dialog, null);
        
        final EditText etDeviceName = view.findViewById(R.id.et_device_name);
        final EditText etDeviceMac = view.findViewById(R.id.et_device_mac);
        final Button btnCancel = view.findViewById(R.id.btn_cancel);
        final Button btnSave = view.findViewById(R.id.btn_save);
        final TextView tvTitle = view.findViewById(R.id.tv_title);

        if (tvTitle != null) {
            tvTitle.setText(R.string.edit_device);
        }

        etDeviceName.setText(device.getName());
        etDeviceMac.setText(device.getMacAddress());

        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String deviceName = etDeviceName.getText().toString().trim();
            String deviceMac = etDeviceMac.getText().toString().trim();

            if (!deviceName.isEmpty() && !deviceMac.isEmpty()) {
                device.setName(deviceName);
                device.setMacAddress(deviceMac);
                saveDevices();
                displayDevices();
                dialog.dismiss();
            } else {
                Toast.makeText(requireContext(), R.string.please_fill_device_info, Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void wakeDevice(String macAddress) {
        LoginInfo loginInfo = LoginInfoUtils.getCurrentLoginInfo(requireContext());

        if (loginInfo != null) {
            SshService.getInstance(requireContext()).executeCommand(
                    loginInfo,
                    "wakeonlan " + macAddress,
                    new SshService.ExecuteCallback() {
                        @Override
                        public void onSuccess(String result) {
                            requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), getString(R.string.wake_command_sent) + result, Toast.LENGTH_SHORT).show());
                        }

                        @Override
                        public void onFailure(String error) {
                            requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), getString(R.string.wake_failed) + error, Toast.LENGTH_SHORT).show());
                        }
                    }
            );
        } else {
            Toast.makeText(requireContext(), R.string.please_login_first, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class Device {
        private String name;
        private String macAddress;

        public Device(String name, String macAddress) {
            this.name = name;
            this.macAddress = macAddress;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getMacAddress() {
            return macAddress;
        }

        public void setMacAddress(String macAddress) {
            this.macAddress = macAddress;
        }
    }
}
