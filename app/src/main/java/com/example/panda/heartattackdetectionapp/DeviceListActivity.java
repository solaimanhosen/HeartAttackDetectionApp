package com.example.panda.heartattackdetectionapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.Set;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.panda.heartattackdetectionapp.R;

public class DeviceListActivity extends Activity {   //AppCompatActivity
    private static final String TAG = "DeviceListActivity";
    private static final boolean D = true;

    TextView textConnectionStatus;
    ListView pairedListView;

    // EXTRA string to send on to mainactivity
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    private BluetoothAdapter mBTAdapter;
    private ArrayAdapter<String>mPairedDevicesArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        textConnectionStatus = (TextView)findViewById(R.id.connecting);

        textConnectionStatus.setTextSize(40);

        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        pairedListView = (ListView)findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);
    }
    @Override
    public void onResume(){
        super.onResume();

        checkBTState();

        mPairedDevicesArrayAdapter.clear();
        textConnectionStatus.setText(" ");
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice>pairedDevices = mBTAdapter.getBondedDevices();
        if(pairedDevices.size() > 0){
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);

            for(BluetoothDevice device : pairedDevices){
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
        else{
            //String noDevices = getResources().getText(R.string.none_paired).toString();
            //mPairedDevicesArrayAdapter.add(noDevices);
            mPairedDevicesArrayAdapter.add("no devices paired");
        }
    }

    // Set up on-click listener for the list (nicked this - unsure)
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            textConnectionStatus.setText("Connecting...");
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Make an intent to start next activity while taking an extra which is the MAC address.
            Intent i = new Intent(DeviceListActivity.this, MainActivity.class);

            i.putExtra(EXTRA_DEVICE_ADDRESS, address);
            startActivity(i);
        }
    };

    private void checkBTState(){
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBTAdapter == null){
            Toast.makeText(getBaseContext(), "Devices is not Supported", Toast.LENGTH_SHORT).show();
            finish();
        }
        else{
            if(!mBTAdapter.isEnabled()){
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }
}
