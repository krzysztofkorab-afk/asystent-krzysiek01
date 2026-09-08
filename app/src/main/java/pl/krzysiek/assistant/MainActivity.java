package pl.krzysiek.assistant;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.speech.*;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MainActivity extends Activity implements RecognitionListener {
    private TextView status, transcript, list; private SpeechRecognizer sr; private boolean started=false;
    private final DateTimeFormatter fmt=DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm",new Locale("pl","PL"));
    @Override public void onCreate(Bundle b){super.onCreate(b);buildUi();requestBasics();}
    @Override protected void onResume(){super.onResume();refreshList(); if(!started && hasAudio()) new Handler(Looper.getMainLooper()).postDelayed(this::startListening,350);}
    private void buildUi(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(44,60,44,30);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setBackgroundColor(Color.WHITE);
        TextView h=new TextView(this);h.setText("Asystent Krzyśka");h.setTextSize(30);h.setTextColor(Color.BLACK);h.setGravity(Gravity.CENTER);root.addView(h,new LinearLayout.LayoutParams(-1,-2));
        status=new TextView(this);status.setText("Uruchamiam mikrofon…");status.setTextSize(21);status.setGravity(Gravity.CENTER);status.setPadding(0,55,0,20);root.addView(status,new LinearLayout.LayoutParams(-1,-2));
        transcript=new TextView(this);transcript.setText("Powiedz np. „w piątek o 7:55 oraz o 14 przypomnij mi o mailu do Bartosza”");transcript.setTextSize(17);transcript.setGravity(Gravity.CENTER);transcript.setPadding(10,15,10,40);root.addView(transcript,new LinearLayout.LayoutParams(-1,-2));
        Button again=new Button(this);again.setText("Mów ponownie");again.setOnClickListener(v->startListening());root.addView(again,new LinearLayout.LayoutParams(-1,-2));
        TextView lab=new TextView(this);lab.setText("\nNajbliższe przypomnienia");lab.setTextSize(19);lab.setTextColor(Color.BLACK);root.addView(lab,new LinearLayout.LayoutParams(-1,-2));
        list=new TextView(this);list.setTextSize(16);list.setPadding(0,10,0,0);root.addView(list,new LinearLayout.LayoutParams(-1,-2));
        ScrollView sc=new ScrollView(this);sc.addView(root);setContentView(sc);
    }
    private boolean hasAudio(){return checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;}
    private void requestBasics(){
        ArrayList<String> p=new ArrayList<>(); if(!hasAudio())p.add(Manifest.permission.RECORD_AUDIO); if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)p.add(Manifest.permission.POST_NOTIFICATIONS);
        if(!p.isEmpty())requestPermissions(p.toArray(new String[0]),44); else ensureExactAlarm();
    }
    private void ensureExactAlarm(){if(Build.VERSION.SDK_INT>=31){AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);if(!am.canScheduleExactAlarms()){status.setText("Potrzebna zgoda na dokładne przypomnienia");try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:"+getPackageName())));}catch(Exception ignored){}}}}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);ensureExactAlarm(); if(hasAudio())startListening(); else status.setText("Włącz dostęp do mikrofonu w ustawieniach aplikacji.");}
    private void startListening(){
        if(!hasAudio()){requestBasics();return;} started=true; if(sr!=null)sr.destroy(); sr=SpeechRecognizer.createSpeechRecognizer(this);sr.setRecognitionListener(this);
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"pl-PL");i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3);
        status.setText("Słucham…");transcript.setText("Mów normalnie. Nie musisz nic więcej naciskać.");sr.startListening(i);
    }
    private void handle(String spoken){
        transcript.setText("Usłyszałem: „"+spoken+"”"); status.setText("Myślę…");
        AiInterpreter.interpret(spoken,ZonedDateTime.now(ZoneId.systemDefault()),r->{
            if(!"create_reminder".equals(r.action)){status.setText(r.reply.isBlank()?"Jeszcze nie obsługuję tego polecenia.":r.reply);return;}
            if(r.times.isEmpty()){status.setText(r.reply.isBlank()?"Nie jestem pewien terminu. Powiedz go jeszcze raz, np. „jutro po południu” albo „w piątek o 14”.":r.reply);return;}
            int ok=0;StringBuilder confirm=new StringBuilder();
            for(ZonedDateTime z:r.times){int id=(int)((System.currentTimeMillis()+z.toInstant().toEpochMilli()+ok)%Integer.MAX_VALUE);Reminder rem=new Reminder(id,z.toInstant().toEpochMilli(),r.text);if(AlarmScheduler.schedule(this,rem)){ReminderStore.add(this,rem);ok++;confirm.append("\n• ").append(z.format(fmt));}}
            if(ok>0){String prefix=r.reply.isBlank()?"Gotowe. Zapisałem "+ok+(ok==1?" przypomnienie:":" przypomnienia:"):r.reply;status.setText(prefix+confirm);}else {status.setText("Nie mogę ustawić dokładnego alarmu. Nadaj aplikacji zgodę „Alarmy i przypomnienia”.");ensureExactAlarm();}
            refreshList();
        });
    }
    private void refreshList(){List<Reminder> all=ReminderStore.load(this);all.sort(Comparator.comparingLong(a->a.triggerAtMillis));StringBuilder b=new StringBuilder();long now=System.currentTimeMillis();for(Reminder r:all)if(r.triggerAtMillis>now){ZonedDateTime z=Instant.ofEpochMilli(r.triggerAtMillis).atZone(ZoneId.systemDefault());b.append("• ").append(z.format(fmt)).append("\n   ").append(r.text).append("\n\n");}list.setText(b.length()==0?"Brak zaplanowanych przypomnień.":b.toString());}
    @Override protected void onDestroy(){if(sr!=null)sr.destroy();super.onDestroy();}
    @Override public void onReadyForSpeech(Bundle p){status.setText("Słucham…");} @Override public void onBeginningOfSpeech(){} @Override public void onRmsChanged(float v){} @Override public void onBufferReceived(byte[] b){} @Override public void onEndOfSpeech(){status.setText("Rozumiem polecenie…");}
    @Override public void onError(int e){started=false;status.setText("Nie usłyszałem polecenia. Naciśnij „Mów ponownie”.");}
    @Override public void onResults(Bundle b){started=false;ArrayList<String> a=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(a!=null&&!a.isEmpty())handle(a.get(0));}
    @Override public void onPartialResults(Bundle b){ArrayList<String>a=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(a!=null&&!a.isEmpty())transcript.setText(a.get(0));}
    @Override public void onEvent(int e,Bundle b){}
}
