package fradiani.env;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Model of a tick
 */
public class ATick {

  public double bid;
  public double ask;
  public long time;
  public String pair;

  /**
   * Constructor create ATick object from CSV line
   */
  public ATick(String _pair, String csv) {
    String[] tick_fields = csv.split(",");

    time = getMillisFromCsv(tick_fields[0]);
    bid = Double.valueOf(tick_fields[1]);
    ask = Double.valueOf(tick_fields[2]);

    pair = _pair;
  }

  /**
   * return array with yyyy mm dd hh mm ss millisec for the tick's time
   */
  public String[] getTimeAsStrings() {
    return getTimeAsStrings(time);
  }

  /**
   * return array with yyyy mm dd hh mm ss millisec for the time sent as long parameter
   */
  public static String[] getTimeAsStrings(long eval_time) {
    String[] str = new String[7];

    GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
    gc.setTimeInMillis(eval_time);
    str[0] = String.valueOf(gc.get(Calendar.YEAR));
    str[1] = labeledInt(gc.get(Calendar.MONTH) + 1);
    str[2] = labeledInt(gc.get(Calendar.DAY_OF_MONTH));
    str[3] = labeledInt(gc.get(Calendar.HOUR_OF_DAY));
    str[4] = labeledInt(gc.get(Calendar.MINUTE));
    str[5] = labeledInt(gc.get(Calendar.SECOND));
    str[6] = labeledMillis(gc.get(Calendar.MILLISECOND));

    return str;
  }

  /**
   * Used to obtain leading zeroes in dates, 01, 02, 03, etc...
   */
  public static String labeledInt(int i) {
    if (i < 10) {
      return "0" + String.valueOf(i);
    } else {
      return String.valueOf(i);
    }
  }

  /**
   * get leading-zeroes representation of a milliseconds field
   */
  public static String labeledMillis(int millis) {
    if (millis < 10) {
      return "00" + String.valueOf(millis);
    } else if (millis < 100) {
      return "0" + String.valueOf(millis);
    } else {
      return String.valueOf(millis);
    }
  }

  /**
   * Used to convert time in csv format to long millisec
   */
  public static long getMillisFromCsv(String csv_time) {
    int year = Integer.valueOf(csv_time.substring(0, 4));
    int month = Integer.valueOf(csv_time.substring(5, 7)) - 1;
    int day = Integer.valueOf(csv_time.substring(8, 10));
    int hour = Integer.valueOf(csv_time.substring(11, 13));
    int minute = Integer.valueOf(csv_time.substring(14, 16));
    int second = Integer.valueOf(csv_time.substring(17, 19));
    int millisec = Integer.valueOf(csv_time.substring(20, 23));

    GregorianCalendar gc = new GregorianCalendar();
    gc.set(year, month, day, hour, minute, second);
    gc.setTimeZone(TimeZone.getTimeZone("GMT"));
    gc.set(Calendar.MILLISECOND, millisec);

    return gc.getTimeInMillis();
  }
}
