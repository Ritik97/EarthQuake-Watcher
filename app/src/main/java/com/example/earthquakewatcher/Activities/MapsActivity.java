package com.example.earthquakewatcher.Activities;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.FontsContract;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.earthquakewatcher.Model.EarthQuake;
import com.example.earthquakewatcher.R;
import com.example.earthquakewatcher.UI.CustomInfoWindow;
import com.example.earthquakewatcher.Util.Constants;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener
        , GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private AlertDialog alertDialog;
    private AlertDialog.Builder builder;
    private RequestQueue requestQueue;

    private Button showList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        showList = findViewById(R.id.list_btn);

        showList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MapsActivity.this, QuakeList.class));
            }
        });

        requestQueue = Volley.newRequestQueue(this);

        getEarthQuakes();
    }

    private void getEarthQuakes() {

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, Constants.URL,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        EarthQuake earthQuake = new EarthQuake();
                        try {
                            JSONArray features = response.getJSONArray("features");
                            for (int i = 0; i < Constants.LIMIT; i++) {
                                JSONObject properties = features.getJSONObject(i).getJSONObject("properties");

                                Log.d("Properties", "onResponse: " + properties.getString("place"));
                                //get geometry object
                                JSONObject geometry = features.getJSONObject(i).getJSONObject("geometry");
                                //get coordinates array
                                JSONArray coordinates = geometry.getJSONArray("coordinates");

                                double latitude = coordinates.getDouble(0);
                                double longitude = coordinates.getDouble(1);
                                Log.d("Place", "Lat:" + latitude + " Long:" + longitude);

                                earthQuake.setPlace(properties.getString("place"));
                                earthQuake.setMagnitude(properties.getDouble("mag"));
                                earthQuake.setTime(properties.getLong("time"));
                                earthQuake.setDetailLink(properties.getString("detail"));
                                earthQuake.setType(properties.getString("type"));
                                earthQuake.setLat(latitude);
                                earthQuake.setLon(longitude);

                                java.text.DateFormat dateFormat = java.text.DateFormat.getDateInstance();
                                String formattedDate = dateFormat.format(new Date(properties.getLong("time"))
                                        .getTime());

                                MarkerOptions markerOptions = new MarkerOptions();

                                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                                markerOptions.title(earthQuake.getPlace());
                                markerOptions.position(new LatLng(latitude, longitude));
                                markerOptions.snippet("Magnitude: " +
                                        earthQuake.getMagnitude() + "\n" +
                                        "Date: " + formattedDate);

                                //TODO: Add circle around the marker where magnitude > 2.0
                                if (earthQuake.getMagnitude() > 2.0) {

                                    CircleOptions circleOptions = new CircleOptions();
                                    circleOptions.center(new LatLng(earthQuake.getLat(), earthQuake.getLon()));
                                    circleOptions.radius(4000);
                                    circleOptions.strokeWidth(3.6f);
                                    circleOptions.fillColor(Color.RED);
                                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

                                    mMap.addCircle(circleOptions);
                                }

                                Marker marker = mMap.addMarker(markerOptions);
                                marker.setTag(earthQuake.getDetailLink());
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 2));

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });

        requestQueue.add(jsonObjectRequest);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setInfoWindowAdapter(new CustomInfoWindow(getApplicationContext()));
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                /*LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                        .position(latLng)
                        .title("I am here!"));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 8));*/
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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            /*mMap.addMarker(new MarkerOptions()
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            .position(latLng)
            .title("I am here!"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 8));*/
        }


        // Add a marker in Sydney and move the camera
        //LatLng sydney = new LatLng(-34, 151);
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
        }
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        //Toast.makeText(getApplicationContext(), marker.getTitle().toString(), Toast.LENGTH_SHORT).show();
        getQuakeDetails(marker.getTag().toString());

    }


    private void getQuakeDetails(String url) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String url = "";
                        try {
                            JSONObject properties = response.getJSONObject("properties");
                            JSONObject products = properties.getJSONObject("products");
                            JSONArray geoserve = products.getJSONArray("geoserve");
                            for (int i = 0; i < geoserve.length(); i++) {

                                JSONObject geoserveObj = geoserve.getJSONObject(i);
                                JSONObject contentsObj = geoserveObj.getJSONObject("contents");
                                JSONObject geoJsonObj = contentsObj.getJSONObject("geoserve.json");

                                url = geoJsonObj.getString("url");

                            }
                            Log.d("URL", "onResponse: " + url);

                            getMoreDetails(url);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        requestQueue.add(jsonObjectRequest);
    }

    public void getMoreDetails(String url) {

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        builder = new AlertDialog.Builder(MapsActivity.this);
                        View view = getLayoutInflater().inflate(R.layout.popup, null);

                        TextView title = view.findViewById(R.id.popTitle);
                        TextView listTitle = view.findViewById(R.id.popListTitle);
                        TextView list = view.findViewById(R.id.popList);

                        Button dismiss = view.findViewById(R.id.dismissPopup);
                        Button dismissPop = view.findViewById(R.id.dismissPop);

                        WebView webView = view.findViewById(R.id.htmlWebView);

                        StringBuilder stringBuilder = new StringBuilder();

                        dismiss.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                alertDialog.dismiss();
                            }
                        });

                        dismissPop.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                alertDialog.dismiss();
                            }
                        });

                        /*In some cases, the tectonicSummary field is empty. So, will have to explicitly check for that*/
                        try {
                            if (response.has("tectonicSummary") && response.getString("tectonicSummary") != null) {

                                JSONObject tectonicSummary = response.getJSONObject("tectonicSummary");

                                if (tectonicSummary.has("text") && tectonicSummary.getString("text") != null) {

                                    String html = tectonicSummary.getString("text");

                                    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        try {
                            JSONArray cities = response.getJSONArray("cities");

                            for (int i = 0; i < cities.length(); i++) {
                                JSONObject cityObject = cities.getJSONObject(i);

                                stringBuilder.append("City: " + cityObject.getString("name")
                                        + "\n" + "Distance: " + cityObject.getString("distance")
                                        + "\n" + "Population: " + cityObject.getString("population"));

                                stringBuilder.append("\n\n");
                            }
                            list.setText(stringBuilder);
                            builder.setView(view);
                            alertDialog = builder.create();
                            alertDialog.show();


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        requestQueue.add(jsonObjectRequest);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }
}
