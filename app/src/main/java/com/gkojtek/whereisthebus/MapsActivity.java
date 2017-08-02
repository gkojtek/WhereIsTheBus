package com.gkojtek.whereisthebus;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.gkojtek.whereisthebus.model.Bus;
import com.gkojtek.whereisthebus.model.BusLocations;
import com.gkojtek.whereisthebus.model.Result;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MapsActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, GoogleMap.OnMarkerClickListener {

    private static final String LOG_TAG = "toshiba";
    private static final boolean DEVELOPER_MODE = false;
    private GoogleMap mGoogleMap;
    private SupportMapFragment mapFrag;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private Location lastKnownLocation;
    //    private Marker mCurrLocationMarker;
    private List<Bus> processedBuses;
    private List<Bus> buses;
    private List<String> selectedBuses;
    HashMap<Integer, Marker> markersHashMap = new HashMap<>();
    BitmapDescriptor arrow;
    private String jsonString;
    private boolean notificationMode;

    //            public static final String BUS_POSITIONS_URL = "https://api.um.warszawa.pl/api/action/busestrams_get/?resource_id=%20f2e5503e-%20927d-4ad3-9500-4ab9e55deb59&apikey=41207ac7-eefe-4b5b-87d8-0704cdec0620&type=1&line=185";
    private SparseArray<Bus> readyToMove;
    private SparseArray<Bus> readyToDraw;

    private volatile String BUS_POSITIONS_URL = "https://api.um.warszawa.pl/api/action/busestrams_get/?resource_id=%20f2e5503e-%20927d-4ad3-9500-4ab9e55deb59&apikey=41207ac7-eefe-4b5b-87d8-0704cdec0620&type=1";
    private volatile String TRAM_POSITIONS_URL = "https://api.um.warszawa.pl/api/action/busestrams_get/?resource_id=%20f2e5503e-%20927d-4ad3-9500-4ab9e55deb59&apikey=41207ac7-eefe-4b5b-87d8-0704cdec0620&type=2";
    private BusDownloader busDownloader;
    private MenuItem alarm;

    List<String> alarmList;
    private Intent alarmIntentService;


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEVELOPER_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectAll()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
        setContentView(R.layout.activity_main);


        buses = new ArrayList<>();
        arrow = getMarkerIconFromDrawable(getDrawable(R.drawable.arrow_circle));
        busDownloader = new BusDownloader();
        alarmList = new ArrayList<>();

        alarmIntentService = new Intent(this, MonitorBusesIntentService.class);
        alarmIntentService.putStringArrayListExtra("chosen", (ArrayList<String>) alarmList);


        getSupportActionBar().setTitle("WITB");
//        getSupportActionBar().setBackgroundDrawable(getDrawable(R.mipmap.ic_launcher));
//        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setLogo(getDrawable(R.mipmap.ic_launcher));
        getSupportActionBar().setIcon(getDrawable(R.mipmap.ic_launcher));

        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);
        setUpMap();
    }

    private BitmapDescriptor getMarkerIconFromDrawable(Drawable drawable) {
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.map_menu, menu);
        MenuItem item = menu.findItem(R.id.menu_search);
        alarm = menu.findItem(R.id.set_alarm_menu);
        SearchView searchView = (SearchView) item.getActionView();
        searchView.setQueryHint("Numer linii");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                selectedBuses = Arrays.asList(query.split("\\s*,\\s*"));
                mGoogleMap.clear();
                markersHashMap = new HashMap<>();

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.stop_following_menu:
                selectedBuses = new ArrayList<>();
                mGoogleMap.clear();
                break;
            case R.id.center_location_menu:
                if (lastKnownLocation != null) {
                    LatLng lastLatLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                    CameraPosition cameraPosition = CameraPosition.builder().target(lastLatLng).zoom(16).bearing(0).build();
                    mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                }
                break;
            case R.id.change_map_type_menu:
                if (mGoogleMap.getMapType() == GoogleMap.MAP_TYPE_HYBRID) {
                    mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                } else {
                    mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                }
                break;
            case R.id.select_bus_menu:
                item.setCheckable(true);
                if (item.isChecked()) {
                    item.setIcon(getDrawable(R.drawable.ic_tram_black_24dp));
                    item.setChecked(false);
                } else {
                    item.setIcon(getDrawable(R.drawable.ic_directions_bus_black_24dp));
                    item.setChecked(true);
                }
                break;
            case R.id.set_alarm_menu_166_plac_hallera:
                item.setCheckable(true);
                if (!item.isChecked()) {
                    alarm.setIcon(getDrawable(R.drawable.ic_alarm_on_black_24dp));
                    item.setChecked(true);
                    startService(alarmIntentService);
                } else {
                    alarm.setIcon(getDrawable(R.drawable.ic_alarm_black_24dp));
                    item.setChecked(false);
                }
                break;
            case R.id.alarm_off_menu:
                alarm.setIcon(getDrawable(R.drawable.ic_alarm_black_24dp));
                // stop intent
        }
        return true;
    }

    void createBusesFromJson(String json) {
//        // De-serialize the JSON string into an array of objects
//        processedBuses = new ArrayList<>();
//
//        Gson gson = new Gson();
//        BusLocations busLocations = gson.fromJson(String.valueOf(json), BusLocations.class);
//        List<Result> result = busLocations.getResult();
//
//        for (Result temp : result) {
//            double lat = temp.getLat();
//            double lon = temp.getLon();
//            LatLng tempLatLng = new LatLng(lat, lon);
//            String tempLine = temp.getLines();
//            String tempBrigade = temp.getBrigade();
//            Bus tempBus = new Bus(tempLatLng, tempLine, tempBrigade);
//            processedBuses.add(tempBus);
//        }

//        new ProcessData().execute(json);


    }

    private void createMarkersFromBuses() {

//        final SparseArray<Bus> readyToMove = new SparseArray<>();
//        final SparseArray<Bus> readyToDraw = new SparseArray<>();
//
//        for (Bus bus : processedBuses) {
//            Integer id = bus.hashCode();
//
//            if (selectedBuses != null && selectedBuses.contains(bus.getLine())) {
//                if (markersHashMap.containsKey(id)) {
//
//                    Marker marker = markersHashMap.get(id);
//                    for (Bus tempBus : buses) {
//                        if (tempBus.hashCode() == bus.hashCode() && !(tempBus.getCurrentLatLng().equals(bus.getCurrentLatLng()))) {
//                            tempBus.updatePosition(bus.getCurrentLatLng());
//                            bus = tempBus;
//                        }
//                    }
//                    readyToMove.put(id, bus);
//
//                    // Update your marker
//                } else {
//                    buses.add(bus);
//                    readyToDraw.put(id, bus);
//                }
//            }
//        }


    }

    private void updateMarkers(final SparseArray<Bus> readyToMove, final SparseArray<Bus> readyToDraw) {
        runOnUiThread(new Runnable() {
            public void run() {
                for (int i = 0; i < readyToMove.size(); i++) {
                    int id = readyToMove.keyAt(i);
                    Bus tempBus = readyToMove.get(id);

                    Marker marker = markersHashMap.get(id);
                    marker.setPosition(tempBus.getCurrentLatLng());
                    if (tempBus.isPositionUpdated()) {
                        marker.setRotation(tempBus.getHeading());
                    }
                }

                for (int i = 0; i < readyToDraw.size(); i++) {
                    int id = readyToDraw.keyAt(i);
                    Bus bus = readyToDraw.get(id);

                    Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                            .position(bus.getCurrentLatLng())
                            .title(bus.getLine())
//                            .snippet("autobus")
                            .anchor(0.5f, 0.5f)
                            .infoWindowAnchor(0.5f, 0.5f)
//                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                            .icon(arrow));

                    markersHashMap.put(id, marker);
                }
            }
        });
    }


    @Override
    public void onPause() {
        super.onPause();

        //stop location updates when Activity is no longer active
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    private void setUpMap() {
        // Retrieve the bus data from the web service
//        new Thread(new Runnable() {
//            public void run() {
//                try {
//                    retrieveAndAddBuses();
//                } catch (IOException e) {
//                    Log.e(LOG_TAG, "Cannot retrive processedBuses", e);
//                    return;
//                }
//            }
//        }).start();


//        Runnable helloRunnable = new Runnable() {
//
//            public void run() {
//                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
//                try {
//                    retrieveAndAddBuses();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//        };
//
//        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
//        executor.scheduleAtFixedRate(helloRunnable, 0, 3, TimeUnit.SECONDS);

        ScheduledExecutorService busExecutor = Executors.newScheduledThreadPool(1);
        busExecutor.scheduleAtFixedRate(goRunnable(BUS_POSITIONS_URL), 0, 3, TimeUnit.SECONDS);

    }

    private Runnable goRunnable(String url) {
        class GoRunnable implements Runnable {
            String url;

            GoRunnable(String url) {
                this.url = url;
            }

            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                try {
                    retrieveAndAddBuses(url);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return new GoRunnable(url);
    }


    protected void retrieveAndAddBuses(String url) throws IOException, JSONException {
        jsonString = busDownloader.downloadJson();
        new ProcessData().execute(jsonString);
//        HttpURLConnection conn = null;
//        final StringBuilder json = new StringBuilder();
//        try {
//            // Connect to the web service
//            URL url = new URL(BUS_POSITIONS_URL);
//            conn = (HttpURLConnection) url.openConnection();
//            InputStreamReader in = new InputStreamReader(conn.getInputStream());
//
//            // Read the JSON data into the StringBuilder
//            int read;
//            char[] buff = new char[1024];
//            while ((read = in.read(buff)) != -1) {
//                json.append(buff, 0, read);
//            }
//        } catch (IOException e) {
//            Log.e(LOG_TAG, "Error connecting to service", e);
//            throw new IOException("Error connecting to service", e);
//        } finally {
//            if (conn != null) {
//                conn.disconnect();
//            }
//        }

        // Create markers for the city data.
        // Must run this on the UI thread since it's a UI operation.

//        createBusesFromJson(json.toString());
//        jsonString = json.toString();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        LatLng warsaw = new LatLng(52.227808, 21.051919);
        mGoogleMap = googleMap;
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(warsaw, 11));
        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                buildGoogleApiClient();
                mGoogleMap.setMyLocationEnabled(true);

            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        } else {
            buildGoogleApiClient();
            mGoogleMap.setMyLocationEnabled(true);
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {
//        if (mCurrLocationMarker != null) {
//            mCurrLocationMarker.remove();
//        }

        //Place current location marker
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
//        MarkerOptions markerOptions = new MarkerOptions();
//        markerOptions.position(latLng);
//        markerOptions.title("Current Position");
//        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
//        mCurrLocationMarker = mGoogleMap.addMarker(markerOptions);


        lastKnownLocation = location;
    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Potrzebuję lokalizacji")
                        .setMessage("Daj.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapsActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mGoogleMap.setMyLocationEnabled(true);
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        CameraUpdate location = CameraUpdateFactory.newLatLngZoom(
                marker.getPosition(), 12);
        mGoogleMap.animateCamera(location);

        return false;
    }

    private class ProcessData extends AsyncTask<String, String, Void> {

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            updateMarkers(readyToMove, readyToDraw);
        }

        @Override
        protected Void doInBackground(String... strings) {


            processedBuses = new ArrayList<>();
            try {
                Gson gson = new Gson();
                BusLocations busLocations = gson.fromJson(String.valueOf(jsonString), BusLocations.class);
                List<Result> result = busLocations.getResult();

                for (Result temp : result) {
                    double lat = temp.getLat();
                    double lon = temp.getLon();
                    LatLng tempLatLng = new LatLng(lat, lon);
                    String tempLine = temp.getLines();
                    String tempBrigade = temp.getBrigade();
                    Bus tempBus = new Bus(tempLatLng, tempLine, tempBrigade);
                    processedBuses.add(tempBus);
                }
            } catch (Exception e) {
                processedBuses = new ArrayList<>();

            }

            // De-serialize the JSON string into an array of objects

//            processedBuses = busDownloader.createBusesFromJson(jsonString);

            readyToMove = new SparseArray<>();
            readyToDraw = new SparseArray<>();

            for (Bus bus : processedBuses) {
                Integer id = bus.hashCode();

                if (selectedBuses != null && selectedBuses.contains(bus.getLine())) {
                    if (markersHashMap.containsKey(id)) {

                        Marker marker = markersHashMap.get(id);
                        for (Bus tempBus : buses) {
                            if (tempBus.hashCode() == bus.hashCode() && !(tempBus.getCurrentLatLng().equals(bus.getCurrentLatLng()))) {
                                tempBus.updatePosition(bus.getCurrentLatLng());
                                bus = tempBus;
                            }
                        }
                        readyToMove.put(id, bus);

                        // Update your marker
                    } else {
                        buses.add(bus);
                        readyToDraw.put(id, bus);
                    }
                }
            }
            return null;
        }
    }
}