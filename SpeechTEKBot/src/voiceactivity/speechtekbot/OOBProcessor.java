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

import java.io.StringReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import voiceactivity.lib.VoiceActivity;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;


/**
 * The PandoraBot service returns a string as a result of a query to a certain bot. This string
 * may contain <oob> tags that demand further processing. OOBPRocessor contains the methods
 * that carry out such functionality.
 * 
 * @author Michael McTear
 * @author Zoraida Callejas
 * @version 2.0, 07/20/14
 */
public class OOBProcessor {

	Class<Activity> viewerActivity;
	Context ctx;
	private static final String LOGTAG = "OOBProcessor";
	Integer msgId;
	Exception exception = null;

	/**
	 * Constructor of the OOBProccesor
	 * @param ctx
	 * @param msgId
	 */
	public OOBProcessor(VoiceActivity ctx, Integer msgId) {
		this.ctx = ctx;
		this.msgId = msgId;
	}

	/**
	 * Parses the response from the Pandora service
	 * 
	 * @param output
	 * @throws Exception
	 *             when the bot is not able to synthesize a message or the
	 *             result cannot be parsed
	 */
	public void processOobOutput(String output) throws Exception {
		if (output != null) {
			// Save the content within the <oob> tags as oobContent
			// Remove oobContent from output and save as textToSpeak

			Pattern pattern = Pattern.compile("<oob>(.*)</oob>");
			Matcher matcher = pattern.matcher(output);
			if (matcher.find()) {
				String oobContent = matcher.group(1);
				String textToSpeak = output.replaceAll("<oob>" + oobContent
						+ "</oob>", "");
				Log.d(LOGTAG, "oobContent " + oobContent);
				Log.d(LOGTAG, "textToSpeak " + textToSpeak);
				processOobContent(oobContent, textToSpeak);
			}
		}
	}

	/**
	 * Processes the contents of the oob tag using XPath and carries out the
	 * corresponding action: find location in map, perform a web search, launch
	 * an app, indicate the battery level, or get directions
	 * It also synthesizes the message that accompanies the oob tag
	 * 
	 * @param oobContent string with the oob content
	 * @param textToSpeak text to be synthesized
	 * @throws Exception when the oob action fails
	 * 
	 */
	public void processOobContent(String oobContent, String textToSpeak) throws Exception {

			//Parse the oobContent to look for other tags (e.g. <map>, <search>...)
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			InputSource s = new InputSource(new StringReader(oobContent));
			Document doc = dBuilder.parse(s);

			doc.getDocumentElement().normalize();
			XPathFactory factory = XPathFactory.newInstance();
			XPath xpath = factory.newXPath();

			// map request extract address and do mapSearch()
			if (oobContent.contains("<map>")) {
				String mapText = null;
				double lat = 0;
				double lng = 0;

				if (oobContent.contains("<myloc>")) {
					mapText = (String) xpath.evaluate("//myloc", doc, XPathConstants.STRING);
					FindLocation findLocation = new FindLocation(ctx);
					lat = findLocation.getLatitude();
					lng = findLocation.getLongitude();
				} else {
					mapText = (String) xpath.evaluate("//map", doc, XPathConstants.STRING);
				}
				Log.d(LOGTAG, "MapText " + mapText);
				mapSearch(mapText, lat, lng, textToSpeak);
			}

			// perform a web search
			if (oobContent.contains("<search>")) {
				String queryText = null;
				queryText = (String) xpath.evaluate("//search", doc, XPathConstants.STRING);
				Log.d(LOGTAG, "QueryText " + queryText);
				search(queryText, textToSpeak);
			}

			// request to launch an app
			if (oobContent.contains("<launch>")) {
				String app = null;
				app = (String) xpath.evaluate("//launch", doc,
						XPathConstants.STRING);
				Log.d(LOGTAG, "App " + app);
				launchApp(app, textToSpeak);
			}

			// battery level request
			if (oobContent.contains("<battery>")) {
				batteryLevel();
			}

			//get direction to a place
			if (oobContent.contains("<directions>")) {
				String from = null;
				String to = null;
				from = (String) xpath.evaluate("//from", doc, XPathConstants.STRING);
				to = (String) xpath.evaluate("//to", doc, XPathConstants.STRING);
				Log.d(LOGTAG, "From " + from);
				Log.d(LOGTAG, "To " + to);

				getDirections(from, to, textToSpeak);
			}

	}

	/**
	 * Launches a map in which the location (lat,lng) is highlighted. It also synthesizes
	 * a message to describe the map
	 * 
	 * @param mapText text describing the map
	 * @param lat latitude of the position to be highlighted
	 * @param lng longitude of the position to be highlighted
	 * @param textToSpeak text to be synthesized along with the map
	 * @throws Exception when the query to the map fails
	 */
	private void mapSearch(String mapText, double lat, double lng, String textToSpeak) throws Exception {
		try {
			((VoiceActivity) ctx).speak(textToSpeak, "EN", msgId);
			mapText = mapText.replace(' ', '+');

			Intent geoIntent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse("geo:"+ lat + "," + lng + "?q=" + mapText));
			ctx.startActivity(geoIntent);

		} catch (Exception e) {
			Log.e(LOGTAG, "Map query for " + mapText + " failed");
			throw new Exception(e);
		}
	}

	/**
	 * Performs a web search for the indicated query and synthesizes a message that describes it
	 * @param query
	 * @param textToSpeak
	 * @throws Exception when the web search fails
	 */
	private void search(String query, String textToSpeak) throws Exception {
		try {
			Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
			intent.putExtra(SearchManager.QUERY, query);
			((VoiceActivity) ctx).speak(textToSpeak, "EN", msgId);
			ctx.startActivity(intent);
		} catch (Exception e) {
			Log.e(LOGTAG, "Search for '" + query + "' failed");
			throw new Exception(e);
		}
	}

	/** 
	 * Checks the battery level and synthesizes it
	 * @throws Exception when the query to the battery level fails
	 */
	private void batteryLevel() throws Exception {
		try {
			Intent batteryIntent = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
			int rawlevel = batteryIntent.getIntExtra("level", -1);
			double scale = batteryIntent.getIntExtra("scale", -1);
			double level = -1;
			int pct;

			if (rawlevel >= 0 && scale > 0) {
				level = rawlevel / scale;
				pct = (int) (level * 100);  //Conversion from the raw battery level to a percentage
				((VoiceActivity) ctx).speak("Your battery level is " + String.valueOf(pct) + "per cent", "EN", msgId);
			}
		} catch (Exception e) {
			Log.e(LOGTAG, "BatteryLevel failed");
			throw new Exception (e);
		}
	}

	/**
	 * Shows a map with the directions from the origin (from) to the destination (to). If
	 * the origin is null, then the current position of the device is used. It also synthesizes
	 * a message that accompanies the map
	 * @param from origin location
	 * @param to destination location
	 * @param textToSpeak message to be synthesized
	 */
	private void getDirections(String from, String to, String textToSpeak) {
		
		try {	
			
			if (from != null) {
				Uri uri = Uri.parse("http://maps.google.com/maps?saddr=" + from + "&daddr=" + to);
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				((VoiceActivity) ctx).speak(textToSpeak, "EN", msgId);
				ctx.startActivity(intent);
	
			} else
			// query just asked for directions 'to X'
			// so assume starting point is current location
			// get values for current location
			{
	
				FindLocation findLocation = new FindLocation(ctx);
				double lat = findLocation.getLatitude();
				double lng = findLocation.getLongitude();
				Uri uri = Uri.parse("http://maps.google.com/maps?saddr=" + lat + "," + lng + "&daddr=" + to);
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				((VoiceActivity) ctx).speak(textToSpeak, "EN", msgId);
				ctx.startActivity(intent);
	
			}
		
		}catch (Exception e){
			Log.e(LOGTAG, "TTS failed");
		}
	}

	/**
	 * Launches an app
	 * 
	 * @param app name of the app to be launched
	 * @throws Exception when the app cannot be launched
	 */

	@SuppressLint("DefaultLocale")
	private void launchApp(String app, String textToSpeak) throws Exception {
		
		PackageManager pm = ctx.getPackageManager();
		List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

		String auxAppName;
		String packageName = null;
		
		for (ApplicationInfo packageInfo : packages) {
			auxAppName = packageInfo.loadLabel(ctx.getPackageManager()).toString();
			
			if(auxAppName.trim().toLowerCase().equals(app.trim().toLowerCase())){
				packageName = packageInfo.packageName;
				break;
			}
		}

		// if app requested is not on the device, report to user
		if (packageName == null) {
			((VoiceActivity) ctx).speak("Could not find the app " + app, "EN", msgId);
		} 
		// it it is in the device, launch it
		else{
			((VoiceActivity) ctx).speak(textToSpeak, "EN", msgId);
			Intent launchApp = pm.getLaunchIntentForPackage(packageName);
			ctx.startActivity(launchApp);
		}
			
	}

}
