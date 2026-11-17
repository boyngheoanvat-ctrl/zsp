package com.eri.battlebot;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.graphics.Path;
import android.hardware.HardwareBuffer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.Locale;

/** Standalone, screenshot-driven Battle button automation; no previous bot code. */
public final class BattleAccessibilityService extends AccessibilityService {
    private Handler handler;
    private SharedPreferences prefs;
    private final BattleDetector detector=new BattleDetector();
    private boolean connected,requesting;
    private BattleDetector.Match last;
    private long lastTap=0;
    private int failures=0;
    private final Runnable tick=new Runnable(){
        @Override public void run(){
            if(!connected)return;
            try{scan();}catch(Throwable ex){report("Lỗi: "+ex.getClass().getSimpleName());}
            handler.postDelayed(this,1600);
        }
    };

    @Override protected void onServiceConnected(){
        super.onServiceConnected();
        handler=new Handler(Looper.getMainLooper());
        prefs=getSharedPreferences("eri_v2",MODE_PRIVATE);
        connected=true;
        prefs.edit().putBoolean("service_ready",true).apply();
        report("Trợ năng sẵn sàng");
        handler.removeCallbacks(tick);handler.post(tick);
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event){}
    @Override public void onInterrupt(){last=null;report("Trợ năng bị ngắt");}
    @Override public void onDestroy(){
        connected=false;
        if(handler!=null)handler.removeCallbacks(tick);
        if(prefs!=null)prefs.edit().putBoolean("service_ready",false).apply();
        super.onDestroy();
    }
    private void scan(){
        if(!prefs.getBoolean("enabled",false)){last=null;return;}
        AccessibilityNodeInfo root=getRootInActiveWindow();
        CharSequence current=root==null?null:root.getPackageName();
        String pkg=current==null?"":current.toString();
        if(!pkg.contains("clashofclans")){
            last=null;report("Chờ mở Clash of Clans ("+pkg+")");return;
        }
        if(requesting)return;
        requesting=true;
        try{
            takeScreenshot(Display.DEFAULT_DISPLAY,getMainExecutor(),new TakeScreenshotCallback(){
                @Override public void onSuccess(ScreenshotResult image){
                    requesting=false;
                    process(image);
                }
                @Override public void onFailure(int code){
                    requesting=false;
                    if(++failures==1 || failures%8==0)report("Không chụp được màn hình, mã "+code);
                }
            });
        }catch(Throwable ex){
            requesting=false;report("Chụp màn hình lỗi: "+ex.getClass().getSimpleName());
        }
    }
    private void process(ScreenshotResult result){
        HardwareBuffer buffer=null;
        Bitmap hardware=null,bitmap=null;
        try{
            buffer=result.getHardwareBuffer();
            ColorSpace cs=result.getColorSpace();
            hardware=Bitmap.wrapHardwareBuffer(buffer,cs);
            if(hardware==null){report("Bitmap rỗng");return;}
            bitmap=hardware.copy(Bitmap.Config.ARGB_8888,false);
            if(bitmap==null){report("Không đọc được ảnh chụp");return;}
            BattleDetector.Match match=detector.detect(bitmap);
            failures=0;
            if(match==null){last=null;report("Đang nhận diện nút Battle từ ảnh...");return;}
            prefs.edit().putString("last_match",String.format(Locale.US,
                "Battle: (%d,%d), vùng %dx%d, tin cậy %.2f",
                match.x,match.y,match.width,match.height,match.confidence)).apply();
            boolean stable=last!=null && Math.abs(match.x-last.x)<Math.max(18,bitmap.getWidth()*.02f)
                && Math.abs(match.y-last.y)<Math.max(18,bitmap.getHeight()*.02f);
            last=match;
            if(!stable){report("Đã thấy Battle, xác nhận khung hình thứ hai");return;}
            if(SystemClock.elapsedRealtime()-lastTap<4500)return;
            if(!prefs.getBoolean("enabled",false))return;
            AccessibilityNodeInfo root=getRootInActiveWindow();
            CharSequence active=root==null?null:root.getPackageName();
            if(active==null || !active.toString().contains("clashofclans"))return;
            tap(match.x,match.y);
            lastTap=SystemClock.elapsedRealtime();
            last=null;
        }catch(Throwable ex){
            last=null;report("Xử lý ảnh lỗi: "+ex.getClass().getSimpleName());
        }finally{
            if(bitmap!=null && !bitmap.isRecycled())bitmap.recycle();
            if(hardware!=null && !hardware.isRecycled())hardware.recycle();
            if(buffer!=null)buffer.close();
        }
    }
    private void tap(int x,int y){
        Path path=new Path();path.moveTo(x,y);
        GestureDescription.StrokeDescription stroke=new GestureDescription.StrokeDescription(path,0,90);
        GestureDescription gesture=new GestureDescription.Builder().addStroke(stroke).build();
        dispatchGesture(gesture,new GestureResultCallback(){
            @Override public void onCompleted(GestureDescription d){report("Đã bấm Battle theo ảnh thực");}
            @Override public void onCancelled(GestureDescription d){report("Android đã huỷ thao tác chạm");}
        },handler);
    }
    private void report(String message){
        if(prefs!=null)prefs.edit().putString("last_status",message).apply();
    }
}
