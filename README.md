# altbeacon-plugin #
The altbeacon-plugin its a cordova plugin that uses altbeacon library to scan and advertise Bluetooth Low Energy Beacons using the Eddystone protocol.

## How to compile ##
Follow the commands below to compile this plugin to your Cordova app. This plugin needs Cordova-plugin-android-support-v4 support.

- `cordova plugin add cordova-plugin-android-support-v4`
- `cordova plugin add ../altbeacon-plugin/`
- `cordova build --stacktrace`
- `cordova run android --device`

## How to use ##
Use the `scan` method to scan for nearby beacon. This method returns 30 seconds window of scanning.
```javascript
     window.plugins.altbeaconPlugin.scan(function(result) {
                for(var i=0; i<result.length; i++) {
                    var beacon = result[i];
                    console.log("I have found this beacon:")
                    console.log(beacon.url);
                    console.log(beacon.mac);
                    console.log(beacon.distance);
                    console.log(beacon.timestamp);
                    console.log(beacon.rssi);
                    console.log(beacon.txPower);
                }
            }, function(err) {
                ....
            });
```
Use the `ads` method to advertise new beacons. This method needs the advertise url and the duration of the advertise window in milliseconds.
``` javascript
window.plugins.altbeaconPlugin.ads("https://teste55.com/HhHhss", 10000, function(result) {
                ...
            }, function(err) {
                ...
            });
```
