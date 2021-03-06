package com.github.xiaomi007.visiblegeofences;

import android.Manifest;
import android.content.DialogInterface;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, OnMapReadyCallback, GoogleMap.OnMapLongClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int PERMISSION_REQUEST = 513;
    private static final float MAP_INIT_ZOOM = 16.0f;

    private GoogleApiClient mClient;
    private GoogleMap mGoogleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        final SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (supportMapFragment != null) {
            supportMapFragment.getMapAsync(this);
        }

        if (!checkPermission()) {
            Log.d(TAG, "onCreate: permission denied");
        } else {
            createClient();
        }


    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: " + requestCode);
        if (requestCode == PERMISSION_REQUEST) {
            boolean allPermissionGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PermissionChecker.PERMISSION_GRANTED) {
                    allPermissionGranted = false;
                }
            }
            Log.d(TAG, "onRequestPermissionsResult: " + allPermissionGranted);
            if (allPermissionGranted) {
                createClient();
            } else {
                checkPermission();
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected");
        initMap();

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void createClient() {
        Log.d(TAG, "createClient");
        mClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
    }

    private boolean checkPermission() {
        int finePermission = PermissionChecker.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int coarsePermission = PermissionChecker.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (finePermission == PermissionChecker.PERMISSION_GRANTED && coarsePermission == PermissionChecker.PERMISSION_GRANTED) {
            Log.d(TAG, "checkPermission: GRANTED");
            return true;
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.d(TAG, "checkPermission: Show rationale");
                new AlertDialog.Builder(this)
                        .setTitle("Why the Location")
                        .setMessage("The Location permission is required to show your position on the map")
                        .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(
                                        MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                        PERMISSION_REQUEST
                                );
                            }
                        })
                        .show();

                return false;
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_REQUEST
                );
                return false;
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        initMap();
    }

    @SuppressWarnings("MissingPermission")
    private void initMap() {
        if (mGoogleMap != null && mClient != null && mClient.isConnected()) {
            Location location = LocationServices.FusedLocationApi.getLastLocation(mClient);
            if (location == null) {
                location = new Location("tokyo");
                location.setLatitude(35.6291);
                location.setLongitude(139.7429);
            }
//            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), MAP_INIT_ZOOM));

            final JsonReader jsonReader = new JsonReader(new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.fences))));
            final Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(jsonReader, JsonObject.class);

            if (jsonObject.has("data")) {
                final JsonArray data = jsonObject.get("data").getAsJsonArray();
                LatLngBounds.Builder latLngBounds = LatLngBounds.builder();
                for (JsonElement fence : data) {
                    final JsonObject obj = fence.getAsJsonObject();
                    final double latitude = obj.get("latitude").getAsDouble();
                    final double longitude = obj.get("longitude").getAsDouble();
                    final double radius = obj.get("radius").getAsDouble();

                    final LatLng latLng = new LatLng(latitude, longitude);
                    latLngBounds.include(latLng);
                    mGoogleMap.setOnMapLongClickListener(this);
                    final float markerColor = radius <= 100 ? BitmapDescriptorFactory.HUE_RED : radius <= 200 ? BitmapDescriptorFactory.HUE_BLUE : radius <= 300 ? BitmapDescriptorFactory.HUE_GREEN : BitmapDescriptorFactory.HUE_YELLOW;
                    final int color = radius <= 100 ? Color.argb(50, 255, 0, 0) : radius <= 200 ? Color.argb(50, 0, 0, 255) : radius <= 300 ? Color.argb(50, 0, 255, 0) : Color.argb(50, 255, 255, 0);
                    mGoogleMap.addMarker(
                            new MarkerOptions()
                                    .draggable(true)
                                    .position(latLng)
                                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
                    );
                    mGoogleMap.addCircle(
                            new CircleOptions()
                                    .center(latLng)
                                    .radius(radius)
                                    .strokeColor(color)
                                    .fillColor(color)
                    );

                }
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds.build(), 0));
            }

            //interaction callback
            }
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        if (mGoogleMap != null) {
            mGoogleMap.addMarker(
                    new MarkerOptions().draggable(true)
                            .position(latLng)
            );
            mGoogleMap.addCircle(new CircleOptions().center(latLng).radius(100).strokeColor(Color.argb(50, 255, 0, 0)).fillColor(Color.argb(50, 255, 0, 0)));
        }
    }
}
