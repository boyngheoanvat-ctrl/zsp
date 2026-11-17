package com.eri.battlebot;

import android.graphics.Rect;
import com.google.mlkit.vision.text.Text;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

/** Offline OCR-assisted navigation, with no guesses or fallback taps. */
public final class CocFeatureNavigator {
    public static final class Target {
        public final int x,y;
        public final String seen,feature;
        public final int score;
        Target(int x,int y,String seen,String feature,int score){
            this.x=x;this.y=y;this.seen=seen;this.feature=feature;this.score=score;
        }
    }
    private static final Map<String,String[]> LABELS = new HashMap<>();
    static {
        LABELS.put("battle", new String[]{"chien dau","dau","battle","attack","tan cong"});
        // Second step after entering Battle: the actual matchmaking button.
        // Exclude generic "Attack"/"Search" to avoid unintended taps.
        LABELS.put("findmatch", new String[]{
            "bat dau tim tran","tim tran","tim tran dau","tim doi thu",
            "tim kiem doi thu","tim kiem tran","bat dau tim doi thu",
            "find a match","find match","find opponent",
            "search for opponent","search for a match","start matchmaking"
        });
        LABELS.put("army", new String[]{"chinh sua doi quan","doi quan","edit army","army","my army","quan doi"});
        LABELS.put("battlelog", new String[]{"nhat ky chien dau","nhat ky tran","battle log","attack log"});
        LABELS.put("build",new String[]{"xay dung","build","builder","nang cap","upgrade"});
        LABELS.put("upgrade",new String[]{"nang cap","upgrade","upgrades"});
        LABELS.put("collect",new String[]{"thu thap","collect","collect all","nhan tai nguyen"});
        LABELS.put("lab",new String[]{"phong thi nghiem","laboratory","nghien cuu","research"});
        LABELS.put("hero",new String[]{"anh hung","heroes","hero hall","nha anh hung"});
        LABELS.put("equipment",new String[]{"trang bi","equipment","blacksmith","lo ren"});
        LABELS.put("pet",new String[]{"thu cung","pets","pet house"});
        LABELS.put("builderbase",new String[]{"lang dem","builder base","builder hall"});
        LABELS.put("clan",new String[]{"hoi","clan","clan chat","tro chuyen hoi"});
        LABELS.put("war",new String[]{"chien tranh","clan war","war","chien tranh hoi"});
        LABELS.put("donate",new String[]{"tang","donate","donation","tang linh","yeu cau"});
        LABELS.put("capital",new String[]{"thu do hoi","clan capital","capital","thu do"});
        LABELS.put("raid",new String[]{"dot kich","raid","raid weekend","cuoc dot kich"});
        LABELS.put("events",new String[]{"su kien","events","event","thu thach"});
        LABELS.put("profile",new String[]{"ho so","profile","tai khoan"});
        LABELS.put("shop",new String[]{"cua hang","shop","store"});
    }
    public static String normalize(String value){
        if(value==null)return "";
        String s=Normalizer.normalize(value.toLowerCase(Locale.ROOT).replace('đ','d'),
            Normalizer.Form.NFD);
        return s.replaceAll("\\p{M}+","").replaceAll("[^a-z0-9 ]"," ")
            .replaceAll(" +"," ").trim();
    }
    public static String label(String feature){
        switch(feature){
            case "battle": return "Chiến đấu";
            case "findmatch": return "Bắt đầu tìm trận";
            case "army": return "Đội quân";
            case "battlelog":return "Nhật ký chiến đấu";
            case "build":return "Xây dựng";
            case "upgrade":return "Nâng cấp";
            case "collect":return "Thu thập";
            case "lab":return "Nghiên cứu";
            case "hero":return "Anh hùng";
            case "equipment":return "Trang bị";
            case "pet":return "Thú cưng";
            case "builderbase":return "Làng đêm";
            case "clan":return "Hội";
            case "war":return "Chiến tranh hội";
            case "donate":return "Tặng lính";
            case "capital":return "Thủ đô hội";
            case "raid":return "Đột kích";
            case "events":return "Sự kiện";
            case "profile":return "Hồ sơ";
            case "shop":return "Cửa hàng";
            default:return feature;
        }
    }
    /**
     * Only tap OCR labels that correspond to the requested feature. Do not
     * auto-press any buy, gem, upgrade-confirmation or resource-spending buttons.
     * Position is scaled back from the screenshot used by the OCR engine.
     */
    public static Target find(Text recognized,String feature,int ocrW,int ocrH,int imageW,int imageH) {
        String[] aliases=LABELS.get(feature);
        if(aliases==null)return null;
        Target best=null;
        for(Text.TextBlock block:recognized.getTextBlocks()){
            for(Text.Line line:block.getLines()){
                Rect bounds=line.getBoundingBox();
                if(bounds==null || bounds.width()<5 || bounds.height()<5)continue;
                String normalized=normalize(line.getText());
                int confidence=0;
                for(String alias:aliases){
                    if(normalized.equals(alias)){confidence=Math.max(confidence,100+alias.length());}
                    else if(alias.length()>=6 && normalized.startsWith(alias+" ")){
                        confidence=Math.max(confidence,60+alias.length());
                    }
                    else if(alias.length()>=6 && normalized.endsWith(" "+alias)){
                        confidence=Math.max(confidence,55+alias.length());
                    }
                }
                if(confidence<55)continue;
                int x=Math.round(bounds.centerX()*imageW/(float)Math.max(1,ocrW));
                int y=Math.round(bounds.centerY()*imageH/(float)Math.max(1,ocrH));
                if(x<10||x>=imageW-10 || y<10||y>=imageH-10)continue;
                if(best==null||confidence>best.score)
                    best=new Target(x,y,line.getText(),feature,confidence);
            }
        }
        return best;
    }
}
