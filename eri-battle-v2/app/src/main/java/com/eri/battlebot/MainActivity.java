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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private TextView status,detail;
    private Button toggle;
    private final Runnable refresh=new Runnable(){
        @Override public void run(){updateUi();handler.postDelayed(this,1200);}
    };
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        prefs=getSharedPreferences("eri_v2",MODE_PRIVATE);
        getWindow().setStatusBarColor(Color.rgb(7,16,43));
        getWindow().setNavigationBarColor(Color.rgb(7,16,43));
        LinearLayout layout=new LinearLayout(this);
        layout.setPadding(28,34,28,28);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(0xff091330);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        scroll.addView(layout);setContentView(scroll);
        layout.addView(text("MOD BY ERI NGUYỄN",24,0xff5bccff,true));
        TextView title=text("BATTLE DETECTOR V2 · KHÔNG DÙNG BOT CŨ",14,0xffb4c7ee,false);
        title.setPadding(0,14,0,18);layout.addView(title);
        layout.addView(text("Nhận diện nút Battle từ ảnh chụp màn hình thực, suy ra tọa độ và chạm. Không sử dụng tọa độ cố định, không có hệ thống key login.",15,Color.WHITE,false));
        status=text("",17,0xff45efd1,true);
        status.setPadding(0,26,0,12);layout.addView(status);
        toggle=button("",0xff217aff);
        toggle.setOnClickListener(v->{
            prefs.edit().putBoolean("enabled",!prefs.getBoolean("enabled",false)).apply();
            updateUi();
        });
        layout.addView(toggle);
        Button access=button("MỞ CÀI ĐẶT TRỢ NĂNG",0xff5a35a5);
        access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        layout.addView(access);
        detail=text("",14,0xffcbd7ed,false);
        detail.setPadding(0,24,0,20);layout.addView(detail);
        layout.addView(text("HƯỚNG DẪN\n1. Bật ERI Battle V2 trong Cài đặt > Trợ năng.\n2. Quay lại đây, bật nhận diện.\n3. Mở Clash of Clans ở chế độ ngang.\n4. Khi Battle hiện ra, bot xác nhận qua 2 ảnh liên tiếp rồi bấm giữa nút.\n\nBản mới chỉ nhận diện và bấm Battle. Không chứa chu kỳ tấn công, đổi acc hay hệ thống bot cũ. Android 11 trở lên.",14,0xffe6eafe,false));
        updateUi();
    }
    @Override public void onResume(){super.onResume();handler.removeCallbacks(refresh);handler.post(refresh);}
    @Override public void onPause(){handler.removeCallbacks(refresh);super.onPause();}
    private void updateUi(){
        if(status==null)return;
        boolean on=prefs.getBoolean("enabled",false),ready=prefs.getBoolean("service_ready",false);
        toggle.setText(on?"DỪNG NHẬN DIỆN":"BẬT NHẬN DIỆN BATTLE");
        status.setText(!ready?"● Trợ năng chưa kết nối":on?"● Đang tìm nút Battle":"● Đang tạm dừng");
        detail.setText("Trạng thái: "+prefs.getString("last_status","Chưa chạy")+"\n"+prefs.getString("last_match","Chưa có tọa độ"));
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
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,128);p.setMargins(0,13,0,5);b.setLayoutParams(p);
        return b;
    }
}
