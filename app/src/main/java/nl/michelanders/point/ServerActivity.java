package nl.michelanders.point;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import nl.michelanders.point.databinding.ActivityMainBinding;
import nl.michelanders.point.databinding.ActivityServerBinding;

public class ServerActivity extends AppCompatActivity {

    private ActivityServerBinding binding;
    private SharedPreferences sharedPreferences;
    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityServerBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Server info");
        }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        queue = SingletonRequestQueue.getInstance(this.getApplicationContext()).
                getRequestQueue();

        binding.backup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = sharedPreferences.getString("serverurl", null);
                String username = sharedPreferences.getString("username", null);
                String password = sharedPreferences.getString("password", null);

                JsonObjectRequest jsonObjectRequest = new AuthJsonObjectRequest
                        (username, password, Request.Method.PUT, url+"/server/backup", null, response -> {
                            Log.d("Response: ",response.toString());

                            getServerInfo();
                            getBackups();
                        }, error -> Toast.makeText(getApplicationContext(),getString(R.string.error_creating_backup), Toast.LENGTH_LONG).show()){
                };

                queue.add(jsonObjectRequest);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        getServerInfo();
        getBackups();
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

    protected void getServerInfo(){
        String url = sharedPreferences.getString("serverurl", null);
        String username = sharedPreferences.getString("username", null);
        String password = sharedPreferences.getString("password", null);

        JsonObjectRequest jsonObjectRequest = new AuthJsonObjectRequest
                (username, password, Request.Method.GET, url+"/server/info", null, response -> {
                    Log.d("Response: ",response.toString());

                    try {
                        binding.uptime.setText(DateUtils.formatElapsedTime((long)Float.parseFloat(response.getString("uptime"))));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> Toast.makeText(getApplicationContext(),getString(R.string.error_retrieving_server_uptime), Toast.LENGTH_LONG).show()){
        };

        queue.add(jsonObjectRequest);
    }

    protected void getBackups(){
        String url = sharedPreferences.getString("serverurl", null);
        String username = sharedPreferences.getString("username", null);
        String password = sharedPreferences.getString("password", null);

        JsonObjectRequest jsonObjectRequest = new AuthJsonObjectRequest
                (username, password, Request.Method.GET, url+"/server/backups", null, response -> {
                    Log.d("getBackups", response.toString());
                    List<Entry<String, String>> backups = new LinkedList<>();

                    for (Iterator<String> it = response.keys(); it.hasNext(); ) {
                        String index = it.next();
                        String date;
                        try {
                            date = response.getString(index);
                            backups.add(new AbstractMap.SimpleEntry<String, String>(index, date));
                        } catch (JSONException e) {
                            Toast.makeText(getApplicationContext(), getString(R.string.error_getting_points, e.getMessage()), Toast.LENGTH_LONG).show();
                        }
                    }

                    // Sort the list based on values (i.e. date strings)
                    Collections.sort(
                            backups,
                            (i1,
                             i2) -> i2.getValue().compareTo(i1.getValue()));

                    int i = 0;
                    for (ListIterator<Entry<String, String>> it = backups.listIterator(); it.hasNext() && i < 10; i++) {
                        Entry<String, String> b = it.next();
                        TableRow row = (TableRow) binding.backupTable.getChildAt(i);
                        row.setVisibility(View.VISIBLE);
                        Button restore = (Button)row.getChildAt(0);
                        TextView date = (TextView) row.getChildAt(1);
                        TextView guid = (TextView) row.getChildAt(2);
                        date.setText(b.getValue());
                        guid.setText(b.getKey());
                        restore.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                String url = sharedPreferences.getString("serverurl", null);
                                String username = sharedPreferences.getString("username", null);
                                String password = sharedPreferences.getString("password", null);

                                JsonObjectRequest jsonObjectRequest = new AuthJsonObjectRequest
                                        (username, password, Request.Method.PUT, url+"/server/restore/" + guid.getText(), null, response -> {
                                            Log.d("Response: ",response.toString());

                                            getServerInfo();
                                            getBackups();
                                        }, error -> Toast.makeText(getApplicationContext(),getString(R.string.error_creating_backup), Toast.LENGTH_LONG).show()){
                                };

                                queue.add(jsonObjectRequest);
                            }
                        });
                    }
                    for (; i < binding.backupTable.getChildCount(); i++) {
                        TableRow row = (TableRow) binding.backupTable.getChildAt(i);
                        row.setVisibility(View.INVISIBLE);
                    }
                }, error -> Toast.makeText(getApplicationContext(),getString(R.string.error_getting_points, error), Toast.LENGTH_LONG).show()){
                };

        queue.add(jsonObjectRequest);
    }

}