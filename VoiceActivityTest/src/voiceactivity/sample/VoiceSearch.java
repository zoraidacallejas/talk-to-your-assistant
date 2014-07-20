package voiceactivity.sample;
/*
 *  Copyright 2014 Zoraida Callejas and Michael McTear
 * 
 *  Shared in GitHub: <https://github.com/zoraidacallejas/talk-to-your-assistant>
 *  
 *  If you want to learn more about speech apps development in Android, take a look at our book:
 *  Voice Application Development for Android, Michael McTear and Zoraida Callejas, 
 *  PACKT Publishing 2013 <http://www.packtpub.com/voice-application-development-for-android/book>,
 *  <http://lsi.ugr.es/zoraida/androidspeechbook>
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.

 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.

 *  You should have received a copy of the GNU General Public License
 *   along with this program. If not, see <http://www.gnu.org/licenses/>. 
 *   
 *  This file is an update of the contents of the book:
 *  Voice Application Development for Android, Michael McTear and Zoraida Callejas, 
 *  PACKT Publishing 2013 <http://www.packtpub.com/voice-application-development-for-android/book>,
 *  <http://lsi.ugr.es/zoraida/androidspeechbook>
 */

import java.util.ArrayList;
import java.util.Locale;

import voiceactivity.lib.VoiceActivity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PorterDuff;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


/**
 * VoiceSearch: initiates a search query based on the words spoken by the user. 
 * It uses the VoiceActivity class from the VoiceActivityLib
 * 
 * @author Zoraida Callejas
 * @author Michael McTear
 * @version 3.1, 07/16/14
 *
 */
public class VoiceSearch extends VoiceActivity {

    private static final String LOGTAG = "VOICESEARCH";
    private static Integer ID_PROMPT_QUERY = 0;
    private static Integer ID_PROMPT_INFO = 1;
	
	/**
	 * Sets up the activity initializing the GUI, the ASR and TTS
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		//Set layout
		setContentView(R.layout.voicesearch);
		
		//Initialize the speech recognizer and synthesizer
		initSpeechInputOutput(getApplicationContext());	
				
		//Set up the speech button
		setSpeakButton();

	}

	/**
	 * Initializes the search button and its listener. When the button is pressed, a feedback is shown to the user
	 * and the recognition starts
	 */
	private void setSpeakButton() {
		// gain reference to speak button
		Button speak = (Button) findViewById(R.id.speech_btn);
		speak.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					//If the user is running the app on a virtual device, they get a Toast
					if("generic".equals(Build.BRAND.toLowerCase(Locale.US))){
						Toast.makeText(getApplicationContext(),"ASR is not supported on virtual devices", Toast.LENGTH_SHORT).show();
						Log.e(LOGTAG, "ASR attempt on virtual device");		
						changeButtonAppearanceToDefault();
					}
					else {
						//Show a feedback to the user indicating that the app has started to listen
						indicateListening(); //Starts listening when done
					}
				}
			});
	}
	
	/**
	 * Starts listening for any user input.
	 * When it recognizes something, the <code>processAsrResult</code> method is invoked. 
	 * If there is any error, the <code>processAsrError</code> method is invoked.
	 */
	private void startListening(){
		
		if(deviceConnectedToInternet()){
			try {
				
				/*Start listening, with the following default parameters:
					* Language = English
					* Recognition model = Free form, 
					* Number of results = 1 (we will use the best result to perform the search)
					*/
				listen(Locale.ENGLISH, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM, 1); //Start listening
			} catch (Exception e) {
				this.runOnUiThread(new Runnable() {  //Toasts must be in the main thread
						public void run() {
							Toast.makeText(getApplicationContext(),"ASR could not be started", Toast.LENGTH_SHORT).show();
							changeButtonAppearanceToDefault();
						}
				});
				
				Log.e(LOGTAG,"ASR could not be started");
				try { speak("Speech recognition could not be started", "EN", ID_PROMPT_INFO); } catch (Exception ex) { Log.e(LOGTAG, "TTS not accessible"); }
	
			}	
		} else {
			
			this.runOnUiThread(new Runnable() { //Toasts must be in the main thread
				public void run() {
					Toast.makeText(getApplicationContext(),"Please check your Internet connection", Toast.LENGTH_SHORT).show();
					changeButtonAppearanceToDefault();
				}
			});	
			try { speak("Please check your Internet connection", "EN", ID_PROMPT_INFO); } catch (Exception ex) { Log.e(LOGTAG, "TTS not accessible"); }
			Log.e(LOGTAG, "Device not connected to Internet");
				
		}
	}

	/**
	 * Provides feedback to the user to show that the app is listening:
	 * 		* It changes the color and the message of the speech button
	 *      * It synthesizes a voice message
	 */
	private void indicateListening() {
		Button button = (Button) findViewById(R.id.speech_btn); //Obtains a reference to the button
		button.setText(getResources().getString(R.string.speechbtn_listening)); //Changes the button's message to the text obtained from the resources folder
		button.getBackground().setColorFilter(getResources().getColor(R.color.speechbtn_listening),PorterDuff.Mode.MULTIPLY); //Changes the button's background to the color obtained from the resources folder
		try {
			speak(getResources().getString(R.string.initial_prompt), "EN", ID_PROMPT_QUERY);
		} catch (Exception e) {
			Log.e(LOGTAG, "TTS not accessible");
			changeButtonAppearanceToDefault();
		} 
	}
	
	/**
	 * Provides feedback to the user to show that the app is performing a search:
	 * 		* It changes the color and the message of the speech button
	 *      * It synthesizes a voice message
	 */
	private void indicateSearch(String criteria) {
		changeButtonAppearanceToDefault();
		try {
			speak(getResources().getString(R.string.searching_prompt)+criteria, "ES", ID_PROMPT_INFO);
		} catch (Exception e) {
			Log.e(LOGTAG, "TTS not accessible");	
		} 
	}
	
	/**
	 * Provides feedback to the user to show that the app is idle:
	 * 		* It changes the color and the message of the speech button
	 */	
	private void changeButtonAppearanceToDefault(){
		Button button = (Button) findViewById(R.id.speech_btn); //Obtains a reference to the button
		button.setText(getResources().getString(R.string.speechbtn_default)); //Changes the button's message to the text obtained from the resources folder
		button.getBackground().setColorFilter(getResources().getColor(R.color.speechbtn_default),PorterDuff.Mode.MULTIPLY);	//Changes the button's background to the color obtained from the resources folder		
	}
	
	/**
	 * Provides feedback to the user (by means of a Toast and a synthesized message) when the ASR encounters an error
	 */
	@Override
	public void processAsrError(int errorCode) {
	
		changeButtonAppearanceToDefault();
		
		String errorMessage;
		switch (errorCode) 
        {
	        case SpeechRecognizer.ERROR_AUDIO: 
	        	errorMessage = "Audio recording error"; 
	            break;
	        case SpeechRecognizer.ERROR_CLIENT: 
	        	errorMessage = "Client side error"; 
	            break;
	        case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: 
	        	errorMessage = "Insufficient permissions" ; 
	            break;
	        case SpeechRecognizer.ERROR_NETWORK: 
	        	errorMessage = "Network related error" ;
	            break;
	        case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:                
	            errorMessage = "Network operation timeout"; 
	            break;
	        case SpeechRecognizer.ERROR_NO_MATCH: 
	        	errorMessage = "No recognition result matched" ; 
	        	break;
	        case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: 
	        	errorMessage = "RecognitionServiceBusy" ; 
	            break;
	        case SpeechRecognizer.ERROR_SERVER: 
	        	errorMessage = "Server sends error status"; 
	            break;
	        case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: 
	        	errorMessage = "No speech input" ; 
	            break;
	        default:
	        	errorMessage = "ASR error";
	        	break;
        }
		
		this.runOnUiThread(new Runnable() { //Toasts must be in the main thread
			public void run() {
				Toast.makeText(getApplicationContext(), "Speech recognition error", Toast.LENGTH_LONG).show();
			}
		});
		
        Log.e(LOGTAG, "Error when attempting to listen: "+ errorMessage);
        try { speak(errorMessage,"EN", ID_PROMPT_INFO); } catch (Exception e) { Log.e(LOGTAG, "TTS not accessible"); }
	}

	/**
	 * Invoked when the ASR is ready to start listening.
	 * In this case we just write it in the log.
	 */
	@Override
	public void processAsrReadyForSpeech() { 
		//Not interested in handling this in this app
		Log.d(LOGTAG, "ASR ready for speech");
	}

	/**
	 * Initiates a Google search intent with the results of the recognition
	 */
	@Override
	public void processAsrResults(ArrayList<String> nBestList, float[] nBestConfidences) {
		
		if(nBestList!=null){

			Log.d(LOGTAG, "ASR found "+nBestList.size()+" results");
		
			if(nBestList.size()>0){
				String bestResult = nBestList.get(0); //We will use the best result
				indicateSearch(bestResult); //Provides feedback to the user that search is going to be started
				
				changeButtonAppearanceToDefault();
				
				googleText(bestResult);
			}
		}
	}
	
	/**
	 * Starts a google query with the text
	 * @param criterion text to be used as search criterion
	 */
	private void googleText(String criterion)
	{
		if(deviceConnectedToInternet())
		{
			//Carries out a web search with the words recognized				
			PackageManager pm = getPackageManager();
			Intent intent = new Intent();
			intent.putExtra(SearchManager.QUERY, criterion);
			intent.setAction(Intent.ACTION_WEB_SEARCH);
			ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY );
			startActivity(intent); 
	
			if( resolveInfo == null )
				Log.e(LOGTAG, "Not possible to carry out ACTION_WEB_SEARCH Intent");	
		}
		else {
			this.runOnUiThread(new Runnable() { //Toasts must be in the main thread
				public void run() {
					Toast.makeText(getApplicationContext(),"Please check your Internet connection", Toast.LENGTH_LONG).show(); //Not possible to carry out the intent
				}
			});
			try { speak("Please check your Internet connection","EN", ID_PROMPT_INFO); } catch (Exception e) { Log.e(LOGTAG, "TTS not accessible"); }
			Log.e(LOGTAG, "Device not connected to Internet");	
		}
	}
	
	/**
	 * Checks whether the device is connected to Internet (returns true) or not (returns false)
	 * From: http://developer.android.com/training/monitoring-device-state/connectivity-monitoring.html
	 */
	public boolean deviceConnectedToInternet() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);  
	    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
	    return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
	}
	
	/**
	 * Shuts down the TTS engine when finished
	 */   
	@Override
	public void onDestroy() {
		super.onDestroy();
		shutdown();
	}

	/**
	 * Invoked when the TTS has finished synthesizing.
	 * 
	 * In this case, it starts recognizing if the message that has just been synthesized corresponds to a question (its id is ID_PROMPT_QUERY),
	 * and does nothing otherwise.
	 * 
	 * @param uttId identifier of the prompt that has just been synthesized (the id is indicated in the speak method when the text is sent
	 * to the TTS engine)
	 */
	@Override
	public void onTTSDone(String uttId) {
		if(uttId.equals(ID_PROMPT_QUERY.toString())) //Only starts listening after the first question
			startListening();
		
	}

	/**
	 * Invoked when the TTS encounters an error.
	 * 
	 * In this case it just writes in the log.
	 */
	@Override
	public void onTTSError(String uttId) {
		Log.e(LOGTAG, "TTS error");
	}

	/**
	 * Invoked when the TTS starts synthesizing
	 * 
	 * In this case it just writes in the log.
	 */
	@Override
	public void onTTSStart(String uttId) {
		Log.e(LOGTAG, "TTS starts speaking");
	}
} 
