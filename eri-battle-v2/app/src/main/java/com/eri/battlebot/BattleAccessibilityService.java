package com.eri.battlebot;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.hardware.HardwareBuffer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Completely new screenshot-driven controller: no legacy Xmagic bot, account
 * switching, native hooks, game memory access or license dependencies.
 */
public final class BattleAccessibilityService extends AccessibilityService {
    private enum Stage { READY, SEARCHING, DEPLOYING, WAIT_RESULT, FINISHED }
    private Handler handler;
    private SharedPreferences prefs;
    private final BattleDetector battleDetector=new BattleDetector();
    private final TroopPlanner planner=new TroopPlanner();
    private TextRecognizer recognizer;
    private EriImGuiOverlay imgui;
    private boolean connected=false,requesting=false,ocrPending=false,manualRequested=false;
    private String foreground="";
    private Stage stage=Stage.READY;
    private BattleDetector.Match previousBattle;
    private int hudConfirmations=0,screenshotFailures=0;
    private long lastBattleTap=0,lastOcrAt=0,lastDeployAt=0;
    private final ArrayDeque<TroopPlanner.Point> pendingTaps=new ArrayDeque<>();
    private int actions=0,totalActions=0;
    private final Runnable tick=new Runnable(){
        @Override public void run(){
            if(!connected)return;
            try { scan(); }
            catch(Throwable ex){report("Lỗi dịch vụ: "+ex.getClass().getSimpleName());}
            handler.postDelayed(this,1300);
        }
    };
    @Override protected void onServiceConnected(){
        super.onServiceConnected();
        handler=new Handler(Looper.getMainLooper());
        prefs=getSharedPreferences("eri_v2",MODE_PRIVATE);
        recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        connected=true;
        prefs.edit().putBoolean("service_ready",true).apply();
        imgui=new EriImGuiOverlay(this,prefs,new EriImGuiOverlay.Actions(){
            @Override public void setEnabled(boolean enabled){
                prefs.edit().putBoolean("enabled",enabled).putBoolean("stop_request",!enabled).apply();
                if(enabled && stage!=Stage.DEPLOYING)
                    setStage(Stage.READY,"Đã bật bot trên menu");
                if(!enabled)stopSequence("Đã dừng bot từ menu");
                if(imgui!=null)imgui.refresh();
            }
            @Override public void deploy(){
                if(stage==Stage.DEPLOYING){report("Đang triển khai lính");return;}
                prefs.edit().putBoolean("enabled",true).putBoolean("stop_request",false).apply();
                manualRequested=true;
                report("Đang chụp màn hình để triển khai lính");
            }
            @Override public void stop(){
                prefs.edit().putBoolean("enabled",false).putBoolean("stop_request",true).apply();
                stopSequence("Đã dừng thao tác từ menu");
            }
            @Override public void reset(){
                prefs.edit().putBoolean("reset_request",true)
                    .putBoolean("enabled",true).putBoolean("stop_request",false).apply();
                report("Đã yêu cầu bắt đầu chu kỳ mới");
            }
        });
        setStage(Stage.READY,"Đang chờ game");
        handler.removeCallbacks(tick);handler.post(tick);
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        if(event!=null && event.getPackageName()!=null){
            String name=event.getPackageName().toString();
            // Touching our accessibility overlay must not hide it as if the
            // user had switched away from the game.
            if(!name.equals(getPackageName()))foreground=name;
        }
    }
    @Override public void onInterrupt(){stopSequence("Trợ năng bị ngắt");}
    @Override public void onDestroy(){
        connected=false;
        if(handler!=null)handler.removeCallbacks(tick);
        stopSequence("Đã ngắt dịch vụ");
        if(imgui!=null){imgui.destroy();imgui=null;}
        if(recognizer!=null)recognizer.close();
        if(prefs!=null)prefs.edit().putBoolean("service_ready",false).apply();
        super.onDestroy();
    }
    private boolean gameForeground(){
        AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root!=null && root.getPackageName()!=null){
            String name=root.getPackageName().toString();
            if(!name.equals(getPackageName()))foreground=name;
        }
        return foreground.toLowerCase(Locale.ROOT).contains("clashofclans");
    }
    private void scan(){
        boolean inGame=gameForeground();
        if(imgui!=null)imgui.show(inGame);
        if(prefs.getBoolean("reset_request",false)){
            prefs.edit().putBoolean("reset_request",false).apply();
            pendingTaps.clear();manualRequested=false;hudConfirmations=0;
            previousBattle=null;lastDeployAt=0;
            setStage(Stage.READY,"Đã đặt lại cho trận tiếp theo");
        }
        if(prefs.getBoolean("stop_request",false) || !prefs.getBoolean("enabled",false)){
            if(!pendingTaps.isEmpty() || stage==Stage.DEPLOYING) stopSequence("Dừng theo yêu cầu");
            prefs.edit().putBoolean("stop_request",false).apply();
            return;
        }
        if(!inGame){previousBattle=null;return;}
        if(stage==Stage.DEPLOYING || requesting || ocrPending)return;
        requesting=true;
        try{
            takeScreenshot(Display.DEFAULT_DISPLAY,getMainExecutor(),new TakeScreenshotCallback(){
                @Override public void onSuccess(ScreenshotResult image){
                    requesting=false;processScreenshot(image);
                }
                @Override public void onFailure(int code){
                    requesting=false;
                    if(++screenshotFailures==1 || screenshotFailures%6==0)
                        report("Không chụp được màn hình (mã "+code+")");
                }
            });
        }catch(Throwable ex){
            requesting=false;report("Chụp màn hình lỗi: "+ex.getClass().getSimpleName());
        }
    }
    private void processScreenshot(ScreenshotResult shot){
        HardwareBuffer buffer=null;
        Bitmap hardware=null,frame=null;
        boolean handedToOcr=false;
        try{
            buffer=shot.getHardwareBuffer();
            ColorSpace cs=shot.getColorSpace();
            hardware=Bitmap.wrapHardwareBuffer(buffer,cs);
            if(hardware==null)return;
            frame=hardware.copy(Bitmap.Config.ARGB_8888,false);
            if(frame==null)return;
            screenshotFailures=0;
            if(!gameForeground()||!prefs.getBoolean("enabled",false))return;
            long now=SystemClock.elapsedRealtime();
            if(manualRequested && stage!=Stage.DEPLOYING){
                manualRequested=false;
                if(lastDeployAt==0 || now-lastDeployAt>25000){
                    startDeployment(frame,"Thả lính bằng nút nổi");
                }else report("Vừa thả lính; bấm Làm mới chu kỳ để thử lại");
                return;
            }
            if(stage==Stage.READY && now-lastBattleTap>=7000){
                BattleDetector.Match found=battleDetector.detect(frame);
                if(found==null){previousBattle=null;}
                else {
                    boolean stable=previousBattle!=null
                        && Math.abs(found.x-previousBattle.x)<Math.max(18,frame.getWidth()*.02f)
                        && Math.abs(found.y-previousBattle.y)<Math.max(18,frame.getHeight()*.02f);
                    previousBattle=found;
                    prefs.edit().putString("last_match","Nút Battle: "+found.x+","+found.y).apply();
                    if(stable){
                        lastBattleTap=now;
                        setStage(Stage.SEARCHING,"Đã thấy Battle, chuyển sang tìm trận");
                        tap(found.x,found.y,()->report("Đã bấm Battle từ ảnh"));
                        return;
                    }
                }
            }
            if(now-lastOcrAt>=2600 && !ocrPending && stage!=Stage.FINISHED){
                lastOcrAt=now;
                handedToOcr=true;
                recognize(frame);
            }
        }catch(Throwable ex){
            report("Xử lý màn hình lỗi: "+ex.getClass().getSimpleName());
        }finally{
            if(!handedToOcr && frame!=null && !frame.isRecycled())frame.recycle();
            if(hardware!=null && !hardware.isRecycled())hardware.recycle();
            if(buffer!=null)buffer.close();
        }
    }
    private void recognize(final Bitmap frame){
        ocrPending=true;
        int w=frame.getWidth(),h=frame.getHeight();
        int scaledW=Math.min(1280,w),scaledH=Math.max(1,Math.round((float)h*scaledW/w));
        Bitmap input=Bitmap.createScaledBitmap(frame,scaledW,scaledH,true);
        if(input==frame)input=frame.copy(Bitmap.Config.ARGB_8888,false);
        final Bitmap small=input;
        try {
            Task<Text> task=recognizer.process(InputImage.fromBitmap(small,0));
            task.addOnSuccessListener(result->{
                try{parseText(result,frame,scaledW,scaledH);}
                catch(Throwable ex){report("OCR: "+ex.getClass().getSimpleName());}
            }).addOnFailureListener(error->report("Không đọc được giao diện trận"))
              .addOnCompleteListener(result->{
                if(!small.isRecycled())small.recycle();
                if(!frame.isRecycled())frame.recycle();
                ocrPending=false;
              });
        }catch(Throwable ex){
            ocrPending=false;
            small.recycle();frame.recycle();
            report("OCR chưa sẵn sàng: "+ex.getClass().getSimpleName());
        }
    }
    private static String plain(String value){
        String lower=value.toLowerCase(Locale.ROOT).replace('đ','d');
        return Normalizer.normalize(lower,Normalizer.Form.NFD)
            .replaceAll("\\p{M}+","").replaceAll("[^a-z0-9 ]"," ").replaceAll(" +"," ").trim();
    }
    private void parseText(Text recognized,Bitmap frame,int ocrW,int ocrH){
        if(!prefs.getBoolean("enabled",false) || !gameForeground())return;
        StringBuilder all=new StringBuilder();
        Rect returnBox=null;
        for(Text.TextBlock block:recognized.getTextBlocks()){
            for(Text.Line line:block.getLines()){
                String lineText=plain(line.getText());
                all.append(lineText).append(' ');
                Rect b=line.getBoundingBox();
                if(b!=null && (lineText.contains("return home") || lineText.contains("ve lang")
                    || lineText.contains("ve nha") || lineText.contains("quay ve")
                    || lineText.contains("tro ve lang"))){
                    if(b.centerX()>ocrW*.18 && b.centerX()<ocrW*.83
                        && b.centerY()>ocrH*.37) returnBox=b;
                }
            }
        }
        String text=all.toString();
        if(stage==Stage.WAIT_RESULT && returnBox!=null
            && prefs.getBoolean("auto_return",true)
            && SystemClock.elapsedRealtime()-lastDeployAt>12000){
            int x=Math.round(returnBox.centerX()*frame.getWidth()/(float)ocrW);
            int y=Math.round(returnBox.centerY()*frame.getHeight()/(float)ocrH);
            setStage(Stage.FINISHED,"Đã nhận diện Về nhà; nhấn vị trí "+x+","+y);
            tap(x,y,()->report("Đã nhấn Về nhà theo OCR"));
            prefs.edit().putBoolean("enabled",false).apply();
            return;
        }
        boolean attack=text.contains("battle ends") || text.contains("end battle")
            || text.contains("surrender") || text.contains("tran dau ket thuc")
            || text.contains("tran dau con") || text.contains("ket thuc tran dau")
            || text.contains("thoi gian con lai");
        boolean results=text.contains("victory") || text.contains("defeat")
            || text.contains("chien thang") || text.contains("that bai");
        if(results && stage==Stage.WAIT_RESULT){
            report("Đã nhận diện kết quả trận; chờ nút Về nhà");
        }
        if(attack && (stage==Stage.READY || stage==Stage.SEARCHING)){
            hudConfirmations++;
            if(hudConfirmations>=2){
                hudConfirmations=0;
                if(prefs.getBoolean("auto_deploy",false))
                    startDeployment(frame,"HUD trận đã xác nhận từ ảnh");
                else setStage(Stage.SEARCHING,"Đang trong trận; nhấn THẢ LÍNH trên nút nổi");
            }else report("Đã thấy HUD trận, xác nhận lần 2");
        }else if(!attack)hudConfirmations=0;
    }
    private void startDeployment(Bitmap frame,String reason){
        if(stage==Stage.DEPLOYING || !prefs.getBoolean("enabled",false))return;
        int slots=Math.max(1,Math.min(7,prefs.getInt("troop_slots",4)));
        int drops=Math.max(1,Math.min(8,prefs.getInt("drops_per_slot",4)));
        TroopPlanner.Plan plan=planner.plan(frame,slots);
        if(plan.deploy.size()<3 || plan.slots.isEmpty()){
            report("Không tìm được đủ vùng triển khai, đã hủy");return;
        }
        pendingTaps.clear();
        for(int s=0;s<plan.slots.size();s++){
            pendingTaps.addLast(plan.slots.get(s));
            for(int j=0;j<drops;j++){
                TroopPlanner.Point p=plan.deploy.get((s+j)%plan.deploy.size());
                int offset=(j%3-1)*(int)(plan.width*.009f);
                int dy=(j%2==0?1:-1)*(int)(plan.height*.010f);
                pendingTaps.addLast(new TroopPlanner.Point(
                    Math.max(4,Math.min(plan.width-4,p.x+offset)),
                    Math.max(4,Math.min(plan.height-4,p.y+dy))));
            }
        }
        lastDeployAt=SystemClock.elapsedRealtime();
        totalActions=pendingTaps.size();actions=0;
        prefs.edit().putString("last_match","Thanh lính: "+plan.slots.get(0)
            +", vùng thả đầu: "+plan.deploy.get(0)
            +(plan.adaptive?" (ô lính tự nhận diện)":" (ô lính ước tính theo ảnh)"))
            .apply();
        setStage(Stage.DEPLOYING,reason+" · "+slots+" ô x "+drops+" lần");
        nextDeploymentTap();
    }
    private void nextDeploymentTap(){
        if(stage!=Stage.DEPLOYING)return;
        if(!prefs.getBoolean("enabled",false)||!gameForeground()){
            stopSequence("Đã ngắt khi rời game / tắt bot");return;
        }
        TroopPlanner.Point point=pendingTaps.pollFirst();
        if(point==null){
            setStage(Stage.WAIT_RESULT,"Đã thả các ô lính đã chọn; chờ kết quả trận");
            return;
        }
        actions++;
        final int current=actions;
        tap(point.x,point.y,()->{
            if(stage==Stage.DEPLOYING && connected){
                if(current%5==0 || current==totalActions)
                    report("Đang thả lính "+current+"/"+totalActions);
                handler.postDelayed(this::nextDeploymentTap,350);
            }
        });
    }
    private void tap(int x,int y,Runnable done){
        if(!connected || !gameForeground())return;
        Path path=new Path();path.moveTo(x,y);
        GestureDescription.StrokeDescription stroke=
            new GestureDescription.StrokeDescription(path,0,95);
        GestureDescription g=new GestureDescription.Builder().addStroke(stroke).build();
        try{
            boolean accepted=dispatchGesture(g,new GestureResultCallback(){
                @Override public void onCompleted(GestureDescription description){
                    if(done!=null)done.run();
                }
                @Override public void onCancelled(GestureDescription description){
                    if(stage==Stage.DEPLOYING)stopSequence("Android huỷ thao tác chạm");
                    else report("Android huỷ chạm");
                }
            },handler);
            if(!accepted){
                if(stage==Stage.DEPLOYING)stopSequence("Không thể gửi thao tác chạm");
                else report("Không thể chạm");
            }
        }catch(Throwable ex){stopSequence("Lỗi cử chỉ: "+ex.getClass().getSimpleName());}
    }
    private void stopSequence(String why){
        pendingTaps.clear();manualRequested=false;hudConfirmations=0;previousBattle=null;
        if(stage==Stage.DEPLOYING){
            setStage(Stage.WAIT_RESULT,why);
        }else report(why);
    }
    private void setStage(Stage next,String reason){
        stage=next;
        if(prefs!=null)prefs.edit().putString("phase",next.name())
            .putString("last_status",reason).apply();
        if(imgui!=null)imgui.refresh();
    }
    private void report(String message){
        if(prefs!=null)prefs.edit().putString("last_status",message).apply();
        if(imgui!=null)imgui.refresh();
    }
}
