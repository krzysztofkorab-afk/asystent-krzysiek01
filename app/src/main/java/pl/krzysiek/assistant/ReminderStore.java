package pl.krzysiek.assistant;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ReminderStore {
    private static final String PREF="reminders";
    private static final String KEY="items";

    public static synchronized List<Reminder> load(Context c){
        List<Reminder> out=new ArrayList<>();
        try{
            String raw=c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString(KEY,"[]");
            JSONArray a=new JSONArray(raw);
            for(int i=0;i<a.length();i++){
                JSONObject o=a.getJSONObject(i);
                out.add(new Reminder(o.getInt("id"),o.getLong("time"),o.getString("text"),o.optBoolean("done",false),o.optLong("completedAt",0L)));
            }
        }catch(Exception ignored){}
        return out;
    }

    public static synchronized void add(Context c, Reminder r){List<Reminder> all=load(c);all.add(r);save(c,all);}
    public static synchronized void remove(Context c,int id){List<Reminder> all=load(c);all.removeIf(r->r.id==id);save(c,all);}

    public static synchronized void markDone(Context c,int id){
        List<Reminder> all=load(c);
        for(Reminder r:all) if(r.id==id){r.done=true;r.completedAtMillis=System.currentTimeMillis();break;}
        save(c,all);
    }

    public static synchronized Reminder snooze(Context c,int id,long newTime){
        List<Reminder> all=load(c); Reminder found=null;
        for(Reminder r:all) if(r.id==id){r.triggerAtMillis=newTime;r.done=false;r.completedAtMillis=0L;found=r;break;}
        save(c,all); return found;
    }

    private static void save(Context c,List<Reminder> all){
        JSONArray a=new JSONArray();
        try{
            for(Reminder r:all){
                JSONObject o=new JSONObject();o.put("id",r.id);o.put("time",r.triggerAtMillis);o.put("text",r.text);o.put("done",r.done);o.put("completedAt",r.completedAtMillis);a.put(o);
            }
            c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putString(KEY,a.toString()).apply();
        }catch(Exception ignored){}
    }
}
