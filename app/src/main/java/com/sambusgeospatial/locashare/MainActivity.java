package com.sambusgeospatial.locashare;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    MapView mMapView;
    LocationDisplay mLocationDisplay;
    TextView locAddress;
    TextView longitudeDisplay;
    TextView latitudeDisplay;
    ExtendedFloatingActionButton shareBtn;
    private boolean locationSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mMapView = findViewById(R.id.mapView);
        locAddress = findViewById(R.id.locAddress);
        longitudeDisplay = findViewById(R.id.longitudeDisplay);
        latitudeDisplay = findViewById(R.id.latitudeDisplay);
        shareBtn = findViewById(R.id.shareBtn);
        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                shareBitmap(getScreenshot(mMapView));
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, "I just used Locashare to share my location! Find me here: \n\nLongitude (X): "
                        + longitudeDisplay.getText().toString() + "\nLatitude (Y): " + latitudeDisplay.getText().toString() + "\n"
                        + locAddress.getText().toString() + "\n\nView on google maps: https://www.google.com/maps/search/?api=1&query=" + latitudeDisplay.getText().toString() + "," + longitudeDisplay.getText().toString());
                sendIntent.setType("text/plain");
                Intent shareIntent = Intent.createChooser(sendIntent, "Share your location with");
                startActivity(shareIntent);
            }
        });
        setupMap();
        setupLocationDisplay();
    }

    private void shareBitmap(Bitmap screenshot) {
        //---Save bitmap to external cache directory---//
        //get cache directory
        File cachePath = new File(getExternalCacheDir(), "images");
        cachePath.mkdirs();

        //create png file
        File file = new File(cachePath, "last_location.png");
        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(file);
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        //---Share File---//
        //get file uri
        Uri myImageFileUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);

        //create a intent

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.putExtra(Intent.EXTRA_TEXT, "I just used Locashare to share my location! Find me here: \n\nLongitude (X): " + longitudeDisplay.getText().toString() + "\nLatitude (Y): " + latitudeDisplay.getText().toString() + "\n" + locAddress.getText().toString() + "View on google maps: https://www.google.com/maps/@" + longitudeDisplay.getText().toString() + "," + latitudeDisplay.getText().toString() + ",15z");
        shareIntent.putExtra(Intent.EXTRA_STREAM, myImageFileUri);
        shareIntent.setType("image/jpeg");

        startActivity(Intent.createChooser(shareIntent, "Share your location via: "));
    }

    private Bitmap getScreenshot(View view) {
        view.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
        view.setDrawingCacheEnabled(false);

        return bitmap;
    }

    private void setupMap() {
        if (mMapView != null) {
            Basemap.Type baseMapType = Basemap.Type.OPEN_STREET_MAP;
            double latitude = 0;
            double longitude = 0;
            int levelOfDetail = 11;
            ArcGISMap map = new ArcGISMap(baseMapType, latitude, longitude, levelOfDetail);
            mMapView.setMagnifierEnabled(true);
            mMapView.setMap(map);
            mMapView.addDrawStatusChangedListener(drawStatusChangedEvent -> {
                Point point = new Point(mMapView.getLocationDisplay().getLocation().getPosition().getX(), mMapView.getLocationDisplay().getLocation().getPosition().getY());
                updateLocation(point);
            });
        }
    }

    private void setupLocationDisplay() {
        mLocationDisplay = mMapView.getLocationDisplay();
        mLocationDisplay.addDataSourceStatusChangedListener(dataSourceStatusChangedEvent -> {

            // If LocationDisplay started OK or no error is reported, then continue.
            if (dataSourceStatusChangedEvent.isStarted() || dataSourceStatusChangedEvent.getError() == null) {
                return;
            }

            int requestPermissionsCode = 2;
            String[] requestPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

            // If an error is found, handle the failure to start.
            // Check permissions to see if failure may be due to lack of permissions.
            if (
                    !(
                            ContextCompat.checkSelfPermission(MainActivity.this, requestPermissions[0]) == PackageManager.PERMISSION_GRANTED
                                    && ContextCompat.checkSelfPermission(MainActivity.this, requestPermissions[1]) == PackageManager.PERMISSION_GRANTED
                                    && ContextCompat.checkSelfPermission(MainActivity.this, requestPermissions[2]) == PackageManager.PERMISSION_GRANTED
                                    && ContextCompat.checkSelfPermission(MainActivity.this, requestPermissions[3]) == PackageManager.PERMISSION_GRANTED
                    )
            ) {

                // If permissions are not already granted, request permission from the user.
                ActivityCompat.requestPermissions(MainActivity.this, requestPermissions, requestPermissionsCode);
            } else {

                // Report other unknown failure types to the user - for example, location services may not
                // be enabled on the device.
                String message = String.format("Error in DataSourceStatusChangedListener: %s", dataSourceStatusChangedEvent
                        .getSource().getLocationDataSource().getError().getMessage());
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }

        });

        mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.COMPASS_NAVIGATION);
        mLocationDisplay.startAsync();
    }

    private void updateLocation(Point location) {
        DecimalFormat df = new DecimalFormat("#.######");
        String x = df.format(location.getX());
        String y = df.format(location.getY());
        setAddress(location.getX(), location.getY());
        longitudeDisplay.setText(x);
        latitudeDisplay.setText(y);

        shareBtn.setEnabled(true);
    }

    private void setAddress(double x, double y) {
        StringRequest jsArrayRequest = new StringRequest(
                Request.Method.POST,
                getResources().getString(R.string.grocode_url),
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        final String address = jsonObject.getJSONObject("address").getString("LongLabel");
                        locAddress.setText(address);
                        Log.e("Geocode: ", jsonObject.toString());
                    } catch (JSONException e) {
                        Toast.makeText(this, "Unable to determine your address!", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }, error -> {
            Toast.makeText(this, "Unable to connect to the server!", Toast.LENGTH_SHORT).show();
        }) {

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/x-www-form-urlencoded");
                return params;
            }

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("f", "json");
                params.put("featureTypes", "");
                params.put("location", x + "," + y);
                return params;
            }
        };

        // Access the RequestQueue through your singleton class.
        AuthenticationInteractor.getInstance(this).clearQueue();
        AuthenticationInteractor.getInstance(this).addToRequestQueue(jsArrayRequest);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            // Location permission was granted. This would have been triggered in response to failing to start the
            // LocationDisplay, so try starting this again.
            mLocationDisplay.startAsync();
        } else {

            // If permission was denied, show toast to inform user what was chosen. If LocationDisplay is started again,
            // request permission UX will be shown again, option should be shown to allow never showing the UX again.
            // Alternative would be to disable functionality so request is not shown again.
            Toast.makeText(MainActivity.this, getResources().getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show();
        }
    }
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }


    @Override
    protected void onPause() {
        if (mMapView != null) {
            mMapView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMapView != null) {
            mMapView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        if (mMapView != null) {
            mMapView.dispose();
        }
        super.onDestroy();
    }
}