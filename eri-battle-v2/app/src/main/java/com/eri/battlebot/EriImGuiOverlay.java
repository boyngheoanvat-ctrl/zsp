package com.eri.battlebot;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * ImGui-inspired Android accessibility overlay (no injection or root).
 * The service can control it without using system-wide draw-over-app permission.
 * The actual Dear ImGui C++ library is not used.
 */
public final class EriImGuiOverlay {
    public interface Actions {
        void setEnabled(boolean enabled);
        void deploy();
        void stop();
        void reset();
    }

    private static final int BG=0xF41B2030;
    private static final int PANEL=0xFF232B3E;
    private static final int DARK=0xFF151B29;
    private static final int ACCENT=0xFF47C6F9;
    private static final int TEXT=0xFFF0F3FA;
    private static final int MUTED=0xFFA5B3C9;
    private static final int GREEN=0xFF43E2B2;
    private static final int RED=0xFFF16F7A;
    private final AccessibilityService service;
    private final SharedPreferences prefs;
    private final Actions actions;
    private final WindowManager wm;
    private FrameLayout root;
    private LinearLayout panel;
    private LinearLayout page;
    private TextView chip;
    private TextView statusView,substatusView;
    private WindowManager.LayoutParams params;
    private boolean added=false,open=false;
    private int selectedTab=0;
    private int screenW,screenH,panelWidth;
    private float startX,startY;
    private int originalX,originalY;
    private boolean moved;

    public EriImGuiOverlay(AccessibilityService service, SharedPreferences prefs, Actions actions){
        this.service=service;
        this.prefs=prefs;
        this.actions=actions;
        this.wm=(WindowManager)service.getSystemService(AccessibilityService.WINDOW_SERVICE);
        updateScreenSize();
        build();
    }

    private int dp(float value){return (int)(value*service.getResources().getDisplayMetrics().density+0.5f);}
    private void updateScreenSize(){
        Point p=new Point();
        wm.getDefaultDisplay().getRealSize(p);
        screenW=p.x;screenH=p.y;
        panelWidth=Math.min(dp(352),Math.max(dp(235),screenW-dp(24)));
    }
    private GradientDrawable fill(int color,float radius,int strokeColor,float strokeDp){
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);d.setCornerRadius(dp(radius));
        if(strokeDp>0)d.setStroke(dp(strokeDp),strokeColor);
        return d;
    }
    private GradientDrawable gradient(int left,int right,float radius){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{left,right});
        d.setCornerRadius(dp(radius));return d;
    }
    private LinearLayout vertical(){
        LinearLayout v=new LinearLayout(service);v.setOrientation(LinearLayout.VERTICAL);return v;
    }
    private LinearLayout horizontal(){
        LinearLayout v=new LinearLayout(service);v.setOrientation(LinearLayout.HORIZONTAL);
        v.setGravity(Gravity.CENTER_VERTICAL);return v;
    }
    private TextView text(String content,int size,int color,boolean bold){
        TextView t=new TextView(service);
        t.setText(content);t.setTextColor(color);t.setTextSize(size);
        t.setGravity(Gravity.CENTER_VERTICAL);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private LinearLayout.LayoutParams lp(int width,int height){
        return new LinearLayout.LayoutParams(width<0?width:dp(width),height<0?height:dp(height));
    }
    private void addSpace(LinearLayout parent,int height){
        View spacer=new View(service);parent.addView(spacer,lp(1,height));
    }
    private void build(){
        root=new FrameLayout(service);
        root.setClipChildren(false);root.setClipToPadding(false);
        chip=text("✦ ERI",14,Color.WHITE,true);
        chip.setGravity(Gravity.CENTER);
        chip.setBackground(gradient(0xFF147CBC,0xFF6D43C8,16));
        FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(dp(67),dp(55),Gravity.TOP|Gravity.LEFT);
        root.addView(chip,cp);
        chip.setOnTouchListener(dragHandler(false));
        panel=vertical();
        panel.setPadding(dp(1),dp(1),dp(1),dp(1));
        panel.setBackground(fill(BG,13,ACCENT,1));
        FrameLayout.LayoutParams pp=new FrameLayout.LayoutParams(panelWidth,FrameLayout.LayoutParams.WRAP_CONTENT);
        root.addView(panel,pp);
        buildPanel();
        applyOpen(false);
    }
    private void buildPanel(){
        LinearLayout header=horizontal();
        header.setPadding(dp(13),dp(2),dp(6),dp(2));
        header.setBackground(gradient(0xFF26354A,0xFF1C2940,12));
        panel.addView(header,lp(-1,52));
        TextView logo=text("◈",23,ACCENT,true);
        header.addView(logo,lp(28,-1));
        LinearLayout headerTexts=vertical();
        headerTexts.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams weight=new LinearLayout.LayoutParams(0,-1,1);
        header.addView(headerTexts,weight);
        TextView title=text("MOD BY ERI NGUYỄN",14,TEXT,true);
        TextView subtitle=text("BATTLE V3 • IMGUI STYLE",10,MUTED,false);
        headerTexts.addView(title);headerTexts.addView(subtitle);
        TextView min=text("—",18,ACCENT,true);
        min.setGravity(Gravity.CENTER);
        min.setBackground(fill(0xFF313A52,8,0,0));
        header.addView(min,lp(35,33));
        min.setOnClickListener(v->applyOpen(false));
        header.setOnTouchListener(dragHandler(true));

        LinearLayout tabs=horizontal();
        tabs.setPadding(dp(8),dp(9),dp(8),dp(8));
        panel.addView(tabs,lp(-1,51));
        String[] names={"CHÍNH","LÍNH","CÀI ĐẶT"};
        for(int i=0;i<names.length;i++){
            final int tab=i;
            TextView b=text(names[i],11,i==selectedTab?TEXT:MUTED,true);
            b.setGravity(Gravity.CENTER);
            b.setTag(i);
            b.setBackground(fill(i==selectedTab?0xFF25678B:0xFF212B3D,6,i==selectedTab?ACCENT:0xFF34405A,1));
            LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(34),1);
            bp.setMargins(dp(3),0,dp(3),0);
            tabs.addView(b,bp);
            b.setOnClickListener(v->{selectedTab=tab;drawTabs(tabs);drawPage();});
        }

        ScrollView scroll=new ScrollView(service);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout.LayoutParams scrollP=new LinearLayout.LayoutParams(-1,Math.min(dp(296),Math.max(dp(144),screenH-dp(173))));
        panel.addView(scroll,scrollP);
        page=vertical();page.setPadding(dp(12),dp(6),dp(12),dp(6));
        scroll.addView(page);
        drawPage();

        LinearLayout footer=horizontal();
        footer.setPadding(dp(13),dp(6),dp(13),dp(7));
        footer.setBackground(fill(0xFF172032,0,0,0));
        panel.addView(footer,lp(-1,37));
        TextView statusDot=text("●",14,GREEN,true);footer.addView(statusDot,lp(19,-1));
        TextView footerTitle=text("ERI • NO ROOT",10,MUTED,true);
        footer.addView(footerTitle,new LinearLayout.LayoutParams(0,-1,1));
        TextView version=text("v3.1",11,ACCENT,true);footer.addView(version);
    }
    private void drawTabs(LinearLayout tabs){
        for(int i=0;i<tabs.getChildCount();i++){
            View v=tabs.getChildAt(i);
            if(v instanceof TextView){
                boolean active=i==selectedTab;
                ((TextView)v).setTextColor(active?TEXT:MUTED);
                v.setBackground(fill(active?0xFF25678B:0xFF212B3D,6,active?ACCENT:0xFF34405A,1));
            }
        }
    }
    private TextView section(LinearLayout where,String label){
        TextView t=text(label,12,ACCENT,true);
        LinearLayout.LayoutParams p=lp(-1,30);p.topMargin=dp(3);
        where.addView(t,p);return t;
    }
    private TextView info(String s){
        TextView t=text(s,12,MUTED,false);
        t.setPadding(dp(10),dp(7),dp(9),dp(7));
        t.setBackground(fill(DARK,7,0xFF38465A,1));
        return t;
    }
    private void actionButton(LinearLayout into,String label,int color,Runnable click,int height){
        Button b=new Button(service);
        b.setAllCaps(false);b.setText(label);
        b.setTextColor(Color.WHITE);b.setTextSize(12);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setBackground(gradient(color,color==RED?0xFF933D55:0xFF453999,8));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(height),1);
        p.setMargins(dp(3),0,dp(3),0);
        into.addView(b,p);
        b.setOnClickListener(v->{click.run();refresh();});
    }
    private CheckBox option(String label,String key,boolean def){
        CheckBox cb=new CheckBox(service);
        cb.setButtonTintList(ColorStateList.valueOf(ACCENT));
        cb.setText(label);cb.setTextColor(TEXT);cb.setTextSize(12);
        cb.setChecked(prefs.getBoolean(key,def));
        cb.setPadding(dp(2),0,dp(2),0);
        cb.setOnCheckedChangeListener((v,checked)->prefs.edit().putBoolean(key,checked).apply());
        return cb;
    }
    private void seek(String title,String key,int lo,int hi,int fallback){
        int value=Math.min(hi,Math.max(lo,prefs.getInt(key,fallback)));
        LinearLayout row=horizontal();
        TextView label=text(title,12,TEXT,false);
        row.addView(label,new LinearLayout.LayoutParams(0,dp(29),1));
        TextView number=text(Integer.toString(value),12,ACCENT,true);
        number.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        row.addView(number,lp(45,29));
        page.addView(row);
        SeekBar bar=new SeekBar(service);
        bar.setMax(hi-lo);bar.setProgress(value-lo);
        bar.setProgressTintList(ColorStateList.valueOf(ACCENT));
        bar.setThumbTintList(ColorStateList.valueOf(ACCENT));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar b,int progress,boolean fromUser){
                number.setText(Integer.toString(lo+progress));
                if(fromUser)prefs.edit().putInt(key,lo+progress).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar b){}
            @Override public void onStopTrackingTouch(SeekBar b){}
        });
        page.addView(bar,lp(-1,34));
    }
    private void drawPage(){
        if(page==null)return;
        page.removeAllViews();
        if(selectedTab==0){
            section(page,"TRẠNG THÁI HỆ THỐNG");
            statusView=info("Đang khởi tạo...");
            page.addView(statusView,lp(-1,50));
            addSpace(page,5);
            substatusView=text("",11,MUTED,false);
            substatusView.setMaxLines(2);
            page.addView(substatusView,lp(-1,43));
            section(page,"ĐIỀU KHIỂN");
            LinearLayout row=horizontal();page.addView(row,lp(-1,45));
            actionButton(row,prefs.getBoolean("enabled",false)?"DỪNG BOT":"BẬT BOT",0xFF157DB2,
                ()->actions.setEnabled(!prefs.getBoolean("enabled",false)),44);
            actionButton(row,"THẢ LÍNH",0xFF168C7D,()->actions.deploy(),44);
            addSpace(page,9);
            LinearLayout second=horizontal();page.addView(second,lp(-1,43));
            actionButton(second,"DỪNG NGAY",RED,()->actions.stop(),42);
            actionButton(second,"LÀM MỚI",0xFF45578E,()->actions.reset(),42);
        } else if(selectedTab==1){
            section(page,"ĐỘI HÌNH / TROOP DEPLOY");
            seek("Số ô lính", "troop_slots",1,7,4);
            seek("Số lượt thả / ô", "drops_per_slot",1,8,4);
            addSpace(page,5);
            page.addView(option("Tự động triển khai lính","auto_deploy",false),lp(-1,42));
            page.addView(info("Số lượt sẽ lấy từ cài đặt. Vị trí tính theo ảnh game, không dùng pixel cố định."),lp(-1,64));
            addSpace(page,8);
            LinearLayout row=horizontal();page.addView(row,lp(-1,43));
            actionButton(row,"THẢ LÍNH NGAY",0xFF168C7D,()->actions.deploy(),42);
        }else{
            section(page,"CÀI ĐẶT TỰ ĐỘNG");
            page.addView(option("Tự nhận diện Battle","enabled",false),lp(-1,38));
            page.addView(option("Tự thả lính trong trận","auto_deploy",false),lp(-1,38));
            page.addView(option("Tự nhấn Về nhà khi kết thúc","auto_return",true),lp(-1,38));
            addSpace(page,7);
            page.addView(info("Menu nổi kiểu ImGui chạy qua Trợ năng Android. Kéo thanh tiêu đề để di chuyển. Nhấn dấu — để thu gọn về icon ERI."),lp(-1,77));
            addSpace(page,9);
            LinearLayout row=horizontal();page.addView(row,lp(-1,40));
            actionButton(row,"THU GỌN MENU",0xFF4A5779,()->applyOpen(false),40);
        }
        refresh();
    }
    public void refresh(){
        if(!added || !open)return;
        if(statusView!=null){
            boolean enabled=prefs.getBoolean("enabled",false);
            String phase=prefs.getString("phase","READY");
            statusView.setText((enabled?"● ĐANG HOẠT ĐỘNG":"● BOT ĐANG DỪNG")+"  •  "+phase);
            statusView.setTextColor(enabled?GREEN:RED);
        }
        if(substatusView!=null)
            substatusView.setText(prefs.getString("last_status","Chưa có sự kiện"));
    }
    private View.OnTouchListener dragHandler(boolean draggingPanel){
        return (v,event)->{
            if(params==null)return true;
            switch(event.getActionMasked()){
                case MotionEvent.ACTION_DOWN:
                    startX=event.getRawX();startY=event.getRawY();
                    originalX=params.x;originalY=params.y;
                    moved=false;return true;
                case MotionEvent.ACTION_MOVE:
                    int dx=Math.round(event.getRawX()-startX);
                    int dy=Math.round(event.getRawY()-startY);
                    if(Math.abs(dx)>dp(5)||Math.abs(dy)>dp(5))moved=true;
                    if(moved){
                        int width=draggingPanel?panelWidth:dp(67);
                        int height=draggingPanel?Math.min(dp(440),screenH-dp(12)):dp(55);
                        params.x=Math.max(dp(2),Math.min(screenW-width-dp(2),originalX+dx));
                        params.y=Math.max(dp(2),Math.min(screenH-height-dp(2),originalY+dy));
                        if(added)wm.updateViewLayout(root,params);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if(draggingPanel){
                        prefs.edit().putInt("menu_panel_x",params.x).putInt("menu_panel_y",params.y).apply();
                    }else{
                        prefs.edit().putInt("menu_chip_x",params.x).putInt("menu_chip_y",params.y).apply();
                        if(!moved)applyOpen(true);
                    }
                    return true;
                default:return true;
            }
        };
    }
    private void applyOpen(boolean wantOpen){
        open=wantOpen;
        chip.setVisibility(open?View.GONE:View.VISIBLE);
        panel.setVisibility(open?View.VISIBLE:View.GONE);
        if(params!=null){
            if(open){
                params.x=prefs.getInt("menu_panel_x",screenW-panelWidth-dp(16));
                params.y=prefs.getInt("menu_panel_y",dp(65));
            }else{
                params.x=prefs.getInt("menu_chip_x",screenW-dp(78));
                params.y=prefs.getInt("menu_chip_y",dp(86));
            }
            int width=open?panelWidth:dp(67);
            int height=open?Math.min(dp(440),screenH-dp(12)):dp(55);
            params.x=Math.max(dp(2),Math.min(screenW-width-dp(2),params.x));
            params.y=Math.max(dp(2),Math.min(screenH-height-dp(2),params.y));
            if(added){
                wm.updateViewLayout(root,params);
                if(open)drawPage();
            }
        }
    }
    public void show(boolean visible){
        if(visible==added)return;
        try{
            if(visible){
                updateScreenSize();
                params=new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT);
                params.gravity=Gravity.TOP|Gravity.LEFT;
                applyOpen(open);
                wm.addView(root,params);added=true;
            }else{
                wm.removeView(root);added=false;
            }
        }catch(Exception e){
            added=false;
            prefs.edit().putString("last_status","Lỗi menu nổi: "+e.getClass().getSimpleName()).apply();
        }
    }
    public void destroy(){show(false);}
}
