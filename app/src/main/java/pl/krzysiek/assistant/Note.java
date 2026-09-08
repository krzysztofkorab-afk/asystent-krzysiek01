package pl.krzysiek.assistant;

public class Note {
    public final int id;
    public final long createdAtMillis;
    public final String text;
    public Note(int id,long createdAtMillis,String text){this.id=id;this.createdAtMillis=createdAtMillis;this.text=text;}
}
