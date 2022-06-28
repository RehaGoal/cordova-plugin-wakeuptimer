# Wakeup/Alarm Clock PhoneGap/Cordova Plugin

### Platform Support

This plugin supports PhoneGap/Cordova apps running on Android.

### Version Requirements

This plugin is meant to work with Cordova 3.5.0+.

## Installation

#### Automatic Installation using PhoneGap/Cordova CLI (Android)
1. Make sure you update your projects to Cordova version 3.5.0+ before installing this plugin.

        cordova platform update android

2. Install this plugin using PhoneGap/Cordova cli:

        cordova plugin add https://github.com/RehaGoal/cordova-plugin-wakeuptimer

## Usage

    // set wakeup timer
    window.wakeuptimer.schedule( successCallback,  
       errorCallback, 
       // a list of alarms to set
       {
            alarms : [{
                type : 'relative',
                time : { seconds : 14 }, // alarm in 14 seconds from now
                extra : { message : 'description of an notification' }
           }] 
       }
    );

     // cancel one or many alarms...
     window.wakeuptimer.cancel( successCallback,
        errorCallback, ids[]
     );

    // example of a callback method
    var successCallback = function(result) {
        if (result.type==='wakeup') {
            console.log('wakeup alarm detected--' + result.extra);
        } else if(result.type==='set'){
            // contains: alarm_type,alarm_date and id 
            console.log('wakeup alarm set--' + result.id);
        } else {
            console.log('wakeup unhandled type (' + result.type + ')');
        }
    }; 
