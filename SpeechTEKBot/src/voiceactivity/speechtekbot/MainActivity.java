package voiceactivity.speechtekbot;

/*
 *  Copyright 2014 Zoraida Callejas and Michael McTear
 * 
 *  This file is shared in GitHub: <https://github.com/zoraidacallejas/talk-to-your-assistant>
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
 */

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import voiceactivity.lib.VoiceActivity;

import android.content.Context;
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


/**
 * Chatbot/VPA that uses the technology of Pandorabots to understand the user queries and provide information
 * 
 * @author Michael McTear
 * @author Zoraida Callejas
 * @version 2.0, 07/20/14
 *
 */

public class MainActivity extends VoiceActivity {

    private static final String LOGTAG = "SpeechTEKBot";
    private static Integer ID_PROMPT_QUERY = 0;	//Id chosen to identify the prompts that involve posing questions to the user
    private static Integer ID_PROMPT_INFO = 1;	//Id chosen to identify the prompts that involve only informing the user
	
	/**
	 * Sets up the activity initializing the GUI, the ASR and TTS
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		//Set layout
		setContentView(R.layout.main);
		
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
						Log.e(LOGTAG, "ASR attempt on virtual device");						
					}
					else {
						//Show a feedback to the user indicating that the app has started to listen
						indicateListening();  
						startListening();
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
					* Recognition model = Free form, 
					* Number of results = 1 (we will use the best result to perform the search)
					*/
				listen(Locale.ENGLISH, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM, 1); //Start listening
			} catch (Exception e) {
				Log.e(LOGTAG, e.getMessage());
			}	
		} else {
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
	 * Provides feedback to the user when the ASR encounters an error
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
		
        Log.e(LOGTAG, "Error when attempting to listen: "+ errorMessage);
		
		try {
			speak(errorMessage,"EN", ID_PROMPT_INFO);
		} catch (Exception e) {
			Log.e(LOGTAG, "English not available for TTS, default language used instead");
		}

	}

	@Override
	public void processAsrReadyForSpeech() { }

	/**
	 * Initiates interaction with Pandorabots with the results of the recognition
	 */
	@Override
	public void processAsrResults(ArrayList<String> nBestList, float[] nBestConfidences) {
		
		if(nBestList!=null){
			if(nBestList.size()>0){
				String bestResult = nBestList.get(0); //We will use the best result
				Log.d(LOGTAG, "Speech input: " + bestResult);
				changeButtonAppearanceToDefault();
				
				/**
				 * EXERCISE 1: comment out the following section for exercises 2 onwards
				 */
				
				
				try {
					speak(bestResult,"EN",ID_PROMPT_INFO);
				} catch (Exception e) {
					Log.e(LOGTAG, "The message '"+bestResult+"' could not be synthesized");
				}
				
				
				/**
				 * EXERCISE 2: uncomment the following section for exercises 2 onwards
				 */
				/*		
				// insert %20 for spaces in query
				bestResult = bestResult.replaceAll(" ", "%20");
				new DoRequest(this).execute(bestResult);	//Initiates the query to Pandora bots, the result is processed in the "processBotResults" method
				*/
			}
		}
	}
	
	/**
	 * Processes the response from Pandorabots ALICE2v. This response can be a simple text with simple HTML tags, or a more complex
	 * text with <oob> tags that must be further processed.
	 * 
	 * @param result response from Pandorabots
	 */
	public void processBotResults(String result){
		
		OOBProcessor oob=new OOBProcessor(this, ID_PROMPT_INFO);
		Log.d(LOGTAG, "Response, contents of that: "+result);

		// Send responses with <oob> for further processing
		if(result.contains("<oob>")){
			try {
				oob.processOobOutput(result);
			} catch (Exception e) {
				Log.d(LOGTAG, e.getMessage());
			}
		}
		// Speak out simple text from Pandorabots after removing any HTML content
		else{
		
				result = removeTags(result);
				try {
					speak(result,"EN",ID_PROMPT_INFO);
				} catch (Exception e) {
					Log.e(LOGTAG, "The message '"+result+"' could not be synthesized");
				}
		}
		}
	
	
	/**
	 * Removes HTML tags from a string
	 * 
	 * @author http://stackoverflow.com/questions/240546/removing-html-from-a-java-string
	 * @param string text with html tags
	 * @return text without html tags
	 */
	private String removeTags(String string) {
		Pattern REMOVE_TAGS = Pattern.compile("<.+?>");

	    if (string == null || string.length() == 0) {
	        return string;
	    }

	    Matcher m = REMOVE_TAGS.matcher(string);
	    return m.replaceAll("");
	}
	
	
	/**
	 * Checks whether the device is connected to Internet (returns true) or not (returns false)
	 * 
	 * @author http://developer.android.com/training/monitoring-device-state/connectivity-monitoring.html
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
		if(uttId.equals(ID_PROMPT_QUERY.toString()))
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

