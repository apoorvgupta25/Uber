package com.parse.starter.uber;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class ViewRequestsActivity extends AppCompatActivity {

    ListView requestListView;
    ArrayList<String> requests = new ArrayList<>();
    ArrayAdapter aad;

    LocationManager locationManager;
    LocationListener locationListener;

    //array adapter for below arrayList is not required b'coz arrayList is necessary only when we need to display
    ArrayList<Double> requestLat = new ArrayList<Double>();
    ArrayList<Double> requestLong = new ArrayList<Double>();
    ArrayList<String> ridersUsername = new ArrayList<>();



//  2.updating List View
    public void updateList(Location location){
        //Query to get nearby Requests
        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
        final ParseGeoPoint geoPointLocation = new ParseGeoPoint(location.getLatitude(), location.getLongitude());      //setting location which is passed
        query.whereNear("Location", geoPointLocation);                                          //getting location near to passed location from the server
        query.whereDoesNotExist("driverUsername");                                                  //Only showing username who does not have driverUsername - i.e. they are not assigned drivers
        query.setLimit(10);                                                                         //10 Request only
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e == null && objects.size() > 0){
                    requests.clear();
                    requestLat.clear();
                    requestLong.clear();
                    for(ParseObject object: objects){                                               //objects are the rows in the "Request" and we are getting the location from that row and finding the distance
                        ParseGeoPoint requestLocation = (ParseGeoPoint) object.get("Location");     //getting Location of all the Request from the server

                        Double distanceInKM = geoPointLocation.distanceInKilometersTo(requestLocation);//distance between the passed location and location of the requests
                        Double distance = (double) Math.round(distanceInKM * 10) / 10;
                        requests.add(distance.toString() + " KM");

                        requestLat.add(requestLocation.getLatitude());                              //adding lat,lon of all the request in an arrayList
                        requestLong.add(requestLocation.getLongitude());
                        ridersUsername.add(object.getString("username"));                       //adding username of all the requests
                    }
                    aad.notifyDataSetChanged();
                }
                else{
                    requests.add("No Nearby Requests");
                }
                aad.notifyDataSetChanged();
            }
        });
    }


    //1.2.when user gives us permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0, locationListener);

                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                updateList(lastKnownLocation);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_requests);

        setTitle("Rider's Request");

        requestListView = findViewById(R.id.requestListView);
        aad = new ArrayAdapter(this, android.R.layout.simple_list_item_1, requests);

        requests.clear();
        requests.add("Getting Nearby Requests...");

        requestListView.setAdapter(aad);


        //3.Passing rider and driver location to the Map after rider clicks one of the Request
        requestListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if(ContextCompat.checkSelfPermission(ViewRequestsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);        //For getting Driver's Location - we are not saving driver's location on the server
                    if (requestLat.size() > position && requestLong.size() > position && lastKnownLocation != null && ridersUsername.size() > position) {
                        Intent intent = new Intent(getApplicationContext(), DriverLocationActivity.class);
                        intent.putExtra("requestLat", requestLat.get(position));              //Passing Request Lat, Long - used for map
                        intent.putExtra("requestLong", requestLong.get(position));
                        intent.putExtra("driverLat", lastKnownLocation.getLatitude());        //Passing Driver Lat, Long
                        intent.putExtra("driverLong", lastKnownLocation.getLongitude());
                        intent.putExtra("username",ridersUsername.get(position));             //Passing the username of the rider selected by driver - used for accepting the request
                        startActivity(intent);
                    }
                }
            }
        });



        //1.1.Adding Location of request in the List
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateList(location);

                //4.Saving Driver's Location in the User
                ParseUser.getCurrentUser().put("DriverLocation",new ParseGeoPoint(location.getLatitude(),location.getLongitude()));
                ParseUser.getCurrentUser().saveInBackground();
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
                updateList(lastKnownLocation);
            }
        }
    }
}
