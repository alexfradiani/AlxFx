package fradiani.env;

import fradiani.env.TickServer.TF;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Model of a Bar
 */
public class ABar {

  public double high;
  public double low;
  public double open;
  public double close;
  public long start;
  public long end;

  /**
   * offset a millisecond time to its closest exact timeframe value
   */
  public static long truncMillisTF(long time, TF timeframe) {
    GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
    gc.setTimeInMillis(time);

    //truncate the time to the timeframe being used
    switch (timeframe) {
      case T_1MIN:
        gc.set(Calendar.SECOND, 0);
        gc.set(Calendar.MILLISECOND, 0);
        break;
      case T_5MIN:
        //round to 5-min timeframe
        int min = gc.get(Calendar.MINUTE);
        min = 5 * (int) (min / 5);
        gc.set(Calendar.MINUTE, min);
        gc.set(Calendar.SECOND, 0);
        gc.set(Calendar.MILLISECOND, 0);
        break;
      case T_15MIN:
        min = gc.get(Calendar.MINUTE);
        min = 15 * (int) (min / 15);
        gc.set(Calendar.MINUTE, min);
        gc.set(Calendar.SECOND, 0);
        gc.set(Calendar.MILLISECOND, 0);
        break;
      case T_30MIN:
        min = gc.get(Calendar.MINUTE);
        min = 30 * (int) (min / 30);
        gc.set(Calendar.MINUTE, min);
        gc.set(Calendar.SECOND, 0);
        gc.set(Calendar.MILLISECOND, 0);
        break;
      case T_1HOUR:
        gc.set(Calendar.MINUTE, 0);
        gc.set(Calendar.SECOND, 0);
        gc.set(Calendar.MILLISECOND, 0);
        break;
    }

    return gc.getTimeInMillis();
  }

  /**
   * Get the value of a timeframe as millisecs e.g: T_1MIN is 60000 millisecs
   */
  public static long tfAsMillis(TF timeframe) {
    int coef = 0;
    switch (timeframe) {
      case T_1MIN:
        coef = 1;
        break;
      case T_5MIN:
        coef = 5;
        break;
      case T_15MIN:
        coef = 15;
        break;
      case T_30MIN:
        coef = 30;
        break;
      case T_1HOUR:
        coef = 60;
        break;
    }

    return coef * 60 * 1000;
  }
}
