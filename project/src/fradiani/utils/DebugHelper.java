package fradiani.utils;

import fradiani.env.ATick;
import static fradiani.env.TickServer.TICK_PATH;
import fradiani.strategies.ALX_Common;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * This class is designed to help the debugging process compare results of 
 * Jforex with AlxFX and verify integrity of tick data
 */
public class DebugHelper {

  /**
   * Verify in the BASE_CSV ticks files if all necessary days have been downloaded for a given period
   *
   * @param from
   * @param to
   */
  public void filesAudit(String from, String to) {
    int incidences = 0;

    //determine 'FROM' in milliseconds
    GregorianCalendar startingTimeGC = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
    String[] ymd_hms = from.split(" ");
    String[] ymd = ymd_hms[0].split("\\.");
    String[] hms = ymd_hms[1].split(":");
    startingTimeGC.set(Integer.valueOf(ymd[0]), Integer.valueOf(ymd[1]) - 1, Integer.valueOf(ymd[2]),
      Integer.valueOf(hms[0]), Integer.valueOf(hms[1]), Integer.valueOf(hms[2]));
    startingTimeGC.set(Calendar.MILLISECOND, 0);
    long from_ml = startingTimeGC.getTimeInMillis();

    //determine 'TO' in milliseconds
    GregorianCalendar endingTimeGC = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
    ymd_hms = to.split(" ");
    ymd = ymd_hms[0].split("\\.");
    hms = ymd_hms[1].split(":");
    endingTimeGC.set(Integer.valueOf(ymd[0]), Integer.valueOf(ymd[1]) - 1, Integer.valueOf(ymd[2]),
      Integer.valueOf(hms[0]), Integer.valueOf(hms[1]), Integer.valueOf(hms[2]));
    endingTimeGC.set(Calendar.MILLISECOND, 0);
    long to_ml = endingTimeGC.getTimeInMillis();

    System.out.println("checking BASE CSV integrity...");
    while (from_ml < to_ml) {
      for (String pair : ALX_Common.getTradeablePairs()) {
        GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        gc.setTimeInMillis(from_ml);
        String year = String.valueOf(gc.get(Calendar.YEAR));
        String month = ATick.labeledInt(gc.get(Calendar.MONTH) + 1);
        String day = ATick.labeledInt(gc.get(Calendar.DAY_OF_MONTH));
        String filename = pair + "_" + year + "_" + month + "_" + day + ".csv";
        String path = TICK_PATH + pair + "/" + year + "/" + filename;

        File file = new File(path);
        if (!file.exists()) {  //file doesn't exist
          if (gc.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {  //is not weekend day
            System.out.println("...missing tick file for: " + path);
            incidences++;
          }
        } else {
          checkHours(path);
        }
      }

      from_ml += 24 * 60 * 60 * 1000; //next day
    }

    System.out.println("files checking done! incidences: " + incidences);
  }

  /**
   * check hours consistency within a file
   */
  private void checkHours(String path) {
    try {
      BufferedReader r = new BufferedReader(new FileReader(path));
      String line = r.readLine();
      long prevTime = 0;
      while ((line = r.readLine()) != null) {
        ATick tick = new ATick("", line);

        if (prevTime != 0) {
          if (tick.time - prevTime > 1 * 60 * 60 * 1000) {
            System.out.println("hours inconsistency in: " + path);
          }
        }

        prevTime = tick.time;
      }
      r.close();
      r = null;
    } catch (Exception e) {
      System.out.println("Possible corrupted file: " + path + " Exception: " + e);
    }
  }

  /**
   * Verify minimum size of downloaded files
   */
  public void filesAuditSize() {
    File[] files = new File("E:\\shared\\BASE_CSV\\TICK").listFiles();
    _auditDirSizes(files);
  }

  private void _auditDirSizes(File[] files) {
    for (File f : files) {
      if (f.isDirectory()) {
        _auditDirSizes(f.listFiles());
      } 
      else if (f.length() < 40 /*at least 1 written line*/) {
        System.out.println("suspect file size in: " + f.getAbsolutePath());
      }
    }
  }

  public static void main(String[] args) {
    DebugHelper h = new DebugHelper();
    //check files in base csv
    h.filesAudit("2010.01.01 12:00:00", "2016.01.01 12:00:00");

    //evaluate all downloaded files size
    //h.filesAuditSize();
  }
}
