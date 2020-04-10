package com.example.lodestar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.emergency.EmergencyNumber;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    ImageView mBackground;
    TextView mTitle, mTimestamp, mLatitude, mLongitude, mAccuracy, mAltitude, mAddress;
    FloatingActionButton mInfo, mRefresh;
    Button mGMaps;
    private GoogleMap mMap;
    LocationManager locationManager;
    LocationListener locationListener;

    String lastLat;
    String lastLon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBackground = findViewById(R.id.background);
        mTitle = findViewById(R.id.title);
        mTimestamp = findViewById(R.id.timestamp);
        mLatitude = findViewById(R.id.latitude);
        mLongitude = findViewById(R.id.longitude);
        mAccuracy = findViewById(R.id.accuracy);
        mAltitude = findViewById(R.id.altitude);
        mAddress = findViewById(R.id.address);
        mInfo = findViewById(R.id.info);
        mRefresh = findViewById(R.id.refresh);
        mGMaps = findViewById(R.id.gmapsButton);

        setRandomBackground();

        updateTimestamp();

        mInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("Disclaimer");
                alertDialog.setMessage("Lodestar is intended to be a last-ditch resource in case of emergency. Please practice safe hiking principles and always: "
                        + "\n" + "\n" + "• Bring a backup form of navigation (e.g. map/sat phone)"
                        + "\n" + "• Let someone know when you should be returning"
                        + "\n" + "• Plan for adverse changes in weather"
                        + "\n" + "• Respect the wildlife");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        });

        mGMaps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                        Uri.parse(generateGMapsURI(lastLat,lastLon)));
                startActivity(intent);
            }
        });

        mRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("android.intent.action.MAIN");
                intent.setComponent(ComponentName.unflattenFromString("com.android.phone/.EmergencyDialer"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                Log.i("Location",location.toString());
                updateTimestamp();
                updateLocationDetails(location);

                //convert LatLon to address
                Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

                try {
                    List<Address> listAddresses = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(),1);

                    if (listAddresses != null && listAddresses.size() > 0) {
                        Log.i("PlaceInfo", listAddresses.get(0).toString());
                        String address = "";

                        if(listAddresses.get(0).getAddressLine(0) != null) {
                            address = listAddresses.get(0).getAddressLine(0);
                        } else {
                            if (listAddresses.get(0).getThoroughfare() != null) {
                                address += listAddresses.get(0).getThoroughfare() + ", ";
                            }
                            if (listAddresses.get(0).getLocality() != null) {
                                address += listAddresses.get(0).getLocality() + ", ";
                            }
                            if (listAddresses.get(0).getPostalCode() != null) {
                                address += listAddresses.get(0).getPostalCode() + ", ";
                            }
                            if (listAddresses.get(0).getAdminArea() != null) {
                                address += listAddresses.get(0).getAdminArea();
                            } else {
                                mAddress.setText("Address Details not found");
                            }
                        }

                        mAddress.setText("Address Details: " + address);

                    } else {
                        mAddress.setText("Address Details not found");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    mAddress.setText("Address Details not found");
                }

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        // has user granted permission?

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }

    }

    private void updateTimestamp() {
        Date currentTime = Calendar.getInstance().getTime();
        SimpleDateFormat localDateFormat = new SimpleDateFormat("HH:mm:ss dd MMM yyyy");
        mTimestamp.setText("Last Updated: " + localDateFormat.format(currentTime));
    }

    private void updateLocationDetails(Location location) {
        mLatitude.setText("Latitude: " + String.valueOf(location.getLatitude()));
        mLongitude.setText("Longitude: " + String.valueOf(location.getLongitude()));
        mAccuracy.setText("Accuracy: Within " + String.valueOf(location.getAccuracy()) + " metres");
        mAltitude.setText("Altitude: " + String.valueOf(location.getAltitude()) + " metres");

        lastLat = String.valueOf(location.getLatitude());
        lastLon = String.valueOf(location.getLongitude());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);
                }
            }
        }
    }


    private void setRandomBackground() {

        Random r = new Random();
        int i = r.nextInt(7);
        Log.i("Random BG int result", String.valueOf(i));

        switch(i) {
            case 0:
                mBackground.setImageResource(R.drawable.fire);
                break;
            case 1:
                mBackground.setImageResource(R.drawable.beam);
                break;
            case 2:
                mBackground.setImageResource(R.drawable.blue_sky);
                break;
            case 3:
                mBackground.setImageResource(R.drawable.sunset);
                break;
            case 4:
                mBackground.setImageResource(R.drawable.nebula);
                break;
            case 5:
                mBackground.setImageResource(R.drawable.night_sky);
                break;
            case 6:
                mBackground.setImageResource(R.drawable.forest);
                break;

        }

    }

    private String generateGMapsURI(String lat, String lon) {
        return "https://www.google.com/maps/search/?api=1&query=" + lat + "," + lon;
    }

    @Override
    public void onBackPressed() {
        finish();
    }

}
