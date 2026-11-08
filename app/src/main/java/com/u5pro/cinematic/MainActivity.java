package com.u5pro.cinematic;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.*;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity implements LocationListener {
    private final int dark=Color.rgb(4,10,21), neon=Color.rgb(0,188,255), white=Color.WHITE;
    private Dashboard dashboard;
    private LocationManager gps;
    private float speedKmh=-1;
    private String gpsMessage="WAITING FOR GPS";
    private final Handler tick=new Handler();
    private final Runnable redraw=new Runnable(){public void run(){if(dashboard!=null)dashboard.invalidate();tick.postDelayed(this,1000);}};
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        dashboard=new Dashboard();setContentView(dashboard);tick.post(redraw);
        requestGps();
    }
    private void requestGps(){
        gps=(LocationManager)getSystemService(LOCATION_SERVICE);
        if(android.os.Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},18);return;
        }
        try{
            if(gps.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                gps.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,this);
                gpsMessage="ACQUIRING GPS SIGNAL";
            }else gpsMessage="ENABLE GPS IN SETTINGS";
        }catch(Exception e){gpsMessage="GPS NOT AVAILABLE";}
        dashboard.invalidate();
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] result){
        super.onRequestPermissionsResult(code,permissions,result);
        if(code==18 && result.length>0 && result[0]==PackageManager.PERMISSION_GRANTED)requestGps();
        else if(code==18){gpsMessage="ALLOW LOCATION FOR SPEED";dashboard.invalidate();}
    }
    @Override public void onLocationChanged(Location l){
        speedKmh=l.hasSpeed()?Math.max(0,l.getSpeed()*3.6f):-1;
        gpsMessage=l.hasSpeed()?"GPS SPEED • LIVE":"GPS FIX • SPEED UNAVAILABLE";
        dashboard.invalidate();
    }
    @Override protected void onDestroy(){
        super.onDestroy();tick.removeCallbacks(redraw);
        if(gps!=null)try{gps.removeUpdates(this);}catch(Exception ignored){}
    }
    void maps(){
        try{Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse("geo:0,0?q=Doha"));startActivity(i);}
        catch(Exception e){Toast.makeText(this,"Install Google Maps or another map app",Toast.LENGTH_LONG).show();}
    }
    void media(int key){
        AudioManager m=(AudioManager)getSystemService(AUDIO_SERVICE);
        m.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,key));
        m.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,key));
    }
    List<ResolveInfo> installed(){
        Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> l=getPackageManager().queryIntentActivities(i,0);
        Collections.sort(l,new ResolveInfo.DisplayNameComparator(getPackageManager()));return l;
    }
    void launch(String... keywords){
        for(ResolveInfo a:installed()){
            String name=a.loadLabel(getPackageManager()).toString().toLowerCase(Locale.ROOT);
            String pkg=a.activityInfo.packageName.toLowerCase(Locale.ROOT);
            for(String keyword:keywords){
                if(name.contains(keyword)||pkg.contains(keyword.replace(" ",""))){
                    try{Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_LAUNCHER);
                        i.setClassName(a.activityInfo.packageName,a.activityInfo.name);startActivity(i);return;}
                    catch(Exception ignored){}
                }
            }
        }
        Toast.makeText(this,"Select your app in Apps",Toast.LENGTH_SHORT).show();apps();
    }
    void apps(){
        ScrollView scroll=new ScrollView(this);
        LinearLayout list=new LinearLayout(this);list.setOrientation(1);list.setPadding(22,12,22,12);
        scroll.addView(list);
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Installed apps").setView(scroll).setNegativeButton("Close",null).create();
        for(ResolveInfo a:installed()){
            TextView item=new TextView(this);item.setText(a.loadLabel(getPackageManager()));item.setTextSize(18);
            item.setTextColor(Color.WHITE);item.setPadding(12,18,12,18);list.addView(item);
            item.setOnClickListener(v->{try{Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_LAUNCHER);
                i.setClassName(a.activityInfo.packageName,a.activityInfo.name);startActivity(i);d.dismiss();}catch(Exception ignored){}});
        }
        d.show();
    }
    class Dashboard extends View{
        Canvas c;Paint p=new Paint(3);
        Dashboard(){super(MainActivity.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        int rgba(int color,int alpha){return (color&0xffffff)|(alpha<<24);}
        void fill(int color){p.setColor(color);p.setShader(null);p.setStrokeWidth(1);p.setStyle(Paint.Style.FILL);}
        void rect(float l,float t,float r,float b,int color,float radius){
            fill(color);c.drawRoundRect(l,t,r,b,radius,radius,p);
        }
        void stroke(float l,float t,float r,float b,int color,float radius,float w){
            fill(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(w);c.drawRoundRect(l,t,r,b,radius,radius,p);p.setStyle(Paint.Style.FILL);
        }
        void line(float x,float y,float xx,float yy,int color,float width){
            fill(color);p.setStrokeWidth(width);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x,y,xx,yy,p);
        }
        void text(String s,float x,float y,int size,int color,boolean bold){
            fill(color);p.setTextSize(size);p.setTypeface(bold?Typeface.create("sans-serif",Typeface.BOLD):Typeface.create("sans-serif",Typeface.NORMAL));
            c.drawText(s,x,y,p);
        }
        void centered(String s,float cx,float y,int size,int color,boolean bold){
            p.setTextSize(size);p.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);
            text(s,cx-p.measureText(s)/2,y,size,color,bold);
        }
        void card(float x,float y,float w,float h){
            fill(Color.rgb(7,22,39));p.setShader(new LinearGradient(x,y,x+w,y+h,Color.rgb(5,26,47),Color.rgb(5,12,26),Shader.TileMode.CLAMP));
            c.drawRoundRect(x,y,x+w,y+h,22,22,p);p.setShader(null);
            stroke(x,y,x+w,y+h,rgba(neon,155),22,1.7f);
            line(x+23,y+h-3,x+w-23,y+h-3,rgba(neon,90),3);
        }
        void mountain(float x,float y,float width,float height,int col){
            Path path=new Path();path.moveTo(x,y+height);
            path.lineTo(x+width*.14f,y+height*.42f);path.lineTo(x+width*.24f,y+height*.67f);
            path.lineTo(x+width*.43f,y+height*.06f);path.lineTo(x+width*.57f,y+height*.52f);
            path.lineTo(x+width*.70f,y+height*.23f);path.lineTo(x+width,y+height);
            path.close();fill(col);c.drawPath(path,p);
        }
        void backdrop(){
            fill(dark);c.drawColor(dark);
            fill(Color.rgb(12,13,36));
            p.setShader(new LinearGradient(100,0,1100,170,new int[]{Color.rgb(9,17,36),Color.rgb(69,31,77),Color.rgb(248,107,67),Color.rgb(5,17,39)},null,Shader.TileMode.CLAMP));
            c.drawRect(80,0,1280,180,p);p.setShader(null);
            mountain(120,5,820,140,Color.rgb(13,27,57));
            mountain(540,-20,620,175,Color.rgb(5,16,36));
            rect(80,83,1280,720,rgba(Color.BLACK,180),0);
        }
        void badge(float x,float y,String symbol,String name){
            rect(x,y,x+128,y+125,Color.rgb(12,30,51),19);
            stroke(x,y,x+128,y+125,rgba(neon,115),19,1);
            centered(symbol,x+64,y+64,45,neon,true);centered(name,x+64,y+105,17,white,false);
        }
        void sidebar(){
            rect(0,0,88,720,Color.rgb(4,19,36),24);stroke(1,1,86,718,rgba(neon,125),24,1.7f);
            String[] symbols={"⌂","▦","➤","♫","ᛒ","⚙"};
            String[] names={"Home","Apps","Maps","Music","BT","Settings"};
            for(int j=0;j<6;j++){
                float y=14+j*113;
                if(j==0){rect(5,y,83,y+104,Color.rgb(11,56,90),19);stroke(5,y,83,y+104,neon,19,2);}
                centered(symbols[j],44,y+49,37,j==0?neon:Color.rgb(159,213,255),true);
                centered(names[j],44,y+79,16,white,false);
            }
        }
        void gauge(){
            card(110,112,325,294);
            centered("GPS SPEED",272,145,17,Color.rgb(160,209,242),true);
            fill(neon);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(7);
            p.setShader(new SweepGradient(272,272,new int[]{Color.rgb(0,60,160),neon,Color.rgb(0,65,205)},null));
            c.drawArc(142,143,402,400,150,240,false,p);p.setShader(null);p.setStyle(Paint.Style.FILL);
            for(int i=0;i<=40;i++){
                double a=Math.toRadians(150+i*6);float inner=i%5==0?112:119;
                float sx=272+(float)Math.cos(a)*inner,sy=272+(float)Math.sin(a)*inner;
                float ex=272+(float)Math.cos(a)*127,ey=272+(float)Math.sin(a)*127;
                line(sx,sy,ex,ey,rgba(neon,i%5==0?240:140),i%5==0?2.4f:1.1f);
            }
            centered(speedKmh<0?"--":String.valueOf(Math.round(speedKmh)),272,283,82,white,true);
            centered("km/h",272,319,21,Color.rgb(204,228,245),false);
            centered(gpsMessage,272,376,13,Color.rgb(138,203,238),false);
        }
        void map(){
            card(449,112,511,294);
            c.save();c.clipRect(452,115,957,402);
            for(int i=0;i<16;i++){
                int off=i*43;line(452+off,150,600+off,400,rgba(Color.rgb(49,97,138),85),3);
            }
            for(int i=0;i<9;i++)line(452,159+i*31,956,139+i*19,rgba(Color.rgb(65,114,159),110),3);
            Path road=new Path();road.moveTo(714,400);road.cubicTo(600,315,843,275,736,196);
            road.cubicTo(696,160,806,140,839,115);
            fill(neon);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(14);
            p.setColor(rgba(neon,75));c.drawPath(road,p);p.setColor(Color.rgb(23,128,254));p.setStrokeWidth(7);c.drawPath(road,p);p.setStyle(Paint.Style.FILL);
            fill(Color.rgb(30,128,252));c.drawCircle(714,346,30,p);centered("▲",714,355,27,white,true);
            c.restore();
            rect(463,124,934,174,rgba(Color.rgb(6,21,39),235),14);
            text("●  GOOGLE MAPS",484,157,24,white,true);
            rect(468,342,717,386,rgba(Color.rgb(5,20,36),240),13);
            text("TAP MAP TO OPEN NAVIGATION",478,370,15,neon,true);
        }
        void clock(){
            card(974,112,291,294);
            centered(new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date()),1120,218,65,white,true);
            centered(new SimpleDateFormat("EEE, d MMM yyyy",Locale.getDefault()).format(new Date()),1120,253,20,Color.rgb(212,228,243),false);
            line(1007,271,1232,271,rgba(neon,130),2);
            centered("◉  DOHA, QATAR",1120,315,22,neon,true);
            centered("Weather not connected",1120,352,16,Color.rgb(179,200,217),false);
        }
        void music(){
            card(110,420,535,169);
            rect(127,437,247,573,Color.rgb(20,17,57),13);
            for(int i=0;i<14;i++){int h=(i*19%65)+16;line(142+i*7,506-h/2,142+i*7,506+h/2,i%2==0?Color.rgb(178,60,246):neon,3);}
            text("NOW PLAYING",270,456,18,white,true);
            text("Controls for active media app",270,487,15,Color.rgb(178,200,221),false);
            line(274,509,606,509,rgba(neon,120),2);
            centered("◀◀",332,557,25,white,true);
            centered("▶ ❙❙",451,557,27,neon,true);
            centered("▶▶",566,557,25,white,true);
        }
        void shortcuts(){
            card(659,420,606,169);
            String[] sym={"▶","♫","◉","▣"};
            String[] name={"YouTube","Spotify","Radio","Apps"};
            for(int i=0;i<4;i++){
                float x=673+i*148;
                rect(x,438,x+133,570,Color.rgb(16,35,57),19);
                stroke(x,438,x+133,570,rgba(neon,90),19,1);
                centered(sym[i],x+66,515,48,i==0?Color.rgb(247,72,75):i==1?Color.rgb(31,215,115):neon,true);
                centered(name[i],x+66,550,19,white,false);
            }
        }
        void dock(){
            rect(110,603,1265,711,Color.rgb(7,25,45),22);stroke(110,603,1265,711,rgba(neon,125),22,1.4f);
            String[] ico={"☎","♟","ᛒ","▣","▷","⚙"};
            String[] lbl={"Phone","Contacts","BT Music","Files","Play Store","Settings"};
            for(int i=0;i<6;i++){
                float x=137+i*189;
                rect(x,615,x+117,671,i==0?Color.rgb(18,145,91):i==2?Color.rgb(0,95,179):Color.rgb(21,63,105),14);
                centered(ico[i],x+58,654,34,white,true);
                centered(lbl[i],x+58,693,16,white,false);
            }
        }
        @Override public void onDraw(Canvas canvas){
            super.onDraw(canvas);this.c=canvas;
            int state=canvas.save();canvas.scale(getWidth()/1280f,getHeight()/720f);
            backdrop();sidebar();
            text("⌁  U5 PRO  /  CINEMATIC DRIVE",113,65,29,white,true);
            text("●",1050,55,25,neon,true);
            text("BT    Wi-Fi",1080,55,21,Color.rgb(178,211,240),false);
            gauge();map();clock();music();shortcuts();dock();canvas.restoreToCount(state);
        }
        @Override public boolean onTouchEvent(MotionEvent event){
            if(event.getAction()!=MotionEvent.ACTION_UP)return true;
            float x=event.getX()*1280f/getWidth(),y=event.getY()*720f/getHeight();
            if(x<88){
                int idx=(int)((y-14)/113);switch(idx){
                    case 0:break;case 1:apps();break;case 2:maps();break;
                    case 3:launch("music","spotify","bt music");break;
                    case 4:launch("bluetooth","bt music");break;
                    case 5:startActivity(new Intent(Settings.ACTION_SETTINGS));break;
                }return true;
            }
            if(y>=112&&y<=406 && x>=449&&x<960){maps();return true;}
            if(y>=420&&y<=589){
                if(x>=110&&x<645){if(x<385)media(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                    else if(x<506)media(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                    else media(KeyEvent.KEYCODE_MEDIA_NEXT);return true;}
                if(x>=659){
                    int index=(int)((x-673)/148);
                    switch(index){case 0:launch("youtube");break;case 1:launch("spotify");break;
                        case 2:launch("radio","fm");break;case 3:apps();break;}
                    return true;
                }
            }
            if(y>=603&&x>=110){
                int idx=Math.min(5,Math.max(0,(int)((x-110)/189)));
                switch(idx){
                    case 0:launch("phone","dialer","bluetooth");break;
                    case 1:launch("contacts");break;
                    case 2:launch("bt music","music","bluetooth");break;
                    case 3:launch("files","file manager");break;
                    case 4:launch("play store");break;
                    case 5:startActivity(new Intent(Settings.ACTION_SETTINGS));break;
                }
            }
            return true;
        }
    }
}
