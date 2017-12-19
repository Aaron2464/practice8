package com.example.aaron.practice8;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.aaron.practice8.Model.User;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;
import java.util.Random;


public class MapsActivity extends FragmentActivity implements
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMapClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        OnMapReadyCallback,
        LocationListener, TextToSpeech.OnInitListener {
    private static final int REQUEST_LOCATION = 2;
    private GoogleMap mMap;
    private LocationManager locMgr;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest locationRequest;
    private Location mlocation;

    private static int UPDATE_INTERVAL = 5000;
    private static int FATEST_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;

    DatabaseReference ref, userref, useruid;
    GeoFire geoFire;

    String bestProv;
    String userId,Uid;
    TextToSpeech textToSpeech;
    Button btnPickupRequest;

    int radius = 1;
    int distance = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        ref = FirebaseDatabase.getInstance().getReference();
        userref =  FirebaseDatabase.getInstance().getReference("Users").child(userId);
        geoFire = new GeoFire(ref);
        textToSpeech = new TextToSpeech(this,this);

        btnPickupRequest = (Button)findViewById(R.id.btnPickupRequest);
        btnPickupRequest.setOnClickListener(btnPickupRequestListener);
        setupMyLocation();

    }
    private Button.OnClickListener btnPickupRequestListener= new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            findUser();
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkPlayServices()) {
                        createLocationRequest();
                        ButtonClicker();
                        displayLocation();
                        buildGoogleApiClient();
                    }
                }
                break;
        }
    }

    private void setupMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION}
                    , REQUEST_LOCATION);
        } else {
            if (checkPlayServices()) {
                buildGoogleApiClient();
                createLocationRequest();
                displayLocation();
            }
        }
    }

    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mlocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mlocation != null) {
            final double latitude = mlocation.getLatitude();
            final double longitude = mlocation.getLongitude();
            userref.child("lat").setValue(latitude);
            userref.child("lng").setValue(longitude);
            geoFire.setLocation(userId, new GeoLocation(latitude, longitude),
                    new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {

                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 18));

                        }
                    });
            Log.d("EDMTDEV", String.format("Your Location was changed: %f / %f", latitude, longitude));
        } else {
            Log.d("EDMTDEV", "Can not get your location");
        }

    }
    private void findUser() {
        DatabaseReference people = FirebaseDatabase.getInstance().getReference();
        GeoFire gfpeople = new GeoFire(people);

        GeoQuery geoQuery = gfpeople.queryAtLocation(new GeoLocation(mlocation.getLatitude(), mlocation.getLongitude()), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, final GeoLocation location) {
                FirebaseDatabase.getInstance().getReference("Users")
                        .addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                for (DataSnapshot postsnot:dataSnapshot.getChildren()) {
                                    User user = dataSnapshot.getValue(User.class);
                                    String Uid = postsnot.child("uid").getValue().toString();
                                    if (!Uid.equals(userId)) {
                                        useruid = FirebaseDatabase.getInstance().getReference("Users").child(Uid);
                                        LatLng userlocation = new LatLng(Double.parseDouble(postsnot.child("lat").getValue().toString()), Double.parseDouble(postsnot.child("lng").getValue().toString()));    //Double.parseDouble(String.valueOf(useruid.child("lat"))), Double.parseDouble(String.valueOf(useruid.child("lng")))
                                        mMap.clear();
                                        mMap.addMarker(new MarkerOptions()
                                                .position(userlocation)   //new LatLng(location.latitude,location.longitude)
                                                .flat(true)
                                                .title(user.getName())
                                                .icon(BitmapDescriptorFactory.defaultMarker()));
                                    }
                                    else
                                    {
                                        return;
                                    }
                                }
                            }
                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FATEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
        locMgr = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        bestProv = locMgr.getBestProvider(criteria, true);
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode))
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, REQUEST_LOCATION).show();
            else {
                Toast.makeText(this, "不支援此裝置", Toast.LENGTH_LONG).show();
                finish();
            }
            return false;
        }
        return true;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng dangerous_area = new LatLng(24.168260, 120.695330);
        mMap.addCircle(new CircleOptions()
                .center(dangerous_area)
                .radius(40)
                .strokeColor(Color.BLUE)
                .fillColor(0x220000FF)
                .strokeWidth(2.0f)
        );

        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(dangerous_area.latitude, dangerous_area.longitude), 0.04f);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {

            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                sendNotification("EDMTDEV", String.format("%s entered the danger area", key));
                textToSpeech.speak("entered the danger area",TextToSpeech.QUEUE_FLUSH,null);
            }

            @Override
            public void onKeyExited(String key) {
                sendNotification("EDMTDEV", String.format("%s exited the danger area", key));
                textToSpeech.speak("exited the danger area",TextToSpeech.QUEUE_FLUSH,null);
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                Log.d("MOVE", String.format("%s moved within the dangerous area [%f/%f]", key, location.latitude, location.longitude));
            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                Log.e("Error", "" + error);
            }
        });
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setOnMyLocationButtonClickListener(this);
        ButtonClicker();
    }

    private void ButtonClicker() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationButtonClickListener(
                new GoogleMap.OnMyLocationButtonClickListener(){

                    @Override
                    public boolean onMyLocationButtonClick() {
                        LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
                        Criteria criteria = new Criteria();
                        criteria.setAccuracy(Criteria.ACCURACY_FINE);
                        String provider = locationManager.getBestProvider(criteria,true);
                        @SuppressLint("MissingPermission")
                                Location location = locationManager.getLastKnownLocation(provider);
                        if (location != null) {
                            Log.d("Location",location.getLatitude()+"/"+location.getLongitude());
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),location.getLongitude()),18));
                        }
                        return false;
                    }
                });
    }

    private void sendNotification(String title, String content) {
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentText(content);
        NotificationManager manager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(this,MapsActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(contentIntent);
        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.defaults |= Notification.DEFAULT_SOUND;

        manager.notify(new Random().nextInt(),notification);
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (locMgr.isProviderEnabled(LocationManager.GPS_PROVIDER) || locMgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                //locMgr.requestLocationUpdates(bestProv, 1000, 1, (android.location.LocationListener) this);
            }
        }
        else{
                Toast.makeText(this, "請開啟定位服務", Toast.LENGTH_LONG).show();
                textToSpeech.speak("please turn on the GPS",TextToSpeech.QUEUE_FLUSH,null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onLocationChanged(Location location) {

        mlocation = location;
        displayLocation();
        findUser();

    }


    @SuppressLint("MissingPermission")
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdate();
    }

    private void startLocationUpdate() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,locationRequest,this);
    }


    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMapClick(LatLng latLng) {

    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }

    @Override
    public void onInit(int status) {
        textToSpeech.setLanguage(Locale.US);
    }
}