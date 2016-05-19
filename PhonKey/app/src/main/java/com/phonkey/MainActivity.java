package com.phonkey;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.movisens.smartgattlib.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends ListActivity {

    private BluetoothAdapter _bluetoothAdapter;
    private Button _startBtn;
    private Button _stopBtn;
    private Handler _handler;
    private LeDeviceListAdapter _LeDeviceListAdapter;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private boolean _scanning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //getActionBar().setTitle("PhonKey");
        _handler = new Handler();

        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        _bluetoothAdapter = bluetoothManager.getAdapter();
        _bluetoothAdapter.enable();

        if(_bluetoothAdapter == null)
        {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        _startBtn = (Button) findViewById(R.id.start_scan);
        _stopBtn = (Button) findViewById(R.id.stop_scan);

        _startBtn.setOnClickListener(onStartBtnClicked);
        _stopBtn.setOnClickListener(onStopBtnClicked);

        _LeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(_LeDeviceListAdapter);
        scanLeDevice(true);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        final BluetoothDevice device = _LeDeviceListAdapter.getDevice(position);
        Toast.makeText(this, device.getName(), Toast.LENGTH_SHORT).show();

        ParcelUuid[] test = device.getUuids();
    }


    View.OnClickListener onStartBtnClicked = new View.OnClickListener()
    {
        @Override
        public void onClick(View v) {
            _LeDeviceListAdapter.clear();
            scanLeDevice(true);
        }
    };

    View.OnClickListener onStopBtnClicked = new View.OnClickListener()
    {
        @Override
        public void onClick(View v) {
            scanLeDevice(false);
        }
    };

    private BluetoothAdapter.LeScanCallback LeScanCallback = new BluetoothAdapter.LeScanCallback()
    {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    _LeDeviceListAdapter.addDevice(device);
                    _LeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    private void scanLeDevice(final boolean enabled) {
        if (enabled) {
            // Stops scanning after a pre-defined scan period.
            _handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    _scanning = false;
                    _bluetoothAdapter.stopLeScan(LeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            _scanning = true;
            _bluetoothAdapter.startLeScan(LeScanCallback);
        } else {
            _scanning = false;
            _bluetoothAdapter.stopLeScan(LeScanCallback);
        }
        invalidateOptionsMenu();
    }

    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = MainActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText("Unknown Device");
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}


