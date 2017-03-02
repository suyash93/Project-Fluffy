package gash.router.persistence;

import com.google.protobuf.ByteString;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.*;

public class CouchDBHandler {

    private static final String PERSISTENCE_DATABASE = "files_data";
    private static final Logger logger = LoggerFactory.getLogger(CouchDBHandler.class);


    /* Reading files from external DB */
    public static ByteString getFile(String id) {
        ByteString byteString = null;
        try {
            String stringUrl = "http://localhost:5984/" + PERSISTENCE_DATABASE + "/" + id;
            logger.info(stringUrl);
            URL url = new URL(stringUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }

            logger.info("Response code is " + connection.getResponseCode());
            JSONObject chunk = new JSONObject(result.toString());
            String chunkString = chunk.getString("File_Data");
            byte[] decoded = Base64.getDecoder().decode(chunkString);
            byteString = ByteString.copyFrom(decoded);
            return byteString;
        } catch (Exception e) {
            logger.info("Null");
            return null;
        }
    }

    /* Listing ids of all the files present */
    public static List<String> getids(String filename) {
        List<String> idList = new ArrayList<String>();
        String url = "http://localhost:5984/" + PERSISTENCE_DATABASE + "/_design/files_data/_view/NewView2?key=%22"
                + filename + "%22";
        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(url);
        getRequest.addHeader("accept", "application/json");
        try {
            HttpResponse response = client.execute(getRequest);
            String json_string = EntityUtils.toString(response.getEntity());
            JSONObject temp1 = new JSONObject(json_string);
            JSONArray array = temp1.getJSONArray("rows");

            for (int i = 0; i < array.length(); i++) {
                String id = array.getJSONObject(i).getString("id");
                idList.add(id);
            }
            return idList;

        } catch (Exception e) {
            return null;
        }
    }

    public static void deletefilewithname(String filename) throws Exception {
        // TODO Auto-generated method stub

        String url = "http://localhost:5984/files_data/_design/files_data/_view/View2?key=%22" + filename + "%22";

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(url);
        getRequest.addHeader("accept", "application/json");
        HttpResponse response = httpClient.execute(getRequest);
        BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
        String output;

        System.out.println("Output from Server .... \n");

        StringBuilder responseStrBuilder = new StringBuilder();

        while ((output = br.readLine()) != null) {
            responseStrBuilder.append(output);
            System.out.println(output);
        }
        JSONObject jsonObject = new JSONObject(responseStrBuilder.toString());
        Map<String, String> id2Rev = new HashMap<String, String>();
        List<String> IDList = new ArrayList<String>();
        JSONArray array = jsonObject.getJSONArray("rows");
        for (int i = 0; i < array.length(); i++) {
            String key = array.getJSONObject(i).getString("id");
            JSONObject JsonObj = array.getJSONObject(i).getJSONObject("value");
            String value = JsonObj.getString("Rev_ID");

            System.out.println("For ID:- " + key + ", Rev ID is:- " + value);
            IDList.add(array.getJSONObject(i).getString("id"));
            id2Rev.put(key, value);
        }

        Iterator it = id2Rev.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            System.out.println(pair.getKey() + " = " + pair.getValue());
            String constdburl = "http://localhost:5984/files_data/" + pair.getKey() + "?rev=" + pair.getValue();
            URL newurl = new URL(constdburl);
            HttpURLConnection httpCon = (HttpURLConnection) newurl.openConnection();
            httpCon.setDoOutput(true);
            httpCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpCon.setRequestMethod("DELETE");
            httpCon.connect();
            httpCon.getInputStream();
            it.remove();
        }

    }

    public static void updateFile(String fileName, ByteString in, int numberOfChunks, int chunkId) {
        if (chunkId == 0) {
            try {
                // Deleting the file once chunk number 0 is received
                deletefilewithname(fileName);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        addFile(fileName, in, numberOfChunks, chunkId);
    }

    // Adding chunked files in Database
    public static void addFile(String fileName, ByteString in, int numberOfChunks, int chunkId) {
        try {
            byte[] byteArray = in.toByteArray();
            String encoded = Base64.getEncoder().encodeToString(byteArray);
            JSONObject object = new JSONObject();
            object.put("FileName", fileName);
            object.put("File_Data", encoded);
            object.put("noOfChunks", numberOfChunks);
            object.put("chunkID", chunkId);
            String stringUrl = "http://localhost:5984/files_data/" + fileName + chunkId;
            URL url = new URL(stringUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PUT");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            OutputStreamWriter osw = new OutputStreamWriter(connection.getOutputStream());
            osw.write(object.toString());
            osw.flush();
            osw.close();
            logger.info("Response code is " + connection.getResponseCode());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}