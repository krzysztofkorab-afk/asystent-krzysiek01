package pl.krzysiek.assistant;

public class Reminder {
    public final int id;
    public long triggerAtMillis;
    public final String text;
    public boolean done;
    public long completedAtMillis;

    public Reminder(int id, long triggerAtMillis, String text) {
        this(id, triggerAtMillis, text, false, 0L);
    }

    public Reminder(int id, long triggerAtMillis, String text, boolean done, long completedAtMillis) {
        this.id=id; this.triggerAtMillis=triggerAtMillis; this.text=text; this.done=done; this.completedAtMillis=completedAtMillis;
    }
}
