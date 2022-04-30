package nl.michelanders.point;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import nl.michelanders.point.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements View.OnLongClickListener, View.OnClickListener {

    private ActivityMainBinding binding;
    private SharedPreferences sharedPreferences;
    private RequestQueue queue;

    private final List<Point> points = new ArrayList<>();

    Gson gson = new Gson();
    private SwipeRefreshLayout swiperefreshlayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        queue = SingletonRequestQueue.getInstance(this.getApplicationContext()).
                getRequestQueue();
        swiperefreshlayout = binding.swiperefresh;
        swiperefreshlayout.setOnRefreshListener(() -> retrieveListOfPoints());
    }

    @Override
    protected void onResume() {
        retrieveListOfPoints();
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.settings){
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }else if (item.getItemId() == R.id.server){
            startActivity(new Intent(this, ServerActivity.class));
            return true;
        }else if (item.getItemId() == R.id.about){
            startActivity(new Intent(this, LicenseActivity.class));
            return true;
        }{
            return super.onOptionsItemSelected(item);
        }
    }

    @SuppressLint("SetTextI18n")  // we set the name of a button and that should definitely *not* come from a resource.
    protected void configureButtons(){
        int i = 0;
        for (ListIterator<Point> it = points.listIterator(); it.hasNext(); i++) {
            Point p = it.next();

            LinearLayout row = (LinearLayout) binding.pointTable.getChildAt(i);

            AppCompatImageView left = (AppCompatImageView) row.getChildAt(0);
            AppCompatImageView right = (AppCompatImageView) row.getChildAt(3);
            TextView name = (TextView) row.getChildAt(1);
            ImageView img = (ImageView) row.getChildAt(2);

            Log.d("configureButtons", p.pointtype + "_" + p.position());

            if(p.pointtype.equals("left")){
                Log.d("configureButtons left ", p.pointtype);

                if(p.position().equals("left")){
                    img.setImageResource(R.drawable.ic_left_left);
                    Log.d("configureButtons exec ", "left:left");
                } else{
                    img.setImageResource(R.drawable.ic_left_straight);
                    Log.d("configureButtons exec ", "left:straight");
                }
            }else if(p.pointtype.equals("right")){
                Log.d("configureButtons not left ", p.pointtype);
                if(p.position().equals("right")){
                    img.setImageResource(R.drawable.ic_right_right);
                    Log.d("configureButtons exec ", "right:right");
                } else{
                    img.setImageResource(R.drawable.ic_right_straight);
                    Log.d("configureButtons exec ", "right_straight");
                }
            }

            row.setVisibility(View.VISIBLE);
            left.setOnClickListener(this);
            right.setOnClickListener(this);
            name.setText(p.name + " (" + p.position() + ")");

            name.setOnLongClickListener(this);
        }
        for (; i < binding.pointTable.getChildCount(); i++) {
            LinearLayout row = (LinearLayout) binding.pointTable.getChildAt(i);
            row.setVisibility(View.INVISIBLE);
        }
    }

    protected void retrieveListOfPoints(){
        String TAG = "retrieveListOfPoints";
        String url = sharedPreferences.getString("serverurl", null);
        String username = sharedPreferences.getString("username", null);
        String password = sharedPreferences.getString("password", null);

        JsonObjectRequest jsonObjectRequest = new AuthJsonObjectRequest
                (username, password, Request.Method.GET, url+"/points", null, response -> {
                    points.clear();
                    for (Iterator<String> it = response.keys(); it.hasNext(); ) {
                        String index = it.next();
                        try {
                            Point p = gson.fromJson(response.getJSONObject(index).toString(), Point.class);
                            points.add(p);
                        } catch (JSONException e) {
                            Toast.makeText(getApplicationContext(),getString(R.string.error_getting_points, e.getMessage()), Toast.LENGTH_LONG).show();
                        }
                    }
                    swiperefreshlayout.setRefreshing(false);
                    configureButtons();
                }, error -> {
                    swiperefreshlayout.setRefreshing(false);
                    Toast.makeText(getApplicationContext(), getString(R.string.error_getting_points, error), Toast.LENGTH_LONG).show();
                });

        queue.add(jsonObjectRequest);
    }

    @Override
    public void onClick(View view) {
        LinearLayout row = (LinearLayout) view.getParent();
        int i = row.indexOfChild(view);
        LinearLayout t = (LinearLayout) row.getParent();
        int r = t.indexOfChild(row);

        // moving a point may take considerable time, especially if the speed
        // is set to a realistic value.
        binding.progressBar.setVisibility(View.VISIBLE);

        String direction = i == 0 ? "/left" : "/right";

        Point p = points.get(r);

        String url = sharedPreferences.getString("serverurl", null);
        String username = sharedPreferences.getString("username", null);
        String password = sharedPreferences.getString("password", null);

        JsonObjectRequest jsonObjectRequest = new AuthJsonObjectRequest
                (username, password, Request.Method.PUT, url+"/point/"+p.index+direction, null, response -> {
                    binding.progressBar.setVisibility(View.INVISIBLE);
                    Point rp = null;
                    try {
                        rp = gson.fromJson(response.getJSONObject("point").toString(), Point.class);
                    } catch (JSONException e) {
                        Toast.makeText(getApplicationContext(),getString(R.string.error_moving_point, e.getMessage()), Toast.LENGTH_LONG).show();
                    }
                    // we update the affected point with the return value so we can show the effect
                    points.set(r, rp);
                    configureButtons();
                }, error -> {
                    binding.progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(getApplicationContext(),getString(R.string.error_moving_point, error), Toast.LENGTH_LONG).show();
                }){
         };

        queue.add(jsonObjectRequest);

    }

    @Override
    public boolean onLongClick(View view) {
        LinearLayout row = (LinearLayout) view.getParent();
        LinearLayout t = (LinearLayout) row.getParent();
        int r = t.indexOfChild(row);
        Intent intent = new Intent(this, ConfigurePoint.class);
        intent.putExtra("pointindex", points.get(r).index);
        startActivity(intent);
        return true;
    }
}
