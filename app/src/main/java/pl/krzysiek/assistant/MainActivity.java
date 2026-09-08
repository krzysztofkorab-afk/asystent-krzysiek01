package pl.krzysiek.assistant;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.speech.*;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MainActivity extends Activity implements RecognitionListener {
    private static final int NAVY=Color.rgb(20,34,52), BLUE=Color.rgb(52,113,235), CYAN=Color.rgb(80,190,230);
    private static final int GREEN=Color.rgb(44,157,101), AMBER=Color.rgb(224,149,39), PURPLE=Color.rgb(126,87,194);
    private static final int BG=Color.rgb(244,247,251), CARD=Color.WHITE, MUTED=Color.rgb(97,108,122);

    private TextView status, transcript, manualWhen, todaySummary;
    private EditText manualText, noteText;
    private LinearLayout remindersContainer, notesContainer;
    private SpeechRecognizer sr; private boolean started=false;
    private ZonedDateTime selectedManualTime;
    private final DateTimeFormatter fmt=DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm",new Locale("pl","PL"));

    @Override public void onCreate(Bundle b){super.onCreate(b);buildUi();requestBasics();}
    @Override protected void onResume(){super.onResume();refreshAll();if(!started&&hasAudio())new Handler(Looper.getMainLooper()).postDelayed(this::startListening,350);}

    private TextView text(String s,float size,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);return v;}
    private GradientDrawable rounded(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(radius);return g;}
    private GradientDrawable cardBg(){GradientDrawable g=rounded(CARD,28);g.setStroke(1,Color.rgb(224,229,237));return g;}
    private void tint(Button b,int color){b.setTextColor(Color.WHITE);b.setBackgroundTintList(ColorStateList.valueOf(color));}
    private TextView section(String title,int color){TextView v=text(title,19,color);v.setTypeface(null,Typeface.BOLD);v.setPadding(0,26,0,10);return v;}

    private void buildUi(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(30,30,30,50);root.setBackgroundColor(BG);

        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setGravity(Gravity.CENTER);hero.setPadding(24,24,24,22);hero.setBackground(rounded(NAVY,34));
        ImageView icon=new ImageView(this);icon.setImageResource(R.drawable.ic_assistant);LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(96,96);hero.addView(icon,ip);
        TextView logo=text("Agent Mado",28,Color.WHITE);logo.setTypeface(null,Typeface.BOLD);logo.setPadding(0,7,0,0);hero.addView(logo);
        TextView sub=text("Twoje przypomnienia, zadania i szybkie notatki",14,Color.rgb(196,220,239));sub.setGravity(Gravity.CENTER);sub.setPadding(0,4,0,0);hero.addView(sub);
        root.addView(hero,new LinearLayout.LayoutParams(-1,-2));

        todaySummary=text("Dzisiaj",18,NAVY);todaySummary.setTypeface(null,Typeface.BOLD);todaySummary.setPadding(20,16,20,16);todaySummary.setBackground(rounded(Color.rgb(227,240,255),24));LinearLayout.LayoutParams tsp=new LinearLayout.LayoutParams(-1,-2);tsp.setMargins(0,14,0,0);root.addView(todaySummary,tsp);

        status=text("Uruchamiam mikrofon…",20,NAVY);status.setGravity(Gravity.CENTER);status.setPadding(0,24,0,8);root.addView(status);
        transcript=text("Powiedz np. „jutro o 8 przypomnij mi o telefonie” albo „zanotuj: klient chce plisę”",16,MUTED);transcript.setGravity(Gravity.CENTER);transcript.setPadding(8,4,8,14);root.addView(transcript);
        Button again=new Button(this);again.setText("🎙  Mów ponownie");tint(again,BLUE);again.setOnClickListener(v->startListening());root.addView(again,new LinearLayout.LayoutParams(-1,-2));

        root.addView(section("Szybka notatka",PURPLE));
        noteText=new EditText(this);noteText.setHint("Np. Kowalski – sprawdzić wymiar moskitiery");noteText.setMinLines(2);noteText.setPadding(18,14,18,14);noteText.setBackground(cardBg());root.addView(noteText,new LinearLayout.LayoutParams(-1,-2));
        Button addNote=new Button(this);addNote.setText("Dodaj notatkę");tint(addNote,PURPLE);addNote.setOnClickListener(v->addManualNote());root.addView(addNote,new LinearLayout.LayoutParams(-1,-2));
        notesContainer=new LinearLayout(this);notesContainer.setOrientation(LinearLayout.VERTICAL);root.addView(notesContainer,new LinearLayout.LayoutParams(-1,-2));

        root.addView(section("Dodaj przypomnienie ręcznie",BLUE));
        manualText=new EditText(this);manualText.setHint("Np. Zadzwonić do Bartka");manualText.setMinLines(2);manualText.setPadding(18,14,18,14);manualText.setBackground(cardBg());root.addView(manualText,new LinearLayout.LayoutParams(-1,-2));
        selectedManualTime=ZonedDateTime.now(ZoneId.systemDefault()).plusHours(1).withMinute(0).withSecond(0).withNano(0);
        LinearLayout dateRow=new LinearLayout(this);dateRow.setOrientation(LinearLayout.HORIZONTAL);dateRow.setGravity(Gravity.CENTER_VERTICAL);dateRow.setPadding(0,8,0,0);
        Button dateBtn=new Button(this);dateBtn.setText("Data");tint(dateBtn,NAVY);dateBtn.setOnClickListener(v->pickDate());
        Button timeBtn=new Button(this);timeBtn.setText("Godzina");tint(timeBtn,NAVY);timeBtn.setOnClickListener(v->pickTime());
        manualWhen=text("",14,MUTED);manualWhen.setPadding(10,0,0,0);updateManualWhen();
        dateRow.addView(dateBtn,new LinearLayout.LayoutParams(0,-2,1));dateRow.addView(timeBtn,new LinearLayout.LayoutParams(0,-2,1));dateRow.addView(manualWhen,new LinearLayout.LayoutParams(0,-2,1.55f));root.addView(dateRow);
        Button addManual=new Button(this);addManual.setText("Dodaj przypomnienie");tint(addManual,BLUE);addManual.setOnClickListener(v->addManualReminder());root.addView(addManual,new LinearLayout.LayoutParams(-1,-2));

        root.addView(section("Plan dnia",NAVY));
        remindersContainer=new LinearLayout(this);remindersContainer.setOrientation(LinearLayout.VERTICAL);root.addView(remindersContainer,new LinearLayout.LayoutParams(-1,-2));

        ScrollView sc=new ScrollView(this);sc.addView(root);setContentView(sc);
    }

    private void pickDate(){ZonedDateTime z=selectedManualTime;new DatePickerDialog(this,(v,y,m,d)->{selectedManualTime=selectedManualTime.withYear(y).withMonth(m+1).withDayOfMonth(d);updateManualWhen();},z.getYear(),z.getMonthValue()-1,z.getDayOfMonth()).show();}
    private void pickTime(){ZonedDateTime z=selectedManualTime;new TimePickerDialog(this,(v,h,m)->{selectedManualTime=selectedManualTime.withHour(h).withMinute(m).withSecond(0).withNano(0);updateManualWhen();},z.getHour(),z.getMinute(),true).show();}
    private void updateManualWhen(){if(manualWhen!=null)manualWhen.setText(selectedManualTime.format(fmt));}

    private void addManualReminder(){
        String t=manualText.getText().toString().trim();if(t.isEmpty()){toast("Wpisz treść przypomnienia.");return;}if(selectedManualTime.toInstant().toEpochMilli()<=System.currentTimeMillis()){toast("Wybierz termin w przyszłości.");return;}
        Reminder r=new Reminder(newId(),selectedManualTime.toInstant().toEpochMilli(),t);if(AlarmScheduler.schedule(this,r)){ReminderStore.add(this,r);manualText.setText("");status.setText("Dodane: "+selectedManualTime.format(fmt));refreshAll();}else{status.setText("Nadaj zgodę „Alarmy i przypomnienia”.");ensureExactAlarm();}
    }

    private void addManualNote(){String t=noteText.getText().toString().trim();if(t.isEmpty()){toast("Wpisz treść notatki.");return;}NoteStore.add(this,new Note(newId(),System.currentTimeMillis(),t));noteText.setText("");status.setText("Notatka zapisana.");refreshNotes();}
    private void addVoiceNote(String spoken){String t=spoken.replaceFirst("(?i)^\\s*(zanotuj|notatka)\\s*[:,-]?\\s*","").trim();if(t.isEmpty()){status.setText("Powiedz, co mam zanotować.");return;}NoteStore.add(this,new Note(newId(),System.currentTimeMillis(),t));status.setText("Zapisane w notatkach.");transcript.setText(t);refreshNotes();}

    private int newId(){return (int)((System.currentTimeMillis()+System.nanoTime())&0x7fffffff);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}

    private boolean hasAudio(){return checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;}
    private void requestBasics(){ArrayList<String> p=new ArrayList<>();if(!hasAudio())p.add(Manifest.permission.RECORD_AUDIO);if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)p.add(Manifest.permission.POST_NOTIFICATIONS);if(!p.isEmpty())requestPermissions(p.toArray(new String[0]),44);else ensureExactAlarm();}
    private void ensureExactAlarm(){if(Build.VERSION.SDK_INT>=31){AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);if(!am.canScheduleExactAlarms()){status.setText("Potrzebna zgoda na dokładne przypomnienia");try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));}catch(Exception ignored){}}}}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);ensureExactAlarm();if(hasAudio())startListening();else status.setText("Włącz dostęp do mikrofonu w ustawieniach aplikacji.");}

    private void startListening(){if(!hasAudio()){requestBasics();return;}started=true;if(sr!=null)sr.destroy();sr=SpeechRecognizer.createSpeechRecognizer(this);sr.setRecognitionListener(this);Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"pl-PL");i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3);status.setText("Słucham…");transcript.setText("Mów normalnie.");sr.startListening(i);}

    private void handle(String spoken){
        transcript.setText("Usłyszałem: „"+spoken+"”");
        if(spoken.trim().toLowerCase(new Locale("pl","PL")).matches("^(zanotuj|notatka).*")){addVoiceNote(spoken);return;}
        status.setText("Myślę…");
        AiInterpreter.interpret(spoken,ZonedDateTime.now(ZoneId.systemDefault()),r->{
            if(!"create_reminder".equals(r.action)){status.setText(r.reply.isBlank()?"Jeszcze nie obsługuję tego polecenia.":r.reply);return;}
            if(r.times.isEmpty()){status.setText("Nie jestem pewien terminu. Powiedz np. „jutro o 8”.");return;}
            int ok=0;StringBuilder c=new StringBuilder();for(ZonedDateTime z:r.times){Reminder rem=new Reminder(newId(),z.toInstant().toEpochMilli(),r.text);if(AlarmScheduler.schedule(this,rem)){ReminderStore.add(this,rem);ok++;c.append("\n• ").append(z.format(fmt));}}
            status.setText(ok>0?"Gotowe. Zapisałem "+ok+(ok==1?" przypomnienie:":" przypomnienia:")+c:"Nie mogę ustawić dokładnego alarmu.");refreshAll();
        });
    }

    private void refreshAll(){refreshSummary();refreshList();refreshNotes();}
    private void refreshSummary(){if(todaySummary==null)return;LocalDate today=LocalDate.now();int todayCount=0,overdue=0;long now=System.currentTimeMillis();for(Reminder r:ReminderStore.load(this)){if(r.done)continue;LocalDate d=Instant.ofEpochMilli(r.triggerAtMillis).atZone(ZoneId.systemDefault()).toLocalDate();if(d.equals(today))todayCount++;if(r.triggerAtMillis<now)overdue++;}todaySummary.setText("Dzisiaj  •  "+todayCount+" zadań"+(overdue>0?"   |   "+overdue+" zaległe":""));}

    private void refreshList(){
        if(remindersContainer==null)return;remindersContainer.removeAllViews();List<Reminder> all=ReminderStore.load(this);all.sort(Comparator.comparingLong(a->a.triggerAtMillis));LocalDate today=LocalDate.now();long now=System.currentTimeMillis();
        int overdue=0,todayN=0,future=0,done=0;
        for(Reminder r:all)if(!r.done&&r.triggerAtMillis<now){if(overdue++==0)remindersContainer.addView(section("Zaległe",AMBER));addReminderRow(r,AMBER);}
        for(Reminder r:all)if(!r.done){LocalDate d=Instant.ofEpochMilli(r.triggerAtMillis).atZone(ZoneId.systemDefault()).toLocalDate();if(d.equals(today)&&r.triggerAtMillis>=now){if(todayN++==0)remindersContainer.addView(section("Dzisiaj",BLUE));addReminderRow(r,BLUE);}}
        for(Reminder r:all)if(!r.done){LocalDate d=Instant.ofEpochMilli(r.triggerAtMillis).atZone(ZoneId.systemDefault()).toLocalDate();if(d.isAfter(today)){if(future++==0)remindersContainer.addView(section("Nadchodzące",NAVY));addReminderRow(r,NAVY);}}
        List<Reminder> rev=new ArrayList<>(all);Collections.reverse(rev);for(Reminder r:rev)if(r.done){if(done++==0)remindersContainer.addView(section("Wykonane",GREEN));addDoneRow(r);}
        if(all.isEmpty()){TextView e=text("Brak przypomnień. Możesz dodać je głosem albo ręcznie.",16,MUTED);e.setPadding(0,8,0,18);remindersContainer.addView(e);}
    }

    private void addReminderRow(Reminder r,int accent){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(18,15,18,12);GradientDrawable bg=cardBg();bg.setStroke(2,accent);card.setBackground(bg);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,0,0,12);
        ZonedDateTime z=Instant.ofEpochMilli(r.triggerAtMillis).atZone(ZoneId.systemDefault());TextView when=text(z.format(fmt),14,accent);when.setTypeface(null,Typeface.BOLD);card.addView(when);TextView body=text(r.text,17,NAVY);body.setPadding(0,5,0,7);card.addView(body);
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        Button done=new Button(this);done.setText("✓ Wykonane");tint(done,GREEN);done.setOnClickListener(v->{AlarmScheduler.cancel(this,r.id);ReminderStore.markDone(this,r.id);refreshAll();});
        Button s30=new Button(this);s30.setText("+30 min");tint(s30,AMBER);s30.setOnClickListener(v->snooze(r,30));
        Button s60=new Button(this);s60.setText("+1 h");tint(s60,BLUE);s60.setOnClickListener(v->snooze(r,60));
        row.addView(done,new LinearLayout.LayoutParams(0,-2,1.3f));row.addView(s30,new LinearLayout.LayoutParams(0,-2,1));row.addView(s60,new LinearLayout.LayoutParams(0,-2,1));card.addView(row);
        Button del=new Button(this);del.setText("Usuń");del.setTextColor(MUTED);del.setOnClickListener(v->{AlarmScheduler.cancel(this,r.id);ReminderStore.remove(this,r.id);refreshAll();});card.addView(del,new LinearLayout.LayoutParams(-1,-2));remindersContainer.addView(card,cp);
    }

    private void snooze(Reminder r,int minutes){AlarmScheduler.cancel(this,r.id);long nt=System.currentTimeMillis()+minutes*60_000L;Reminder updated=ReminderStore.snooze(this,r.id,nt);if(updated!=null&&AlarmScheduler.schedule(this,updated)){status.setText("Odłożone o "+minutes+(minutes==60?" minut.":" min."));}refreshAll();}

    private void addDoneRow(Reminder r){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(18,13,18,10);card.setBackground(rounded(Color.rgb(233,247,239),24));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,0,0,10);TextView body=text("✓  "+r.text,16,Color.rgb(40,101,73));card.addView(body);Button del=new Button(this);del.setText("Usuń z historii");del.setOnClickListener(v->{ReminderStore.remove(this,r.id);refreshAll();});card.addView(del);remindersContainer.addView(card,cp);}

    private void refreshNotes(){
        if(notesContainer==null)return;notesContainer.removeAllViews();List<Note> notes=NoteStore.load(this);Collections.reverse(notes);for(Note n:notes)addNoteRow(n);if(notes.isEmpty()){TextView e=text("Brak notatek.",14,MUTED);e.setPadding(0,8,0,2);notesContainer.addView(e);}
    }
    private void addNoteRow(Note n){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(16,12,16,10);card.setBackground(rounded(Color.rgb(244,238,252),22));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,8,0,0);TextView t=text(n.text,16,NAVY);card.addView(t);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);Button remind=new Button(this);remind.setText("Przypomnij za 1 h");tint(remind,PURPLE);remind.setOnClickListener(v->{long when=System.currentTimeMillis()+60*60_000L;Reminder r=new Reminder(newId(),when,n.text);if(AlarmScheduler.schedule(this,r)){ReminderStore.add(this,r);status.setText("Notatka zamieniona w przypomnienie za 1 godzinę.");refreshAll();}});Button del=new Button(this);del.setText("Usuń");del.setOnClickListener(v->{NoteStore.remove(this,n.id);refreshNotes();});row.addView(remind,new LinearLayout.LayoutParams(0,-2,2));row.addView(del,new LinearLayout.LayoutParams(0,-2,1));card.addView(row);notesContainer.addView(card,cp);}

    @Override protected void onDestroy(){if(sr!=null)sr.destroy();super.onDestroy();}
    @Override public void onReadyForSpeech(Bundle p){status.setText("Słucham…");}
    @Override public void onBeginningOfSpeech(){}
    @Override public void onRmsChanged(float v){}
    @Override public void onBufferReceived(byte[] b){}
    @Override public void onEndOfSpeech(){status.setText("Rozumiem polecenie…");}
    @Override public void onError(int e){started=false;status.setText("Nie usłyszałem polecenia. Naciśnij „Mów ponownie”.");}
    @Override public void onResults(Bundle b){started=false;ArrayList<String>a=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(a!=null&&!a.isEmpty())handle(a.get(0));}
    @Override public void onPartialResults(Bundle b){ArrayList<String>a=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(a!=null&&!a.isEmpty())transcript.setText(a.get(0));}
    @Override public void onEvent(int e,Bundle b){}
}
