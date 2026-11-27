package com.cg.pixelguardian;
import android.content.*;
import android.content.pm.*;
import android.os.Build;
import java.util.*;
import org.json.*;
final class SixthSense {
 static synchronized String scan(Context c) throws Exception {
  SharedPreferences p=GuardianAudit.prefs(c);
  PackageManager pm=c.getPackageManager();
  JSONObject old=new JSONObject(p.getString("sixth_snapshot","{}")), next=new JSONObject();
  boolean enrolled=p.contains("sixth_snapshot");
  int score=0; List<String> events=new ArrayList<>();
  for(PackageInfo a:pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)){
   JSONObject item=new JSONObject(); long v=Build.VERSION.SDK_INT>=28?a.getLongVersionCode():a.versionCode;
   item.put("version",v);
   TreeSet<String> granted=new TreeSet<>();
   if(a.requestedPermissions!=null&&a.requestedPermissionsFlags!=null)for(int i=0;i<Math.min(a.requestedPermissions.length,a.requestedPermissionsFlags.length);i++)if((a.requestedPermissionsFlags[i]&PackageInfo.REQUESTED_PERMISSION_GRANTED)!=0)granted.add(a.requestedPermissions[i]);
   item.put("granted",new JSONArray(granted));
   next.put(a.packageName,item);
   JSONObject prior=old.optJSONObject(a.packageName);
   if(enrolled&&prior==null){events.add("Newly visible: "+a.packageName);score+=5;}
   if(prior!=null){
    if(v<prior.optLong("version",v)){events.add("Version decreased: "+a.packageName);score+=20;}
    JSONArray before=prior.optJSONArray("granted");Set<String> previous=new HashSet<>();if(before!=null)for(int i=0;i<before.length();i++)previous.add(before.optString(i));
    for(String perm:granted)if(!previous.contains(perm)&&(perm.contains("CAMERA")||perm.contains("RECORD_AUDIO")||perm.contains("LOCATION")||perm.contains("CONTACTS")||perm.contains("SMS"))){events.add("New sensitive grant: "+a.packageName+" "+perm);score+=10;}
   }
  }
  long last=p.getLong("last_audit",0);if(last>0&&System.currentTimeMillis()-last>6*3600000L){events.add("Audit gap exceeds six hours; Android scheduling may be responsible");score+=10;}
  p.edit().putString("sixth_snapshot",next.toString()).apply();
  String result="Sixth Sense: "+next.length()+" visible packages; "+(enrolled?"change review score "+Math.min(100,score)+"/100 (heuristic, not malware probability)":"initial observation saved, not a trusted clean baseline");
  for(String e:events)result+="; "+e;
  if(score>=20)GuardianAudit.notifyUser(c,result);
  return result;
 }
}
