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
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private static final String FILE_TAG = "save/load";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;

    private List<Contact> contacts;
    private HashSet<String> prevContacts = new HashSet<>();
    private boolean scanning = false;
    private Context context;
    private ArrayList<ScanFilter> filters = new ArrayList<>();
    private ScanSettings settings;
    private BluetoothLeScanner scanner;
    private FusedLocationProviderClient fusedLocationClient;
    private ArrayAdapter<Contact> arrayAdapter;

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            final String TAG = "onScanResult";
            BluetoothDevice device = result.getDevice();
            final String address = device.getAddress();
            if (!prevContacts.contains(address)) {
                final String name = device.getName();
                final Date now = Calendar.getInstance().getTime();
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location loc) {
                                double lat = loc.getLatitude();
                                double lng = loc.getLongitude();
                                contacts.add(new Contact(name, address, now, lat, lng));
                                prevContacts.add(address);
                                Log.d(TAG, (name == null ? "null" : name) + "," + address);
                                arrayAdapter.notifyDataSetChanged();
                            }
                        });
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
        double lat = contact.getLatitude();
        double lng = contact.getLongitude();
        @SuppressLint("DefaultLocale")
        String geoString = String.format("geo:%f,%f?z=21", lat, lng);
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
                + "\n\nAre you sure you want to delete this contact?";
        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(message)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Contact contact = contacts.get(pos);
                        contacts.remove(pos);
                        prevContacts.remove(contact.getAddress());
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

    public void onShowAllClick(MenuItem item) {
        if (!contacts.isEmpty()) {
            Toast.makeText(context, "TODO: Map Activity", Toast.LENGTH_SHORT).show();
            int size = contacts.size();
            double[] lats = new double[size];
            double[] lngs = new double[size];
            for (int i = 0;  i < size; ++i) {
                Contact contact = contacts.get(i);
                lats[i] = contact.getLatitude();
                lngs[i] = contact.getLongitude();
            }
            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra(MapActivity.ARG_LATS, lats);
            intent.putExtra(MapActivity.ARG_LNGS, lngs);
            startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();

        // Load previous contacts from storage.
        File storage = new File(getFilesDir(), "contacts");
        if (storage.exists()) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                Contact[] ctcList = mapper.readValue(storage, Contact[].class);
                contacts = new ArrayList<>(Arrays.asList(ctcList));
            } catch (IOException e) {
                Toast.makeText(context, "Couldn't load contacts", Toast.LENGTH_SHORT).show();
                Log.e(FILE_TAG, "Couldn't load contacts", e);
                contacts = new ArrayList<>();
            }
        } else {
            contacts = new ArrayList<>();
        }

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
                break;
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
        LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        assert locationManager != null;
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage("Location services must be enabled to use this application")
                    .setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            Intent enable = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(enable);
                        }
                    })
                    .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    }).create().show();
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        File storage = new File(getFilesDir(), "contacts");
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(storage, contacts.toArray());
        } catch (IOException e) {
            Toast.makeText(context, "Couldn't save contacts", Toast.LENGTH_SHORT).show();
            Log.e(FILE_TAG, "Couldn't save contacts", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
}
