package pl.krzysiek.assistant;

import android.Manifest;
import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.speech.*;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MainActivity extends Activity implements RecognitionListener {
    private static final int INK=Color.rgb(11,18,32), SURFACE=Color.rgb(255,255,255), BG=Color.rgb(246,248,252);
    private static final int BLUE=Color.rgb(79,140,255), ICE=Color.rgb(125,211,252), PURPLE=Color.rgb(139,92,246);
    private static final int MINT=Color.rgb(34,197,139), AMBER=Color.rgb(245,158,11), RED=Color.rgb(239,83,80);
    private static final int MUTED=Color.rgb(105,116,135), LINE=Color.rgb(229,234,242), SOFTBLUE=Color.rgb(238,245,255);

    private TextView status, transcript, manualWhen, todaySummary, todayCaption;
    private EditText manualText, noteText;
    private LinearLayout remindersContainer, notesContainer;
    private SpeechRecognizer sr; private boolean started=false;
    private ZonedDateTime selectedManualTime;
    private View micOrb;
    private ObjectAnimator pulseX,pulseY;
    private final DateTimeFormatter fmt=DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm",new Locale("pl","PL"));

    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(INK);getWindow().setNavigationBarColor(INK);buildUi();requestBasics();}
    @Override protected void onResume(){super.onResume();refreshAll();if(!started&&hasAudio())new Handler(Looper.getMainLooper()).postDelayed(this::startListening,500);}

    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private TextView text(String s,float size,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setFontFeatureSettings("kern");return v;}
    private GradientDrawable shape(int color,float radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp((int)radius));return g;}
    private GradientDrawable stroke(int fill,int line,float radius){GradientDrawable g=shape(fill,radius);g.setStroke(dp(1),line);return g;}
    private GradientDrawable gradient(int a,int b,float radius){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});g.setCornerRadius(dp((int)radius));return g;}
    private TextView section(String eyebrow,String title){LinearLayout box=new LinearLayout(this);return text(title,22,INK);}
    private void margins(View v,int top,int bottom){ViewGroup.LayoutParams raw=v.getLayoutParams();if(raw instanceof LinearLayout.LayoutParams){LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)raw;p.setMargins(0,dp(top),0,dp(bottom));v.setLayoutParams(p);}}
    private Button button(String label,int fill,int color){Button b=new Button(this);b.setText(label);b.setTextSize(14);b.setTextColor(color);b.setAllCaps(false);b.setTypeface(null,Typeface.BOLD);b.setGravity(Gravity.CENTER);b.setPadding(dp(14),dp(9),dp(14),dp(9));b.setBackground(shape(fill,18));b.setStateListAnimator(null);b.setMinHeight(0);b.setMinWidth(0);return b;}
    private TextView sectionTitle(String kicker,String title){LinearLayout wrap=new LinearLayout(this);return null;}

    private void addSectionHeader(LinearLayout root,String kicker,String title){
        TextView k=text(kicker.toUpperCase(new Locale("pl","PL")),11,BLUE);k.setTypeface(null,Typeface.BOLD);k.setLetterSpacing(.12f);LinearLayout.LayoutParams kp=new LinearLayout.LayoutParams(-1,-2);kp.setMargins(dp(2),dp(30),0,dp(4));root.addView(k,kp);
        TextView h=text(title,22,INK);h.setTypeface(null,Typeface.BOLD);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.setMargins(0,0,0,dp(12));root.addView(h,hp);
    }

    private void buildUi(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setClipToPadding(false);sc.setBackgroundColor(BG);sc.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(16),dp(18),dp(44));root.setBackgroundColor(BG);sc.addView(root,new ScrollView.LayoutParams(-1,-2));

        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(22),dp(20),dp(22),dp(22));hero.setBackground(gradient(INK,Color.rgb(26,43,72),30));hero.setElevation(dp(10));
        LinearLayout brand=new LinearLayout(this);brand.setOrientation(LinearLayout.HORIZONTAL);brand.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon=new ImageView(this);icon.setImageResource(R.drawable.ic_assistant);brand.addView(icon,new LinearLayout.LayoutParams(dp(54),dp(54)));
        LinearLayout brandText=new LinearLayout(this);brandText.setOrientation(LinearLayout.VERTICAL);brandText.setPadding(dp(12),0,0,0);TextView logo=text("AGENT MADO",23,Color.WHITE);logo.setTypeface(null,Typeface.BOLD);logo.setLetterSpacing(.05f);brandText.addView(logo);TextView sub=text("Twój inteligentny organizer",13,Color.rgb(184,204,230));brandText.addView(sub);brand.addView(brandText,new LinearLayout.LayoutParams(0,-2,1));
        TextView live=text("●  LIVE",11,ICE);live.setTypeface(null,Typeface.BOLD);live.setPadding(dp(10),dp(7),dp(10),dp(7));live.setBackground(shape(Color.rgb(25,57,82),20));brand.addView(live);hero.addView(brand);

        LinearLayout voice=new LinearLayout(this);voice.setOrientation(LinearLayout.VERTICAL);voice.setGravity(Gravity.CENTER);voice.setPadding(0,dp(22),0,dp(8));
        FrameLayout orbWrap=new FrameLayout(this);LinearLayout.LayoutParams ow=new LinearLayout.LayoutParams(dp(112),dp(112));ow.gravity=Gravity.CENTER_HORIZONTAL;
        View halo=new View(this);halo.setBackground(shape(Color.argb(38,125,211,252),56));orbWrap.addView(halo,new FrameLayout.LayoutParams(-1,-1));
        TextView orb=text("✦",36,Color.WHITE);orb.setGravity(Gravity.CENTER);orb.setBackground(gradient(BLUE,PURPLE,48));orb.setElevation(dp(12));FrameLayout.LayoutParams op=new FrameLayout.LayoutParams(dp(88),dp(88),Gravity.CENTER);orbWrap.addView(orb,op);micOrb=orb;orbWrap.setOnClickListener(v->startListening());voice.addView(orbWrap,ow);
        status=text("Gotowy do działania",20,Color.WHITE);status.setTypeface(null,Typeface.BOLD);status.setGravity(Gravity.CENTER);status.setPadding(0,dp(13),0,dp(4));voice.addView(status,new LinearLayout.LayoutParams(-1,-2));
        transcript=text("Dotknij orb lub po prostu mów po uruchomieniu",14,Color.rgb(184,204,230));transcript.setGravity(Gravity.CENTER);transcript.setMaxLines(3);voice.addView(transcript,new LinearLayout.LayoutParams(-1,-2));hero.addView(voice);
        Button again=button("🎙  Mów ponownie",Color.WHITE,INK);again.setOnClickListener(v->startListening());LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(48));ap.setMargins(0,dp(10),0,0);hero.addView(again,ap);root.addView(hero,new LinearLayout.LayoutParams(-1,-2));animateIn(hero,0);

        LinearLayout summary=new LinearLayout(this);summary.setOrientation(LinearLayout.HORIZONTAL);summary.setGravity(Gravity.CENTER_VERTICAL);summary.setPadding(dp(18),dp(16),dp(18),dp(16));summary.setBackground(stroke(SURFACE,LINE,22));summary.setElevation(dp(2));
        LinearLayout sumText=new LinearLayout(this);sumText.setOrientation(LinearLayout.VERTICAL);todaySummary=text("Dzisiaj",20,INK);todaySummary.setTypeface(null,Typeface.BOLD);todayCaption=text("Twój dzień jest pod kontrolą",13,MUTED);sumText.addView(todaySummary);sumText.addView(todayCaption);summary.addView(sumText,new LinearLayout.LayoutParams(0,-2,1));TextView spark=text("✦",24,BLUE);spark.setGravity(Gravity.CENTER);spark.setBackground(shape(SOFTBLUE,18));summary.addView(spark,new LinearLayout.LayoutParams(dp(48),dp(48)));root.addView(summary,new LinearLayout.LayoutParams(-1,-2));margins(summary,14,0);animateIn(summary,90);

        addSectionHeader(root,"Capture","Szybka notatka");
        LinearLayout noteCard=new LinearLayout(this);noteCard.setOrientation(LinearLayout.VERTICAL);noteCard.setPadding(dp(16),dp(14),dp(16),dp(14));noteCard.setBackground(stroke(SURFACE,LINE,22));noteCard.setElevation(dp(2));
        noteText=new EditText(this);noteText.setHint("Zapisz klienta, wymiar, telefon, pomysł…");noteText.setHintTextColor(Color.rgb(150,160,176));noteText.setTextColor(INK);noteText.setTextSize(16);noteText.setMinLines(2);noteText.setBackgroundColor(Color.TRANSPARENT);noteText.setPadding(0,0,0,dp(8));noteCard.addView(noteText,new LinearLayout.LayoutParams(-1,-2));Button addNote=button("＋  Zapisz notatkę",PURPLE,Color.WHITE);addNote.setOnClickListener(v->addManualNote());noteCard.addView(addNote,new LinearLayout.LayoutParams(-1,dp(46)));root.addView(noteCard);notesContainer=new LinearLayout(this);notesContainer.setOrientation(LinearLayout.VERTICAL);root.addView(notesContainer,new LinearLayout.LayoutParams(-1,-2));animateIn(noteCard,150);

        addSectionHeader(root,"Schedule","Nowe przypomnienie");
        LinearLayout reminderCard=new LinearLayout(this);reminderCard.setOrientation(LinearLayout.VERTICAL);reminderCard.setPadding(dp(16),dp(14),dp(16),dp(16));reminderCard.setBackground(stroke(SURFACE,LINE,22));reminderCard.setElevation(dp(2));
        manualText=new EditText(this);manualText.setHint("Co mam Ci przypomnieć?");manualText.setHintTextColor(Color.rgb(150,160,176));manualText.setTextColor(INK);manualText.setTextSize(16);manualText.setMinLines(2);manualText.setBackgroundColor(Color.TRANSPARENT);manualText.setPadding(0,0,0,dp(8));reminderCard.addView(manualText);
        selectedManualTime=ZonedDateTime.now(ZoneId.systemDefault()).plusHours(1).withMinute(0).withSecond(0).withNano(0);
        LinearLayout dateRow=new LinearLayout(this);dateRow.setOrientation(LinearLayout.HORIZONTAL);dateRow.setGravity(Gravity.CENTER_VERTICAL);Button dateBtn=button("▣  Data",SOFTBLUE,BLUE);dateBtn.setOnClickListener(v->pickDate());Button timeBtn=button("◷  Godzina",SOFTBLUE,BLUE);timeBtn.setOnClickListener(v->pickTime());dateRow.addView(dateBtn,new LinearLayout.LayoutParams(0,dp(42),1));LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,dp(42),1);tp.setMargins(dp(8),0,0,0);dateRow.addView(timeBtn,tp);reminderCard.addView(dateRow);
        manualWhen=text("",13,MUTED);manualWhen.setPadding(dp(3),dp(10),0,dp(10));updateManualWhen();reminderCard.addView(manualWhen);
        Button addManual=button("＋  Dodaj przypomnienie",BLUE,Color.WHITE);addManual.setOnClickListener(v->addManualReminder());reminderCard.addView(addManual,new LinearLayout.LayoutParams(-1,dp(48)));root.addView(reminderCard);animateIn(reminderCard,220);

        addSectionHeader(root,"Focus","Plan dnia");remindersContainer=new LinearLayout(this);remindersContainer.setOrientation(LinearLayout.VERTICAL);root.addView(remindersContainer,new LinearLayout.LayoutParams(-1,-2));
        setContentView(sc);
    }

    private void animateIn(View v,long delay){v.setAlpha(0f);v.setTranslationY(dp(18));v.animate().alpha(1f).translationY(0).setStartDelay(delay).setDuration(520).setInterpolator(new DecelerateInterpolator()).start();}
    private void setListeningFx(boolean on){if(micOrb==null)return;if(on){if(pulseX==null){pulseX=ObjectAnimator.ofFloat(micOrb,View.SCALE_X,1f,1.08f,1f);pulseY=ObjectAnimator.ofFloat(micOrb,View.SCALE_Y,1f,1.08f,1f);for(ObjectAnimator a:new ObjectAnimator[]{pulseX,pulseY}){a.setDuration(1100);a.setRepeatCount(ValueAnimator.INFINITE);a.setInterpolator(new DecelerateInterpolator());}}pulseX.start();pulseY.start();}else{if(pulseX!=null)pulseX.cancel();if(pulseY!=null)pulseY.cancel();micOrb.animate().scaleX(1f).scaleY(1f).setDuration(180).start();}}

    private void pickDate(){ZonedDateTime z=selectedManualTime;new DatePickerDialog(this,(v,y,m,d)->{selectedManualTime=selectedManualTime.withYear(y).withMonth(m+1).withDayOfMonth(d);updateManualWhen();},z.getYear(),z.getMonthValue()-1,z.getDayOfMonth()).show();}
    private void pickTime(){ZonedDateTime z=selectedManualTime;new TimePickerDialog(this,(v,h,m)->{selectedManualTime=selectedManualTime.withHour(h).withMinute(m).withSecond(0).withNano(0);updateManualWhen();},z.getHour(),z.getMinute(),true).show();}
    private void updateManualWhen(){if(manualWhen!=null)manualWhen.setText("Zaplanowano  •  "+selectedManualTime.format(fmt));}

    private void addManualReminder(){String t=manualText.getText().toString().trim();if(t.isEmpty()){toast("Wpisz treść przypomnienia.");return;}if(selectedManualTime.toInstant().toEpochMilli()<=System.currentTimeMillis()){toast("Wybierz termin w przyszłości.");return;}Reminder r=new Reminder(newId(),selectedManualTime.toInstant().toEpochMilli(),t);if(AlarmScheduler.schedule(this,r)){ReminderStore.add(this,r);manualText.setText("");status.setText("Gotowe ✓");transcript.setText("Przypomnienie: "+selectedManualTime.format(fmt));refreshAll();}else{status.setText("Potrzebuję zgody");transcript.setText("Nadaj zgodę „Alarmy i przypomnienia”.");ensureExactAlarm();}}
    private void addManualNote(){String t=noteText.getText().toString().trim();if(t.isEmpty()){toast("Wpisz treść notatki.");return;}NoteStore.add(this,new Note(newId(),System.currentTimeMillis(),t));noteText.setText("");status.setText("Zapisane ✓");transcript.setText("Notatka trafiła do Twojej listy.");refreshNotes();}
    private void addVoiceNote(String spoken){String t=spoken.replaceFirst("(?i)^\\s*(zanotuj|notatka)\\s*[:,-]?\\s*","").trim();if(t.isEmpty()){status.setText("Co zanotować?");return;}NoteStore.add(this,new Note(newId(),System.currentTimeMillis(),t));status.setText("Zapisane ✓");transcript.setText(t);refreshNotes();}
    private int newId(){return (int)((System.currentTimeMillis()+System.nanoTime())&0x7fffffff);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}

    private boolean hasAudio(){return checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;}
    private void requestBasics(){ArrayList<String> p=new ArrayList<>();if(!hasAudio())p.add(Manifest.permission.RECORD_AUDIO);if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)p.add(Manifest.permission.POST_NOTIFICATIONS);if(!p.isEmpty())requestPermissions(p.toArray(new String[0]),44);else ensureExactAlarm();}
    private void ensureExactAlarm(){if(Build.VERSION.SDK_INT>=31){AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);if(!am.canScheduleExactAlarms()){status.setText("Potrzebuję zgody");try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));}catch(Exception ignored){}}}}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);ensureExactAlarm();if(hasAudio())startListening();else status.setText("Włącz mikrofon");}
    private void startListening(){if(!hasAudio()){requestBasics();return;}started=true;if(sr!=null)sr.destroy();sr=SpeechRecognizer.createSpeechRecognizer(this);sr.setRecognitionListener(this);Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"pl-PL");i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3);status.setText("Słucham…");transcript.setText("Powiedz, co mam zrobić.");setListeningFx(true);sr.startListening(i);}
    private void handle(String spoken){transcript.setText("„"+spoken+"”");if(spoken.trim().toLowerCase(new Locale("pl","PL")).matches("^(zanotuj|notatka).*")){addVoiceNote(spoken);return;}status.setText("Analizuję…");AiInterpreter.interpret(spoken,ZonedDateTime.now(ZoneId.systemDefault()),r->{if(!"create_reminder".equals(r.action)){status.setText("Nie rozumiem");transcript.setText(r.reply.isBlank()?"Spróbuj powiedzieć to inaczej.":r.reply);return;}if(r.times.isEmpty()){status.setText("Brakuje terminu");transcript.setText("Powiedz np. „jutro o 8”.");return;}int ok=0;StringBuilder c=new StringBuilder();for(ZonedDateTime z:r.times){Reminder rem=new Reminder(newId(),z.toInstant().toEpochMilli(),r.text);if(AlarmScheduler.schedule(this,rem)){ReminderStore.add(this,rem);ok++;c.append(" • ").append(z.format(fmt));}}status.setText(ok>0?"Gotowe ✓":"Nie udało się");transcript.setText(ok>0?"Zapisałem "+ok+(ok==1?" przypomnienie":" przypomnienia")+c:"Nie mogę ustawić dokładnego alarmu.");refreshAll();});}

    private void refreshAll(){refreshSummary();refreshList();refreshNotes();}
    private void refreshSummary(){if(todaySummary==null)return;LocalDate today=LocalDate.now();int todayCount=0,overdue=0;long now=System.currentTimeMillis();for(Reminder r:ReminderStore.load(this)){if(r.done)continue;LocalDate d=Instant.ofEpochMilli(r.triggerAtMillis).atZone(ZoneId.systemDefault()).toLocalDate();if(d.equals(today))todayCount++;if(r.triggerAtMillis<now)overdue++;}todaySummary.setText(todayCount==0?"Spokojny dzień":"Dzisiaj · "+todayCount+(todayCount==1?" zadanie":" zadań"));todayCaption.setText(overdue>0?overdue+" zaległe — warto je domknąć":"Wszystko pod kontrolą");}
    private TextView miniHeader(String title,int color){TextView h=text(title.toUpperCase(new Locale("pl","PL")),11,color);h.setTypeface(null,Typeface.BOLD);h.setLetterSpacing(.1f);h.setPadding(dp(2),dp(15),0,dp(8));return h;}
    private void refreshList(){if(remindersContainer==null)return;remindersContainer.removeAllViews();List<Reminder> all=ReminderStore.load(this);all.sort(Comparator.comparingLong(a->a.triggerAtMillis));LocalDate today=LocalDate.now();long now=System.currentTimeMillis();int overdue=0,todayN=0,future=0,done=0;for(Reminder r:all)if(!r.done&&r.triggerAtMillis<now){if(overdue++==0)remindersContainer.addView(miniHeader("Zaległe",AMBER));addReminderRow(r,AMBER);}for(Reminder r:all)if(!r.done){LocalDate d=Instant.ofEpochMilli(r.triggerAtMillis).atZone(ZoneId.systemDefault()).toLocalDate();if(d.equals(today)&&r.triggerAtMillis>=now){if(todayN++==0)remindersContainer.addView(miniHeader("Dzisiaj",BLUE));addReminderRow(r,BLUE);}}for(Reminder r:all)if(!r.done){LocalDate d=Instant.ofEpochMilli(r.triggerAtMillis).atZone(ZoneId.systemDefault()).toLocalDate();if(d.isAfter(today)){if(future++==0)remindersContainer.addView(miniHeader("Nadchodzące",PURPLE));addReminderRow(r,PURPLE);}}List<Reminder> rev=new ArrayList<>(all);Collections.reverse(rev);for(Reminder r:rev)if(r.done){if(done++==0)remindersContainer.addView(miniHeader("Wykonane",MINT));addDoneRow(r);}if(all.isEmpty()){LinearLayout empty=new LinearLayout(this);empty.setPadding(dp(18),dp(20),dp(18),dp(20));empty.setGravity(Gravity.CENTER);empty.setBackground(stroke(SURFACE,LINE,22));TextView e=text("✦  Nic pilnego. Dobry moment na nowe zadanie.",15,MUTED);empty.addView(e);remindersContainer.addView(empty);}}
    private void addReminderRow(Reminder r,int accent){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(17),dp(15),dp(17),dp(14));card.setBackground(stroke(SURFACE,LINE,22));card.setElevation(dp(2));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,0,0,dp(10));ZonedDateTime z=Instant.ofEpochMilli(r.triggerAtMillis).atZone(ZoneId.systemDefault());LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);TextView dot=text("●",12,accent);top.addView(dot);TextView when=text(z.format(fmt),13,accent);when.setTypeface(null,Typeface.BOLD);when.setPadding(dp(7),0,0,0);top.addView(when);card.addView(top);TextView body=text(r.text,17,INK);body.setTypeface(null,Typeface.BOLD);body.setPadding(0,dp(7),0,dp(12));card.addView(body);LinearLayout row=new LinearLayout(this);Button doneB=button("✓  Wykonane",Color.rgb(232,249,241),Color.rgb(20,130,88));doneB.setOnClickListener(v->{AlarmScheduler.cancel(this,r.id);ReminderStore.markDone(this,r.id);refreshAll();});Button s30=button("+30 min",Color.rgb(255,247,230),Color.rgb(180,111,0));s30.setOnClickListener(v->snooze(r,30));Button s60=button("+1 h",SOFTBLUE,BLUE);s60.setOnClickListener(v->snooze(r,60));row.addView(doneB,new LinearLayout.LayoutParams(0,dp(42),1.35f));LinearLayout.LayoutParams p2=new LinearLayout.LayoutParams(0,dp(42),1);p2.setMargins(dp(6),0,0,0);row.addView(s30,p2);LinearLayout.LayoutParams p3=new LinearLayout.LayoutParams(0,dp(42),1);p3.setMargins(dp(6),0,0,0);row.addView(s60,p3);card.addView(row);Button del=button("Usuń",Color.TRANSPARENT,MUTED);del.setOnClickListener(v->{AlarmScheduler.cancel(this,r.id);ReminderStore.remove(this,r.id);refreshAll();});LinearLayout.LayoutParams dpv=new LinearLayout.LayoutParams(-1,dp(38));dpv.setMargins(0,dp(4),0,0);card.addView(del,dpv);remindersContainer.addView(card,cp);animateIn(card,0);}
    private void snooze(Reminder r,int minutes){AlarmScheduler.cancel(this,r.id);long nt=System.currentTimeMillis()+minutes*60_000L;Reminder updated=ReminderStore.snooze(this,r.id,nt);if(updated!=null&&AlarmScheduler.schedule(this,updated)){status.setText("Odłożone ✓");transcript.setText(minutes==60?"Wrócę do tego za godzinę.":"Wrócę do tego za 30 minut.");}refreshAll();}
    private void addDoneRow(Reminder r){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.HORIZONTAL);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp(16),dp(13),dp(10),dp(13));card.setBackground(stroke(Color.rgb(244,252,248),Color.rgb(214,241,228),20));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,0,0,dp(8));TextView body=text("✓  "+r.text,15,Color.rgb(36,112,82));body.setTypeface(null,Typeface.BOLD);card.addView(body,new LinearLayout.LayoutParams(0,-2,1));Button del=button("×",Color.TRANSPARENT,MUTED);del.setTextSize(20);del.setOnClickListener(v->{ReminderStore.remove(this,r.id);refreshAll();});card.addView(del,new LinearLayout.LayoutParams(dp(42),dp(42)));remindersContainer.addView(card,cp);}
    private void refreshNotes(){if(notesContainer==null)return;notesContainer.removeAllViews();List<Note> notes=NoteStore.load(this);Collections.reverse(notes);for(Note n:notes)addNoteRow(n);if(notes.isEmpty()){TextView e=text("Brak zapisanych notatek",13,MUTED);e.setPadding(dp(3),dp(10),0,0);notesContainer.addView(e);}}
    private void addNoteRow(Note n){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(13),dp(16),dp(12));card.setBackground(stroke(Color.rgb(250,248,255),Color.rgb(235,228,253),20));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,dp(8),0,0);TextView tag=text("NOTATKA",10,PURPLE);tag.setTypeface(null,Typeface.BOLD);tag.setLetterSpacing(.1f);card.addView(tag);TextView t=text(n.text,16,INK);t.setPadding(0,dp(5),0,dp(9));card.addView(t);LinearLayout row=new LinearLayout(this);Button remind=button("◷  Przypomnij za 1 h",Color.rgb(242,237,255),PURPLE);remind.setOnClickListener(v->{long when=System.currentTimeMillis()+60*60_000L;Reminder r=new Reminder(newId(),when,n.text);if(AlarmScheduler.schedule(this,r)){ReminderStore.add(this,r);status.setText("Gotowe ✓");transcript.setText("Przypomnę za godzinę.");refreshAll();}});Button del=button("Usuń",Color.TRANSPARENT,MUTED);del.setOnClickListener(v->{NoteStore.remove(this,n.id);refreshNotes();});row.addView(remind,new LinearLayout.LayoutParams(0,dp(42),2));row.addView(del,new LinearLayout.LayoutParams(0,dp(42),1));card.addView(row);notesContainer.addView(card,cp);animateIn(card,0);}

    @Override protected void onDestroy(){setListeningFx(false);if(sr!=null)sr.destroy();super.onDestroy();}
    @Override public void onReadyForSpeech(Bundle p){status.setText("Słucham…");setListeningFx(true);}
    @Override public void onBeginningOfSpeech(){}
    @Override public void onRmsChanged(float v){}
    @Override public void onBufferReceived(byte[] b){}
    @Override public void onEndOfSpeech(){status.setText("Rozumiem…");setListeningFx(false);}
    @Override public void onError(int e){started=false;setListeningFx(false);status.setText("Spróbuj ponownie");transcript.setText("Nie usłyszałem całego polecenia.");}
    @Override public void onResults(Bundle b){started=false;setListeningFx(false);ArrayList<String>a=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(a!=null&&!a.isEmpty())handle(a.get(0));}
    @Override public void onPartialResults(Bundle b){ArrayList<String>a=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(a!=null&&!a.isEmpty())transcript.setText(a.get(0));}
    @Override public void onEvent(int e,Bundle b){}
}
