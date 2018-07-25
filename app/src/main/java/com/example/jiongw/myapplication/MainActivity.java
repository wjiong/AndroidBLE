package com.example.jiongw.myapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private UUID[] ids = new UUID[]{UUID.fromString("ff51b30e-d7e2-4d93-8842-a7c4a57dfb07")};
    private UUID id = UUID.fromString("ff51b30e-d7e2-4d93-8842-a7c4a57dfb10");
    private BluetoothDevice mDevice;
    private BluetoothGattCharacteristic mCharacteristic;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private BluetoothGatt mBluetoothGatt;

    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;

    private static final int STATE_CONNECTING = 1;

    private static final int STATE_CONNECTED = 2;

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(ids, mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();


        final Button button1 = findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                scanLeDevice(true);
            }
        });
        final Button button2 = findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                scanLeDevice(false);
            }
        });
        final Button button3 = findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                send(true);
            }
        });

        final Button button4 = findViewById(R.id.button4);
        button4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                send(false);
            }
        });

        if (Build.VERSION.SDK_INT >= 23)
            requestPermissions(new String[]{"Show Location"}, 1);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "not supported", Toast.LENGTH_SHORT).show();
            finish();
        }
        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    @Override
    protected  void onStop() {
        super.onStop();

        close();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] res) {
        switch (code) {
            case 1:
                Log.i("Jiong", "granted");
                break;
            default:
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    Log.d("ble", String.format("BluetoothGat ReadRssi[%d]", rssi));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDevice = device;
                            if (mScanning) {
                                mScanning = false;
                                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                            }
                            connect();
                        }
                    });
                }
            };

    private void connect() {
        if (mBluetoothGatt != null) {
            if (mBluetoothGatt.connect()) {
                Log.d("ble", "Trying to create a existing connection.");
                mConnectionState = STATE_CONNECTING;
            }
            return;
        }
        mBluetoothGatt = mDevice.connectGatt(this, true, mGattCallback);
        Log.d("ble", "Trying to create a new connection.");
        mConnectionState = STATE_CONNECTING;
    }

    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w("ble", "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState = STATE_CONNECTED;
                Log.i("ble", "Connected to GATT server.");

                Log.i("ble", "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = STATE_DISCONNECTED;
                Log.i("ble", "Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
           if (status == BluetoothGatt.GATT_SUCCESS) {
               Log.i("ble", "discover success.");
               List<BluetoothGattService> list = gatt.getServices();
               for (BluetoothGattService service : list)
               {
                   mCharacteristic = service.getCharacteristic(id);

                   if (mCharacteristic != null)
                   {
                       Log.i("ble", "found characteristic");
                       break;
                   }
               }
            } else {
                Log.w("ble", "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("ble", "read success.");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.i("ble", "characteristic changed.");

        }
    };

    private void read()
    {
        if (mCharacteristic == null)
        {
            Log.e("ble", "no characteristic");
        }

        mBluetoothGatt.readCharacteristic(mCharacteristic);
    }

    private void send(boolean isOn)
    {
        if (mCharacteristic == null)
        {
            Log.e("ble", "no characteristic");
        }

        byte[] value = new byte[1];
        value[0] = isOn ? (byte)(0&0XFF) : (byte)(1&0XFF);//(byte) (21 & 0xFF);
        mCharacteristic.setValue(value);
        boolean res =mBluetoothGatt.writeCharacteristic(mCharacteristic);
        Log.i("ble", "write result is :" + res);
    }
}
