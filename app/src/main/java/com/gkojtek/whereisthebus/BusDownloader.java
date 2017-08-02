package com.gkojtek.whereisthebus;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class BusDownloader {

    private String previousJson;


    public static final String BUS_POSITIONS_URL = "https://api.um.warszawa.pl/api/action/busestrams_get/?resource_id=%20f2e5503e-%20927d-4ad3-9500-4ab9e55deb59&apikey=41207ac7-eefe-4b5b-87d8-0704cdec0620&type=1";
    public static final String TRAM_POSITIONS_URL = "https://api.um.warszawa.pl/api/action/busestrams_get/?resource_id=%20f2e5503e-%20927d-4ad3-9500-4ab9e55deb59&apikey=41207ac7-eefe-4b5b-87d8-0704cdec0620&type=2";

    public String downloadJson() throws IOException {

        HttpURLConnection conn = null;
        final StringBuilder json = new StringBuilder();
        try {
            // Connect to the web service

            URL url = new URL(BUS_POSITIONS_URL);
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Read the JSON data into the StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                json.append(buff, 0, read);
            }

        } catch (IOException e) {
            throw new IOException("Error connecting to service", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return previousJson = json.toString();

    }


}
