package com.example.aaron.practice8;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public class UserinfoWindow implements GoogleMap.InfoWindowAdapter {
    View myView;

    public UserinfoWindow(Context context){
        myView = LayoutInflater.from(context)
                .inflate(R.layout.user_info_window,null);
    }


    @Override
    public View getInfoWindow(Marker marker) {
        TextView txtPickupTitle = (TextView)myView.findViewById(R.id.txtInfo);
        txtPickupTitle.setText(marker.getTitle());
        return myView;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }
}
