package voiceactivity.speechtekbot;

/** Copyright 2014 Dr Richard Wallace */

import android.util.Log;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class NetworkClient {
    private static final String TAG = "PandorabotsTalkAPIDemo";
    public static String baseUrl = "qa.pandorabots.com";
    public static String botId = "drwallace/speechtekbot"; 
    public static String custId = null;
    public NetworkClient(MainActivity main) {
    }
    private URL requestUrl(String input) {
        try {
            Log.i(TAG, "in Spec custId="+custId);
            String spec =
                    String.format("%s?botid=%s%s&input=%s&format=json",
                            "https://" + baseUrl + "/pandora/talk-xml",
                            botId,
                            custId == null ? "" : "&custid=" + custId,
                            URLEncoder.encode(input));
            Log.i(TAG, spec);
            return new URL(spec);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
    private String readResponse(BufferedReader reader) {
        StringBuilder sb = new StringBuilder();
        String NL = System.getProperty("line.separator");
        try {
            for (; ; ) {
                String line = reader.readLine();
                Log.i(TAG, "readResponse read: " + line);
                if (line == null) {
                    break;
                }
                sb.append(line).append(NL);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return sb.toString();
    }
   
    public String doServerRequest(String input) {
        String response = "Network unreachable";
        try {
            if (botId == null) {
                Log.i(TAG, "For some reason bot Id is null");
                response = "Unable to locate bot";
            } else {
                HttpURLConnection conn =
                        (HttpURLConnection) requestUrl(input).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setReadTimeout(60000);
                String userAgent = System.getProperty("http.agent");
                Log.i(TAG, "HTTPUrlConnection user-agent=" + userAgent);
                conn.connect();
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(conn.getInputStream()));
                String responseContent = readResponse(reader);
                Log.i(TAG, "responseContent=" + responseContent);
                try {
                    JSONObject jsonObj = new JSONObject(responseContent);
                    response = jsonObj.getString("that");
                    custId = jsonObj.getString("custid");
                    Log.i(TAG, "From JSON custId="+custId);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                conn.disconnect();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        		
        return response;
    }
}

