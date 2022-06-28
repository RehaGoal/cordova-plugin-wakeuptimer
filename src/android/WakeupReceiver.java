package org.nypr.cordova.wakeupplugin;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class WakeupReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "WakeupReceiver";

    @SuppressLint({ "SimpleDateFormat", "NewApi" })
    @Override
    public void onReceive(Context context, Intent intent) {
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Log.i(LOG_TAG, "wakeuptimer expired at " + sdf.format(new Date().getTime()));

        try {
            String packageName = context.getPackageName();
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            String className = launchIntent.getComponent().getClassName();
            Log.i(LOG_TAG, "launching activity for class " + className);

            @SuppressWarnings("rawtypes")
            Class c = Class.forName(className);

            Intent i = new Intent(context, c);
            i.putExtra("wakeup", true);
            Bundle extrasBundle = intent.getExtras();
            String extras=null;

            if (extrasBundle!=null && extrasBundle.get("extra")!=null) {
                extras = extrasBundle.get("extra").toString();
            }

            if (extras!=null) {
                i.putExtra("extra", extras);
            }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);

            if (WakeupPlugin.connectionCallbackContext!=null) {
                JSONObject o=new JSONObject();
                o.put("type", "wakeup");
                if (extrasBundle!=null && extrasBundle.get("id")!=null) {
                    o.put("id", extrasBundle.getInt("id"));
                }
                if (extras!=null) {
                    o.put("extra", extras);
                }
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, o);
                pluginResult.setKeepCallback(true);
                WakeupPlugin.connectionCallbackContext.sendPluginResult(pluginResult);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
