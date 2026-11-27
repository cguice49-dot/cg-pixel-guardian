package com.cg.pixelguardian;
import android.content.*;
import android.os.*;
import org.json.*;
final class DeviceHistory {
 static synchronized String capture(Context c)throws Exception{
  SharedPreferences p=c.getSharedPreferences("aegis_device_history",Context.MODE_PRIVATE);
  JSONArray history=new JSONArray(p.getString("samples","[]"));
  JSONObject now=new JSONObject();
  now.put("time",System.currentTimeMillis());
  now.put("storage",new StatFs(c.getFilesDir().getPath()).getAvailableBytes());
  now.put("patch",Build.VERSION.SECURITY_PATCH);
  now.put("android",Build.VERSION.RELEASE);
  BatteryManager bm=c.getSystemService(BatteryManager.class);
  int battery=bm==null?-1:bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
  if(battery>=0&&battery<=100)now.put("battery",battery);
  Intent state=c.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
  if(state!=null&&state.hasExtra(BatteryManager.EXTRA_TEMPERATURE))now.put("temperature",state.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)/10.0);
  JSONObject prior=history.optJSONObject(history.length()-1);
  history.put(now);
  JSONArray bounded=new JSONArray();for(int i=Math.max(0,history.length()-168);i<history.length();i++)bounded.put(history.getJSONObject(i));
  if(!p.edit().putString("samples",bounded.toString()).commit())throw new java.io.IOException("History save failed");
  StringBuilder s=new StringBuilder("Local device snapshot • "+new java.util.Date(now.getLong("time"))+"\nSamples retained: "+bounded.length()+" / 168\nAndroid: "+now.getString("android")+"\nPatch: "+now.getString("patch")+"\nFree storage: "+now.getLong("storage")/1048576+" MiB\nBattery: "+(now.has("battery")?now.getInt("battery")+"%":"Unavailable")+"\nTemperature: "+(now.has("temperature")?now.getDouble("temperature")+" °C":"Unavailable"));
  if(prior!=null){s.append("\n\nChanges since previous snapshot:\nStorage: ").append((now.getLong("storage")-prior.getLong("storage"))/1048576).append(" MiB");if(now.has("battery")&&prior.has("battery"))s.append("\nBattery: ").append(now.getInt("battery")-prior.getInt("battery")).append(" percentage points");if(!now.getString("patch").equals(prior.optString("patch")))s.append("\nSecurity patch changed.");if(!now.getString("android").equals(prior.optString("android")))s.append("\nAndroid version changed.");}
  if(now.getLong("storage")<1073741824L)s.append("\nREVIEW — less than 1 GiB free storage.");
  return s.append("\n\nSnapshots are not continuous monitoring or a failure prediction. No personal files are scanned.").toString();
 }
}
