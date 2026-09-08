package pl.krzysiek.assistant;

import android.app.*;
import android.content.*;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL="reminders";
    @Override public void onReceive(Context c,Intent intent){
        int id=intent.getIntExtra("id",(int)(System.currentTimeMillis()%100000));String text=intent.getStringExtra("text");if(text==null)text="Masz zaplanowane przypomnienie";
        NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel(CHANNEL,"Przypomnienia",NotificationManager.IMPORTANCE_HIGH);ch.setDescription("Ważne przypomnienia Agenta Mado");nm.createNotificationChannel(ch);}
        Intent open=new Intent(c,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(c,id,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        android.app.Notification n=new android.app.Notification.Builder(c,CHANNEL).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Agent Mado").setContentText(text).setStyle(new android.app.Notification.BigTextStyle().bigText(text)).setPriority(android.app.Notification.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(pi).build();nm.notify(id,n);
    }
}
