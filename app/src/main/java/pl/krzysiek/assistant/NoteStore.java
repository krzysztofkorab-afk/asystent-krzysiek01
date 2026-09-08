package pl.krzysiek.assistant;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public class NoteStore {
    private static final String PREF="notes",KEY="items";
    public static synchronized List<Note> load(Context c){
        List<Note> out=new ArrayList<>();
        try{JSONArray a=new JSONArray(c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString(KEY,"[]"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);out.add(new Note(o.getInt("id"),o.getLong("created"),o.getString("text")));}}catch(Exception ignored){}
        return out;
    }
    public static synchronized void add(Context c,Note n){List<Note> all=load(c);all.add(n);save(c,all);}
    public static synchronized void remove(Context c,int id){List<Note> all=load(c);all.removeIf(n->n.id==id);save(c,all);}
    private static void save(Context c,List<Note> all){JSONArray a=new JSONArray();try{for(Note n:all){JSONObject o=new JSONObject();o.put("id",n.id);o.put("created",n.createdAtMillis);o.put("text",n.text);a.put(o);}c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putString(KEY,a.toString()).apply();}catch(Exception ignored){}}
}
