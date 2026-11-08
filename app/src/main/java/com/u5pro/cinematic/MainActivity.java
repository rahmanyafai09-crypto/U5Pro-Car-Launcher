package com.u5pro.cinematic;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.*;
import android.net.Uri;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity implements LocationListener {
    private final int bg=Color.rgb(7,14,26), card=Color.rgb(14,27,46), blue=Color.rgb(62,194,255), muted=Color.rgb(150,177,198);
    private TextView clock, speed, gpsStatus;
    private LocationManager locations;
    private final Handler handler=new Handler();
    private final Runnable ticker=new Runnable(){ public void run(){
        if(clock!=null) clock.setText(new SimpleDateFormat("hh:mm a",Locale.getDefault()).format(new Date()));
        handler.postDelayed(this,15000);
    }};
    int dp(float n){return (int)(getResources().getDisplayMetrics().density*n+0.5f);}
    GradientDrawable shape(int color,int stroke){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(18));
        d.setStroke(dp(1),stroke);return d;
    }
    TextView label(String s,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        t.setGravity(Gravity.CENTER);return t;
    }
    LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    void add(LinearLayout parent,View v,int w,int h){parent.addView(v,new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h)));}
    void gap(LinearLayout parent,int size,boolean horizontal){add(parent,new View(this),horizontal?size:1,horizontal?1:size);}
    LinearLayout panel(){LinearLayout l=column();l.setGravity(Gravity.CENTER);l.setPadding(dp(12),dp(12),dp(12),dp(12));l.setBackground(shape(card,Color.rgb(24,77,112)));return l;}
    void shortcut(LinearLayout bar,String text,Runnable action){
        TextView b=label(text,15,Color.WHITE,true);
        b.setBackground(shape(Color.rgb(20,43,67),Color.rgb(31,104,145)));
        b.setOnClickListener(v->action.run());
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(62),1);p.setMargins(dp(5),0,dp(5),0);
        bar.addView(b,p);
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        LinearLayout root=column();root.setBackgroundColor(bg);root.setPadding(dp(18),dp(14),dp(18),dp(12));setContentView(root);
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=label("◈  U5 PRO  /  CINEMATIC DRIVE",19,blue,true);title.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        top.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));
        clock=label("",25,Color.WHITE,true);add(top,clock,190,48);add(root,top,-1,48);
        gap(root,10,false);
        LinearLayout center=new LinearLayout(this);center.setOrientation(0);
        root.addView(center,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout gauge=panel();center.addView(gauge,new LinearLayout.LayoutParams(0,-1,1.0f));
        add(gauge,label("GPS SPEED",17,muted,true),-1,42);
        speed=label("--",75,blue,true);add(gauge,speed,-1,110);
        add(gauge,label("km/h",19,Color.WHITE,false),-1,34);
        gpsStatus=label("Waiting for GPS",12,muted,false);add(gauge,gpsStatus,-1,32);
        gap(center,10,true);
        LinearLayout map=panel();center.addView(map,new LinearLayout.LayoutParams(0,-1,1.18f));
        add(map,label("NAVIGATION",20,blue,true),-1,44);
        TextView mapText=label("GOOGLE MAPS\n\nTap to open navigation",22,Color.WHITE,true);
        mapText.setBackground(shape(Color.rgb(13,35,55),Color.rgb(26,81,117)));
        map.addView(mapText,new LinearLayout.LayoutParams(-1,0,1));
        mapText.setOnClickListener(v->openMaps());
        gap(map,10,false);
        TextView nav=label("OPEN MAPS  ↗",16,blue,true);add(map,nav,-1,38);nav.setOnClickListener(v->openMaps());
        gap(center,10,true);
        LinearLayout music=panel();center.addView(music,new LinearLayout.LayoutParams(0,-1,1.0f));
        add(music,label("NOW PLAYING",20,blue,true),-1,40);
        add(music,label("♪",70,Color.WHITE,false),-1,115);
        add(music,label("Music controls",17,muted,false),-1,42);
        LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);
        shortcut(controls,"⏮",()->media(KeyEvent.KEYCODE_MEDIA_PREVIOUS));
        shortcut(controls,"⏯",()->media(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
        shortcut(controls,"⏭",()->media(KeyEvent.KEYCODE_MEDIA_NEXT));
        add(music,controls,-1,62);
        gap(root,12,false);
        LinearLayout dock=new LinearLayout(this);dock.setGravity(Gravity.CENTER);
        shortcut(dock,"◈  APPS",()->apps());
        shortcut(dock,"⌖  MAPS",()->openMaps());
        shortcut(dock,"☎  PHONE",()->launchAny("phone","dialer","bluetooth"));
        shortcut(dock,"♫  MUSIC",()->launchAny("music","spotify","youtube music","bt music"));
        shortcut(dock,"◉  RADIO",()->launchAny("radio","fm"));
        shortcut(dock,"⚙  SETTINGS",()->startActivity(new Intent(Settings.ACTION_SETTINGS)));
        add(root,dock,-1,66);
        handler.post(ticker);startGps();
    }
    void startGps(){
        locations=(LocationManager)getSystemService(LOCATION_SERVICE);
        if(android.os.Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},42);return;
        }
        try{
            if(locations.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                Location last=locations.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if(last!=null)onLocationChanged(last);
                locations.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,this);
            } else gpsStatus.setText("Enable location / GPS");
        }catch(Exception e){gpsStatus.setText("GPS unavailable");}
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){
        super.onRequestPermissionsResult(requestCode,permissions,results);
        if(requestCode==42 && results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED)startGps();
        else if(requestCode==42)gpsStatus.setText("Location permission needed");
    }
    @Override public void onLocationChanged(Location location){
        speed.setText(String.valueOf(Math.round(location.getSpeed()*3.6f)));
        gpsStatus.setText(location.hasSpeed()?"Live GPS speed":"GPS fix acquired");
    }
    @Override protected void onDestroy(){super.onDestroy();handler.removeCallbacks(ticker);if(locations!=null)try{locations.removeUpdates(this);}catch(Exception ignored){}}
    void media(int key){AudioManager am=(AudioManager)getSystemService(AUDIO_SERVICE);am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,key));am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,key));}
    void openMaps(){
        Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse("geo:0,0?q=Doha"));
        try{startActivity(i);}catch(Exception e){Toast.makeText(this,"Install a maps app",Toast.LENGTH_SHORT).show();}
    }
    List<ResolveInfo> installed(){
        Intent intent=new Intent(Intent.ACTION_MAIN);intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list=getPackageManager().queryIntentActivities(intent,0);
        Collections.sort(list,new ResolveInfo.DisplayNameComparator(getPackageManager()));return list;
    }
    void launchAny(String... terms){
        for(ResolveInfo ri:installed()){
            String name=ri.loadLabel(getPackageManager()).toString().toLowerCase(Locale.ROOT);
            String pkg=ri.activityInfo.packageName.toLowerCase(Locale.ROOT);
            for(String term:terms)if(name.contains(term)||pkg.contains(term.replace(" ",""))){
                try{Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_LAUNCHER);
                    i.setClassName(ri.activityInfo.packageName,ri.activityInfo.name);startActivity(i);return;}catch(Exception ignored){}
            }
        }
        Toast.makeText(this,"App not found; open Apps to choose",Toast.LENGTH_SHORT).show();apps();
    }
    void apps(){
        LinearLayout list=column();list.setPadding(dp(14),dp(10),dp(14),dp(10));
        ScrollView scroll=new ScrollView(this);scroll.addView(list);
        android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle("Installed apps").setView(scroll).setNegativeButton("Close",null).create();
        for(ResolveInfo ri:installed()){
            TextView row=label(ri.loadLabel(getPackageManager()).toString(),17,Color.WHITE,false);
            row.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);row.setPadding(dp(16),0,0,0);
            add(list,row,-1,52);
            row.setOnClickListener(v->{try{Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_LAUNCHER);
                i.setClassName(ri.activityInfo.packageName,ri.activityInfo.name);startActivity(i);dialog.dismiss();}
                catch(Exception e){Toast.makeText(this,"Cannot open app",Toast.LENGTH_SHORT).show();}});
        }
        dialog.show();
    }
}
