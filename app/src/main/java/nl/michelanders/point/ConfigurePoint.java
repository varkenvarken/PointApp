package nl.michelanders.point;

import static java.lang.Double.max;
import static java.lang.Double.min;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.preference.PreferenceManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

import nl.michelanders.point.databinding.ActivityConfigurePointBinding;

public class ConfigurePoint extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private ActivityConfigurePointBinding binding;
    private SharedPreferences sharedPreferences;
    private RequestQueue queue;
    private Point point;
    private final boolean[] freePorts = new boolean[16];
    private String pointIndex;
    private final Gson gson = new Gson();
    private boolean syncing = false;
    private AlertDialog.Builder builder;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConfigurePointBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Configure Point");
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        queue = SingletonRequestQueue.getInstance(this.getApplicationContext()).
                getRequestQueue();
        Intent intent = getIntent();
        pointIndex = intent.getStringExtra("pointindex");

        binding.pointname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                point.name = editable.toString();
            }
        });

        binding.description.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                point.description = editable.toString();
            }
        });

        builder = new AlertDialog.Builder(this);

        Spinner pointTypeSpinner = (Spinner)findViewById(R.id.pointtypespinner);
        pointTypeSpinner.setAdapter(new PointTypeAdapter(this, R.layout.row, Point.pointTypes));
        pointTypeSpinner.setOnItemSelectedListener(this);

        Spinner defaultPositionSpinner = (Spinner)findViewById(R.id.defaultpositionspinner);
        defaultPositionSpinner.setAdapter(new PointDefaultPositionAdapter(this, R.layout.row, Point.defaultPositions));
        defaultPositionSpinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        if( adapterView.getId() == R.id.defaultpositionspinner){
            point._default = Point.defaultPositions[i];
        }else if ( adapterView.getId() == R.id.pointtypespinner) {
            point.pointtype = Point.pointTypes[i];
        }else{
            Log.d("onItemSelected", adapterView.toString());
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}

    public class PointTypeAdapter extends ArrayAdapter<String> {

        public PointTypeAdapter(Context context, int textViewResourceId,
                                String[] objects) {
            super(context, textViewResourceId, objects);
        }

        @Override
        public View getDropDownView(int position, View convertView,
                                    @NonNull ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        public View getCustomView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater=getLayoutInflater();
            View row=inflater.inflate(R.layout.row, parent, false);
            TextView label=(TextView)row.findViewById(R.id.pointtype);
            label.setText(Point.pointTypes[position]);

            ImageView icon=(ImageView)row.findViewById(R.id.icon);

            if (Point.pointTypes[position].equals("left")){
                icon.setImageResource(R.drawable.ic_left);
            } else if(Point.pointTypes[position].equals("right")){
                icon.setImageResource(R.drawable.ic_right);
            }
            return row;
        }
    }

    public class PointDefaultPositionAdapter extends ArrayAdapter<String> {

        public PointDefaultPositionAdapter(Context context, int textViewResourceId,
                                String[] objects) {
            super(context, textViewResourceId, objects);
        }

        @Override
        public View getDropDownView(int position, View convertView,
                                    @NonNull ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        public View getCustomView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater=getLayoutInflater();
            View row=inflater.inflate(R.layout.row, parent, false);
            TextView label=(TextView)row.findViewById(R.id.pointtype);
            label.setText(Point.defaultPositions[position]);

            ImageView icon=(ImageView)row.findViewById(R.id.icon);

            if (Point.defaultPositions[position].equals("left")){
                icon.setImageResource(R.drawable.ic_left);
            } else if(Point.defaultPositions[position].equals("right")){
                icon.setImageResource(R.drawable.ic_right);
            }
            return row;
        }
    }

    @Override
    protected void onResume() {
        getPoint();
        syncing = false;
        binding.sync.setChecked(false);
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            // Respond to the action bar's Up/Home button
            super.onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.server) {
            startActivity(new Intent(this, ServerActivity.class));
            return true;
        }else if(item.getItemId() ==  R.id.settings){
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }else if (item.getItemId() == R.id.about) {
            startActivity(new Intent(this, LicenseActivity.class));
            return true;
        }return super.onOptionsItemSelected(item);
    }

    protected void getPoint(){
        String url = sharedPreferences.getString("serverurl", null);
        String username = sharedPreferences.getString("username", null);
        String password = sharedPreferences.getString("password", null);

        JsonObjectRequest jsonObjectRequest = new AuthJsonObjectRequest
                (username, password, Request.Method.GET, url+"/point/"+ pointIndex, null, response -> {
                    Log.d("Response: ",response.toString());

                    try {
                        point = gson.fromJson(response.getJSONObject("point").toString(), Point.class);
                        JSONArray array = response.getJSONArray("freeports");
                        Arrays.fill(freePorts, false);
                        for (int i=0; i< array.length(); i++){
                            int fp = array.getInt(i);
                            freePorts[fp] = true;
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    point.index = pointIndex;
                    configureLayout();
                }, error -> Toast.makeText(getApplicationContext(),getString(R.string.error_retrieving_point), Toast.LENGTH_LONG).show()){
        };

        queue.add(jsonObjectRequest);
    }

    protected void savePoint(){
        String url = sharedPreferences.getString("serverurl", null);
        String username = sharedPreferences.getString("username", null);
        String password = sharedPreferences.getString("password", null);

        JSONObject body = point.toJSON();
        JsonObjectRequest jsonObjectRequest = new AuthJsonObjectRequest
                (username, password, Request.Method.PUT, url+"/point/"+ pointIndex + "/save", body, response -> getPoint(), error -> Toast.makeText(getApplicationContext(),getString(R.string.error_saving_point), Toast.LENGTH_LONG).show()){
        };
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(20000,0,1.0f));
        queue.add(jsonObjectRequest);
    }

    protected void deletePoint(){
        String url = sharedPreferences.getString("serverurl", null);
        String username = sharedPreferences.getString("username", null);
        String password = sharedPreferences.getString("password", null);

        JsonObjectRequest jsonObjectRequest = new AuthJsonObjectRequest
                (username, password, Request.Method.DELETE, url+"/point/"+ pointIndex, null, response -> {
                    // after a successful delete we return to the list of points
                    finish();
                }, error -> {
                    if(error.networkResponse != null){
                        switch(error.networkResponse.statusCode){
                            case 403:Toast.makeText(getApplicationContext(),getString(R.string.error_deleting_last_point), Toast.LENGTH_LONG).show();
                                    break;
                            case 404:
                            default:Toast.makeText(getApplicationContext(),getString(R.string.error_deleting_point), Toast.LENGTH_LONG).show();
                        }
                    }else{
                        Toast.makeText(getApplicationContext(),getString(R.string.error_deleting_point), Toast.LENGTH_LONG).show();
                    }
                }){
        };
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(20000,0,1.0f));
        jsonObjectRequest.setShouldRetryConnectionErrors(false);
        queue.add(jsonObjectRequest);
    }

    protected void addPoint(){
        String url = sharedPreferences.getString("serverurl", null);
        String username = sharedPreferences.getString("username", null);
        String password = sharedPreferences.getString("password", null);

        JsonObjectRequest jsonObjectRequest = new AuthJsonObjectRequest
                (username, password, Request.Method.POST, url+"/points/add", null, response -> {
                    try {
                        point = gson.fromJson(response.getJSONObject("point").toString(), Point.class);
                        JSONArray array = response.getJSONArray("freeports");
                        Arrays.fill(freePorts, false);
                        for (int i=0; i< array.length(); i++){
                            int fp = array.getInt(i);
                            freePorts[fp] = true;
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    pointIndex = point.index;
                    configureLayout();
                }, error -> {
                    if(error.networkResponse != null && error.networkResponse.statusCode == 409){
                        Toast.makeText(getApplicationContext(),getString(R.string.error_adding_too_many_point), Toast.LENGTH_LONG).show();
                    }else{
                        Toast.makeText(getApplicationContext(),getString(R.string.error_adding_point), Toast.LENGTH_LONG).show();
                    }
                }){
        };
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(20000,0,1.0f));
        queue.add(jsonObjectRequest);
    }

    protected void move(float v){
        String url = sharedPreferences.getString("serverurl", null);
        String username = sharedPreferences.getString("username", null);
        String password = sharedPreferences.getString("password", null);

        if(syncing) {
            JsonObjectRequest jsonObjectRequest = new AuthJsonObjectRequest
                    (username, password, Request.Method.PUT, url + "/point/" + pointIndex + "/move/" + v, null, response -> {
                        Log.d("Response: ", response.toString());
                        // we do not change anything in the layout
                    }, error -> Toast.makeText(getApplicationContext(),getString(R.string.error_moving_point, ""), Toast.LENGTH_LONG).show()) {
            };
            jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(20000, 0, 1.0f));
            queue.add(jsonObjectRequest);
        }
    }


    @SuppressLint("DefaultLocale")
    protected void configureLayout(){
        binding.pointname.setText(point.name);
        // text change listeners are added in OnCreate

        binding.enable.setChecked(point.enabled);
        binding.enable.setOnClickListener(view -> point.enabled = ((SwitchCompat)view).isChecked());

        for (int i=0; i<binding.portlist.getChildCount(); i++){
            Button button = (Button)binding.portlist.getChildAt(i);
            if(i == point.port){button.setBackgroundColor(Color.BLUE); button.setTextColor(Color.WHITE);}
            else{button.setBackgroundColor(Color.WHITE); button.setTextColor(Color.BLUE);}

            button.setEnabled((i == point.port) || freePorts[i]);
            Log.d(String.valueOf(i), String.valueOf((i == point.port) || freePorts[i]));
            int finalI = i;
            button.setOnClickListener(view -> {
                point.port = finalI;
                configureLayout();
            });
        }

        binding.valueLeft.setText(String.format("%.2f",point._left));
        binding.decLeft.setOnClickListener(view -> {
            point._left = (float)max(point._left - 0.01, -1.0);
            move(point._left);
            configureLayout();
        });
        binding.incLeft.setOnClickListener(view -> {
            point._left = (float)min(point._left + 0.01, 1.0);
            move(point._left);
            configureLayout();
        });

        binding.valueRight.setText(String.format("%.2f",point._right));
        binding.decRight.setOnClickListener(view -> {
            point._right = (float)max(point._right - 0.01, -1.0);
            configureLayout();
        });
        binding.incRight.setOnClickListener(view -> {
            point._right = (float)min(point._right + 0.01, 1.0);
            configureLayout();
        });

        binding.valueMid.setText(String.format("%.2f",point._mid));
        binding.decMid.setOnClickListener(view -> {
            point._mid = (float)max(point._mid - 0.01, -1.0);
            configureLayout();
        });
        binding.incMid.setOnClickListener(view -> {
            point._mid = (float)min(point._mid + 0.01, 1.0);
            configureLayout();
        });

        binding.valueSpeed.setText(String.format("%.2f",point.speed));
        binding.decSpeed.setOnClickListener(view -> {
            point.speed = (float)max(point.speed - 0.01, 0.0);
            configureLayout();
        });
        binding.incSpeed.setOnClickListener(view -> {
            point.speed = (float)min(point.speed + 0.01, 2.0);
            configureLayout();
        });

        binding.valueDeltat.setText(String.format("%.3f",point.deltat));
        binding.decDeltat.setOnClickListener(view -> {
            point.deltat = (float)max(point.deltat - 0.005, 0.005);
            configureLayout();
        });
        binding.incDeltat.setOnClickListener(view -> {
            point.deltat = (float)min(point.deltat + 0.005, 0.1);
            configureLayout();
        });

        binding.sync.setOnClickListener(view -> {
            syncing = ((SwitchCompat)view).isChecked();
            Toast.makeText(getApplicationContext(),"Changes will be send to Point (if enabled)",Toast.LENGTH_SHORT).show();
        });

        binding.save.setOnClickListener(view -> savePoint());
        binding.reset.setOnClickListener(view -> getPoint());
        binding.delete.setOnClickListener(view -> {
                builder.setMessage("Delete point forever?")
                        .setTitle("Confirm delete");
                builder.setPositiveButton("Delete", (dialog, id) -> {
                    Toast.makeText(getApplicationContext(),"Deleting point",Toast.LENGTH_SHORT).show();
                    deletePoint();
                });
                builder.setNegativeButton("Cancel", (dialog, id) -> {
                    // User cancelled the dialog
                });
                AlertDialog dialog = builder.create();

                dialog.show();

        });

        binding.add.setOnClickListener(view -> {
            Toast.makeText(getApplicationContext(),"Adding new point",Toast.LENGTH_SHORT).show();
            addPoint();
        });
    }

}