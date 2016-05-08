package fradiani.env;

import fradiani.strategies.ALX_Common;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

/**
 * Server to provide ticks and bars for all instruments from local CSV data.
 *
 * LOGIC for serving ticks: 
 * 1. chunks of string lines are read from the csv files an kept in cache 
 * 2. the lines of the pairs are ordered serving the closer in time 
 * 3. when a chunk is completed, another one is loaded until the data of that day is done
 *
 */
public class TickServer {

  public static final String SEP = "/"; // separator character
  public static final String TICK_PATH = "/Users/alx/dev/alx-github/AlxFx/csv_data/";
  public static final int CHUNK_SIZE = 1000;  //size of strings array for cache chunk of readed files for every pair

  private Set<CacheChunk> chunks;
  private long runningTime;  //current server's time

  private final Set<ABarListener> blisteners;  //Set of Bar Listeners

  public enum TF {  //timeframes used for bars requests
    T_1MIN,
    T_5MIN,
    T_15MIN,
    T_30MIN,
    T_1HOUR,
    NONE
  }

  //class to handle blocks of cache for ticks
  private class CacheChunk {

    String pair;
    BufferedReader reader;
    int lastChunkedLine;
    ArrayList<String> cache;
    Boolean reachedEOF;

    public CacheChunk(String _pair, BufferedReader _reader) {
      pair = _pair;
      reader = _reader;
      lastChunkedLine = 0;
      reachedEOF = false;
      cache = new ArrayList<>();
    }

    //load from the buffered reader
    public void loadCache() {
      int count = lastChunkedLine;
      String line;
      try {
        while ((line = reader.readLine()) != null) {
          count++;
          if (count == 1 /*headers line*/ || beforeRunningTime(line) || line.equals("")) {
            continue;  //lines previously cached or invalid
          }
          cache.add(line);

          if (count == lastChunkedLine + CHUNK_SIZE) {  //taken enough for a chunk
            lastChunkedLine = count;
            break;  //don't read more
          }
        }

        if (line == null) {
          if (cache.size() > 0) //use chunk with final ticks of the day
          {
            lastChunkedLine = count;
          } else //all ticks of given day have been loaded
          {
            reachedEOF = true;
          }
        }
      } catch (IOException ex) {
        System.err.println("Error reading buffer. " + ex);
      }
    }

    //empty and recreate the cache array
    public void clearCache() {
      try {
        reader.close();
      } catch (Exception ex) {
        System.out.println("reader stream alreaded closed or null. " + ex);
      }
      reader = null;

      lastChunkedLine = 0;
      reachedEOF = true;  //only set to false when a new buffer is loaded

      cache.clear();
      cache = null;
      cache = new ArrayList<>();
    }

    //check if a line is before the server running time. To be Omitted
    private Boolean beforeRunningTime(String line) {
      String timestamp = line.split(",")[0];

      return (ATick.getMillisFromCsv(timestamp) < runningTime);
    }
  }

  /**
   * Constructor
   */
  public TickServer(String[] pairs, long startingTime) {
    chunks = new HashSet<>();

    runningTime = startingTime;

    //Initialize the chunks with buffers
    System.out.println("creating TickServer...");
    GregorianCalendar startingTimeGC = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
    startingTimeGC.setTimeInMillis(startingTime);
    for (String pair : pairs) {
      String year = String.valueOf(startingTimeGC.get(Calendar.YEAR));
      String month = ATick.labeledInt(startingTimeGC.get(Calendar.MONTH) + 1);
      String day = ATick.labeledInt(startingTimeGC.get(Calendar.DAY_OF_MONTH));
      String filename = pair + "_" + year + "_" + month + "_" + day + ".csv";
      String path = TICK_PATH + pair + SEP + year + SEP + filename;

      System.out.println("...loading " + pair + " chunks");
      CacheChunk chunk;
      try {
        BufferedReader r = new BufferedReader(new FileReader(path));
        chunk = new CacheChunk(pair, r);
      } catch (Exception ex) {
        System.out.println("Exception creating TickServer. " + ex);
        chunk = new CacheChunk(pair, null);
        chunk.reachedEOF = true;  //this chunk doesn't have data
      }
      chunks.add(chunk);
      if (chunk.reader != null) {
        chunk.loadCache();
      }
    }

    blisteners = new HashSet<>(); //initialize listeners to add them later by request
    System.out.println("done creating TickServer!");
  }

  /**
   * returns current server running time
   */
  public long getTime() {
    return runningTime;
  }

  /**
   * Method to provide ticks to the testing environment PUBLIC CALL
   */
  public ATick getNextTick() throws Exception {
    ATick tick = getNextTick(1);

    for (ABarListener bl : blisteners) //check if a bar is triggered in the listeners
    {
      bl.renderTick(tick);
    }

    return tick;
  }

  /**
   * Method to provide ticks to the testing environment recursion is checked when days do not have any data for any pair Abort with exception when
   * there are several days with any data
   */
  private ATick getNextTick(int recursion) throws Exception {
    ATick tick;

    //loop all chunks to select the closer tick to current running time
    long closestTickTime = Long.MAX_VALUE;
    CacheChunk closestChunk = null;
    for (CacheChunk chunk : chunks) {
      if (chunk.reachedEOF) //that pair doesn't have more ticks for that day
      {
        continue;
      }

      //get the milliseconds time of the first tick in cache
      String tickcsv = chunk.cache.get(0);
      String tickcsvTime = tickcsv.split(",")[0];
      long timestamp = ATick.getMillisFromCsv(tickcsvTime);

      if (timestamp < closestTickTime) {  //this could be the next tick to return
        closestTickTime = timestamp;
        closestChunk = chunk;
      }
    }

    if (closestChunk != null) {  //there are ticks available to send
      String csv = closestChunk.cache.remove(0);

      //check chunk state for next iteration
      if (closestChunk.cache.size() == 0) //no more elements in this chunk's cache
      {
        closestChunk.loadCache();
      }

      //prepare and return the tick
      tick = new ATick(closestChunk.pair, csv);
      runningTime = closestTickTime;

      return tick;
    } else {  //No more data in any pair for this day. Set next day caches
      try {
        loadNextDayCache();
      } catch (Exception ex) {
        System.out.println("exception loading day: " + ex);
        int charge = 0;
        if (ex.getMessage().equals("missing_day")) {
          charge++;
        }

        if (recursion > 5) //no more than five days in a row without data
        {
          throw new Exception("NOT ENOUGH DATA IN BASE CSV");
        } else {
          runningTime += (24 * 60 * 60 * 1000 + 1000);  //set a leap in the running time
          return getNextTick(recursion + charge);
        }
      }

      return getNextTick(recursion);
    }
  }

  /**
   * Prepare all chunks of cache with next day with data.
   *
   * throw exception when there's a day with no data for any pair
   */
  private void loadNextDayCache() throws Exception {
    int unloaded = 0;

    //Establish the year month day of the path
    GregorianCalendar gc = new GregorianCalendar();
    gc.setTimeZone(TimeZone.getTimeZone("GMT"));
    gc.setTimeInMillis(runningTime);

    //set to the start of next calendar day
    gc.add(Calendar.DAY_OF_MONTH, 1);
    gc.set(Calendar.HOUR_OF_DAY, 0);
    gc.set(Calendar.MINUTE, 0);
    gc.set(Calendar.SECOND, 0);
    gc.set(Calendar.MILLISECOND, 0);

    String year = String.valueOf(gc.get(Calendar.YEAR));
    String month = ATick.labeledInt(gc.get(Calendar.MONTH) + 1);
    String day = ATick.labeledInt(gc.get(Calendar.DAY_OF_MONTH));

    //update the file streams of all chunks
    for (CacheChunk chunk : chunks) {
      String filename = chunk.pair + "_" + year + "_" + month + "_" + day + ".csv";
      String path = TICK_PATH + chunk.pair + SEP + year + SEP + filename;

      chunk.clearCache();
      //check if the file exists to create the cache loading process.. if not, a counter of non-loaded days will start
      try {
        chunk.reader = new BufferedReader(new FileReader(path));
        chunk.reachedEOF = false;

        chunk.loadCache();
      } catch (Exception ex) {
        System.out.println("couldn't load " + path + " exception: " + ex);
        unloaded++;
      }
    }

    //if there's no data for any pair for this day
    if (unloaded == chunks.size()) {
      if (gc.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
        throw new Exception("missing_day");
      } else {
        throw new Exception("");
      }
    }
  }

  /**
   * Load a specific tick closer to the time param. Used by volume calculator when there are no ticks for a pair previously loaded.
   */
  public ATick getLastTick(long time, String pair) {
    GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
    gc.setTimeInMillis(time);

    String year = String.valueOf(gc.get(Calendar.YEAR));
    String month = ATick.labeledInt(gc.get(Calendar.MONTH) + 1);
    String day = ATick.labeledInt(gc.get(Calendar.DAY_OF_MONTH));
    String filename = pair + "_" + year + "_" + month + "_" + day + ".csv";
    String path = TICK_PATH + pair + SEP + year + SEP + filename;

    try {
      BufferedReader r = new BufferedReader(new FileReader(path));
      String line;
      int count = 0;
      while ((line = r.readLine()) != null) {
        count++;
        if (count == 1 || line.equals("")) {
          continue;
        }
        String timestamp = line.split(",")[0];
        if (ATick.getMillisFromCsv(timestamp) < time) {
          continue;  //before time
        }
        return new ATick(pair, line);
      }
    } catch (Exception e) {
      System.err.println("Cannot get lastTick from Server. " + e);
      return null;
    }
    return null;
  }

  /**
   * Provide specific requested bars
   *
   * @param pair instrument to be consulted
   * @param timeframe period of the bars
   * @param time from where to start the bars
   * @param shifts number of bars from start backwards in time
   */
  public ABar[] getBars(final String pair, final TF timeframe, long time, final int shifts) {
    System.out.println("getBars: " + shifts + " for: " + timeframe.toString() + " " + pair);
    ArrayList<ABar> bars;
    int noDayCount = 0;

    //helper class to construct the bars
    class BarConstructor {

      ABar formingBar;  //current bar that is being created

      public BarConstructor() {
        formingBar = null;
      }

      public void addBars(long time, ArrayList<String> ticks, ArrayList<ABar> bars) {  //sequentally add bars to the array
        //walk the array in reversal, from newer to older
        int size = ticks.size();
        for (int i = size - 1; i >= 0; i--) {
          //tick to be computed
          ATick tick = new ATick(pair, ticks.get(i));

          //check bar in creation-process
          if (formingBar == null) {  //first time
            formingBar = new ABar();
            formingBar.open = formingBar.close = formingBar.high = 0;
            formingBar.low = Double.MAX_VALUE;
            formingBar.end = time;
            formingBar.start = time - ABar.tfAsMillis(timeframe);
          }

          //check if the bar is still valid
          while (tick.time < formingBar.start) {  //time to finish the bar
            //add if it's a valid bar
            if (formingBar.open != 0) {
              bars.add(formingBar);
              //if the count of bars is already enough, end the loop...
              if (bars.size() >= shifts) {
                return;  //DONE
              }
              //create a new one
              long prevStart = formingBar.start; //maintain last time
              long prevEnd = formingBar.end;
              formingBar = new ABar();
              formingBar.open = formingBar.close = formingBar.high = 0;
              formingBar.low = Double.MAX_VALUE;
              formingBar.start = prevStart;
              formingBar.end = prevEnd;
            }

            //use previous start as the end of the new backwards bar
            formingBar.end = formingBar.start;
            formingBar.start = formingBar.end - ABar.tfAsMillis(timeframe);
          }

          //evaluate high, low, open
          formingBar.open = tick.bid;  //open is always the older value of that bar
          if (formingBar.close == 0) {
            formingBar.close = tick.bid;
          }
          if (tick.bid > formingBar.high) {
            formingBar.high = tick.bid;
          }
          if (tick.bid < formingBar.low) {
            formingBar.low = tick.bid;
          }
        }
      }
    }

    bars = new ArrayList<>();
    //bar-constructor helper
    BarConstructor bconstructor = new BarConstructor();

    //establish the right start in the TF
    time = ABar.truncMillisTF(time, timeframe);
    String[] timeStr = ATick.getTimeAsStrings(time);

    //set the value of the first file to load
    String filepath;
    GregorianCalendar g = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
    g.set(Integer.valueOf(timeStr[0]), Integer.valueOf(timeStr[1]) - 1, Integer.valueOf(timeStr[2]),
      Integer.valueOf(timeStr[3]), Integer.valueOf(timeStr[4]), Integer.valueOf(timeStr[5]));
    g.add(Calendar.DAY_OF_MONTH, 1 /*for first iteration*/);

    //loop until fulfill bars specified by shifts
    while (bars.size() < shifts) {
      //load the previous day ticks to continue bars creation
      g.add(Calendar.DAY_OF_MONTH, -1);  //one day less
      g.set(Calendar.HOUR_OF_DAY, Integer.valueOf(timeStr[3]));
      g.set(Calendar.MINUTE, Integer.valueOf(timeStr[4]));
      g.set(Calendar.SECOND, Integer.valueOf(timeStr[5]));
      g.set(Calendar.MILLISECOND, 0);
      timeStr = ATick.getTimeAsStrings(g.getTimeInMillis());

      //set the file directory to open
      filepath = TICK_PATH + pair + SEP + timeStr[0] + SEP
        + pair + "_" + timeStr[0] + "_" + timeStr[1] + "_" + timeStr[2] + ".csv";

      //create the array with ticks data
      ArrayList<String> ticks = new ArrayList<>();
      try {
        BufferedReader r = new BufferedReader(new FileReader(filepath));
        //read ticks from the file that are within range
        @SuppressWarnings("UnusedAssignment")
        String line = r.readLine();  //read the header line to skip it...
        while ((line = r.readLine()) != null) {
          long tickTime = ATick.getMillisFromCsv(line.split(",")[0]);
          if (tickTime > time) {
            break;
          } else {
            ticks.add(line);  //add to tne list of ticks to be processed
          }
        }

        //After ticks have been read, run BACKWARDS to create the bars
        if (ticks.size() > 0) {
          bconstructor.addBars(time, ticks, bars);
        }
      } catch (Exception ex) {
        if (g.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {  //weekends don't have data
          noDayCount++;
          System.out.println("NO TICK DATA FOR DAY " + ALX_Common.getReadableTime(g.getTimeInMillis()) + ". " + ex);
        }

        if (noDayCount >= 5) {
          System.err.println("NOT ENOUGH DATA IN BASE CSV.");
          return null;  //abort method
        }
      }
    }

    //convert and return the array
    ABar[] arr = new ABar[bars.size()];
    bars.toArray(arr);

    return arr;
  }

  /**
   * Add a bar listener to be called when a new bar of the specified timeframe is present
   */
  public void addBarListener(ABarListener bl) {
    blisteners.add(bl);
  }

  /**
   * main method for TESTING
   */
  public static void main(String args[]) {
    //evaluating FROM
    GregorianCalendar start = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
    start.set(2015, 11 /*with -1*/, 17, 0, 0, 0);

    //evaluating TO
    GregorianCalendar end = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
    end.set(2015, 11 /*with -1*/, 17, 1, 0, 0);

    //pairs to evaluate
    String[] pairs = {"AUDCAD", "AUDCHF", "AUDJPY"};

    TickServer server = new TickServer(pairs, start.getTimeInMillis());
    while (server.getTime() <= end.getTimeInMillis()) {  //debug ticks...
      try {
        ATick tick = server.getNextTick();

        String[] tStr = tick.getTimeAsStrings();
        System.out.println("tick " + tick.pair + " " + tStr[0] + "." + tStr[1] + "."
          + tStr[2] + " " + tStr[3] + ":" + tStr[4] + ":" + tStr[5] + "." + tStr[6]);
      } catch (Exception ex) {  //there's not enough data
        System.err.println("Exception getting ticks. " + ex);
        break;
      }
    }
  }
}
