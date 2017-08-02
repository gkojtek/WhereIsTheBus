package com.gkojtek.whereisthebus;

import android.app.IntentService;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.gkojtek.whereisthebus.model.Bus;
import com.gkojtek.whereisthebus.model.BusLocations;
import com.gkojtek.whereisthebus.model.Result;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MonitorBusesIntentService extends IntentService {

    private String BUS_POSITIONS_URL = "https://api.um.warszawa.pl/api/action/busestrams_get/?resource_id=%20f2e5503e-%20927d-4ad3-9500-4ab9e55deb59&apikey=41207ac7-eefe-4b5b-87d8-0704cdec0620&type=1";
    private BusDownloader busDownloader;
    private ArrayList<Bus> processedBuses;
    private String INTENTSERVICE = "IntentService";
    private String jsonString;
    private GeofencingClient geofencingClient;
    private ArrayList<Geofence> geofenceArrayList;

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    public MonitorBusesIntentService() {
        super("MonitorBusesIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        busDownloader = new BusDownloader();
        geofencingClient = LocationServices.getGeofencingClient(this);
        Log.d(INTENTSERVICE, "onHandleIntent");

        ArrayList<String> alarmList = intent.getStringArrayListExtra("chosen");
        Log.d(INTENTSERVICE, "przekazano - wielkość: " + String.valueOf(alarmList.size()));

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
                Log.d(INTENTSERVICE, "run GoRunnable");
                try {
                    jsonString = busDownloader.downloadJson();
                    new ProcessData().execute(jsonString);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return new GoRunnable(url);
    }

    private class ProcessData extends AsyncTask<String, String, Void> {

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
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

            for (Bus bus : processedBuses) {
                Integer id = bus.hashCode();

                Log.d(INTENTSERVICE, bus.getLine());

//                if (selectedBuses != null && selectedBuses.contains(bus.getLine())) {
//                    if (markersHashMap.containsKey(id)) {
//
//                        Marker marker = markersHashMap.get(id);
//                        for (Bus tempBus : buses) {
//                            if (tempBus.hashCode() == bus.hashCode() && !(tempBus.getCurrentLatLng().equals(bus.getCurrentLatLng()))) {
//                                tempBus.updatePosition(bus.getCurrentLatLng());
//                                bus = tempBus;
//                            }
//                        }
//                        readyToMove.put(id, bus);
//
//                        // Update your marker
//                    } else {
//                        buses.add(bus);
//                        readyToDraw.put(id, bus);
//                    }
//                }
            }
            return null;
        }
    }

}
