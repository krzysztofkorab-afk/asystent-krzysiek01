package pl.krzysiek.assistant;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmScheduler {
    public static boolean schedule(Context c, Reminder r) {
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        Intent i=new Intent(c,ReminderReceiver.class).putExtra("id",r.id).putExtra("text",r.text);
        PendingIntent pi=PendingIntent.getBroadcast(c,r.id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        try {if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms())return false;am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,r.triggerAtMillis,pi);return true;}catch(Exception e){return false;}
    }
    public static void cancel(Context c,int id){AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);Intent i=new Intent(c,ReminderReceiver.class);PendingIntent pi=PendingIntent.getBroadcast(c,id,i,PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);if(pi!=null){am.cancel(pi);pi.cancel();}}
}
