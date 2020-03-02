package com.parse.starter.uber;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;


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
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class DriverLocationActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    Intent intent;


//  2.Accept Request
    public void acceptRequest(View view){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");                             //Query to "Request" class
        query.whereEqualTo("username", intent.getStringExtra("username"));                    //Finding the username in the "Request" class whose username is equal to the passed username
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e == null && objects.size() > 0){
                    for(ParseObject object : objects){
                        object.put("driverUsername", ParseUser.getCurrentUser().getUsername());     //adding the driverUsername to the Rider whom the driver himself has selected - It is assigning rider a driver
                        Log.i("Driver ","Added");
                        object.saveInBackground(new SaveCallback() {
                            @Override
                            public void done(ParseException e) {
                                if(e == null){
                                    //Giving the direction to the driver
                                    Intent directionIntent = new Intent(android.content.Intent.ACTION_VIEW,
                                            Uri.parse("http://maps.google.com/maps?saddr=" + (intent.getDoubleExtra("driverLat",0)+","+ intent.getDoubleExtra("driverLong",0) + "&daddr=" + intent.getDoubleExtra("requestLat",0) + "," + intent.getDoubleExtra("requestLong",0))));  //giving the co-ordinates to the map to find direction
                                    startActivity(directionIntent);

                                }
                            }
                        });
                    }
                }
            }
        });
    }

//  Oncreate
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_location);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }



//  On map ready
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        intent = getIntent();

        //1.Showing driver and rider location as zoomed as possible
        LatLng driverLocation = new LatLng(intent.getDoubleExtra("driverLat",0), intent.getDoubleExtra("driverLong",0));
        LatLng requestLocation = new LatLng(intent.getDoubleExtra("requestLat",0), intent.getDoubleExtra("requestLong",0));

        ArrayList<Marker> markers = new ArrayList<>();                                              //Creating Marker array list to store the lat, long of the two markers
        markers.add(mMap.addMarker(new MarkerOptions().position(driverLocation).title("Your Location")));   //adding marker for drivers location
        markers.add(mMap.addMarker(new MarkerOptions().position(requestLocation).title("Rider's Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))));   //adding marker for rider's Location
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker marker : markers) {
            builder.include(marker.getPosition());
        }
        LatLngBounds bounds = builder.build();

        int padding = 50; // offset from edges of the map in pixels
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

        //mMap.moveCamera(cu);
        mMap.animateCamera(cu);     //To add animation
    }
}
