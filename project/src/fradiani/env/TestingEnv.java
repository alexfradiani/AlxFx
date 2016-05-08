package fradiani.env;

import com.google.gson.Gson;
import fradiani.env.AIndicator.INDITYPE;
import fradiani.env.TickServer.TF;
import fradiani.indicators.ATR;
import fradiani.indicators.SMA;
import fradiani.indicators.Stoch;
import fradiani.strategies.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

/**
 * Testing Environment Execute a strategy using TickServer Simulate orders and profit/loss
 */
public class TestingEnv {

  public static final double CONTRACTSIZE = 100000;  //standard size of a contract
  public static final double COMM_MILL = 35;  //commission applied per million USD

  public static final String REPORTS_PATH = "/Users/alx/dev/alx-github/AlxFx/JFExtendedReports/public_html/";

  private Boolean paused;
  private Boolean aborted;
  private AStrategy strategy;
  private final TickServer server;
  private Set<AIndicator> indicators;
  private final List<AOrder> orders;
  private final Set<ATick> lastTicks;

  private String[] pairs;  //pairs for the test
  private String periodStart, periodEnd;  //strings for the periods

  private long startingTime, endingTime;

  private double ballance;  //simulated capital affected by orders profit/loss + commissions
  private double equity;  //capital afer closed orders

  private final ALogger log;  //log used to show events in the report

  /**
   * Constructor
   *
   * @throws Exception
   */
  @SuppressWarnings("LeakingThisInConstructor")  //this is for the strategy "onSettingEnv" call...
  public TestingEnv() throws Exception {
    pairs = null;
    periodStart = periodEnd = null;

    paused = aborted = false;

    //Create instance of the strategy class
    Object obj = Class.forName(ALX_Common.strategyName).newInstance();
    strategy = AStrategy.class.cast(obj);

    //initialize strategy.
    //IMPORTANT: all strategies must set the pairs and the periods for the test
    strategy.onSettingEnv(this);

    //verify pairs and period taken from the strategy
    if (pairs == null) {
      throw new Exception("STRATEGY MUST ESTABLISH PAIRS FOR THE TEST");
    } else {
      try {
        //evaluating FROM
        GregorianCalendar startingTimeGC = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        String[] ymd_hms = periodStart.split(" ");
        String[] ymd = ymd_hms[0].split("\\.");
        String[] hms = ymd_hms[1].split(":");
        startingTimeGC.set(Integer.valueOf(ymd[0]), Integer.valueOf(ymd[1]) - 1, Integer.valueOf(ymd[2]),
          Integer.valueOf(hms[0]), Integer.valueOf(hms[1]), Integer.valueOf(hms[2]));
        startingTimeGC.set(Calendar.MILLISECOND, 0);
        startingTime = startingTimeGC.getTimeInMillis();

        //evaluating TO
        GregorianCalendar endingTimeGC = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        ymd_hms = periodEnd.split(" ");
        ymd = ymd_hms[0].split("\\.");
        hms = ymd_hms[1].split(":");
        endingTimeGC.set(Integer.valueOf(ymd[0]), Integer.valueOf(ymd[1]) - 1, Integer.valueOf(ymd[2]),
          Integer.valueOf(hms[0]), Integer.valueOf(hms[1]), Integer.valueOf(hms[2]));
        endingTimeGC.set(Calendar.MILLISECOND, 0);
        endingTime = endingTimeGC.getTimeInMillis();
      } catch (Exception ex) {
        throw new Exception("FAILURE TO SET PERIODS OF THE TEST. " + ex);
      }
    }
    //create the server for ticks
    server = new TickServer(pairs, startingTime);

    //container of indicators
    indicators = new HashSet<>();

    //ticks used for getLastTick and getTimeOfLastTick
    lastTicks = new HashSet<>();

    //container of orders
    orders = new ArrayList<>();

    //create the log for the report
    log = new ALogger();
    log.write(startingTime, "Strategy Start", "Strategy is started");

    strategy.onStart();  //strategy starting code
  }

  /**
   * Execution loop
   */
  public final void run() {
    long logstep = server.getTime();
    while (server.getTime() <= endingTime) {
      try {
        //execution control
        if (aborted) {
          break;  //end loop
        } else if (paused) {
          continue;
        }

        ATick tick = server.getNextTick();
        if (tick.time > endingTime) {
          break;
        }

        //store the last ticks for each pair
        setLastTicks(tick);

        renderOrders(tick);  //render the tick with env orders

        strategy.onTick(tick);  //process tick in the strategy
        if (server.getTime() - logstep > 5 * 60 * 1000 /*FIVE MINS*/) {
          logStep();
          logstep = server.getTime();
        }
      } 
      catch (Exception ex) {  //there's not enough data
        System.err.println("Exception in Test Run. " + ex);
        break;
      }
    }
    strategy.onStop();

    indicators.clear();
    indicators = null;

    finalizeOrders();  //close any orders left behind
  }

  private void logStep() {
    long time = server.getTime();
    String[] arr = ATick.getTimeAsStrings(time);
    String logstring = "...running: " + arr[0] + "." + arr[1] + "." + arr[2] + " "
      + arr[3] + ":" + arr[4] + ":" + arr[5];

    System.out.println(logstring);
  }

  /**
   * Pause execution
   */
  public void pause() {
    paused = true;
  }

  /**
   * Stop strategy immediately
   */
  public void abort() {
    aborted = true;
  }

  /**
   * set the pairs for the test. Called in the onStart method of the strategy
   *
   * @param _pairs String
   */
  public void setPairs(String _pairs[]) {
    pairs = _pairs;
  }

  /**
   * set the periods for the test.
   */
  public void setPeriods(String start, String end) {
    periodStart = start;
    periodEnd = end;
  }

  /**
   * Set the initial equity for the test
   */
  public void setInitialCapital(double _equity) {
    equity = _equity;
  }

  /**
   * Method to set-up the indicators that are used by a strategy
   */
  public void setIndiListener(INDITYPE type, String uid, String pair, int bufferSize, int period, TF timeframe) {
    AIndicator newIndi;
    switch (type) {
      case SMA:  //Simple Moving Average
        newIndi = new SMA(this, uid, pair, bufferSize, period, timeframe);
        break;
      case STOCH:  //Stochastics
        newIndi = new Stoch(this, uid, pair, bufferSize, period, timeframe);
        break;
      case ATR:  //Average True Range
        newIndi = new ATR(this, uid, pair, bufferSize, period, timeframe);
        break;
      default:
        System.err.println("The indicator requested doesn't exist.");
        return;
    }

    server.addBarListener(newIndi);
    indicators.add(newIndi);
  }

  /**
   * get a previously set indicator
   */
  public AIndicator getIndicator(String uid) {
    for (AIndicator indi : indicators) {
      if (indi.uid.equals(uid)) {
        return indi;
      }
    }

    System.out.println("The requested indicator doesn't exist in the Environment.");
    return null;
  }

  /**
   * GET BARS method to get bars from the server using current running time
   */
  public ABar[] getBars(String pair, TF tf, int shifts) {
    return server.getBars(pair, tf, server.getTime(), shifts);
  }

  /**
   * Set the last ticks of each pair
   */
  private void setLastTicks(ATick tick) {
    for (ATick otick : lastTicks) {
      if (otick.pair.equals(tick.pair)) {  //replace older tick for that pair
        lastTicks.remove(otick);
        lastTicks.add(tick);

        return;
      }
    }
    //if there's no tick for that pair yet
    lastTicks.add(tick);
  }

  /**
   * GET the last tick received from the server for a particular pair
   */
  public ATick getLastTick(String pair) {
    for (ATick tick : lastTicks) {
      if (tick.pair.equals(pair)) {
        return tick;
      }
    }

    //if the current cache doesn't have a tick, load directly from server
    return server.getLastTick(server.getTime(), pair);
  }

  /**
   * Generic method to obtain the exact current time that is running the environment
   */
  public long getTimeOfLastTick() {
    long time = 0;
    for (ATick tick : lastTicks) {
      if (tick.time > time) {
        time = tick.time;
      }
    }

    if (time == 0) //there aren't any thinks loaded yet
    {
      return startingTime;
    } else {
      return time;
    }
  }

  /**
   * returns the start time of the test
   */
  public long getTestStartingTime() {
    return startingTime;
  }

  /**
   * ORDER creation
   *
   * @param label String user-defined identification of the order
   * @param pair Pair applied to the order
   * @param type BUY or SELL
   * @param price price at which the order will be executed
   * @param sl Stop Loss
   * @param tp Take Profits
   * @param size Lot Size
   */
  public AOrder createOrder(String label, String pair, AOrder.OType type,
    double price, double sl, double tp, double size) {
    AOrder order = new AOrder();
    order.label = label;
    order.open_time = getTimeOfLastTick();
    order.open_price = price;
    order.pair = pair;
    order.tp = tp;
    order.sl = sl;
    order.type = type;
    order.size = size;

    //calculate the commission
    double usdvol = getVolInUSD(size * CONTRACTSIZE, pair);
    if (usdvol == 0) {
      System.out.println("Couldn't obtain volume for commission. Order execution aborted.");
      return null;
    }

    order.commission = 2 * (usdvol / 1000000) * COMM_MILL;
    equity += -1 * order.commission;  //it can be added to equity instantly

    order.status = AOrder.OStatus.OPENED;
    orders.add(order);
    renderOrders(getLastTick(pair));

    strategy.onMessage(AOrder.OMsg.OPENED, order);  //send message to the strategy
    //write to report log
    log.write(getTimeOfLastTick(), "Order Created", "Order [" + order.label + ", " + order.pair + ", "
      + order.type.toString() + ", " + order.size + " at " + order.open_price + "] created");
    log.write(getTimeOfLastTick(), "Commission", "Commission [" + String.valueOf(ALX_Common.normalize(order.commission, 2)) + "]");

    return order;
  }

  /**
   * Get the volume of a pair in USD Necessary to calculate order commission
   */
  private double getVolInUSD(double amount, String basepair) {
    double vol = 0;

    String leftCurr = ALX_Common.splitPairCurrencies(basepair)[0];
    if (leftCurr.equals("USD")) {
      vol = amount;
    } else {
      for (ATick tick : lastTicks) {
        String[] crrs = ALX_Common.splitPairCurrencies(tick.pair);
        if (crrs[0].equals(leftCurr) && crrs[1].equals("USD")) {
          vol = amount * tick.ask;
        }
      }

      if (vol == 0) {  //no tick for the necessary in lastTicks...
        for (String p : pairs) {
          String[] crrs = ALX_Common.splitPairCurrencies(p);
          if (crrs[0].equals(leftCurr) && crrs[1].equals("USD")) {
            ATick tick = getLastTick(p);
            vol = amount * tick.ask;
          }
        }
      }
    }

    return vol;
  }

  /**
   * Internal method to verify closing of orders by TP/SL
   */
  private void renderOrders(ATick tick) {
    ballance = 0;
    for (AOrder order : orders) {
      if (order.status == AOrder.OStatus.OPENED) {  //order is active
        if (tick.pair.equals(order.pair)) {
          switch (order.type) {
            case BUY:
              order.profit = order.size * (tick.bid - order.open_price) / ALX_Common.getPipValue(order.pair);
              if (order.tp != 0 && tick.bid >= order.tp) {
                closeOrder(order);
              } else if (order.sl != 0 && tick.bid <= order.sl) {
                closeOrder(order);
              }
              break;
            case SELL:
              order.profit = order.size * (order.open_price - tick.ask) / ALX_Common.getPipValue(order.pair);
              if (order.tp != 0 && tick.ask <= order.tp) {
                closeOrder(order);
              } else if (order.sl != 0 && tick.ask >= order.sl) {
                closeOrder(order);
              }
              break;
          }
        }

        if (order.status == AOrder.OStatus.OPENED) //if order wasn't closed on this tick...
        {
          ballance += order.profit;
        }
      }
    }
    ballance += equity;
  }

  /**
   * ORDER Close
   *
   * @param order
   */
  public void closeOrder(AOrder order) {
    ATick lastTick = getLastTick(order.pair);

    switch (order.type) {
      case BUY:
        order.profit = order.size * (lastTick.bid - order.open_price) / ALX_Common.getPipValue(order.pair);
        order.close_price = lastTick.bid;
        break;
      case SELL:
        order.profit = order.size * (order.open_price - lastTick.ask) / ALX_Common.getPipValue(order.pair);
        order.close_price = lastTick.ask;
        break;
    }
    order.close_time = lastTick.time;
    order.status = AOrder.OStatus.CLOSED;

    //convert to equity the profit or loss
    equity += order.profit;

    //send back msg to the strategy
    strategy.onMessage(AOrder.OMsg.CLOSED, order);
    //write to report log
    log.write(order.close_time, "Order Closed", "Order [" + order.label + ", " + order.pair + ", "
      + order.type.toString() + ", " + order.size + " at " + order.open_price + "] closed "
      + ALX_Common.getReadableTime(order.close_time) + " with profit: " + ALX_Common.normalize(order.profit, 2));

    //remove from orders
    order = null;
    orders.remove(order);
  }

  /**
   * WHEN strategy execution ends close any orders that weren't finalized by the strategy
   */
  private void finalizeOrders() {
    for (AOrder order : orders) {
      if (order.status == AOrder.OStatus.OPENED) //close all opened orders
      {
        closeOrder(order);
      }
    }
  }

  /**
   * Return the money ballance of the current strategy Test
   *
   * @return
   */
  public double getBallance() {
    return ballance;
  }

  /**
   * generate report file. emulating dukascopy format to reuse extended-report web code
   */
  public void createReport() {
    //output file with the html
    String[] datestr = ATick.getTimeAsStrings(startingTime);
    String timestamp = String.valueOf(System.currentTimeMillis());
    String filename = "AlxFx Report " + datestr[0] + "_" + datestr[1] + "_" + datestr[2] + "_" + timestamp + ".html";
    try {
      FileWriter htmlwriter = new FileWriter(REPORTS_PATH + filename, false);

      //read the template code
      BufferedReader r = new BufferedReader(new FileReader(REPORTS_PATH + "alxfx_reptemplate.html"));
      String line;
      while ((line = r.readLine()) != null) {
        htmlwriter.append(line);
      }
      r.close();

      //finish html code with json variables to be used by page javascript
      String strpairs = "";
      for (int i = 0; i < pairs.length; i++) {
        if (i > 0) {
          strpairs += ", ";
        }
        strpairs += pairs[i];
      }
      String reptitle = ALX_Common.strategyName + " report for " + strpairs + " from " + periodStart + " to " + periodEnd;
      htmlwriter.append("<script type=\"text/javascript\">" + '\n');
      htmlwriter.append("var j_reptitle = \"" + reptitle + "\";");
      htmlwriter.append("var j_initialdeposit = " + ALX_Common.equity + ";");  //starting equity
      htmlwriter.append("var j_finalequity = " + equity + ";");  //final equity

      //totalize commissions
      double totalcommi = 0;
      for (AOrder order : orders) {
        totalcommi += order.commission;
      }
      htmlwriter.append("var j_commissions = " + totalcommi + ";");

      //list of orders as JSON
      Gson gson = new Gson();
      htmlwriter.append("var jorders = " + gson.toJson(orders) + ";");

      //list of events from the report log
      htmlwriter.append("var jevents = " + gson.toJson(log.getEvents()) + ";");

      htmlwriter.append("</script>");
      htmlwriter.append("<script type=\"text/javascript\" src=\"Xreport.js\"></script>");
      htmlwriter.append("</body></html>");
      htmlwriter.flush();
      htmlwriter.close();
    } catch (Exception e) {
      System.err.println("Exception creating report. " + e);
    }
  }

  /**
   * Main execute function
   *
   * @param args
   */
  public static void main(String args[]) {
    try {
      //execute the test
      TestingEnv environment = new TestingEnv();
      environment.run();

      //report results
      environment.createReport();
      System.out.println("test finished!");
    } catch (Exception ex) {
      System.err.println(ex);
    }
  }
}
