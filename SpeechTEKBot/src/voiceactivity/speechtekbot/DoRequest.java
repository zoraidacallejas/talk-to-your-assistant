package voiceactivity.speechtekbot;

/** Copyright 2014 Dr Richard Wallace */

import android.os.AsyncTask;
import android.util.Log;

public class DoRequest extends AsyncTask<String, Void, String> {
    private final MainActivity main;
    private NetworkClient networkClient;
    private static final String TAG = "PandorabotsTalkAPIDemo";
    public DoRequest(MainActivity main) {
        super();
        this.main = main;
        this.networkClient = new NetworkClient(main);
    }
   
    @Override
    protected String doInBackground(String... strings) {
        return networkClient.doServerRequest(strings[0]);
    }
    @Override
    protected void onPostExecute(String result) {
        try {
            main.processBotResults(result);
        } catch (Exception ex) {
            Log.i(TAG, "Something went wrong with onPostExecute");
            main.processBotResults("Something went wrong with onPostExecute");
        }
    }
}
