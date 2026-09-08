package pl.krzysiek.assistant;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.speech.*;
import android.view.Gravity;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MainActivity extends Activity implements RecognitionListener {
    private TextView status, transcript, manualWhen;
    private EditText manualText;
    private LinearLayout remindersContainer;
    private SpeechRecognizer sr;
    private boolean started=false;
    private ZonedDateTime selectedManualTime;
    private final DateTimeFormatter fmt=DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm",new Locale("pl","PL"));

    @Override public void onCreate(Bundle b){super.onCreate(b);buildUi();requestBasics();}
    @Override protected void onResume(){super.onResume();refreshList(); if(!started && hasAudio()) new Handler(Looper.getMainLooper()).postDelayed(this::startListening,350);}

    private TextView section(String text){
        TextView v=new TextView(this);v.setText(text);v.setTextSize(19);v.setTextColor(Color.rgb(20,34,52));v.setPadding(0,26,0,10);return v;
    }

    private GradientDrawable cardBg(){
        GradientDrawable g=new GradientDrawable();g.setColor(Color.rgb(247,249,252));g.setCornerRadius(24);g.setStroke(1,Color.rgb(220,226,234));return g;
    }

    private void buildUi(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(36,44,36,40);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setBackgroundColor(Color.WHITE);

        ImageView icon=new ImageView(this);icon.setImageResource(R.drawable.ic_assistant);LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(104,104);ip.gravity=Gravity.CENTER_HORIZONTAL;root.addView(icon,ip);
        TextView logo=new TextView(this);logo.setText("Asystent Krzyśka");logo.setTextSize(29);logo.setTextColor(Color.rgb(20,34,52));logo.setGravity(Gravity.CENTER);logo.setPadding(0,8,0,0);root.addView(logo,new LinearLayout.LayoutParams(-1,-2));

        status=new TextView(this);status.setText("Uruchamiam mikrofon…");status.setTextSize(20);status.setGravity(Gravity.CENTER);status.setPadding(0,34,0,14);root.addView(status,new LinearLayout.LayoutParams(-1,-2));
        transcript=new TextView(this);transcript.setText("Powiedz np. „jutro o 8 przypomnij mi o telefonie do Bartka”");transcript.setTextSize(16);transcript.setGravity(Gravity.CENTER);transcript.setPadding(10,10,10,18);root.addView(transcript,new LinearLayout.LayoutParams(-1,-2));

        Button again=new Button(this);again.setText("Mów ponownie");again.setOnClickListener(v->startListening());root.addView(again,new LinearLayout.LayoutParams(-1,-2));

        root.addView(section("Dodaj ręcznie"),new LinearLayout.LayoutParams(-1,-2));
        manualText=new EditText(this);manualText.setHint("Np. Zadzwonić do Bartka");manualText.setSingleLine(false);manualText.setMinLines(2);manualText.setPadding(18,14,18,14);manualText.setBackground(cardBg());root.addView(manualText,new LinearLayout.LayoutParams(-1,-2));

        selectedManualTime=ZonedDateTime.now(ZoneId.systemDefault()).plusHours(1).withMinute(0).withSecond(0).withNano(0);
        LinearLayout dateRow=new LinearLayout(this);dateRow.setOrientation(LinearLayout.HORIZONTAL);dateRow.setGravity(Gravity.CENTER_VERTICAL);dateRow.setPadding(0,8,0,0);
        Button dateBtn=new Button(this);dateBtn.setText("Data");dateBtn.setOnClickListener(v->pickDate());
        Button timeBtn=new Button(this);timeBtn.setText("Godzina");timeBtn.setOnClickListener(v->pickTime());
        manualWhen=new TextView(this);manualWhen.setTextSize(15);manualWhen.setPadding(12,0,0,0);updateManualWhen();
        dateRow.addView(dateBtn,new LinearLayout.LayoutParams(0,-2,1));dateRow.addView(timeBtn,new LinearLayout.LayoutParams(0,-2,1));dateRow.addView(manualWhen,new LinearLayout.LayoutParams(0,-2,1.5f));root.addView(dateRow,new LinearLayout.LayoutParams(-1,-2));

        Button addManual=new Button(this);addManual.setText("Dodaj przypomnienie");addManual.setOnClickListener(v->addManualReminder());root.addView(addManual,new LinearLayout.LayoutParams(-1,-2));

        root.addView(section("Przypomnienia"),new LinearLayout.LayoutParams(-1,-2));
        remindersContainer=new LinearLayout(this);remindersContainer.setOrientation(LinearLayout.VERTICAL);root.addView(remindersContainer,new LinearLayout.LayoutParams(-1,-2));

        ScrollView sc=new ScrollView(this);sc.addView(root);setContentView(sc);
    }

    private void pickDate(){
        ZonedDateTime z=selectedManualTime;
        new DatePickerDialog(this,(v,y,m,d)->{selectedManualTime=selectedManualTime.withYear(y).withMonth(m+1).withDayOfMonth(d);updateManualWhen();},z.getYear(),z.getMonthValue()-1,z.getDayOfMonth()).show();
    }

    private void pickTime(){
        ZonedDateTime z=selectedManualTime;
        new TimePickerDialog(this,(v,h,m)->{selectedManualTime=selectedManualTime.withHour(h).withMinute(m).withSecond(0).withNano(0);updateManualWhen();},z.getHour(),z.getMinute(),true).show();
    }

    private void updateManualWhen(){if(manualWhen!=null)manualWhen.setText(selectedManualTime.format(fmt));}

    private void addManualReminder(){
        String text=manualText.getText().toString().trim();
        if(text.isEmpty()){Toast.makeText(this,"Wpisz treść przypomnienia.",Toast.LENGTH_SHORT).show();return;}
        if(selectedManualTime.toInstant().toEpochMilli()<=System.currentTimeMillis()){Toast.makeText(this,"Wybierz termin w przyszłości.",Toast.LENGTH_SHORT).show();return;}
        int id=(int)((System.currentTimeMillis()+selectedManualTime.toInstant().toEpochMilli())%Integer.MAX_VALUE);
        Reminder rem=new Reminder(id,selectedManualTime.toInstant().toEpochMilli(),text);
        if(AlarmScheduler.schedule(this,rem)){
            ReminderStore.add(this,rem);manualText.setText("");status.setText("Dodane: "+selectedManualTime.format(fmt));refreshList();
        }else{status.setText("Nie mogę ustawić alarmu. Nadaj zgodę „Alarmy i przypomnienia”.");ensureExactAlarm();}
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

    private void refreshList(){
        if(remindersContainer==null)return;
        remindersContainer.removeAllViews();
        List<Reminder> all=ReminderStore.load(this);all.sort(Comparator.comparingLong(a->a.triggerAtMillis));long now=System.currentTimeMillis();
        int active=0;
        for(Reminder r:all)if(r.triggerAtMillis>now){if(active==0)remindersContainer.addView(section("Nadchodzące"));addReminderRow(r,false);active++;}
        if(active==0){TextView empty=new TextView(this);empty.setText("Brak zaplanowanych przypomnień.");empty.setTextSize(16);empty.setPadding(0,8,0,14);remindersContainer.addView(empty);}

        boolean historyHeader=false;
        List<Reminder> reversed=new ArrayList<>(all);Collections.reverse(reversed);
        for(Reminder r:reversed)if(r.triggerAtMillis<=now){if(!historyHeader){remindersContainer.addView(section("Historia"));historyHeader=true;}addReminderRow(r,true);}
    }

    private void addReminderRow(Reminder r, boolean past){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(20,16,20,12);card.setBackground(cardBg());
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,0,0,12);
        TextView title=new TextView(this);ZonedDateTime z=Instant.ofEpochMilli(r.triggerAtMillis).atZone(ZoneId.systemDefault());title.setText((past?"Wykonane · ":"Nadchodzi · ")+z.format(fmt));title.setTextSize(15);title.setTextColor(past?Color.DKGRAY:Color.rgb(20,34,52));card.addView(title);
        TextView text=new TextView(this);text.setText(r.text);text.setTextSize(17);text.setPadding(0,5,0,4);card.addView(text);
        Button del=new Button(this);del.setText("Usuń");del.setOnClickListener(v->{AlarmScheduler.cancel(this,r.id);ReminderStore.remove(this,r.id);refreshList();});card.addView(del,new LinearLayout.LayoutParams(-1,-2));
        remindersContainer.addView(card,cp);
    }

    @Override protected void onDestroy(){if(sr!=null)sr.destroy();super.onDestroy();}
    @Override public void onReadyForSpeech(Bundle p){status.setText("Słucham…");}
    @Override public void onBeginningOfSpeech(){}
    @Override public void onRmsChanged(float v){}
    @Override public void onBufferReceived(byte[] b){}
    @Override public void onEndOfSpeech(){status.setText("Rozumiem polecenie…");}
    @Override public void onError(int e){started=false;status.setText("Nie usłyszałem polecenia. Naciśnij „Mów ponownie”.");}
    @Override public void onResults(Bundle b){started=false;ArrayList<String> a=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(a!=null&&!a.isEmpty())handle(a.get(0));}
    @Override public void onPartialResults(Bundle b){ArrayList<String>a=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(a!=null&&!a.isEmpty())transcript.setText(a.get(0));}
    @Override public void onEvent(int e,Bundle b){}
}
