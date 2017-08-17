package org.easymed.pibluetoothdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.ParcelUuid;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by travistang on 17/8/2017.
 */

public class PiBluetoothHelper {

    // this should be the context of your the main activity
    private Context mContext;

    // bluetooth-specific members

    private BluetoothAdapter mBluetoothAdapter;
    public static final String SERVICE_UUID = "ffffffff-ffff-ffff-ffff-fffffffffff0";
    public static final String CHARACTERISTIC_UUID = "ffffffff-ffff-ffff-ffff-fffffffffff1";
    private ScanFilter mPiFilter;

    public PiBluetoothHelper(Context context)
    {
        // acquire application context
        this.mContext = context;
        // initialize bluetooth adapter
        this.mBluetoothAdapter = ((BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        ScanFilter.Builder filterBuilder = new ScanFilter.Builder();
        this.mPiFilter = filterBuilder
//                            .setServiceUuid(ParcelUuid.fromString(SERVICE_UUID))
                            .build();
        // define behaviour

    }
    /*
        methods that interacts with the Raspberry Pi.
     */

    /*
        A method to look for Raspberry pi.
        when the Pi with desired service UUID is found (FFFFFFFFFFFFFFFFFFFFF0),
        the onScanResult() method of the callback passed will be called.

        A RuntimeException will be thrown if the bluetooth function is not enabled by the time this method is called.
        Or it just simply not capable to use Bluetooth LE.
    */
    public void lookForPi(ScanCallback callback) throws RuntimeException
    {
        if(!isDeviceBLEAvailable() || !isBluetoothEnabled())
            throw new RuntimeException("Bluetooth LE is not available on this device at this moment.");
        final BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
        final ScanSettings settings = (new ScanSettings.Builder())
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .build();
        // prepare scan filter list
        List<ScanFilter> filterList = new ArrayList<>();
        filterList.add(this.mPiFilter);

        scanner.startScan(filterList,settings,callback);
    }

    /*
        Send emit signal to the raspberry pi.
        This will instruct the Pi to emit Wifi signal and disconnect from the Wifi it is connecting to.

        The function will return false if something wrong has encountered while sending the signal,
        such as the required characteristics or service not found in the given connection.

        If everything is fine it will return true.
     */
    public boolean sendEmitSignal(BluetoothGatt connection)
    {
        return sendCommand(connection,"emit");
    }

    public boolean sendWifiSignal(BluetoothGatt connection)
    {
        return sendCommand(connection,"wifi");
    }



    // methods that help figuring out the overview of the device. Such as whether it supports BLE and whether Bluetooth is enabled.
    public final boolean isDeviceBLEAvailable()
    {
        return (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE));
    }

    public final boolean isBluetoothEnabled()
    {
        if(null == mBluetoothAdapter) return false;
        return mBluetoothAdapter.isEnabled();
    }



    // internal method to handle command-sending process
    private boolean sendCommand(BluetoothGatt connection, String cmd)
    {
        connection.discoverServices();
        final BluetoothGattService service = connection.getService(java.util.UUID.fromString(SERVICE_UUID));
        if (service == null) return false;

        final BluetoothGattCharacteristic characteristic = service
                .getCharacteristic(java.util.UUID.fromString(CHARACTERISTIC_UUID));
        if (characteristic == null) return false;

        characteristic.setValue(cmd.getBytes());
        return connection.writeCharacteristic(characteristic);
    }

}
