package nl.michelanders.point;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import nl.michelanders.point.databinding.ActivityConfigurePointBinding;
import nl.michelanders.point.databinding.ActivityLicenseBinding;

public class LicenseActivity extends AppCompatActivity {

    private ActivityLicenseBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLicenseBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("About");
        }
        binding.license.loadDataWithBaseURL(null,
                "<b>PointApp</b> Copyright (C) 2022<br>Michel Anders (varkenvarken)" +
                        "<p>This program comes with ABSOLUTELY NO WARRANTY; for details, see <a href=\"https://raw.githubusercontent.com/varkenvarken/PointApp/master/LICENSE\">LICENSE</a>" +
                        "<p>This is free software, and you are welcome to redistribute it under certain conditions;",
                "text/html", "utf-8", null);
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
}