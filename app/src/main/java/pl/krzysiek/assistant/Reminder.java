package pl.krzysiek.assistant;

public class Reminder {
    public final int id;
    public final long triggerAtMillis;
    public final String text;
    public Reminder(int id, long triggerAtMillis, String text) {
        this.id = id; this.triggerAtMillis = triggerAtMillis; this.text = text;
    }
}
