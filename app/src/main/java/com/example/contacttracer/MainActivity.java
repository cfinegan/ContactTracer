package com.example.contacttracer;

import androidx.appcompat.app.AlertDialog;
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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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

public class MainActivity extends AppCompatActivity
        implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;

    private ArrayList<Contact> contacts = new ArrayList<>();
    private HashSet<String> prevContacts = new HashSet<>();
    private boolean scanning = false;
    private Context context;
    private ArrayList<ScanFilter> filters = new ArrayList<>();
    private ScanSettings settings;
    private BluetoothLeScanner scanner;
    private LocationManager locationManager;
    private ArrayAdapter<Contact> arrayAdapter;

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            final String TAG = "onScanResult";
            BluetoothDevice device = result.getDevice();
            String address = device.getAddress();
            if (!prevContacts.contains(address)) {
                String name = device.getName();
                Date now = Calendar.getInstance().getTime();
                @SuppressLint("MissingPermission")
                Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                contacts.add(new Contact(name, address, now, loc));
                prevContacts.add(address);
                Log.d(TAG, (name == null ? "null" : name) + "," + address);
                arrayAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Toast.makeText(context, "Scan failure!", Toast.LENGTH_SHORT).show();
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
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final int pos = position;
        String message = contacts.get(position).toString()
                + "\n\nAre you sure you want to delete this record?";
        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(message)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Contact contact = contacts.get(pos);
                        contacts.remove(pos);
                        prevContacts.remove(contact.address);
                        arrayAdapter.notifyDataSetChanged();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create().show();
        return true;
    }

    public void onDeleteAllClick(MenuItem item) {
        if (!contacts.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage("Are you sure you want to remove all records?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            contacts.clear();
                            prevContacts.clear();
                            arrayAdapter.notifyDataSetChanged();
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create().show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();

        // Initialize list view to reflects contents of 'contacts' member variable.
        ListView contactView = findViewById(R.id.contactView);
        arrayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, contacts);
        contactView.setAdapter(arrayAdapter);
        contactView.setOnItemClickListener(this);
        contactView.setOnItemLongClickListener(this);

        // Make sure we aren't missing necessary platform features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(context, "Bluetooth LE feature not found", Toast.LENGTH_LONG).show();
            finish();
        }
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) {
            Toast.makeText(context, "Location GPS feature not found", Toast.LENGTH_LONG).show();
            finish();
        }

        // Make sure we have all the permissions we need.
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

        // Initialize Bluetooth.
        final BluetoothManager btm = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        assert btm != null;
        BluetoothAdapter bluetoothAdapter = btm.getAdapter();
        assert bluetoothAdapter != null;
        if (!bluetoothAdapter.isEnabled()) {
            Intent enable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enable, REQUEST_ENABLE_BT);
        }
        settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();
        scanner = bluetoothAdapter.getBluetoothLeScanner();

        // Initialize GPS.
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
}
