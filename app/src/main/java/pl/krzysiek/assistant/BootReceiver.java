package pl.krzysiek.assistant;
import android.content.*;
public class BootReceiver extends BroadcastReceiver { @Override public void onReceive(Context c,Intent i){long now=System.currentTimeMillis();for(Reminder r:ReminderStore.load(c))if(!r.done&&r.triggerAtMillis>now)AlarmScheduler.schedule(c,r);} }
