package com.eri.battlebot;

import android.graphics.Bitmap;

/** Finds the Battle button using only live screenshot pixels and shape. */
public final class BattleDetector {
    public static final class Match {
        public final int x,y,width,height,pixels;
        public final float confidence;
        Match(int x,int y,int width,int height,int pixels,float confidence){
            this.x=x;this.y=y;this.width=width;this.height=height;this.pixels=pixels;this.confidence=confidence;
        }
    }
    private static boolean yellow(int rgb){
        int r=(rgb>>>16)&255,g=(rgb>>>8)&255,b=rgb&255;
        return r>=175 && g>=135 && b<=150 && r>=g && r-b>75 && g-b>45;
    }
    public Match detect(Bitmap original){
        int actualW=original.getWidth(),actualH=original.getHeight();
        if(actualW<=actualH || actualW<450 || actualH<220)return null;
        int W=480,H=Math.max(1,Math.round(480f*actualH/actualW));
        Bitmap small=Bitmap.createScaledBitmap(original,W,H,true);
        int[] px=new int[W*H];small.getPixels(px,0,W,0,0,W,H);
        if(small!=original)small.recycle();
        int left=Math.max(0,(int)(W*.018f)),right=Math.min(W-1,(int)(W*.18f));
        int top=(int)(H*.70f),bottom=Math.min(H-1,(int)(H*.99f));
        byte[] seen=new byte[W*H];
        int[] queue=new int[(right-left+1)*(bottom-top+1)];
        Match best=null;double bestScore=0;
        for(int y=top;y<=bottom;y++)for(int x=left;x<=right;x++){
            int seed=y*W+x;
            if(seen[seed]!=0 || !yellow(px[seed]))continue;
            int head=0,tail=0,minX=x,maxX=x,minY=y,maxY=y;
            queue[tail++]=seed;seen[seed]=1;
            while(head<tail){
                int at=queue[head++],cy=at/W,cx=at%W;
                if(cx<minX)minX=cx;if(cx>maxX)maxX=cx;
                if(cy<minY)minY=cy;if(cy>maxY)maxY=cy;
                int[] nearby={at-1,at+1,at-W,at+W};
                for(int next:nearby){
                    if(next<0 || next>=px.length)continue;
                    int nx=next%W,ny=next/W;
                    if(nx<left || nx>right || ny<top || ny>bottom)continue;
                    if(seen[next]!=0 || !yellow(px[next]))continue;
                    seen[next]=1;queue[tail++]=next;
                }
            }
            int width=maxX-minX+1,height=maxY-minY+1;
            if(tail<80 || width<14 || height<10 || width>W*.14 || height>H*.27)continue;
            double aspect=width/(double)height, fill=tail/(double)(width*height);
            if(aspect<.6 || aspect>2.3 || fill<.23)continue;
            int centerX=Math.round((minX+maxX+1)*.5f*actualW/W);
            int centerY=Math.round((minY+maxY+1)*.5f*actualH/H);
            if(centerX<actualW*.03 || centerX>actualW*.16 || centerY<actualH*.76)continue;
            double score=tail*fill;
            if(score>bestScore){
                bestScore=score;
                best=new Match(centerX,centerY,Math.round(width*actualW/(float)W),Math.round(height*actualH/(float)H),tail,(float)fill);
            }
        }
        return best;
    }
}
