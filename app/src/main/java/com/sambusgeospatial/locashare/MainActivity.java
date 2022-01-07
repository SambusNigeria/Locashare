package com.sambusgeospatial.locashare;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
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
    RelativeLayout mapContainer;
    ExtendedFloatingActionButton shareBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOverflowIcon(AppCompatResources.getDrawable(this, R.drawable.ic_basemap));
        setSupportActionBar(toolbar);


        mMapView = findViewById(R.id.mapView);
        locAddress = findViewById(R.id.locAddress);
        longitudeDisplay = findViewById(R.id.longitudeDisplay);
        latitudeDisplay = findViewById(R.id.latitudeDisplay);
        shareBtn = findViewById(R.id.shareBtn);
        mapContainer = findViewById(R.id.mapContainer);

        shareBtn.setOnClickListener(view -> {
            if (screenshot == null) {
                captureScreenshotAsync();
            }

            final Bitmap bitmap = addWaterMark(screenshot);
            try {
                // getExternalFilesDir() + "/Pictures" should match the declaration in fileprovider.xml paths
                File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "my_location" + System.currentTimeMillis() + ".png");
                FileOutputStream fOut = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                fOut.flush();
                fOut.close();
                file.setReadable(true, false);
                final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                // wrap File object into a content provider. NOTE: authority here should match authority in manifest declaration
                Uri bmpUri = FileProvider.getUriForFile(MainActivity.this, "com.sambusgeospatial.fileprovider.Locashare", file);

                intent.putExtra(Intent.EXTRA_STREAM, bmpUri);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_TEXT, "I just used Locashare to share my location! Find me here: \n\nLongitude (X): "
                        + longitudeDisplay.getText().toString() + "\nLatitude (Y): " + latitudeDisplay.getText().toString() + "\n"
                        + locAddress.getText().toString() + "\n\nView on google maps: https://www.google.com/maps/search/?api=1&query=" + latitudeDisplay.getText().toString() + "," + longitudeDisplay.getText().toString());
                startActivity(Intent.createChooser(intent, "Share location via"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        setupMap();
        setupLocationDisplay();
    }

    Bitmap screenshot = (Bitmap) null;

    public void captureScreenshotAsync() {

        // export the image from the mMapView
        final ListenableFuture<Bitmap> export = mMapView.exportImageAsync();
        export.addDoneListener(() -> {
            try {
                screenshot = export.get();
                Log.d("TAG", "Captured the image!!");

            } catch (Exception e) {
                Toast
                        .makeText(getApplicationContext(), "Failed to capture screenshot because:" + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                Log.e("GFG", "Failed to capture screenshot because:" + e.getMessage());
            }
        });

    }

    private Bitmap addWaterMark(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap result = Bitmap.createBitmap(w, h, src.getConfig());
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(src, 0, 0, null);

        Bitmap waterMark = BitmapFactory.decodeResource(this.getResources(), R.drawable.logo1);
        canvas.drawBitmap(waterMark, 5, 5, null);

        return result;
    }

    ArcGISMap map;

    private void setupMap() {
        if (mMapView != null) {
            Basemap.Type baseMapType = Basemap.Type.TOPOGRAPHIC_VECTOR;
            double latitude = 0;
            double longitude = 0;
            int levelOfDetail = 16;
            map = new ArcGISMap(baseMapType, latitude, longitude, levelOfDetail);

            mMapView.setMap(map);
            mMapView.addDrawStatusChangedListener(drawStatusChangedEvent -> {
                Point point = new Point(mMapView.getLocationDisplay().getLocation().getPosition().getX(), mMapView.getLocationDisplay().getLocation().getPosition().getY());
                updateLocation(point);
                captureScreenshotAsync();
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_dark_grey) {
            map.setBasemap(Basemap.createDarkGrayCanvasVector());
            return true;
        }
        if (id == R.id.action_light_grey) {
            map.setBasemap(Basemap.createLightGrayCanvasVector());
            return true;
        }
        if (id == R.id.action_osm) {
            map.setBasemap(Basemap.createOpenStreetMap());
            return true;
        }
        if (id == R.id.action_imagery) {
            map.setBasemap(Basemap.createImageryWithLabelsVector());
            return true;
        }
        if (id == R.id.action_streets) {
            map.setBasemap(Basemap.createStreetsNightVector());
            return true;
        }
        if (id == R.id.action_default) {
            map.setBasemap(Basemap.createTopographicVector());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


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