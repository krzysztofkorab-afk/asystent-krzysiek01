package pl.krzysiek.assistant;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        long now=System.currentTimeMillis();
        for(Reminder r:ReminderStore.load(c)) if(r.triggerAtMillis>now) AlarmScheduler.schedule(c,r);
    }
}
