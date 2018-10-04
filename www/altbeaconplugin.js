function AltbeaconPlugin() {}

AltbeaconPlugin.prototype.scan = function(successCallback, errorCallback) {
    var options = {};
    cordova.exec(successCallback, errorCallback, 'AltbeaconPlugin', 'scan', [options]);
}

AltbeaconPlugin.prototype.ads = function(url, duration, successCallback, errorCallback) {
    var options = {};
    options.url      = url;
    options.duration = duration; // em millisegundos
    cordova.exec(successCallback, errorCallback, 'AltbeaconPlugin', 'ads', [options]);
}

AltbeaconPlugin.install = function() {
    if (!window.plugins) {
      window.plugins = {};
    }
    window.plugins.altbeaconPlugin = new AltbeaconPlugin();
    return window.plugins.altbeaconPlugin;
};
cordova.addConstructor(AltbeaconPlugin.install);