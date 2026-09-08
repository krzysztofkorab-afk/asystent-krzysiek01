package pl.krzysiek.assistant;

import android.os.Handler;
import android.os.Looper;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * AI-first interpreter. Calls the private backend when configured and falls back
 * to the local Polish parser when the backend is unavailable.
 */
public final class AiInterpreter {
    public static class Result {
        public final String action;
        public final String text;
        public final List<ZonedDateTime> times;
        public final String reply;
        public Result(String action, String text, List<ZonedDateTime> times, String reply) {
            this.action=action; this.text=text; this.times=times; this.reply=reply;
        }
    }
    public interface Callback { void onResult(Result result); }

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static void interpret(String spoken, ZonedDateTime now, Callback cb) {
        String base = BuildConfig.AI_BASE_URL;
        if (base == null || base.isBlank() || base.contains("YOUR_BACKEND")) {
            localFallback(spoken, now, cb); return;
        }
        EXEC.execute(() -> {
            try {
                URL u = new URL(base.replaceAll("/$", "") + "/interpret");
                HttpURLConnection c=(HttpURLConnection)u.openConnection();
                c.setConnectTimeout(5000); c.setReadTimeout(12000); c.setRequestMethod("POST");
                c.setRequestProperty("Content-Type","application/json; charset=utf-8"); c.setDoOutput(true);
                JSONObject req=new JSONObject(); req.put("utterance",spoken); req.put("now",now.toString()); req.put("timezone",now.getZone().getId());
                byte[] body=req.toString().getBytes(StandardCharsets.UTF_8);
                try(OutputStream os=c.getOutputStream()){os.write(body);}
                int code=c.getResponseCode();
                InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();
                String json=new String(in.readAllBytes(),StandardCharsets.UTF_8);
                if(code<200||code>=300) throw new IOException("HTTP "+code+" "+json);
                JSONObject o=new JSONObject(json);
                String action=o.optString("action","create_reminder");
                String text=o.optString("text",spoken);
                String reply=o.optString("reply","");
                List<ZonedDateTime> times=new ArrayList<>();
                JSONArray a=o.optJSONArray("times"); if(a!=null) for(int i=0;i<a.length();i++) times.add(ZonedDateTime.parse(a.getString(i)));
                Result r=new Result(action,text,times,reply);
                MAIN.post(()->cb.onResult(r));
            } catch(Exception ex) {
                localFallback(spoken,now,cb);
            }
        });
    }

    private static void localFallback(String spoken,ZonedDateTime now,Callback cb){
        PolishReminderParser.Result p=PolishReminderParser.parse(spoken,now);
        MAIN.post(()->cb.onResult(new Result("create_reminder",p.text,p.times,"")));
    }
}
