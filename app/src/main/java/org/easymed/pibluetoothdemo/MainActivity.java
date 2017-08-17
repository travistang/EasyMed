package org.easymed.pibluetoothdemo;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;

public class MainActivity extends AppCompatActivity {

    // UI components
    private Button connectBtn;
    private Button emitBtn;
    private Button wifiBtn;

    private ListView bluetoothDeviceListView;
    private BluetoothListViewAdapter bluetoothDeviceListAdapter;

    // logic members
    private List<BluetoothDevice> deviceList;
    private PiBluetoothHelper bluetoothHelper;

    private BluetoothGatt pi;
    private int selectPosition = -1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // associate class member to components on layout
        connectBtn = (Button)findViewById(R.id.connect_btn);
        emitBtn = (Button)findViewById(R.id.emit_btn);
        wifiBtn = (Button)findViewById(R.id.wifi_btn);
        bluetoothDeviceListView = (ListView)findViewById(R.id.list_view);

        // set up the bluetooth device list.
        deviceList = new ArrayList<>();
        bluetoothDeviceListAdapter = new BluetoothListViewAdapter(this,R.layout.bluetooth_device_overview_layout,deviceList);
        bluetoothDeviceListView.setAdapter(bluetoothDeviceListAdapter);

        bluetoothHelper = new PiBluetoothHelper(this);

        // associate components with listeners
        connectBtn.setOnClickListener(onConnectButtonClick());
        emitBtn.setOnClickListener(onEmitButtonClick());
        wifiBtn.setOnClickListener(onWifiButtonClick());
        bluetoothDeviceListView.setOnItemClickListener(onBluetoothOverviewItemClick());

        // start scanning
        bluetoothHelper.lookForPi(bleScanCallback());
    }

    /*
        Component listeners
     */
    private View.OnClickListener onEmitButtonClick()
    {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(pi == null)
                {
                    Toast.makeText(MainActivity.this,
                            "None of the device is connected. Please pair the Pi with your device and try again",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if(!bluetoothHelper.sendEmitSignal(pi))
                {
                    Toast.makeText(MainActivity.this,
                            "Unable to send emit signal",
                            Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    private View.OnClickListener onWifiButtonClick()
    {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(pi == null)
                {
                    Toast.makeText(MainActivity.this,
                            "None of the device is connected. Please pair the Pi with your device and try again",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if(!bluetoothHelper.sendWifiSignal(pi))
                {
                    Toast.makeText(MainActivity.this,
                            "Unable to send wifi signal",
                            Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    private View.OnClickListener onConnectButtonClick()
    {
        return new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // disconnect any existing connection
                if(pi != null) {
                    pi.disconnect();
                }

                // get the device the user selected
                // invalid selection
                if (selectPosition < 0 || selectPosition > deviceList.size())
                {
                    Toast.makeText(MainActivity.this,
                            "Please select a device to connect first",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                BluetoothDevice selectedDevice = deviceList.get(selectPosition);
                pi = selectedDevice.connectGatt(MainActivity.this,false,bleConnectionHandler());

                if(pi == null)
                {
                    Toast.makeText(MainActivity.this,
                            "Unable to connect to device selected",
                            Toast.LENGTH_SHORT).show();

                } else
                {
                    Toast.makeText(MainActivity.this,
                            "Connected",
                            Toast.LENGTH_SHORT).show();
                }

                // reset select position
                selectPosition = -1;
            }

        };
    }
    private AdapterView.OnItemClickListener onBluetoothOverviewItemClick()
    {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bluetoothDeviceListView.requestFocusFromTouch();
                bluetoothDeviceListView.setSelection(position);
                selectPosition = position;
            }
        };
    }

    /*
           Bluetooth connection callback
     */
    private BluetoothGattCallback bleConnectionHandler()
    {
        return new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if(newState == STATE_CONNECTED)
                {
                    Toast.makeText(MainActivity.this,
                            "Connected to device",
                            Toast.LENGTH_SHORT).show();
                }else
                {
                    Toast.makeText(MainActivity.this,
                            "Disconnected from device",
                            Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    private ScanCallback bleScanCallback()
    {
        return new ScanCallback() {
            private void notifyChange()
            {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        bluetoothDeviceListAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                // add newly discovered device to the list
                BluetoothDevice newDevice = result.getDevice();
                if(newDevice == null) return;

                // check address to avoid duplication
                for(BluetoothDevice dev : deviceList)
                {
                    if(dev.getAddress().equals(newDevice.getAddress()))return;
                }
                deviceList.add(newDevice);
                notifyChange();
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                // add the list of newly discovered device to the list
                for(ScanResult res : results)
                {
                    BluetoothDevice newDevice = res.getDevice();
                    for(BluetoothDevice dev : deviceList)
                    {
                        if(dev.getAddress().equals(newDevice.getAddress()))return;
                    }
                    deviceList.add(newDevice);
                }
                notifyChange();
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.d("Scan callback","onScanFailed");
                Toast.makeText(MainActivity.this, "Failed to look for devices", Toast.LENGTH_SHORT).show();
            }
        };
    }

    /*
        Custom adapter that handles the interaction between the device list and the ListView
        this need to be overridden because the cells are using custom view (bluetooth_device_overview_layout)
        and the way to display each device info needs to be specified (therefore overriding getView(...))
     */
    private class BluetoothListViewAdapter extends ArrayAdapter<BluetoothDevice>
    {

        private LayoutInflater inflater;
        public BluetoothListViewAdapter (Context context,int layoutRes,List<BluetoothDevice> devList)
        {
            super(context,layoutRes,devList);
            inflater = (LayoutInflater)this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub
            // check and create cell row view if not created before
            View vi = convertView;
            if (vi == null)
                vi = inflater.inflate(R.layout.bluetooth_device_overview_layout, null);

            // get references of the UI components of the cell row
            TextView deviceAddressTextView = (TextView) vi.findViewById(R.id.device_address);
            TextView deviceNameTextView = (TextView) vi.findViewById(R.id.device_name);

            // get the corresponding BluetoothDevice reference
            BluetoothDevice device = this.getItem(position);

            // modify view according the info from device

            deviceNameTextView.setText(device.getName());
            deviceAddressTextView.setText(device.getAddress());

            return vi;
        }
    }
}
