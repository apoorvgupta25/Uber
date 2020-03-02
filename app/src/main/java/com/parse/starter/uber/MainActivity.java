package com.parse.starter.uber;

import android.content.Intent;
import android.os.Bundle;
import android.service.autofill.SaveInfo;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

import com.parse.LogInCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class MainActivity extends AppCompatActivity {


    Button getStarted;
    public void loginUser(){
        if(ParseUser.getCurrentUser().get("RiderOrDriver").equals("rider")){        //rider
            startActivity(new Intent(getApplicationContext(),RiderActivity.class));
        }
        else{           //Driver
            startActivity(new Intent(getApplicationContext(), ViewRequestsActivity.class));
        }
    }


    //1.getstarted
    public void getStarted(View view){
        Switch userTypeSwitch = findViewById(R.id.userTypeSwitch);
        Log.i("Switch User", String.valueOf(userTypeSwitch.isChecked()));

        String userType = "rider";
        if(userTypeSwitch.isChecked()){
            userType = "driver";
        }

        ParseUser.getCurrentUser().put("RiderOrDriver",userType);

        //2.Saving to Parse
        ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                loginUser();
            }
        });
    }

//  Oncreate
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().hide();

        getStarted = findViewById(R.id.button2);
        if(ParseUser.getCurrentUser() == null){
            ParseAnonymousUtils.logIn(new LogInCallback() {
                @Override
                public void done(ParseUser user, ParseException e) {
                    if(e == null){
                        Log.i("Info ", "Anonymous Login Successful");
                    }
                    else{
                        Log.i("Info " ,"Anonymous Login Failed");
                    }
                }
            });
        }
        else{
            if(ParseUser.getCurrentUser().getList("RiderOrDriver") != null){
                Log.i("Info","Redirecting as " + ParseUser.getCurrentUser().get("RiderOrDriver"));
                loginUser();
            }
        }
        ParseAnalytics.trackAppOpenedInBackground(getIntent());
    }
}