package com.eri.battlebot;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private TextView status,detail;
    private Button toggle;
    private final Runnable refresh=new Runnable(){
        @Override public void run(){updateUi();handler.postDelayed(this,1000);}
    };
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        prefs=getSharedPreferences("eri_v2",MODE_PRIVATE);
        getWindow().setStatusBarColor(0xff07102b);
        getWindow().setNavigationBarColor(0xff07102b);
        LinearLayout layout=new LinearLayout(this);
        layout.setPadding(28,25,28,28);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(0xff091330);
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(layout);
        setContentView(scroll);
        layout.addView(text("MOD BY ERI NGUYỄN",24,0xff5bccff,true));
        TextView title=text("BATTLE V3 · TỰ TRIỂN KHAI LÍNH",14,0xffb4c7ee,true);
        title.setPadding(0,12,0,14);layout.addView(title);
        layout.addView(text("Bot mới độc lập. Chụp ảnh game qua Trợ năng, tính vị trí thanh lính và vùng triển khai theo màn hình thực tế. Có nút THẢ LÍNH ngay trong game.",14,Color.WHITE,false));
        status=text("",17,0xff45efd1,true);
        status.setPadding(0,18,0,12);layout.addView(status);
        toggle=button("",0xff217aff);
        toggle.setOnClickListener(v->{
            boolean next=!prefs.getBoolean("enabled",false);
            prefs.edit().putBoolean("enabled",next).putBoolean("stop_request",!next).apply();
            updateUi();
        });
        layout.addView(toggle);
        layout.addView(check("Tự triển khai khi nhận ra màn hình trận", "auto_deploy",false));
        layout.addView(check("Tự nhấn Về nhà khi thấy kết quả trận", "auto_return",true));
        slider(layout,"Số ô lính sử dụng", "troop_slots",1,7,4);
        slider(layout,"Số lần thả mỗi ô", "drops_per_slot",1,8,4);
        Button reset=button("LÀM MỚI CHU KỲ / CHO PHÉP TRẬN MỚI",0xff1b877c);
        reset.setOnClickListener(v->{
            prefs.edit().putBoolean("reset_request",true).putBoolean("stop_request",false)
                .putBoolean("enabled",true).apply();
            updateUi();
        });
        layout.addView(reset);
        Button access=button("CÀI ĐẶT TRỢ NĂNG",0xff5a35a5);
        access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        layout.addView(access);
        detail=text("",14,0xffcbd7ed,false);
        detail.setPadding(0,22,0,20);layout.addView(detail);
        layout.addView(text("CÁCH DÙNG\n1. Bật ERI Battle V2/V3 trong Cài đặt Trợ năng.\n2. Bật bot và chọn số ô lính, số lần thả.\n3. Vào Clash of Clans. Bot tìm nút Battle. Khi trong trận, nó nhận diện thông báo trận và tự chọn lính, thả theo vùng ở mép bản đồ.\n4. Nếu không nhận diện được HUD, nhấn nút nổi THẢ LÍNH trong game. Nút DỪNG sẽ ngắt các lượt chạm còn lại.\n5. Khi trận kết thúc, tùy chọn Tự về nhà chỉ bấm nếu đọc được nút Về nhà từ ảnh.\n\nLưu ý: Chỉ thả các ô lính đã chọn, không bảo đảm thắng; không dùng GPS, root hoặc mã bot Xmagic cũ.",14,0xffe6eafe,false));
        updateUi();
    }
    private CheckBox check(String label,String key,boolean defaultValue){
        CheckBox cb=new CheckBox(this);
        cb.setText(label);cb.setTextColor(0xffe9ecff);cb.setTextSize(15);
        cb.setButtonTintList(android.content.res.ColorStateList.valueOf(0xff50c6ff));
        cb.setPadding(0,10,0,10);
        cb.setChecked(prefs.getBoolean(key,defaultValue));
        cb.setOnCheckedChangeListener((button,checked)->prefs.edit().putBoolean(key,checked).apply());
        return cb;
    }
    private void slider(LinearLayout root,String title,String key,int min,int max,int defaultValue){
        TextView label=text("",14,0xffebefff,true);
        label.setPadding(0,12,0,4);root.addView(label);
        SeekBar seek=new SeekBar(this);
        int value=Math.max(min,Math.min(max,prefs.getInt(key,defaultValue)));
        seek.setMax(max-min);
        seek.setProgress(value-min);
        label.setText(title+": "+value);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar s,int progress,boolean fromUser){
                int number=min+progress;label.setText(title+": "+number);
                if(fromUser)prefs.edit().putInt(key,number).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar s){}
            @Override public void onStopTrackingTouch(SeekBar s){}
        });
        root.addView(seek);
    }
    @Override public void onResume(){super.onResume();handler.removeCallbacks(refresh);handler.post(refresh);}
    @Override public void onPause(){handler.removeCallbacks(refresh);super.onPause();}
    private void updateUi(){
        if(status==null)return;
        boolean on=prefs.getBoolean("enabled",false),ready=prefs.getBoolean("service_ready",false);
        toggle.setText(on?"DỪNG TỰ ĐỘNG":"BẬT BOT BATTLE V3");
        status.setText(!ready?"● Trợ năng chưa kết nối":on?"● Bot đang theo dõi màn hình":"● Bot đã tạm dừng");
        detail.setText("Trạng thái: "+prefs.getString("last_status","Chưa chạy")
            +"\nChu kỳ: "+prefs.getString("phase","Đợi bật bot")
            +"\nTọa độ: "+prefs.getString("last_match","Chưa nhận diện"));
    }
    private TextView text(String v,int sp,int color,boolean bold){
        TextView t=new TextView(this);t.setText(v);t.setTextSize(sp);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        t.setLineSpacing(2,1.14f);return t;
    }
    private Button button(String label,int color){
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(15);b.setTextColor(Color.WHITE);
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{color,0xff8547ef});
        bg.setCornerRadius(25);b.setBackground(bg);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,112);p.setMargins(0,10,0,5);
        b.setLayoutParams(p);return b;
    }
}
