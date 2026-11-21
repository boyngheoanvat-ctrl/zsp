package com.eri.zsm;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public final class EriBridge {
    private EriBridge() {}
    private static native String nativeStatus();
    private static native void nativeCommand(int cmd,int a,int b,int c,int d);
    private static final ExecutorService jobs=Executors.newSingleThreadExecutor();
    private static final Handler handler=new Handler(Looper.getMainLooper());
    private static final int[] rainbow={0xffff0000,0xffff7700,0xffffff00,0xff00ff00,0xff00ffff,0xff0000ff,0xffff00ff};
    private static Activity activity;
    private static FrameLayout layer;
    private static ScrollView scroll;
    private static TextView heading, status;
    private static int frame=0;
    private static int dp(int n){return (int)(n*activity.getResources().getDisplayMetrics().density+0.5f);}
    private static GradientDrawable bg(int fill,int border){
        GradientDrawable d=new GradientDrawable();
        d.setColor(fill);d.setCornerRadius(dp(13));d.setStroke(dp(1),border);return d;
    }
    private static TextView text(String s,int size,int color){
        TextView t=new TextView(activity);
        t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(dp(5),dp(7),dp(5),dp(7));
        return t;
    }
    private static Button button(String s,LinearLayout parent){
        Button b=new Button(activity);b.setAllCaps(false);b.setText(s);b.setTextSize(13);
        b.setTextColor(Color.WHITE);b.setBackground(bg(0xff123c58,0xff138ec2));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(42));p.bottomMargin=dp(8);
        parent.addView(b,p);return b;
    }
    private static EditText edit(String hint,String val,LinearLayout row){
        EditText e=new EditText(activity);e.setSingleLine(true);e.setText(val);e.setHint(hint);
        e.setTextColor(Color.WHITE);e.setHintTextColor(0xffa9b9c2);e.setTextSize(12);
        e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_SIGNED);
        e.setBackground(bg(0xff0e2638,0xff2c647d));
        e.setPadding(dp(8),0,dp(6),0);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(42),1);
        p.rightMargin=dp(4);row.addView(e,p);return e;
    }
    private static int number(EditText e,int def){
        try{return Integer.parseInt(e.getText().toString().trim());}catch(Exception x){return def;}
    }
    private static void call(final int cmd,final int a,final int b,final int c,final int d){
        if(status!=null)status.setText("Đang kiểm tra runtime...");
        jobs.execute(new Runnable(){@Override public void run(){
            try{nativeCommand(cmd,a,b,c,d);}
            catch(Throwable t){android.util.Log.e("ERI_ZSM","native action error",t);}
        }});
    }
    public static void install(final Activity a){
        if(a!=null)a.runOnUiThread(new Runnable(){@Override public void run(){show(a);}});
    }
    private static void show(Activity a){
        if(activity==a&&layer!=null&&layer.getParent()!=null)return;
        activity=a;
        ViewGroup root=a.findViewById(android.R.id.content);if(root==null)return;
        layer=new FrameLayout(a);layer.setClipChildren(false);layer.setClickable(false);
        root.addView(layer,new ViewGroup.LayoutParams(-1,-1));
        TextView icon=text("ERI",18,0xffcaffff);icon.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        icon.setGravity(Gravity.CENTER);icon.setBackground(bg(0xff02334c,0xff00e5ff));
        FrameLayout.LayoutParams ib=new FrameLayout.LayoutParams(dp(58),dp(58),Gravity.RIGHT|Gravity.TOP);
        ib.setMargins(0,dp(75),dp(14),0);layer.addView(icon,ib);
        scroll=new ScrollView(a);scroll.setFillViewport(false);
        scroll.setBackground(bg(0xf0081825,0xff18b9fb));
        FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(dp(330),-2,Gravity.CENTER);
        layer.addView(scroll,sp);
        final LinearLayout panel=new LinearLayout(a);panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12),dp(10),dp(12),dp(10));scroll.addView(panel);
        heading=text("Mod By Eri Nguyễn",21,Color.CYAN);heading.setGravity(Gravity.CENTER);
        heading.setTypeface(Typeface.DEFAULT,Typeface.BOLD);panel.addView(heading);
        TextView sub=text("ERI ZSM | Android ARM64 | IL2CPP",12,0xff9ad4e9);
        sub.setGravity(Gravity.CENTER);panel.addView(sub);
        status=text("Đang kết nối runtime...",12,0xffddf6ff);
        status.setBackground(bg(0xff0b3449,0xff266683));panel.addView(status);
        button("Kiểm tra IL2CPP",panel).setOnClickListener(new View.OnClickListener(){
            @Override public void onClick(View v){call(0,0,0,0,0);}
        });
        button("Kiểm tra xe hiện tại",panel).setOnClickListener(new View.OnClickListener(){
            @Override public void onClick(View v){call(1,0,0,0,0);}
        });
        panel.addView(text("Garage - Vehicle ID muốn xem trước:",12,0xffb6d7e7));
        LinearLayout row=new LinearLayout(a);panel.addView(row);
        final EditText id=edit("ID","1",row);
        button("Xem trước xe (GarageMgr)",panel).setOnClickListener(new View.OnClickListener(){
            @Override public void onClick(View v){call(2,number(id,0),0,0,0);}
        });
        panel.addView(text("Ghép trận: mode / map / người / useMap",12,0xffb6d7e7));
        LinearLayout row2=new LinearLayout(a);panel.addView(row2);
        final EditText mode=edit("Mode","0",row2);
        final EditText map=edit("Map","0",row2);
        final EditText people=edit("Người","2",row2);
        final EditText use=edit("MapType","0",row2);
        button("QuickMatch (một lần)",panel).setOnClickListener(new View.OnClickListener(){
            @Override public void onClick(View v){
                call(3,number(mode,-1),number(map,-1),number(people,0),number(use,-1));
            }
        });
        button("Replay đang chờ (ChallengeShadow)",panel).setOnClickListener(new View.OnClickListener(){
            @Override public void onClick(View v){call(4,0,0,0,0);}
        });
        panel.addView(text("Tốc độ xe: chưa có ánh xạ hook vật lý Android.",11,0xffeed5ac));
        panel.addView(text("Mod By Eri Nguyễn • Hỗ trợ Zalo",12,0xff38c8ff));
        button("Đóng menu",panel).setOnClickListener(new View.OnClickListener(){
            @Override public void onClick(View v){scroll.setVisibility(View.GONE);}
        });
        icon.setOnClickListener(new View.OnClickListener(){
            @Override public void onClick(View v){
                scroll.setVisibility(scroll.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);
            }
        });
        handler.post(new Runnable(){@Override public void run(){
            if(layer==null||layer.getParent()==null)return;
            heading.setTextColor(rainbow[(frame++)%rainbow.length]);
            try{status.setText(nativeStatus());}
            catch(Throwable t){status.setText("Native API chưa hoạt động");}
            handler.postDelayed(this,500);
        }});
    }
}