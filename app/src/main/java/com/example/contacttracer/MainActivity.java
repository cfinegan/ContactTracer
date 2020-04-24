package com.example.contacttracer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;

    private BluetoothAdapter bt;
    private ArrayList<Contact> contacts = new ArrayList<>();
    private HashSet<String> prevContacts = new HashSet<>();
    private boolean scanning = false;
    private Context context;
    private ArrayList<ScanFilter> filters = new ArrayList<>();
    private ScanSettings settings;
    private BluetoothLeScanner scanner;
    private LocationManager lm;
    private ArrayAdapter<Contact> arrayAdapter;
    private ListView contactView;

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            final String TAG = "onScanResult";
            BluetoothDevice device = result.getDevice();
            String address = device.getAddress();
            if (!prevContacts.contains(address)) {
                //Toast.makeText(context, "Device found!", Toast.LENGTH_SHORT).show();
                String name = device.getName();
                Date now = Calendar.getInstance().getTime();
                @SuppressLint("MissingPermission")
                Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                contacts.add(new Contact(name, address, now, loc));
                prevContacts.add(address);
                Log.d(TAG, (name == null ? "null" : name) + "," + address);
                arrayAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Toast.makeText(context, "Scan failure!", Toast.LENGTH_LONG).show();
            Log.e("scanCallback", "BLE scan failed with code: " + errorCode);
        }
    };

    public void onScanClick(View view) {
        Button btn = (Button)view;
        if (scanning) {
            scanner.stopScan(leScanCallback);
            btn.setText(R.string.scan_start);
            scanning = false;
        } else {
            scanner.startScan(filters, settings, leScanCallback);
            btn.setText(R.string.scan_stop);
            scanning = true;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Contact contact = contacts.get(position);
        Location loc = contact.location;

        @SuppressLint("DefaultLocale")
        String geoString = String.format("geo:%f,%f?z=21", loc.getLatitude(), loc.getLongitude());
        Intent maps = new Intent(Intent.ACTION_VIEW, Uri.parse(geoString));
        maps.setPackage("com.google.android.apps.maps");
        if (maps.resolveActivity(getPackageManager()) != null) {
            startActivity(maps);
        } else {
            Toast.makeText(context, "Couldn't launch Google Maps", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();

        contactView = (ListView)findViewById(R.id.contactView);
        arrayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, contacts);
        contactView.setAdapter(arrayAdapter);
        contactView.setOnItemClickListener(this);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(context, "Bluetooth LE feature not found", Toast.LENGTH_LONG).show();
            finish();
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) {
            Toast.makeText(context, "Location GPS feature not found", Toast.LENGTH_LONG).show();
            finish();
        }

        String[] perms = new String[]{
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION};
        boolean needPerms = false;
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(context, p) != PackageManager.PERMISSION_GRANTED) {
                needPerms = true;
            }
        }
        if (needPerms) {
            ActivityCompat.requestPermissions(this, perms, REQUEST_PERMISSIONS);
        }

        final BluetoothManager btm = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        assert btm != null;
        bt = btm.getAdapter();
        assert bt != null;

        if (!bt.isEnabled()) {
            Intent enable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enable, REQUEST_ENABLE_BT);
        }

        settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                //.setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
                .build();

        scanner = bt.getBluetoothLeScanner();

        lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        Log.d("__ON_CREATE__", "hello world");
    }
}
