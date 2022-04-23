package nl.michelanders.point;

import static java.lang.Double.max;
import static java.lang.Double.min;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import nl.michelanders.point.databinding.ActivityConfigurePointBinding;

public class ConfigurePoint extends AppCompatActivity {

    private ActivityConfigurePointBinding binding;
    private SharedPreferences sharedPreferences;
    private RequestQueue queue;
    private Point point;
    private boolean[] freeports = new boolean[16];
    private String pointindex;
    private Gson gson = new Gson();;
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
        pointindex = intent.getStringExtra("pointindex");

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
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                super.onBackPressed();
                return true;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void getPoint(){
        String TAG = "getPoint";
        String url = sharedPreferences.getString("serverurl", null);
        String username = sharedPreferences.getString("username", null);
        String password = sharedPreferences.getString("password", null);

        JsonObjectRequest jsonObjectRequest = new AuthJsonObjectRequest
                (username, password, Request.Method.GET, url+"/point/"+ pointindex, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("Response: ",response.toString());

                        try {
                            point = gson.fromJson(response.getJSONObject("point").toString(), Point.class);
                            JSONArray array = response.getJSONArray("freeports");
                            for(int i=0; i<freeports.length;i++){freeports[i]=false;}
                            for (int i=0; i< array.length(); i++){
                                int fp = array.getInt(i);
                                freeports[fp] = true;
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        point.index = pointindex;
                        configureLayout();
                    }

                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("error retrieving point "+ pointindex, error.toString());
                    }
                }){
        };

        queue.add(jsonObjectRequest);
    }

    protected void savePoint(){
        String TAG = "savePoint";
        String url = sharedPreferences.getString("serverurl", null);
        String username = sharedPreferences.getString("username", null);
        String password = sharedPreferences.getString("password", null);

        JSONObject body = null;
        body = point.toJSON();
        Log.d(TAG, body.toString());
        JsonObjectRequest jsonObjectRequest = new AuthJsonObjectRequest
                (username, password, Request.Method.PUT, url+"/point/"+ pointindex + "/save", body, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("Response: ",response.toString());

                        getPoint();
                    }

                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("error saving point "+ pointindex, error.toString());
                    }
                }){
        };
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(20000,0,1.0f));
        queue.add(jsonObjectRequest);
    }

    protected void deletePoint(){
        String TAG = "deletePoint";
        String url = sharedPreferences.getString("serverurl", null);
        String username = sharedPreferences.getString("username", null);
        String password = sharedPreferences.getString("password", null);

        JsonObjectRequest jsonObjectRequest = new AuthJsonObjectRequest
                (username, password, Request.Method.DELETE, url+"/point/"+ pointindex, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("Response: ",response.toString());

                        finish();
                    }

                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("error deleting point "+ pointindex, error.toString());
                    }
                }){
        };
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(20000,0,1.0f));
        queue.add(jsonObjectRequest);
    }

    protected void addPoint(){
        String TAG = "addPoint";
        String url = sharedPreferences.getString("serverurl", null);
        String username = sharedPreferences.getString("username", null);
        String password = sharedPreferences.getString("password", null);

        JsonObjectRequest jsonObjectRequest = new AuthJsonObjectRequest
                (username, password, Request.Method.POST, url+"/points/add", null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("Response: ",response.toString());
                        try {
                            point = gson.fromJson(response.getJSONObject("point").toString(), Point.class);
                            JSONArray array = response.getJSONArray("freeports");
                            for(int i=0; i<freeports.length;i++){freeports[i]=false;}
                            for (int i=0; i< array.length(); i++){
                                int fp = array.getInt(i);
                                freeports[fp] = true;
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        pointindex = point.index;
                        configureLayout();
                    }

                }, error -> Log.d("error adding new point ", error.toString())){
        };
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(20000,0,1.0f));
        queue.add(jsonObjectRequest);
    }

    protected void move(float v){
        String TAG = "move";
        String url = sharedPreferences.getString("serverurl", null);
        String username = sharedPreferences.getString("username", null);
        String password = sharedPreferences.getString("password", null);

        if(syncing) {
            JsonObjectRequest jsonObjectRequest = new AuthJsonObjectRequest
                    (username, password, Request.Method.PUT, url + "/point/" + pointindex + "/move/" + String.valueOf(v), null, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            Log.d("Response: ", response.toString());
                            // we do not change anything in the layout
                        }

                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d("error moving point " + pointindex, error.toString());
                        }
                    }) {
            };
            jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(20000, 0, 1.0f));
            queue.add(jsonObjectRequest);
        }
    }


    protected void configureLayout(){
        binding.pointname.setText(point.name);
        // text change listeners are added in OnCreate

        binding.enable.setChecked(point.enabled);
        binding.enable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                point.enabled = ((SwitchCompat)view).isChecked();
            }
        });

        for (int i=0; i<binding.portlist.getChildCount(); i++){
            Button button = (Button)binding.portlist.getChildAt(i);
            if(i == point.port){button.setBackgroundColor(Color.BLUE); button.setTextColor(Color.WHITE);}
            else{button.setBackgroundColor(Color.WHITE); button.setTextColor(Color.BLUE);}

            button.setEnabled((i == point.port) || freeports[i]);
            Log.d(String.valueOf(i), String.valueOf((i == point.port) || freeports[i]));
            int finalI = i;
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    point.port = finalI;
                    configureLayout();
                }
            });
        }

        binding.valueLeft.setText(String.format("%.2f",point._left));
        binding.decLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                point._left = (float)max(point._left - 0.01, -1.0);
                move(point._left);
                configureLayout();
            }
        });
        binding.incLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                point._left = (float)min(point._left + 0.01, 1.0);
                move(point._left);
                configureLayout();
            }
        });

        binding.valueRight.setText(String.format("%.2f",point._right));
        binding.decRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                point._right = (float)max(point._right - 0.01, -1.0);
                configureLayout();
            }
        });
        binding.incRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                point._right = (float)min(point._right + 0.01, 1.0);
                configureLayout();
            }
        });

        binding.valueMid.setText(String.format("%.2f",point._mid));
        binding.decMid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                point._mid = (float)max(point._mid - 0.01, -1.0);
                configureLayout();
            }
        });
        binding.incMid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                point._mid = (float)min(point._mid + 0.01, 1.0);
                configureLayout();
            }
        });

        binding.valueSpeed.setText(String.format("%.2f",point.speed));
        binding.decSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                point.speed = (float)max(point.speed - 0.01, 0.0);
                configureLayout();
            }
        });
        binding.incSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                point.speed = (float)min(point.speed + 0.01, 2.0);
                configureLayout();
            }
        });

        binding.valueDeltat.setText(String.format("%.3f",point.deltat));
        binding.decDeltat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                point.deltat = (float)max(point.deltat - 0.005, 0.005);
                configureLayout();
            }
        });
        binding.incDeltat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                point.deltat = (float)min(point.deltat + 0.005, 0.1);
                configureLayout();
            }
        });

        binding.sync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                syncing = ((SwitchCompat)view).isChecked();
                Toast.makeText(getApplicationContext(),"Changes will be send to Point (if enabled)",Toast.LENGTH_SHORT).show();
            }
        });

        binding.save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                savePoint();
            }
        });
        binding.reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getPoint();
            }
        });
        binding.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    builder.setMessage("Delete point forever?")
                            .setTitle("Confirm delete");
                    builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Toast.makeText(getApplicationContext(),"Deleting point",Toast.LENGTH_SHORT).show();
                            deletePoint();
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
                    AlertDialog dialog = builder.create();

                    dialog.show();

            }
        });

        binding.add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(),"Adding new point",Toast.LENGTH_SHORT).show();
                addPoint();
            }
        });
    }

}