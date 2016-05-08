package fradiani.strategies;

import fradiani.env.AIndicator;
import fradiani.env.AOrder;
import fradiani.env.AOrder.OMsg;
import fradiani.strategies.jv3.JV3_UI;
import fradiani.env.AStrategy;
import fradiani.env.ATick;
import fradiani.env.TestingEnv;
import fradiani.env.TickServer;
import fradiani.strategies.jv3.Benchmarker;
import fradiani.strategies.jv3.Correlator;
import fradiani.strategies.jv3.NReporter;
import fradiani.strategies.jv3.NReporter.NewsEntry;
import fradiani.strategies.jv3.NReporter.CatchNews;
import fradiani.strategies.jv3.TriggerManager;
import fradiani.strategies.jv3.TradeManager.O_Phase;
import fradiani.strategies.jv3.TradeManager.PreorderState;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

/**
 * ALX STRATEGIES Version 3: TO BE USED ONLY IN AlxFX Testing Environment MA (Moving Averages) && Stochastics Daily Multicurrency && News filtered
 * Strategy
 */
public class JV3_DMAStochs implements AStrategy {
  //context variable

  public TestingEnv env;

  //Components of the strategy
  private Set<TriggerManager> triggers;
  private NReporter reporter;
  private NewsEntry[] currentNews;
  private Correlator correlator;
  public Benchmarker bmark;

  //trades control variables
  public ArrayList<String> todayTradedPairs;
  public int today;  //day of the current trading cycle
  public int nOrders;  //number of orders sent, used by the benchmarker

  //UI visualization
  public Boolean uiIsEnabled = true;
  private JV3_UI ui = null;

  @Override
  public void onSettingEnv(TestingEnv environment) {
    uiIsEnabled = true;  //run test with or without UI

    env = environment;
    //set pairs and periods for the test
    env.setPairs(ALX_Common.getTradeablePairs());  //define the pairs that will generate ticks
    env.setPeriods(ALX_Common.periodStart, ALX_Common.periodEnd);  //FROM - TO
  }

  /**
   * Strategy is started
   */
  @Override
  public void onStart() {
    System.out.println("INIT Strategy");

    //INIT list of traded pairs, for correlation filtering
    todayTradedPairs = new ArrayList<>();
    today = -1;
    nOrders = 0;

    //define the triggers that will be monitoring received ticks
    triggers = new HashSet<>();
    for (String pair : ALX_Common.getTradeablePairs()) {
      TriggerManager trigger = new TriggerManager(this, pair);
      triggers.add(trigger);
    }
    System.out.println("Trigger Mananagers set");

    //----------------------------------------------------------------UI visualization
    if (uiIsEnabled) {
      ui = new JV3_UI(this, "AlxFX: strategy: JV3 - UI", triggers);
      ui.setVisible(true);
      System.out.println("UI started");
    } else {
      System.out.println("UI is disabled.");
    }

    //----------------------------------------------------------------Correlation handler
    correlator = new Correlator(env, ui);
    System.out.println("Correlator started");

    //----------------------------------------------------------------News handler
    currentNews = null;
    try {
      reporter = new NReporter();
    } catch (Exception e) {
      System.err.println(e);
    }
    class Catcher implements CatchNews {

      @Override
      public void onNews(NewsEntry[] newsList) {
        currentNews = newsList;

        System.out.println("loaded news from server");
        if (ui != null && uiIsEnabled) //ui is enabled and object has already been created
        {
          ui.renderNews(currentNews);
        }
      }
    }
    Catcher catcher = new Catcher();
    reporter.listeners.add(catcher);
    System.out.println("News Reporter started");

    //Set indicators used by the strategy
    for (String pair : ALX_Common.getTradeablePairs()) {
      env.setIndiListener(AIndicator.INDITYPE.SMA, "SMA_2DAYS_" + pair, pair, 3, 48, TickServer.TF.T_1HOUR);
      env.setIndiListener(AIndicator.INDITYPE.SMA, "SMA_4HOURS_" + pair, pair, 3, 48, TickServer.TF.T_5MIN);
      env.setIndiListener(AIndicator.INDITYPE.STOCH, "Stochs_1MIN_" + pair, pair, 3, 5, TickServer.TF.T_1MIN);
      env.setIndiListener(AIndicator.INDITYPE.ATR, "ATR_24HOURS_" + pair, pair, 2, 24, TickServer.TF.T_1HOUR);
    }

    System.out.println("Strategy onStart Completed!");
  }

  @Override
  public void onTick(ATick tick) {
    //update trigger data of received instrument tick
    for (TriggerManager trigger : triggers) {
      if (trigger.trackedPair.equals(tick.pair)) { //trigger instrument match the received in the tick
        trigger.updateIndis();
      }
    }

    //update ui time
    if (uiIsEnabled) {
      ui.renderHeader(tick.time);
    }

    //Day checking
    GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
    gc.setTimeInMillis(tick.time);
    int day = gc.get(Calendar.DAY_OF_MONTH);

    if (day != this.today) {  //new day, check news calendar
      System.out.println("Periodical News checking...");

      int month = gc.get(Calendar.MONTH) + 1;
      int year = gc.get(Calendar.YEAR);
      try {
        reporter.getNews(day, month, year);
      } catch (Exception e) {
        System.err.println("news scheduler exception: " + e);
      }

      verifyToday();

      today = day;
      if (bmark == null) {
        bmark = new Benchmarker(today);
      }
    }
  }

  @Override
  public void onMessage(OMsg message, AOrder order) {
    for (TriggerManager trigger : triggers) {
      if (trigger.tradeMgmt.getOrder() != null) {
        if (trigger.tradeMgmt.getOrder() == order) {  //process the respective order manager
          trigger.tradeMgmt.onOrderMessage(message, order);
        }
      }
    }

    if (uiIsEnabled && ui != null) {
      ui.renderAccInfo(env.getBallance());
    }
  }

  @Override
  public void onStop() {
    //benchmark result
    bmark.totalizeStats();

    //close all orders
    for (TriggerManager trigger : triggers) {
      trigger.tradeMgmt.forceClose();
    }
  }

  /**
   * validate candidate trade coming from Trigger Manager
   */
  public void handleTriggerCandidate(TriggerManager trigger, AOrder.OType otype) {
    String instrument = trigger.trackedPair;
    long ctime = env.getLastTick(instrument).time;

    if (trigger.tradeMgmt.ophase == O_Phase.ORDER) {
      return; //there's already an order under process. Skip this.
    }
    //NEWS: Filter if recent news could affect this pair
    if (currentNews == null) {
      if (trigger.tradeMgmt.pstate != PreorderState.FILTERED_BY_NEWS_NOTLOADED) {
        System.out.println("News have not been loaded. Filtering " + otype.toString() + " on "
          + instrument + " at " + ALX_Common.getReadableTime(ctime));
        trigger.tradeMgmt.pstate = PreorderState.FILTERED_BY_NEWS_NOTLOADED;
      }

      return; //FILTERED
    } else if (currentNews.length == 0) {
      System.out.println("No News tracked this day. continuing filtering check...");
      if (trigger.tradeMgmt.pstate == PreorderState.FILTERED_BY_NEWS_NOTLOADED) //reset state
      {
        trigger.tradeMgmt.pstate = PreorderState.CLEARED;
      }
    } else {
      if (trigger.tradeMgmt.pstate == PreorderState.FILTERED_BY_NEWS_NOTLOADED) //reset state
      {
        trigger.tradeMgmt.pstate = PreorderState.CLEARED;
      }

      for (NewsEntry n : currentNews) {
        //check if news belong to a recognized currency
        if (Arrays.asList(ALX_Common.recognizedCurrencies).contains(n.currency)) {
          String[] currencies = ALX_Common.splitPairCurrencies(instrument);
          String leftCurrency = currencies[0];
          String rightCurrency = currencies[1];

          if (n.currency.equals(leftCurrency) || n.currency.equals(rightCurrency)
            || n.currency.equals("ALL")) { //news match... check hours to decide filter
            //date object for current tick
            GregorianCalendar currentGc = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
            currentGc.setTime(new Date(ctime));

            //date object for the news
            String[] hhmm = NReporter.validateNewsTime(n, currentNews);
            if (hhmm == null) //no valid time to track...
            {
              return;
            }

            GregorianCalendar newsGc = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
            newsGc.set(currentGc.get(GregorianCalendar.YEAR), currentGc.get(GregorianCalendar.MONTH),
              currentGc.get(GregorianCalendar.DAY_OF_MONTH), Integer.parseInt(hhmm[0]), Integer.parseInt(hhmm[1]));

            //compare the two dates
            BigInteger difference = BigInteger.valueOf(Math.abs(newsGc.getTimeInMillis() - currentGc.getTimeInMillis()));
            BigInteger oneHour = new BigInteger("3600000");
            if (difference.compareTo(oneHour) < 0) { //less than 1 hour
              if (trigger.tradeMgmt.pstate == PreorderState.FILTERED_BY_NEWS_NEAR) {
                return;
              } else {
                String pairdesc = trigger.trackedPair + " at " + ALX_Common.getReadableTime(ctime);
                String msg = "BLOCKED BY NEWS [" + pairdesc + "] Filtered  by " + n.currency + " news at " + n.time;

                System.out.println(msg);
                if (uiIsEnabled) {
                  trigger.ui.renderTradeAction(msg);
                }

                trigger.tradeMgmt.pstate = PreorderState.FILTERED_BY_NEWS_NEAR;
                return;
              }
            }
          }
        }
      }
    }

    //CORRELATOR: Filter if previous trades in the same day have correlation with this candidate
    if (todayTradedPairs.size() > 0) {
      //if correlator is loading, filter all triggers
      if (correlator.isUpdatingTables()) {
        if (trigger.tradeMgmt.pstate == PreorderState.FILTERED_BY_CORRELATION_UPDATING) {
          return;
        } else {
          System.out.println("Correlations have not been loaded. Filtering " + otype.toString() + " on "
            + instrument + " at " + ALX_Common.getReadableTime(ctime));
          trigger.tradeMgmt.pstate = PreorderState.FILTERED_BY_CORRELATION_UPDATING;

          return;  //FILTERED
        }
      } else {
        if (trigger.tradeMgmt.pstate == PreorderState.FILTERED_BY_CORRELATION_UPDATING) {
          trigger.tradeMgmt.pstate = PreorderState.CLEARED;
        }

        for (String tradedPair : todayTradedPairs) {
          if (tradedPair.equals(instrument)) //if it's the same, maintain previous trade msg
          {
            return; //FILTERED
          }
          double coef = correlator.getCorrelationFor(instrument, tradedPair);
          coef = ALX_Common.normalize(coef, 2);
          if (coef >= Correlator.CORRELATION_THRESHOLD) {
            if (trigger.tradeMgmt.pstate == PreorderState.FILTERED_BY_CORRELATION) {
              return;
            } else {
              trigger.tradeMgmt.pstate = PreorderState.FILTERED_BY_CORRELATION;

              String pairdesc = trigger.trackedPair + " at " + ALX_Common.getReadableTime(ctime);
              String msg = "BLOCKED BY CORRELATION [" + pairdesc + "] " + coef + " correlated to traded " + tradedPair;
              System.out.println(msg);
              if (uiIsEnabled) {
                trigger.ui.renderTradeAction(msg);
              }

              return; //FILTERED
            }
          }
        }
      }
    }

    //PREDAY FILTER - avoid overload of first-time of day trades
    long pd_ml = trigger.preDayFilter.getTimeInMillis();
    if (ctime - pd_ml < 0) {
      if (trigger.tradeMgmt.pstate == PreorderState.FILTERED_BY_PREDAY_TIME) {
        return;
      }

      String pairdesc = trigger.trackedPair + " at " + ALX_Common.getReadableTime(ctime);
      String msg = "BLOCKED BY PREDAY TIME [" + pairdesc + "] set to: "
        + ALX_Common.getReadableTime(trigger.preDayFilter.getTimeInMillis());
      System.out.println(msg);
      if (uiIsEnabled) {
        trigger.ui.renderTradeAction(msg);
      }

      trigger.tradeMgmt.pstate = PreorderState.FILTERED_BY_PREDAY_TIME;

      return;  //FILTERED
    } else if (trigger.tradeMgmt.pstate == PreorderState.FILTERED_BY_PREDAY_TIME) {
      trigger.tradeMgmt.pstate = PreorderState.CLEARED;
    }

    //passed all filters, send to trade manager for order execution.
    trigger.tradeMgmt.processOrder(trigger, otype);
  }

  /**
   * Check current time to verify if current day has changed to clean todayTradedPairs
   */
  public void verifyToday() {
    System.out.println("Setting new day...");

    todayTradedPairs.clear();  //new day, remove previously traded and start again

    //clear states of blocked triggers by correlation
    for (TriggerManager trigger : triggers) {
      if (trigger.tradeMgmt.pstate == PreorderState.FILTERED_BY_CORRELATION) {
        trigger.tradeMgmt.pstate = PreorderState.CLEARED;

        if (ui != null && uiIsEnabled) {
          trigger.ui.renderTradeAction("Last trade action: -");
        }
      }
    }

    try {
      if (correlator.isUpdatingTables() == false && correlator.isOutdated(env.getTimeOfLastTick())) {
        correlator.updateTables(env, ui);
        if (ui != null && uiIsEnabled) {
          ui.renderCorrelator(correlator, true);
        }
      }
    } catch (Exception e) {
      System.err.println(e);
    }
  }
}
