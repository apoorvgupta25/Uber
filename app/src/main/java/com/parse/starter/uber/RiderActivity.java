package com.parse.starter.uber;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class RiderActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    LocationManager locationManager;
    LocationListener locationListener;
    Button callUberButton;
    Boolean requestActive = false;

    Handler handler = new Handler();
    TextView infoTextView;

    Boolean driverActive = false;
//  5.2.CheckFor Updates of Driver's Location
    public void checkForUpdates(){

        //Check if someone(driver) has picked up the request of not - by checking the drivers username in the "Requests"
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        query.whereExists("driverUsername");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e == null && objects.size() > 0) {
                    driverActive = true;                                                            //setting to true when we realise when the driver is on the way
                    ParseQuery<ParseUser> query = ParseUser.getQuery();
                    query.whereEqualTo("username", objects.get(0).getString("driverUsername")); //checking with User who is the driver, who picked up the request;(To get the rider username) objects belong to the "Request"

                    query.findInBackground(new FindCallback<ParseUser>() {
                        @Override
                        public void done(List<ParseUser> objects, ParseException e) {
                            if(e == null && objects.size() > 0){
                                final ParseGeoPoint driverLocation = objects.get(0).getParseGeoPoint("DriverLocation");       //object refer to "User", we are getting the driver location from server, that we saved in ViewRequestActivity

                                if (ContextCompat.checkSelfPermission(RiderActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);            //LastKnownLocation is of rider as we are in the RiderActivity
                                    if (lastKnownLocation != null) {
                                        ParseGeoPoint riderLocation = new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());

                                        Double distanceInKM = driverLocation.distanceInKilometersTo(riderLocation);     //distance between the riderLocation and driverLocation
                                        if (distanceInKM < 0.01) {                                                        //driver arrives
                                            infoTextView.setText("Your driver is here");

                                            ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
                                            query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());

                                            query.findInBackground(new FindCallback<ParseObject>() {
                                                @Override
                                                public void done(List<ParseObject> objects, ParseException e) {
                                                    if(e == null){
                                                        for (ParseObject object : objects){
                                                            object.deleteInBackground();
                                                        }
                                                    }
                                                }
                                            });

                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    infoTextView.setText("");
                                                    callUberButton.setVisibility(View.VISIBLE);
                                                    callUberButton.setText("Call Uber");
                                                    requestActive = false;
                                                    driverActive = false;
                                                }
                                            },5000);

                                        }
                                        else {
                                            Double distance = (double) Math.round(distanceInKM * 10) / 10;
                                            infoTextView.setText("Your driver is " + distance.toString() + "Kilometers way");

                                            //Adding Marker for Driver and Rider
                                            LatLng driverLocationLatLong = new LatLng(driverLocation.getLatitude(), driverLocation.getLongitude());
                                            LatLng riderLocationLatLong = new LatLng(riderLocation.getLatitude(), riderLocation.getLongitude());

                                            ArrayList<Marker> markers = new ArrayList<>();                                              //Creating Marker array list to store the lat, long of the two markers
                                            mMap.clear();
                                            markers.add(mMap.addMarker(new MarkerOptions().position(driverLocationLatLong).title("Driver Location")));   //adding marker for drivers location
                                            markers.add(mMap.addMarker(new MarkerOptions().position(riderLocationLatLong).title("Your Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))));   //adding marker for rider's Location
                                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                            for (Marker marker : markers) {
                                                builder.include(marker.getPosition());
                                            }
                                            LatLngBounds bounds = builder.build();

                                            int padding = 50; // offset from edges of the map in pixels
                                            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

                                            mMap.animateCamera(cu);

                                            callUberButton.setVisibility(View.INVISIBLE);

                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    checkForUpdates();
                                                }
                                            },2000);
                                        }
                                    }
                                }
                            }
                        }
                    });
                }
                //5.1.Updating the driver's location to the rider with Text every 2 second
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        checkForUpdates();
                    }
                },2000);
            }
        });
    }

//  4.Logout
    public void logout(View view){
        ParseUser.logOut();
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
    }

//  2.Calling and cancelling Uber
    public void callUber(View view){

        //2.2.Cancel Uber
        if(requestActive){      //if the request is active and the user clicks the button, then we want to cancel the uber
            //3.2.Managing Request - when the user clicks the Button and We check if there is an active request in the server
            ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");     //Query into the Request Class of the parse server
            query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());               //finding the username equal to current username
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if(e == null && objects.size() > 0){                                            //checking the Request class if there are any request to call uber if yes then requestActive is  set to true
                    for(ParseObject object : objects) {                                             //delete all objects from the parse server
                            object.deleteInBackground();                                            //no need of call back
                        }
                        requestActive = false;                                                      //request is not Active now, therefore Button title is Call,
                        callUberButton.setText("Call Uber");                                        //now the user can Call the uber
                    }
                }
            });
        }else {
            //2.1.Call Uber, create an uber request in the server
            //checking if permission granted then go ahead
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation != null) {
                    //Saving the location to Parse in the "Request" Object
                    ParseObject request = new ParseObject("Request");
                    request.put("username", ParseUser.getCurrentUser().getUsername());
                    ParseGeoPoint parseGeoPoint = new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                    request.put("Location", parseGeoPoint);

                    request.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                        if (e == null) {
                            requestActive = true;                                                   //request is Active now, therefore Button title is Cancel,
                            callUberButton.setText("Cancel Uber");                                  //now the user can cancel uber

                            checkForUpdates();
                        }
                        }
                    });
                }
            }
        }
    }


//   update Map
    public void updateMap(Location location){

        if(driverActive != false) {
            mMap.clear();
            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 9.0f));
        }
    }

//    1.2.when user gives us permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0, locationListener);

                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    updateMap(lastKnownLocation);
            }
        }
    }

//    1.1.Making Map
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateMap(location);
            }

            //useless Method
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override
            public void onProviderEnabled(String provider) {}
            @Override
            public void onProviderDisabled(String provider) {}
        };
        /*
        if (Build.VERSION.SDK_INT < 23) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
        else{
        */
            //Checking for permission
            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        else {                                                                                      //if we get permission then we request location access again
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if(lastKnownLocation != null){
                    updateMap(lastKnownLocation);
                }
            }
//        }
    }

//   Oncreate
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        callUberButton = findViewById(R.id.callUberButton);
        infoTextView = findViewById(R.id.infoTextView);

        //3.1.Managing Request - when the user logs in and We check if there is an active request in the server from the current user, then we set Button to cancel uber
        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");         //Query into the Request Class of the parse server
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());                   //finding the username equal to current username
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e == null && objects.size() > 0){                                                //checking the Request class if there are any request to call uber if yes then requestActive is  set to true
                    requestActive = true;                                                           //request is Active now, therefore Button title is Cancel,
                    callUberButton.setText("Cancel Uber");                                          //now the user can cancel uber

                    checkForUpdates();
                }
            }
        });
    }
}
