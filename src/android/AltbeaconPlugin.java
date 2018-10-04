package com.rodrigo.davila.cordova.plugin;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.utils.UrlBeaconUrlCompressor;
import org.altbeacon.beacon.BeaconTransmitter;
import org.altbeacon.beacon.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Destroyable;

import java.util.Iterator;
import java.net.MalformedURLException;

import android.content.ServiceConnection;
import android.content.Intent;

import android.os.Handler;

public class AltbeaconPlugin extends CordovaPlugin {

    class MyBeacon {
        public String url;
        public String mac;
        public double distance;
        public long timestamp;
        public int rssi;
        public int txPower;
        

        public JSONObject toJSONObject() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("url", url);
                obj.put("mac", mac);
                obj.put("distance", distance);
                obj.put("timestamp", timestamp);
                obj.put("rssi", rssi);
                obj.put("txPower", txPower);
                return obj;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    protected static final String TAG = "VirtualCredentialScanner";
    private static final long BEACON_TIME_LIMIT = 30000;
    private BeaconManager beaconManager;
    private BeaconTransmitter beaconTransmitter;
    private BeaconConsumer consumer;
    private boolean isBluetoothEnable;
    private HashMap<String, MyBeacon> beaconsMap = new HashMap<String, MyBeacon>();


    private JSONArray toJSONArray(HashMap<String, MyBeacon> map) {
        JSONArray array = new JSONArray();
        Iterator it = beaconsMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            MyBeacon beacon = (MyBeacon)pair.getValue();
            array.put(beacon.toJSONObject());
        }
        return array;
    }
  
    @Override
    public boolean execute(String action, JSONArray args,
    final CallbackContext callbackContext) {
        
        try {
            if (action.equals("scan")) {
                JSONObject options = args.getJSONObject(0);
                removeOldBeacons();
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, toJSONArray(beaconsMap));
                callbackContext.sendPluginResult(pluginResult);

            }
            else if (action.equals("ads")) {
                JSONObject options = args.getJSONObject(0);
                String url    = options.getString("url");
                Long duration = options.getLong("duration");
                advertise(url, duration);
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
                callbackContext.sendPluginResult(pluginResult);

            } else {
                callbackContext.error("\"" + action + "\" is not a recognized action.");
                return false;
            }
        } catch (JSONException e) {
            callbackContext.error("Error encountered: " + e.getMessage());
            return false;
        }
        

        return true;
    }

    @Override
    public void onDestroy() {
        beaconManager.unbind(consumer);
    }

    @Override
    public void onPause(boolean m) {
        beaconManager.unbind(consumer);
    }

    @Override
    public void onResume(boolean m) {
        beaconManager.bind(consumer);
    }

    @Override
    protected void pluginInitialize() {

        cordova.requestPermission(this, 1, Manifest.permission.ACCESS_COARSE_LOCATION);
        cordova.requestPermission(this, 2, Manifest.permission.ACCESS_FINE_LOCATION);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        isBluetoothEnable = mBluetoothAdapter != null;

        if (isBluetoothEnable) {
            Context context = cordova.getActivity().getApplicationContext();
            beaconManager = BeaconManager.getInstanceForApplication(context);

            // Detect the URL frame:
            beaconManager.getBeaconParsers().add(new BeaconParser().
                    setBeaconLayout(BeaconParser.EDDYSTONE_URL_LAYOUT));

            consumer = new BeaconConsumer() {
                @Override
                public void onBeaconServiceConnect() {
                    beaconManager.addRangeNotifier(new RangeNotifier() {
                        @SuppressLint("LongLogTag")
                        @Override
                        public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                            if (beacons.size() > 0) {
                                for (Beacon beacon : beacons) {
                                    Log.i(TAG, "The beacon I see is beacon: " + beacon.getBluetoothAddress());
                                    Log.i(TAG, "The beacon I see is about " + beacon.getDistance() + " meters away.");
            
                                    if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x10) {
                                        // This is a Eddystone-URL frame
                                        String url = UrlBeaconUrlCompressor.uncompress(beacon.getId1().toByteArray());
                                        Log.d(TAG, "I see a beacon with an url: " + url +
                                                " at " + beacon.getDistance() + " meters away.");

                                        MyBeacon mybeacon = new MyBeacon();
                                        mybeacon.url = url;
                                        mybeacon.mac = beacon.getBluetoothAddress();
                                        mybeacon.distance = beacon.getDistance();
                                        mybeacon.timestamp = System.currentTimeMillis();
                                        mybeacon.rssi = beacon.getRssi();
                                        mybeacon.txPower = beacon.getTxPower();
                                        beaconsMap.put(url, mybeacon);                                        
                                    }
                                }

                                removeOldBeacons();

                            }
                        }
                    });
                                
                    try {
                        beaconManager.startRangingBeaconsInRegion(new Region("senior-range-id", null, null, null));
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public Context getApplicationContext() {
                    return cordova.getActivity().getApplicationContext();
                }

                @Override
                public void unbindService(ServiceConnection serviceConnection) {
                    cordova.getActivity().unbindService(serviceConnection);
                }

                @Override
                public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
                    return cordova.getActivity().bindService(intent, serviceConnection, i);
                }
            };

            beaconManager.bind(consumer);


        } else {
            Log.e(TAG, "Unable to initialize Bluetooth.");
        }

        Log.i(TAG, "startBeaconMonitorService initialized");
    }

    private void removeOldBeacons() {
        Iterator it = beaconsMap.entrySet().iterator();
        Long now = System.currentTimeMillis();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            if ((now - ((MyBeacon)pair.getValue()).timestamp) > BEACON_TIME_LIMIT) {
                it.remove(); // avoids a ConcurrentModificationException
                Log.d(TAG, "beacon with url: " + pair.getKey() + " removed due time limit");
            }
        }
    }

    private void advertise(String url, Long duration) {
        try {
            if (beaconTransmitter != null && beaconTransmitter.isStarted()) {
               beaconTransmitter.stopAdvertising();
            }

            beaconManager.unbind(consumer);            

            byte[] urlBytes = UrlBeaconUrlCompressor.compress(url);
            Identifier encodedUrlIdentifier = Identifier.fromBytes(urlBytes, 0, urlBytes.length, false);
            ArrayList<Identifier> identifiers = new ArrayList<Identifier>();
            identifiers.add(encodedUrlIdentifier);
            Beacon beacon = new Beacon.Builder()
                    .setIdentifiers(identifiers)
                    .setManufacturer(0x0118)
                    .setTxPower(-69)
                    .build();
            BeaconParser beaconParser = new BeaconParser()
                    .setBeaconLayout(BeaconParser.EDDYSTONE_URL_LAYOUT);
            beaconTransmitter = new BeaconTransmitter(cordova.getActivity().getApplicationContext(), beaconParser);
            beaconTransmitter.startAdvertising(beacon);
            Log.d(TAG, "beaconTransmitter started.....");

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "beaconTransmitter finished.");
                    if (beaconTransmitter != null && beaconTransmitter.isStarted()) {
                        beaconTransmitter.stopAdvertising();
                    }
                    beaconManager.bind(consumer);
                }
            }, duration);

        } catch (MalformedURLException e) {
            Log.d(TAG, "That URL cannot be parsed");
        }

    }

}