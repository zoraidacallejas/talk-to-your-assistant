package voiceactivity.speechtekbot;

/*
 *  Code adapted from examples in the Book:
 *  Reto Meier: Professional Android 4 Application Development, Chapter 13. Wrox, 2012
 *  
 */
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class FindLocation implements LocationListener {
	
	private LocationManager locationManager;
    private double latitude;
    private double longitude;
    private Criteria criteria;
    private String provider;
    
    // fire the updateWithNewLocation method whenever a location change is detected
    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
          updateWithNewLocation(location);
        }
        public void onProviderDisabled(String provider) {}
        public void onProviderEnabled(String provider) {}
        public void onStatusChanged(String provider, int status, 
                                    Bundle extras) {}
      };

    public FindLocation(Context context) {
        locationManager = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);
        
        criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(true);
        provider = locationManager.getBestProvider(criteria, true);
       
        Location location = locationManager.getLastKnownLocation(provider);
        updateWithNewLocation(location);
        
        // updates restricted to every 2 seconds and only when movement
        // of more than 10 metres has been detected
        locationManager.requestLocationUpdates(provider,2000,10,locationListener);
    }
	    
    private void updateWithNewLocation(Location location) {
 if (location != null) {            
             latitude = location.getLatitude();
             longitude = location.getLongitude();
           }
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
}