package com.eri.battlebot;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
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
 * In-game overlay styled after the provided ERI COC screenshot.
 * Pure Android Accessibility overlay, not injected native Dear ImGui.
 * This component never changes MainActivity's original layout.
 */
public final class EriImGuiOverlay {
    public interface Actions {
        void setEnabled(boolean enabled);
        void deploy();
        void stop();
        void reset();
        void navigate(String feature);
    }
    private static final int BG=0xF5141E3B, PANEL=0xEF1B294B, DEEP=0xFF0B1430;
    private static final int CYAN=0xFF57D8FF, TEXT=0xFFECF5FF, MUTED=0xFF9FB1D0;
    private static final int RED=0xFFFB6679, GREEN=0xFF67F0C3, BLUE=0xFF368DFF;
    private final AccessibilityService service;
    private final SharedPreferences prefs;
    private final Actions actions;
    private final WindowManager wm;
    private FrameLayout root;
    private LinearLayout menu;
    private LinearLayout bodyPage;
    private TextView icon, headerStatus, logStatus, logDetails, runButton;
    private WindowManager.LayoutParams params;
    private boolean attached=false, expanded=false, dragging=false;
    private int currentTab=0, selectedMode=0, screenWidth=0,screenHeight=0, menuWidth=0,menuHeight=0;
    private int dragX,dragY;
    private float touchX,touchY;

    public EriImGuiOverlay(AccessibilityService service,SharedPreferences prefs,Actions actions){
        this.service=service; this.prefs=prefs; this.actions=actions;
        this.wm=(WindowManager)service.getSystemService(Context.WINDOW_SERVICE);
        dimensions();
        create();
    }
    private int dp(float x){return Math.round(x*service.getResources().getDisplayMetrics().density);}
    private void dimensions(){
        Point p=new Point();wm.getDefaultDisplay().getRealSize(p);
        screenWidth=p.x;screenHeight=p.y;
        menuWidth=Math.min(dp(540),Math.max(dp(275),screenWidth-dp(18)));
        menuHeight=Math.min(dp(360),Math.max(dp(210),screenHeight-dp(12)));
    }
    private GradientDrawable fill(int color,int radius,int border){
        GradientDrawable g=new GradientDrawable();
        g.setColor(color);g.setCornerRadius(dp(radius));
        if(border!=0)g.setStroke(dp(1),border);
        return g;
    }
    private GradientDrawable gradient(int a,int b,int radius){
        GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{a,b});
        g.setCornerRadius(dp(radius));return g;
    }
    private TextView text(String str,int size,int color,boolean strong){
        TextView t=new TextView(service);
        t.setText(str);t.setTextSize(size);t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        if(strong)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private LinearLayout horizontal(){
        LinearLayout l=new LinearLayout(service);
        l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }
    private LinearLayout vertical(){
        LinearLayout l=new LinearLayout(service);l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }
    private LinearLayout.LayoutParams size(int w,int h){
        return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));
    }
    private void space(LinearLayout target,int height){
        View v=new View(service);target.addView(v,size(1,height));
    }
    private void create(){
        root=new FrameLayout(service);
        root.setClipChildren(false);
        icon=text("✦\nERI",13,Color.WHITE,true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(gradient(0xFF18478B,0xFF0E1F4D,16));
        root.addView(icon,new FrameLayout.LayoutParams(dp(62),dp(62),Gravity.TOP|Gravity.LEFT));
        icon.setOnTouchListener(dragListener(false));

        menu=vertical();
        menu.setBackground(fill(BG,15,0xFF3E6BA0));
        menu.setClipChildren(true);
        FrameLayout.LayoutParams mp=new FrameLayout.LayoutParams(menuWidth,menuHeight,Gravity.TOP|Gravity.LEFT);
        root.addView(menu,mp);
        drawMenu();
        applyExpanded(false);
    }
    private TextView control(String label,int color){
        TextView b=text(label,17,color,true);b.setGravity(Gravity.CENTER);
        b.setBackground(fill(0xFF152040,7,0xFF34446E));
        return b;
    }
    private void drawMenu(){
        menu.removeAllViews();
        LinearLayout head=horizontal();head.setPadding(dp(8),dp(5),dp(6),dp(5));
        head.setBackground(gradient(0xFF152C55,0xFF0C1939,11));
        menu.addView(head,size(-1,49));
        TextView logo=text("✦",22,CYAN,true);logo.setGravity(Gravity.CENTER);
        logo.setBackground(fill(0xFF0C2B58,9,0xFF377CC7));
        head.addView(logo,size(37,37));
        LinearLayout name=vertical();
        name.setPadding(dp(9),0,0,0);
        name.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams nameP=new LinearLayout.LayoutParams(0,-1,1);
        head.addView(name,nameP);
        TextView title=text("ERI COC",16,CYAN,true);
        name.addView(title);
        TextView sub=text("Mod By Eri Nguyễn",10,MUTED,false);
        name.addView(sub);
        headerStatus=text("● Đang dừng",11,MUTED,false);
        headerStatus.setPadding(dp(8),0,dp(8),0);
        headerStatus.setSingleLine(true);headerStatus.setMaxWidth(dp(165));
        headerStatus.setBackground(fill(0xFF101B38,13,0xFF354B7A));
        head.addView(headerStatus,size(144,34));
        View gutter=new View(service);head.addView(gutter,size(5,1));

        TextView gear=control("⚙",MUTED);
        head.addView(gear,size(33,32));
        gear.setOnClickListener(v->{currentTab=6;drawMenu();});
        TextView minus=control("−",CYAN);
        head.addView(minus,size(33,32));
        minus.setOnClickListener(v->applyExpanded(false));
        TextView close=control("×",RED);
        head.addView(close,size(33,32));
        close.setOnClickListener(v->applyExpanded(false));
        head.setOnTouchListener(dragListener(true));

        LinearLayout content=horizontal();
        menu.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=vertical();
        nav.setPadding(dp(3),dp(4),dp(4),dp(3));
        nav.setBackground(fill(0xFF0F1937,0,0));
        content.addView(nav,size(76,-1));
        String[] labels={"CHÍNH","ĐÊM","HỘI","NÂNG","KINH ĐÔ","ACC","LOG"};
        String[] pict={"⌂","☾","♜","⚒","★","♙","≡"};
        for(int i=0;i<labels.length;i++){
            final int n=i;
            TextView tab=text(pict[i]+" "+labels[i],10,i==currentTab?TEXT:MUTED,i==currentTab);
            tab.setGravity(Gravity.CENTER);
            tab.setBackground(fill(i==currentTab?0xFF1A4E7D:0xFF111B39,8,i==currentTab?CYAN:0));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,0,1);
            p.setMargins(0,0,0,dp(2));
            nav.addView(tab,p);
            tab.setOnClickListener(v->{currentTab=n;drawMenu();});
        }
        ScrollView scroll=new ScrollView(service);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        content.addView(scroll,new LinearLayout.LayoutParams(0,-1,1));
        bodyPage=vertical();
        bodyPage.setPadding(dp(10),dp(6),dp(9),dp(5));
        scroll.addView(bodyPage);
        drawPage();

        LinearLayout footer=horizontal();
        footer.setPadding(dp(10),dp(4),dp(10),dp(5));
        footer.setBackground(fill(0xFF111F40,0,0));
        menu.addView(footer,size(-1,51));
        runButton=text("▶  RUN",19,Color.WHITE,true);
        runButton.setGravity(Gravity.CENTER);
        runButton.setBackground(gradient(0xFF267BF5,0xFF8A4DEA,12));
        footer.addView(runButton,new LinearLayout.LayoutParams(0,-1,1));
        runButton.setOnClickListener(v->runCurrentTab());
        refresh();
    }
    private void section(String label){
        TextView t=text(label,11,0xFF78C7EB,true);t.setAllCaps(true);
        LinearLayout.LayoutParams p=size(-1,28);
        bodyPage.addView(t,p);
    }
    private void note(String description){
        TextView t=text(description,11,MUTED,false);
        t.setPadding(dp(9),dp(6),dp(8),dp(6));
        t.setBackground(fill(0xFF101C3C,8,0xFF263B6A));
        bodyPage.addView(t,size(-1,-2));
    }
    private void cardRow(String[] labels,String[] emojis,int active){
        LinearLayout row=horizontal();bodyPage.addView(row,size(-1,94));
        for(int i=0;i<labels.length;i++){
            final int mode=i;
            boolean enabled=i!=1;
            boolean selected=i==active;
            LinearLayout card=vertical();
            card.setGravity(Gravity.CENTER);
            card.setPadding(dp(2),dp(2),dp(2),dp(2));
            card.setBackground(fill(selected?0xFF245181:0xFF162547,11,
                selected?CYAN:0xFF344B71));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(91),1);
            p.setMargins(dp(2),0,dp(2),0);row.addView(card,p);
            TextView big=text(emojis[i],29,enabled?0xFFF5CD73:MUTED,true);
            big.setGravity(Gravity.CENTER);card.addView(big,size(-1,46));
            TextView name=text(labels[i],9,enabled?TEXT:MUTED,selected);
            name.setGravity(Gravity.CENTER);name.setMaxLines(2);
            card.addView(name,size(-1,34));
            if(enabled){
                card.setOnClickListener(v->{
                    selectedMode=mode;prefs.edit().putInt("eri_selected_mode",mode).apply();
                    drawMenu();
                });
            } else {
                card.setAlpha(.48f);
            }
        }
    }
    private void actionRow(String first,String firstAction,String second,String secondAction){
        LinearLayout row=horizontal();bodyPage.addView(row,size(-1,36));
        action(row,first,firstAction,0xFF226DA0);
        action(row,second,secondAction,0xFF2D477D);
    }
    private void action(LinearLayout parent,String label,String feature,int shade){
        TextView button=text(label,11,TEXT,true);button.setGravity(Gravity.CENTER);
        button.setBackground(gradient(shade,0xFF354B82,7));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(34),1);
        p.setMargins(dp(2),0,dp(2),0);parent.addView(button,p);
        button.setOnClickListener(v->perform(feature));
    }
    private void option(String label,String key,boolean defaultValue){
        CheckBox cb=new CheckBox(service);
        cb.setText(label);cb.setTextSize(12);cb.setTextColor(TEXT);
        cb.setButtonTintList(ColorStateList.valueOf(CYAN));
        cb.setChecked(prefs.getBoolean(key,defaultValue));
        cb.setOnCheckedChangeListener((btn,checked)->
            prefs.edit().putBoolean(key,checked).apply());
        bodyPage.addView(cb,size(-1,32));
    }
    private void slider(String name,String key,int min,int max,int defaultValue){
        int value=Math.max(min,Math.min(max,prefs.getInt(key,defaultValue)));
        LinearLayout row=horizontal();bodyPage.addView(row,size(-1,27));
        TextView title=text(name,12,TEXT,true);
        row.addView(title,new LinearLayout.LayoutParams(0,-1,1));
        TextView number=text(Integer.toString(value),12,CYAN,true);
        row.addView(number,size(36,-1));
        SeekBar bar=new SeekBar(service);bar.setMax(max-min);
        bar.setProgress(value-min);bar.setProgressTintList(ColorStateList.valueOf(CYAN));
        bar.setThumbTintList(ColorStateList.valueOf(CYAN));
        bodyPage.addView(bar,size(-1,32));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int progress,boolean user){
                number.setText(Integer.toString(min+progress));
                if(user)prefs.edit().putInt(key,min+progress).apply();
            }
            public void onStartTrackingTouch(SeekBar s){}
            public void onStopTrackingTouch(SeekBar s){}
        });
    }
    private void drawPage(){
        if(bodyPage==null)return;
        bodyPage.removeAllViews();logStatus=null;logDetails=null;
        switch(currentTab){
            case 0:
                selectedMode=Math.max(0,Math.min(3,prefs.getInt("eri_selected_mode",0)));
                section("CHẾ ĐỘ BOT");
                cardRow(new String[]{"ĐẤU THƯỜNG","ĐẤU RANK\nCHƯA CÓ","CHỈ CHO LÍNH","CHỈ XIN LÍNH"},
                    new String[]{"⚔","♛","♜","✚"},selectedMode);
                section("CÀI ĐẶT TRIỂN KHAI");
                option("Tự thả lính khi vào trận","auto_deploy",false);
                slider("Số ô lính", "troop_slots",1,7,4);
                section("ĐẦU HÀNG THÔNG MINH");
                note("Tính năng này chưa triển khai. RUN chỉ chạy chế độ được hỗ trợ.");
                break;
            case 1:
                section("LÀNG ĐÊM");
                note("Mở làng thợ xây và các chức năng đang hiển thị trong game.");
                space(bodyPage,7);
                actionRow("MỞ LÀNG ĐÊM","builderbase","CHIẾN ĐẤU","battle");
                space(bodyPage,7);
                option("Tự nhấn Về nhà khi nhận diện","auto_return",true);
                break;
            case 2:
                section("HỘI / CLAN");
                actionRow("VÀO HỘI","clan","CHIẾN TRANH","war");
                space(bodyPage,7);
                actionRow("TẶNG LÍNH","donate","ĐỘT KÍCH","raid");
                space(bodyPage,7);
                actionRow("SỰ KIỆN","events","NHẬT KÝ","battlelog");
                note("Các nút chỉ mở mục mà OCR đọc được. Không tự chi tài nguyên.");
                break;
            case 3:
                section("NÂNG CẤP");
                actionRow("XÂY DỰNG","build","NGHIÊN CỨU","lab");
                space(bodyPage,7);
                actionRow("ANH HÙNG","hero","TRANG BỊ","equipment");
                space(bodyPage,7);
                actionRow("THÚ CƯNG","pet","THU TÀI NGUYÊN","collect");
                note("Không tự mua gem hoặc xác nhận nâng cấp.");
                break;
            case 4:
                section("KINH ĐÔ HỘI");
                actionRow("THỦ ĐÔ","capital","ĐỘT KÍCH","raid");
                space(bodyPage,8);
                actionRow("HỘI","clan","CHIẾN TRANH","war");
                note("Điều hướng theo chữ hiển thị trong game. Không tự dùng huy chương.");
                break;
            case 5:
                section("TÀI KHOẢN / ACC");
                actionRow("HỒ SƠ","profile","ĐỘI QUÂN","army");
                space(bodyPage,7);
                actionRow("NHẬT KÝ ĐẤU","battlelog","CỬA HÀNG","shop");
                note("Không tự chuyển tài khoản hoặc truy cập thông tin đăng nhập.");
                break;
            default:
                section("NHẬT KÝ HOẠT ĐỘNG");
                logStatus=text("",11,GREEN,true);
                bodyPage.addView(logStatus,size(-1,27));
                logDetails=text("",10,MUTED,false);
                logDetails.setMaxLines(12);
                bodyPage.addView(logDetails,size(-1,-2));
                actionRow("QUÉT MÀN HÌNH","scan","LÀM MỚI","reset");
                break;
        }
        refresh();
    }
    private void runCurrentTab(){
        switch(currentTab){
            case 0:
                if(selectedMode==0){
                    actions.setEnabled(true);
                    actions.reset();
                    applyExpanded(false);
                }else if(selectedMode==2){
                    perform("donate");
                }else if(selectedMode==3){
                    perform("clan");
                }else{
                    prefs.edit().putString("last_status",
                        "Chế độ Đấu rank chưa hỗ trợ").apply();
                    refresh();
                }break;
            case 1:perform("builderbase");break;
            case 2:perform("clan");break;
            case 3:perform("build");break;
            case 4:perform("capital");break;
            case 5:perform("profile");break;
            default:refresh();break;
        }
    }
    private void perform(String feature){
        if("reset".equals(feature)){actions.reset();refresh();return;}
        actions.navigate(feature);
        applyExpanded(false);
    }
    public void refresh(){
        if(headerStatus!=null){
            boolean running=prefs.getBoolean("enabled",false);
            headerStatus.setText(running?"● "+prefs.getString("phase","BOT ĐANG CHẠY"):"● Bot đã dừng hoàn toàn");
            headerStatus.setTextColor(running?GREEN:MUTED);
        }
        if(logStatus!=null)
            logStatus.setText(prefs.getString("last_status","Chưa có hoạt động"));
        if(logDetails!=null)
            logDetails.setText(prefs.getString("recent_log","Chưa có log"));
    }
    private View.OnTouchListener dragListener(boolean header){
        return (v,e)->{
            if(params==null)return true;
            switch(e.getActionMasked()){
                case MotionEvent.ACTION_DOWN:
                    touchX=e.getRawX();touchY=e.getRawY();
                    dragX=params.x;dragY=params.y;dragging=false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int dx=Math.round(e.getRawX()-touchX);
                    int dy=Math.round(e.getRawY()-touchY);
                    if(Math.abs(dx)>dp(5)||Math.abs(dy)>dp(5))dragging=true;
                    if(dragging){
                        params.x=clamp(dragX+dx,dp(2),screenWidth-(expanded?menuWidth:dp(62))-dp(2));
                        params.y=clamp(dragY+dy,dp(2),screenHeight-(expanded?menuHeight:dp(62))-dp(2));
                        if(attached)wm.updateViewLayout(root,params);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if(header){
                        prefs.edit().putInt("panel_x",params.x).putInt("panel_y",params.y).apply();
                    }else{
                        prefs.edit().putInt("bubble_x",params.x).putInt("bubble_y",params.y).apply();
                        if(!dragging)applyExpanded(true);
                    }
                    return true;
                default:return true;
            }
        };
    }
    private int clamp(int v,int min,int max){return Math.max(min,Math.min(Math.max(min,max),v));}
    private void applyExpanded(boolean state){
        expanded=state;
        if(icon!=null)icon.setVisibility(state?View.GONE:View.VISIBLE);
        if(menu!=null)menu.setVisibility(state?View.VISIBLE:View.GONE);
        if(params!=null){
            params.width=state?menuWidth:dp(62);
            params.height=state?menuHeight:dp(62);
            params.x=clamp(prefs.getInt(state?"panel_x":"bubble_x",
                state?dp(55):screenWidth-dp(72)),dp(2),screenWidth-params.width-dp(2));
            params.y=clamp(prefs.getInt(state?"panel_y":"bubble_y",dp(58)),
                dp(2),screenHeight-params.height-dp(2));
            if(attached)wm.updateViewLayout(root,params);
        }
    }
    public void show(boolean visible){
        if(visible==attached)return;
        try{
            if(visible){
                dimensions();
                params=new WindowManager.LayoutParams(
                    expanded?menuWidth:dp(62),expanded?menuHeight:dp(62),
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT);
                params.gravity=Gravity.TOP|Gravity.LEFT;
                applyExpanded(expanded);
                wm.addView(root,params);
                attached=true;
            }else{
                wm.removeView(root);attached=false;
            }
        }catch(Exception e){
            attached=false;
            prefs.edit().putString("last_status",
                "Lỗi menu nổi: "+e.getClass().getSimpleName()).apply();
        }
    }
    public void destroy(){show(false);}
}
