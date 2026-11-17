package com.eri.battlebot;

import android.graphics.Bitmap;
import java.util.ArrayList;
import java.util.List;

/** Pure screenshot analysis: calculate troop bar centres and green perimeter drop zones. */
public final class TroopPlanner {
    public static final class Point {
        public final int x,y;
        Point(int x,int y) { this.x=x; this.y=y; }
        @Override public String toString(){return "("+x+","+y+")";}
    }
    public static final class Plan {
        public final List<Point> slots, deploy;
        public final int width,height;
        public final boolean adaptive;
        Plan(List<Point> slots,List<Point> deploy,int width,int height,boolean adaptive){
            this.slots=slots;this.deploy=deploy;this.width=width;this.height=height;this.adaptive=adaptive;
        }
    }
    private static int clamp(int a,int lo,int hi){return Math.max(lo,Math.min(hi,a));}
    private static int lum(int color) {
        return (((color>>>16)&255)*3+((color>>>8)&255)*6+(color&255))/10;
    }
    private static double greenScore(Bitmap b, float px, float py) {
        int w=b.getWidth(),h=b.getHeight(),x=(int)(px*w),y=(int)(py*h);
        int r=Math.max(2,(int)(w*.013));
        int total=0,green=0;
        for(int j=-r;j<=r;j+=3)for(int i=-r;i<=r;i+=3){
            int sx=clamp(x+i,0,w-1),sy=clamp(y+j,0,h-1);
            int rgb=b.getPixel(sx,sy);
            int red=(rgb>>>16)&255,g=(rgb>>>8)&255,blue=rgb&255;
            if(g>red*0.97 && g>blue*1.10 && g>63 && g<245)green++;
            total++;
        }
        return total==0?0:(double)green/total;
    }
    /**
     * Positions remain proportional to the *live* screenshot. We rank perimeter
     * candidates by terrain color, not by hardcoded 2800x1260 pixels.
     * This is a heuristic: a green pixel cannot prove a troop is deployable.
     */
    public Plan plan(Bitmap frame,int requestedSlots) {
        int width=frame.getWidth(),height=frame.getHeight();
        int count=clamp(requestedSlots,1,7);
        Bitmap b=Bitmap.createScaledBitmap(frame,800,Math.max(1,(int)(800f*height/width)),true);
        int w=b.getWidth(),h=b.getHeight();
        // Locate the troop HUD's horizontal top boundary via mean image gradient.
        int bestY=(int)(h*.80),bestEnergy=0;
        for(int y=(int)(h*.74);y<(int)(h*.90);y+=2) {
            int energy=0;
            for(int x=(int)(w*.03);x<(int)(w*.75);x+=5)
                energy+=Math.abs(lum(b.getPixel(x,y))-lum(b.getPixel(x,y+2)));
            if(energy>bestEnergy){bestEnergy=energy;bestY=y;}
        }
        float barTop=(float)bestY/h;
        float troopY=barTop+(1f-barTop)*.52f;
        troopY=Math.max(.84f,Math.min(.954f,troopY));
        // Find periodic bright card separators. Prefer adaptive spacing when
        // several peaks at a plausible tile interval are detected.
        List<Integer> boundaries=new ArrayList<>();
        for(int x=(int)(w*.022);x<(int)(w*.67);x+=2){
            long edge=0;
            for(int y=(int)(h*Math.max(barTop,.80f));y<(int)(h*.975f);y+=3){
                edge+=Math.abs(lum(b.getPixel(x,y))-lum(b.getPixel(Math.min(w-1,x+2),y)));
            }
            if(edge>1350 && (boundaries.isEmpty()||x-boundaries.get(boundaries.size()-1)>w*.032))
                boundaries.add(x);
        }
        float start=.052f,step=.070f;
        boolean adaptive=false;
        if(boundaries.size()>=5){
            List<Integer> gaps=new ArrayList<>();
            for(int i=1;i<boundaries.size();i++){
                int gap=boundaries.get(i)-boundaries.get(i-1);
                if(gap>=w*.045 && gap<=w*.12)gaps.add(gap);
            }
            if(gaps.size()>=3) {
                gaps.sort(Integer::compareTo);
                float candidate=gaps.get(gaps.size()/2)/(float)w;
                if(candidate>.045f && candidate<.11f){
                    start=boundaries.get(0)/(float)w+candidate*.5f;
                    step=candidate;adaptive=true;
                }
            }
        }
        List<Point> slots=new ArrayList<>();
        for(int i=0;i<count;i++){
            float px=Math.min(.85f,start+i*step);
            slots.add(new Point(clamp((int)(px*width),1,width-2),
                clamp((int)(troopY*height),1,height-2)));
        }
        // Only perimeter regions away from the troop HUD, result UI and centre.
        float[][] candidates={
            {.24f,.22f},{.30f,.28f},{.75f,.22f},{.70f,.30f},
            {.17f,.38f},{.83f,.39f},{.23f,.59f},{.77f,.59f},
            {.32f,.19f},{.68f,.19f},{.16f,.55f},{.84f,.55f}
        };
        List<Integer> available=new ArrayList<>();
        for(int i=0;i<candidates.length;i++)available.add(i);
        available.sort((i,j)->Double.compare(
            greenScore(b,candidates[j][0],candidates[j][1]),
            greenScore(b,candidates[i][0],candidates[i][1])));
        List<Point> deploy=new ArrayList<>();
        for(int i:available){
            float[] p=candidates[i];
            boolean tooClose=false;
            for(Point prior:deploy){
                if(Math.abs(prior.x-p[0]*width)<width*.10f
                    && Math.abs(prior.y-p[1]*height)<height*.10f){tooClose=true;break;}
            }
            if(!tooClose)deploy.add(new Point((int)(p[0]*width),(int)(p[1]*height)));
            if(deploy.size()==6)break;
        }
        b.recycle();
        return new Plan(slots,deploy,width,height,adaptive);
    }
}
