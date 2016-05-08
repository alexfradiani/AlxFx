package fradiani.strategies.jv3;

import fradiani.env.ABar;
import fradiani.env.TestingEnv;
import fradiani.env.TickServer.TF;
import fradiani.strategies.ALX_Common;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

/**
 * Read correlation data from CSV file
 */
public class Correlator {
  //absolute path for csv source

  public static String CorrelatorCSV = "/Users/alx/dev/alx-github/AlxFx/tmp/correlation_forexticket.csv";
  //limit of correlation coeficient used to consider two pairs correlated enough to filter each other
  public static double CORRELATION_THRESHOLD = .6;
  //number of periods for the correlation calculation
  private static final int NPERIODS = 50;

  private Boolean isUpdating;  //switch to know when the correlator is under updating

  /**
   * Load the correlation tables for history loaded data tables must be updated again on every call to updateTables()
   */
  public Correlator(TestingEnv env, JV3_UI ui) {
    isUpdating = true;
    try {
      if (this.isOutdated(env.getTimeOfLastTick())) {
        this.updateTables(env, ui);
      } else {
        isUpdating = false;
      }
    } catch (Exception e) {
      System.err.println("exception in correlator constructor: " + e);
    }
  }

  /**
   * Update method to load again the correlation tables should be called every certain time
   * to keep the correlation values up to date.
   */
  public final void updateTables(final TestingEnv env, final JV3_UI ui) {
    System.out.println("generating correlation table");
    isUpdating = true;

    //Prepare CSV file
    try {
      FileWriter writer = new FileWriter(CorrelatorCSV, false /*overwrite*/);

      writer.append("Correlation table - " + NPERIODS + " periods");
      writer.append('\n');
      writer.append("ALX fradiani libs generated");
      writer.append('\n');

      //get the time of the correlation values
      long ctime = env.getTimeOfLastTick();
      SimpleDateFormat df = new SimpleDateFormat("E, dd MM yyyy");
      df.setTimeZone(TimeZone.getTimeZone("GMT"));
      writer.append(df.format(ctime));
      writer.append('\n');

      //column headers
      writer.append("pair1,pair2,15min,30min,Hourly");
      writer.append('\n');

      //LOOP all instruments
      String pairs[] = ALX_Common.getTradeablePairs();
      for (String pair1 : pairs) {
        for (String pair2 : pairs) {
          if (pair1.equals(pair2)) {
            continue;
          }

          System.out.println("parsing " + pair1 + ", " + pair2);
          //write identifiers of the pairs
          writer.append(pair1);
          writer.append(',');
          writer.append(pair2);
          writer.append(',');

          //n-period 15 mins bars
          ABar[] prevBars1 = env.getBars(pair1, TF.T_15MIN, NPERIODS);
          ABar[] prevBars2 = env.getBars(pair2, TF.T_15MIN, NPERIODS);
          double[] p1Arr = new double[prevBars1.length];
          for (int i = 0; i < p1Arr.length; i++) {
            p1Arr[i] = prevBars1[i].close;
          }
          double[] p2Arr = new double[prevBars2.length];
          for (int i = 0; i < p2Arr.length; i++) {
            p2Arr[i] = prevBars2[i].close;
          }
          PearsonsCorrelation pearsons = new PearsonsCorrelation();
          double corrval = pearsons.correlation(p1Arr, p2Arr);
          writer.append(String.valueOf(ALX_Common.normalize(corrval, 2)));
          writer.append(',');

          //n-period 30 mins bars
          prevBars1 = env.getBars(pair1, TF.T_30MIN, NPERIODS);
          prevBars2 = env.getBars(pair2, TF.T_30MIN, NPERIODS);
          p1Arr = new double[prevBars1.length];
          for (int i = 0; i < p1Arr.length; i++) {
            p1Arr[i] = prevBars1[i].close;
          }
          p2Arr = new double[prevBars2.length];
          for (int i = 0; i < p2Arr.length; i++) {
            p2Arr[i] = prevBars2[i].close;
          }
          corrval = pearsons.correlation(p1Arr, p2Arr);
          writer.append(String.valueOf(ALX_Common.normalize(corrval, 2)));
          writer.append(',');

          //n-period hourly bars
          prevBars1 = env.getBars(pair1, TF.T_1HOUR, NPERIODS);
          prevBars2 = env.getBars(pair2, TF.T_1HOUR, NPERIODS);
          p1Arr = new double[prevBars1.length];
          for (int i = 0; i < p1Arr.length; i++) {
            p1Arr[i] = prevBars1[i].close;
          }
          p2Arr = new double[prevBars2.length];
          for (int i = 0; i < p2Arr.length; i++) {
            p2Arr[i] = prevBars2[i].close;
          }
          corrval = pearsons.correlation(p1Arr, p2Arr);
          writer.append(String.valueOf(ALX_Common.normalize(corrval, 2)));
          writer.append(',');

          writer.append('\n');
        }
      }

      //save to file
      writer.flush();
      writer.close();
      System.out.println("Done generating correlations");

      isUpdating = false;

      if (ui != null) {
        ui.renderCorrelator(this, false);
      }
    } 
    catch (Exception e) {
      System.err.println("correlator exception: " + e.getMessage());
    }
  }

  /**
   * retrieves correlation values for two pairs return correlation average between 5MIN, 15MIN, HOURLY and DAILY
   */
  public double getCorrelationFor(String pair1, String pair2) {
    BufferedReader br;
    String line;
    String csvSplitBy = ",";

    double avg = 0; //average to be returned

    try {
      br = new BufferedReader(new FileReader(CorrelatorCSV));
      Boolean crossUnfound = true;
      while ((line = br.readLine()) != null && crossUnfound) {
        String[] vals = line.split(csvSplitBy);

        if (vals[0].equals(pair1) && vals[1].equals(pair2)) {  //cross that is being searched
          avg = (Math.abs(Float.valueOf(vals[2])) + Math.abs(Float.valueOf(vals[3])) + Math.abs(Float.valueOf(vals[4]))) / 3;

          crossUnfound = false;
        }
      }

      //if correlator file doesn't have the necessary pairs, throw message
      if (crossUnfound) {
        System.err.println("CORRELATOR FILE DOESN'T HAVE ALL NECESSARY INSTRUMENTS!");
      }

      br.close();
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }

    return avg;
  }

  /**
   * Get date for last csv file update
   */
  public String getLastUpdate() {
    BufferedReader br;
    String line;

    String lastUpdate = ""; //date of the file

    //check is file exists
    File file = new File(CorrelatorCSV);
    if (!file.exists()) {
      return lastUpdate;
    }

    try {
      br = new BufferedReader(new FileReader(CorrelatorCSV));
      int i = 0;
      while ((line = br.readLine()) != null) {
        if (i == 2) {
          lastUpdate = line;
          break;
        }
        i++;
      }

      br.close();
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }

    return lastUpdate;
  }

  /**
   * Check if correlation file needs to be updated
   */
  public final Boolean isOutdated(long ctime) throws Exception {
    Boolean outdated = false;

    String lastUpdateStr = this.getLastUpdate();

    if (lastUpdateStr.isEmpty()) //there's no previous correlations file
    {
      return true;
    }

    String[] dateStr = lastUpdateStr.split(" ");

    int month = Integer.parseInt(dateStr[2]) - 1;

    GregorianCalendar now = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
    now.setTimeInMillis(ctime);

    GregorianCalendar cupdate = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
    cupdate.set(Integer.parseInt(dateStr[3]), month, Integer.parseInt(dateStr[1]));
    cupdate.set(Calendar.HOUR_OF_DAY, 0);
    cupdate.set(Calendar.MINUTE, 0);
    cupdate.set(Calendar.SECOND, 0);

    long now_ml = now.getTimeInMillis();
    long update_ml = cupdate.getTimeInMillis();
    long difference = Math.abs(update_ml - now_ml);
    if (difference >= 86400000) //less than one day
    {
      outdated = true;
    }

    return outdated;
  }

  /**
   * boolean to check if the correlator is available or is loading the tables
   */
  public Boolean isUpdatingTables() {
    return isUpdating;
  }
}
