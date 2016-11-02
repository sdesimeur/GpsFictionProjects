package com.sdesimeur.android.gpsfiction.classes;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.text.format.Time;

import com.sdesimeur.android.gpsfiction.activities.GpsFictionActivity;
import com.sdesimeur.android.gpsfiction.geopoint.MyGeoPoint;

import java.util.HashMap;
import java.util.HashSet;

public class MyLocationListener implements LocationListener, SensorEventListener {
    private static final double LATITUDE = 49.59266;
    private static final double LONGITUDE = 3.65649;
    private final static float MINBEARINGCHANGED = 5;
    private final static float SPEEDLIMIT = 0.003f;
    private HashMap<MyLocationListener.REGISTER, HashSet<PlayerBearingListener>> playerBearingListener = null;
    private HashMap<MyLocationListener.REGISTER, HashSet<PlayerLocationListener>> playerLocationListener = null;
    //private static final HashSet <PlayerLocationListener> playerLocationListener = new HashSet <PlayerLocationListener> ();
    //private static final HashSet <PlayerLocationListener> playerLocationListenerZone = new HashSet <PlayerLocationListener> ();
    private MyGeoPoint lastPlayerGeoPoint = null;
    private Time lastTimePlayerGeoPoint = null;
    private MyGeoPoint playerGeoPoint = null;
    private Time timePlayerGeoPoint = null;
    private boolean firstLocation;
    private LocationManager locationManager = null;
    //	private static final MyLocationListener staticLocationListener=new MyLocationListener();
//	private static final SensorEventListener staticCompassListener = staticLocationListener;
    private boolean compassActive;
    private float bearingOfPlayer;
    private float compassBearing;
    private float locationBearing;
    private SensorManager sensorManager = null;
    private Sensor sensorsOrientation = null;
    private GpsFictionActivity gpsFictionActivity = null;

    public MyLocationListener() {
        this.playerBearingListener = new HashMap<>();
        this.playerLocationListener = new HashMap<>();
        for (REGISTER i : REGISTER.values()) {
            this.playerBearingListener.put(i, new HashSet<PlayerBearingListener>());
            this.playerLocationListener.put(i, new HashSet<PlayerLocationListener>());
        }
        this.lastPlayerGeoPoint = new MyGeoPoint();
        this.lastTimePlayerGeoPoint = new Time();
        this.playerGeoPoint = new MyGeoPoint();
        this.timePlayerGeoPoint = new Time();
        this.firstLocation = true;
        this.compassActive = false;
        this.bearingOfPlayer = 0;
        this.compassBearing = 0;
        this.locationBearing = 0;
    }

    public HashSet<PlayerBearingListener> getPlayerBearingListener(REGISTER i) {
        return playerBearingListener.get(i);
    }

    public HashSet<PlayerLocationListener> getPlayerLocationListener(REGISTER i) {
        return playerLocationListener.get(i);
    }

    public Bundle getByBundle() {
        Bundle dest = new Bundle();
        dest.putFloat("bop", this.bearingOfPlayer);
        dest.putFloat("cb", this.compassBearing);
        dest.putFloat("lb", this.locationBearing);
        double[] coord = {this.playerGeoPoint.getLatitude(), this.playerGeoPoint.getLongitude()};
        dest.putDoubleArray("pgp", coord);
        coord = new double[]{this.lastPlayerGeoPoint.getLatitude(), this.lastPlayerGeoPoint.getLongitude()};
        dest.putDoubleArray("lpgp", coord);
        dest.putLong("tpgp", this.timePlayerGeoPoint.toMillis(true));
        dest.putLong("ltpgp", this.lastTimePlayerGeoPoint.toMillis(true));
        return dest;
    }

    public void setByBundle(Bundle in) {
        this.bearingOfPlayer = in.getFloat("bop");
        this.compassBearing = in.getFloat("cb");
        this.locationBearing = in.getFloat("lb");
        double[] coord = in.getDoubleArray("pgp");
        this.playerGeoPoint = new MyGeoPoint(coord[0], coord[1]);
        coord = in.getDoubleArray("lpgp");
        this.lastPlayerGeoPoint = new MyGeoPoint(coord[0], coord[1]);
        this.timePlayerGeoPoint.set(in.getLong("tpgp"));
        this.lastTimePlayerGeoPoint.set(in.getLong("ltpgp"));
    }

    public void init(GpsFictionActivity gpsFictionActivity) {
        this.gpsFictionActivity = gpsFictionActivity;
        this.startLocationListener();

    }

    /*static public enum SENSORTOREGISTER  {
        BEARING,
        LOCATION
    }*/
    public void addPlayerLocationListener(REGISTER type, PlayerLocationListener listener) {
        this.playerLocationListener.get(type).add(listener);
        if (playerGeoPoint != null) listener.onLocationPlayerChanged(playerGeoPoint);
    }

    public void removePlayerLocationListener(REGISTER type, PlayerLocationListener listener) {
        this.playerLocationListener.get(type).remove(listener);
    }

    public void addPlayerBearingListener(REGISTER type, PlayerBearingListener listener) {
        this.playerBearingListener.get(type).add(listener);
            listener.onBearingPlayerChanged(bearingOfPlayer);
    }

    public void removePlayerBearingListener(REGISTER type, PlayerBearingListener listener) {
        this.playerBearingListener.get(type).remove(listener);
    }

    public void firePlayerLocationListener() {
        if (playerGeoPoint != null)
        for (REGISTER i : REGISTER.values()) {
            for (PlayerLocationListener listener : this.playerLocationListener.get(i)) {
                listener.onLocationPlayerChanged(playerGeoPoint);
            }
        }
    }

    public void firePlayerBearingListener() {
        for (REGISTER i : REGISTER.values()) {
            for (PlayerBearingListener listener : this.playerBearingListener.get(i)) {
                listener.onBearingPlayerChanged(bearingOfPlayer);
            }
        }
    }

    public LocationManager getLocationManager() {
        return this.locationManager;
    }

    public void onLocationChanged(Location location) {
        this.setNewPlayerGeopoint(location);
        this.firePlayerLocationListener();
    }

    @Override
    public void onProviderDisabled(String provider) {
//TODO 	onProviderDisabled
    }

    @Override
    public void onProviderEnabled(String provider) {
//TODO onProviderEnabled
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
//TODO onStatusChanged
    }

    private void setNewPlayerGeopoint(Location location) {
        if (this.firstLocation) {
            this.firstLocation = false;
            this.timePlayerGeoPoint.setToNow();
            //this.playerGeoPoint.setGeoPoint(location);
            this.playerGeoPoint = new MyGeoPoint(location);
        }
        this.lastTimePlayerGeoPoint.set(this.timePlayerGeoPoint);
        //this.lastPlayerGeoPoint.setGeoPoint(this.playerGeoPoint);
        this.lastPlayerGeoPoint = new MyGeoPoint(this.playerGeoPoint);
        this.timePlayerGeoPoint.setToNow();
        //this.playerGeoPoint.setGeoPoint(location);
        this.playerGeoPoint = new MyGeoPoint(location);
        this.locationBearing = this.lastPlayerGeoPoint.bearingTo(this.playerGeoPoint);
        this.setCompassActive();
    }

    private void calculateNewPlayerBearing() {
        // TODO Auto-generated method stub
        if (this.compassActive) {
            this.bearingOfPlayer = this.compassBearing;
        } else {
            this.bearingOfPlayer = this.locationBearing;
        }
        this.firePlayerBearingListener();
    }

    public void startLocationListener() {
        //TODO verifier que le GPS est active, sinon lancer une boite de dialog pour le faire activer
        this.locationManager = (LocationManager) this.getGpsFictionActivity().getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this.getGpsFictionActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this.getGpsFictionActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 200, 0.5f, this);
        Location location = this.locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location == null) {
            this.playerGeoPoint = new MyGeoPoint(LATITUDE, LONGITUDE);
        } else {
            this.playerGeoPoint = new MyGeoPoint(location);
        }
        this.lastPlayerGeoPoint = this.playerGeoPoint;
        this.sensorManager = (SensorManager) this.getGpsFictionActivity().getSystemService(Context.SENSOR_SERVICE);

    }

    public GpsFictionActivity getGpsFictionActivity() {
        return this.gpsFictionActivity;
    }

    public void removeGpsFictionUpdates() {
        if (ActivityCompat.checkSelfPermission(this.getGpsFictionActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this.getGpsFictionActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        this.locationManager.removeUpdates(this);
    }

    public MyGeoPoint getPlayerGeoPoint() {
        return this.playerGeoPoint;
    }

    public boolean isCompassActive() {
        return this.compassActive;
    }

    public void setCompassActive() {
        if (this.sensorManager != null) {
            float deltaTimeInMilliSeconds = (float) (this.timePlayerGeoPoint.toMillis(true) - this.lastTimePlayerGeoPoint.toMillis(true));
            float speed = 3600 * this.lastPlayerGeoPoint.distanceTo(this.playerGeoPoint) / deltaTimeInMilliSeconds;
            this.compassActive = (speed < SPEEDLIMIT);
        } else {
            this.compassActive = false;
        }
        if (this.compassActive) {
            this.sensorsOrientation = this.sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
            this.sensorManager.registerListener(this, this.sensorsOrientation, SensorManager.SENSOR_DELAY_GAME);
        } else {
            if (this.sensorManager != null) {
                this.sensorManager.unregisterListener(this, this.sensorsOrientation);
            }
        }
        this.calculateNewPlayerBearing();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // TODO Auto-generated method stub
        float newCompassBearing = event.values[0];
        float diffBearing = Math.abs(newCompassBearing - this.compassBearing);
        if (!((diffBearing < MINBEARINGCHANGED) || (diffBearing > (360 - MINBEARINGCHANGED)))) {
            this.compassBearing = newCompassBearing;
            this.calculateNewPlayerBearing();
        }
    }

    public float getBearingOfPlayer() {
        return this.bearingOfPlayer;
    }

    public void clearFragmentListener() {
        this.getPlayerBearingListener(REGISTER.FRAGMENT).clear();
        this.getPlayerLocationListener(REGISTER.FRAGMENT).clear();
    }

    public void clearZoneListener() {
        this.getPlayerBearingListener(REGISTER.ZONE).clear();
        this.getPlayerLocationListener(REGISTER.ZONE).clear();
    }

    public void clearViewListener() {
        this.getPlayerBearingListener(REGISTER.VIEW).clear();
        this.getPlayerLocationListener(REGISTER.VIEW).clear();
    }

    public void clearAllListener() {
        this.clearFragmentListener();
        this.clearViewListener();
        //this.clearZoneListener();
    }

    static public enum REGISTER {
        MARKER,
        ZONE,
        HOLDERVIEW,
        ADAPTERVIEW,
        VIEW,
        LAYOUT,
        FRAGMENT
    }
}
