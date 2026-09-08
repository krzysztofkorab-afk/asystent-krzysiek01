package pl.krzysiek.assistant;

import java.text.Normalizer;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.regex.*;

public class PolishReminderParser {
    public static class Result { public final String text; public final List<ZonedDateTime> times; Result(String t,List<ZonedDateTime> z){text=t;times=z;} }
    private static String ascii(String s){return Normalizer.normalize(s.toLowerCase(Locale.forLanguageTag("pl")),Normalizer.Form.NFD).replaceAll("\\p{M}","");}
    public static Result parse(String original,ZonedDateTime now){
        String s=ascii(original).replace(',',' '); LocalDate date=now.toLocalDate();
        if(s.contains("pojutrze"))date=date.plusDays(2);else if(s.contains("jutro"))date=date.plusDays(1);else if(!(s.contains("dzisiaj")||s.contains("dzis"))){
            Map<String,DayOfWeek> wd=new LinkedHashMap<>();wd.put("poniedzial",DayOfWeek.MONDAY);wd.put("wtorek",DayOfWeek.TUESDAY);wd.put("srode",DayOfWeek.WEDNESDAY);wd.put("sroda",DayOfWeek.WEDNESDAY);wd.put("czwartek",DayOfWeek.THURSDAY);wd.put("piatek",DayOfWeek.FRIDAY);wd.put("sobote",DayOfWeek.SATURDAY);wd.put("sobota",DayOfWeek.SATURDAY);wd.put("niedziele",DayOfWeek.SUNDAY);wd.put("niedziela",DayOfWeek.SUNDAY);for(Map.Entry<String,DayOfWeek>e:wd.entrySet())if(s.contains(e.getKey())){date=date.with(TemporalAdjusters.nextOrSame(e.getValue()));break;}
            Matcher md=Pattern.compile("\\b(\\d{1,2})[.\\-/](\\d{1,2})(?:[.\\-/](\\d{2,4}))?\\b").matcher(s);if(md.find()){int d=Integer.parseInt(md.group(1)),m=Integer.parseInt(md.group(2)),y=md.group(3)==null?date.getYear():Integer.parseInt(md.group(3));if(y<100)y+=2000;try{date=LocalDate.of(y,m,d);if(md.group(3)==null&&date.isBefore(now.toLocalDate()))date=date.plusYears(1);}catch(Exception ignored){}}
            String[] months={"stycznia","lutego","marca","kwietnia","maja","czerwca","lipca","sierpnia","wrzesnia","pazdziernika","listopada","grudnia"};for(int mi=0;mi<months.length;mi++){Matcher mm=Pattern.compile("\\b(\\d{1,2})\\s+"+months[mi]+"(?:\\s+(\\d{4}))?").matcher(s);if(mm.find()){int y=mm.group(2)==null?date.getYear():Integer.parseInt(mm.group(2));try{date=LocalDate.of(y,mi+1,Integer.parseInt(mm.group(1)));if(mm.group(2)==null&&date.isBefore(now.toLocalDate()))date=date.plusYears(1);}catch(Exception ignored){}break;}}
        }
        Matcher rel=Pattern.compile("za\\s+(\\d+)\\s*(minut|minuty|min|godzin|godziny|godz|h)").matcher(s);if(rel.find()){int n=Integer.parseInt(rel.group(1));ZonedDateTime z=rel.group(2).startsWith("min")?now.plusMinutes(n):now.plusHours(n);return new Result(cleanText(original),Collections.singletonList(z.withSecond(0).withNano(0)));}
        List<LocalTime> times=new ArrayList<>();Matcher tm=Pattern.compile("(?:o(?:\\s+godzinie)?\\s+)([01]?\\d|2[0-3])(?:(?:[:.])([0-5]\\d))?(?=\\D|$)").matcher(s);while(tm.find())times.add(LocalTime.of(Integer.parseInt(tm.group(1)),tm.group(2)==null?0:Integer.parseInt(tm.group(2))));if(times.isEmpty()){if(s.contains("rano"))times.add(LocalTime.of(8,0));else if(s.contains("po poludniu"))times.add(LocalTime.of(15,0));else if(s.contains("wieczorem"))times.add(LocalTime.of(19,0));}
        if(times.isEmpty())return new Result(cleanText(original),Collections.emptyList());List<ZonedDateTime> out=new ArrayList<>();for(LocalTime t:times){ZonedDateTime z=ZonedDateTime.of(date,t,now.getZone());if(date.equals(now.toLocalDate())&&!z.isAfter(now))z=z.plusDays(1);out.add(z);}return new Result(cleanText(original),out);
    }
    private static String cleanText(String s){String t=s.trim();Matcher m=Pattern.compile("(?i)przypomnij\\s+mi\\s*(?:,|:|-)?\\s*(.*)$").matcher(t);if(m.find()&&!m.group(1).isBlank())t=m.group(1).trim();t=t.replaceFirst("(?i)^\\s*żebym\\s+","");return t.isBlank()?"Zaplanowane przypomnienie":t;}
}
