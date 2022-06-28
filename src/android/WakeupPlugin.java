package org.nypr.cordova.wakeupplugin;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.HashSet;
import java.util.Set;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class WakeupPlugin extends CordovaPlugin {

    private static final String LOG_TAG = "WakeupPlugin";

    public static CallbackContext connectionCallbackContext;

    private static Set<Integer> alarmIds = new HashSet<Integer>();
    private static int offsetId = 10000;

    @Override
    public void onReset() {
        // app startup

        Log.i(LOG_TAG, "Wakeup Plugin onReset");
        Bundle extras = cordova.getActivity().getIntent().getExtras();
        if (extras==null || !extras.getBoolean("wakeup", false)) {
            resetPrefsToDefault(cordova.getActivity().getApplicationContext());
        }
        super.onReset();
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        boolean ret=true;
        try {
            if(action.equalsIgnoreCase("schedule")) {
                JSONObject options=args.getJSONObject(0);
                Context context = cordova.getActivity().getApplicationContext();
                JSONArray alarms;

                if (options.has("alarms")) {
                    alarms = options.getJSONArray("alarms");
                } else {
                    alarms = new JSONArray();
                }

                WakeupPlugin.connectionCallbackContext = callbackContext;

                setAlarms(context, alarms);

                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
            } else if (action.equalsIgnoreCase("cancel")) {
                cancelAlarms(cordova.getActivity().getApplicationContext(), args);

                WakeupPlugin.connectionCallbackContext = callbackContext;
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
            } else {
                PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, LOG_TAG + " error: invalid action (" + action + ")");
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
                ret=false;
            }
        } catch (JSONException e) {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, LOG_TAG + " error: invalid json");
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            ret = false;
        } catch (Exception e) {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, LOG_TAG + " error: " + e.getMessage());
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            ret = false;
        }
        return ret;
    }

    @SuppressLint({ "SimpleDateFormat", "NewApi" })
    private static void setAlarms(Context context, JSONArray alarms) throws JSONException{

        for(int i=0;i<alarms.length();i++) {
            JSONObject alarm=alarms.getJSONObject(i);

            if (!alarm.has("type")) {
                throw new JSONException("alarm missing type: " + alarm.toString());
            }

            if (!alarm.has("time")) {
                throw new JSONException("alarm missing time: " + alarm.toString());
            }

            String type = alarm.getString("type");
            JSONObject time=alarm.getJSONObject("time");

            if (type.equals("relative")) {
                int id = getAlarmIDIncremental();
                Calendar alarmDate=getTimeFromNow(time);
                Intent intent = new Intent(context, WakeupReceiver.class);
                if (alarm.has("extra")) {
                    intent.putExtra("extra", alarm.getJSONObject("extra").toString());
                    intent.putExtra("id", id);
                }

                saveToPrefs(context);

                setNotification(context, type, alarmDate, intent, id);
            }
        }
    }

    private static void setNotification(Context context, String type, Calendar alarmDate, Intent intent, int id) throws JSONException {
        if (alarmDate!=null) {
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Log.i(LOG_TAG,"setting alarm at " + sdf.format(alarmDate.getTime()) + " id: " + id);

            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent sender = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            if (Build.VERSION.SDK_INT>=19) {
                if (Build.VERSION.SDK_INT>=23) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmDate.getTimeInMillis(), sender);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmDate.getTimeInMillis(), sender);
                }
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, alarmDate.getTimeInMillis(), sender);
            }

            if (WakeupPlugin.connectionCallbackContext!=null) {
                String extras = intent.getStringExtra("extra");

                JSONObject o=new JSONObject();
                o.put("type", "set");
                o.put("alarm_type", type);
                o.put("alarm_date", alarmDate.getTimeInMillis());
                o.put("id", id);
                
                if (extras!=null) {
                    o.put("extra", extras);
                }

                Log.i(LOG_TAG, "alarm time in millis: " + alarmDate.getTimeInMillis());

                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, o);
                pluginResult.setKeepCallback(true);
                WakeupPlugin.connectionCallbackContext.sendPluginResult(pluginResult);
            }
        }
    }

    private static Calendar getTimeFromNow( JSONObject time) throws JSONException {
        TimeZone defaultz = TimeZone.getDefault();
        Calendar calendar = new GregorianCalendar(defaultz);
        calendar.setTime(new Date());

        int seconds=(time.has("seconds")) ? time.getInt("seconds") : -1;

        if (seconds>=0) {
            calendar.add(Calendar.SECOND, seconds);
        } else {
            calendar=null;
        }

        return calendar;
    }

    private static void cancelAlarm(Context context, int id) {
        if (alarmIds.contains(id)) {
            Log.i(LOG_TAG, "cancelling alarm id " + id);
            Intent intent = new Intent(context, WakeupReceiver.class);
            PendingIntent sender = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(sender);
            removeAlarmId(context, id);
        }
    }

    private static void cancelAll(Context context) {
        Log.i(LOG_TAG, "canceling all active alarms");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> storedAlarms = new HashSet<String>();

        prefs.getStringSet("alarmIds", storedAlarms);

        Set<Integer> shadowAlarmIds = new HashSet<Integer>(alarmIds);

        for (String alarmId : storedAlarms) {
            shadowAlarmIds.add(Integer.parseInt(alarmId));
        }

        for (Integer id : shadowAlarmIds) {
            cancelAlarm(context, id);
        }
    }

    private static void cancelAlarms(Context context, JSONArray ids) {
        Log.i(LOG_TAG, "canceling provided alarms");
        try {
            for (int i=0;i<ids.length();i++) {
                cancelAlarm(context, ids.getInt(i));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected static void resetPrefsToDefault(Context context) {
        SharedPreferences prefs;
        SharedPreferences.Editor editor;

        cancelAll(context);
        resetAlarmIds();

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        editor = prefs.edit();
        editor.putStringSet("alarmsIds", new HashSet<String>());
        editor.commit();
    }

    private static void saveToPrefs(Context context) {
        SharedPreferences prefs;
        SharedPreferences.Editor editor;

        Set<String> alarms = new HashSet<String>(alarmIds.size());
        for (Integer id : alarmIds) {
            alarms.add(id.toString());
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        editor = prefs.edit();
        editor.putStringSet("alarmsIds", alarms);
        editor.commit();
    }

    private static void resetAlarmIds () {
        alarmIds = new HashSet<Integer>();
        offsetId = 10000;
    }

    private static int getAlarmIDIncremental() {
        int id = offsetId++;
        alarmIds.add(id);
        return id;
    }

    private static void removeAlarmId(Context context, int alarmId) {
        alarmIds.remove(alarmId);
        saveToPrefs(context);
    }

}
