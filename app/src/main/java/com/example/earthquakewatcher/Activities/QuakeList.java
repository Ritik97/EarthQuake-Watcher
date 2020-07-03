package com.example.earthquakewatcher.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.earthquakewatcher.Model.EarthQuake;
import com.example.earthquakewatcher.R;
import com.example.earthquakewatcher.Util.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class QuakeList extends AppCompatActivity {
    private ArrayList<String> arrayList;
    private ListView listView;
    private RequestQueue requestQueue;
    private ArrayAdapter arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quake_list);

        arrayList = new ArrayList<>();
        listView = findViewById(R.id.list_view);
        requestQueue = Volley.newRequestQueue(this);

        getAllQuakes(Constants.URL);

    }

    private void getAllQuakes(String url) {

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        EarthQuake earthQuake = new EarthQuake();

                        try {
                            JSONArray features = response.getJSONArray("features");
                            for (int i = 0; i < Constants.LIMIT; i++) {
                                JSONObject properties = features.getJSONObject(i).getJSONObject("properties");

                                JSONObject geometry = features.getJSONObject(i).getJSONObject("geometry");

                                JSONArray coordinates = geometry.getJSONArray("coordinates");

                                double lat = coordinates.getDouble(0);
                                double lon = coordinates.getDouble(1);

                                earthQuake.setPlace(properties.getString("place"));
                                earthQuake.setType(properties.getString("type"));
                                earthQuake.setTime(properties.getLong("time"));

                                earthQuake.setLat(lat);
                                earthQuake.setLon(lon);

                                arrayList.add(earthQuake.getPlace());

                            }

                            //Instantiating array adapter, to use it in List view
                            arrayAdapter = new ArrayAdapter<>(QuakeList.this, android.R.layout.simple_list_item_1,
                                    android.R.id.text1, arrayList);

                            listView.setAdapter(arrayAdapter);

                            arrayAdapter.notifyDataSetChanged();

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

}
