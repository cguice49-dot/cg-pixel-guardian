package com.cg.pixelguardian;
import android.content.Context;
import android.net.Uri;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;
import java.util.*;
final class EngineeringReport {
 static final class Result {
  int errors,warnings,other;
  final Map<String,Integer> categories=new TreeMap<>();
  final Set<String> types=new HashSet<>();
 }
 static Result read(Context c,Uri uri)throws Exception{
  try(InputStream in=c.getContentResolver().openInputStream(uri)){
   if(in==null)throw new IOException("Report unavailable");
   ByteArrayOutputStream out=new ByteArrayOutputStream();
   byte[] buffer=new byte[4096];int n;
   while((n=in.read(buffer))!=-1){if(out.size()+n>4*1024*1024)throw new IOException("Report exceeds 4 MB");out.write(buffer,0,n);}
   XmlPullParser p=Xml.newPullParser();p.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL,false);
   p.setInput(new ByteArrayInputStream(out.toByteArray()),null);
   Result r=new Result();boolean root=false;
   for(int e=p.nextToken();e!=XmlPullParser.END_DOCUMENT;e=p.nextToken()){
    if(e==XmlPullParser.DOCDECL)throw new IOException("DOCTYPE not allowed");
    if(e!=XmlPullParser.START_TAG)continue;
    if(p.getDepth()==1){if(!"issues".equals(p.getName()))throw new IOException("Not an Android lint report");root=true;}
    if(p.getDepth()!=2||!"issue".equals(p.getName()))continue;
    String id=p.getAttributeValue(null,"id"),severity=p.getAttributeValue(null,"severity");
    if(id==null||severity==null)throw new IOException("Missing issue metadata");
    r.types.add(id);
    if("Fatal".equalsIgnoreCase(severity)||"Error".equalsIgnoreCase(severity))r.errors++;
    else if("Warning".equalsIgnoreCase(severity))r.warnings++;else r.other++;
    r.categories.put(id,r.categories.containsKey(id)?r.categories.get(id)+1:1);
   }
   if(!root)throw new IOException("Missing report root");return r;
  }
 }
}
