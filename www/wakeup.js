var exec = require("cordova/exec");

/**
 * This is a global variable called wakeup exposed by cordova
 */    
var Wakeup = function(){};

Wakeup.prototype.schedule = function(success, error, options) {
    exec(success, error, "WakeupPlugin", "schedule", [options]);
};

Wakeup.prototype.cancel = function(success, error, ids) {
    ids = Array.isArray(ids) ? Array.from(ids) : [ids];
    exec(success, error, "WakeupPlugin", "cancel", ids);
};

module.exports = new Wakeup();
