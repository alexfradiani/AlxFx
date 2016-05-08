package fradiani.strategies.jv3;

import fradiani.env.AOrder;
import fradiani.env.TestingEnv;
import fradiani.indicators.ATR;
import fradiani.indicators.SMA;
import fradiani.indicators.Stoch;
import fradiani.strategies.ALX_Common;
import fradiani.strategies.ALX_Common.Direction;
import fradiani.strategies.JV3_DMAStochs;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * TriggerManager control variables that fire trade events on an instrument
 */
public class TriggerManager {

  public JV3_DMAStochs strategy;
  public TradeManager tradeMgmt;

  public String trackedPair;
  public double sma2Days[];
  public double sma4Hours[];
  public double stochs[][];

  public Direction sma2DaysDirection;
  public Direction sma4HoursDirection;
  public Direction stochsDirection;

  public GregorianCalendar preDayFilter;

  //UI controls associated with this trigger
  public JV3_UI.TriggerUI ui;

  /**
   * Constructor, receive the tracked pair
   */
  public TriggerManager(JV3_DMAStochs _strategy, String _trackedPair) {
    strategy = _strategy;
    trackedPair = _trackedPair;

    sma2Days = new double[3];
    sma4Hours = new double[3];
    stochs = new double[2][2];

    sma2DaysDirection = sma4HoursDirection = stochsDirection = Direction.NONE;

    //link to a trade manager
    tradeMgmt = new TradeManager(this);

    //preday hour:minute, to avoid overloading of triggers at the beginning of the day
    //int hh = 1 + (int)(Math.random() * 8);
    //int mm = (int)(Math.random() * 60);
    int[] hm = ALX_Common.getWiredPreDay(_trackedPair);

    String dmy = ALX_Common.periodStart.split(" ")[0];
    String dmyArr[] = dmy.split("\\.");

    preDayFilter = new GregorianCalendar();
    preDayFilter.setTimeZone(TimeZone.getTimeZone("GMT"));
    preDayFilter.set(Calendar.YEAR, Integer.valueOf(dmyArr[0]));
    preDayFilter.set(Calendar.MONTH, Integer.valueOf(dmyArr[1]) - 1);
    preDayFilter.set(Calendar.DAY_OF_MONTH, Integer.valueOf(dmyArr[2]));
//        preDayFilter.set(Calendar.HOUR_OF_DAY, hh);
//        preDayFilter.set(Calendar.MINUTE, mm);
    preDayFilter.set(Calendar.HOUR_OF_DAY, hm[0]);
    preDayFilter.set(Calendar.MINUTE, hm[1]);
  }

  /**
   * Alternative constructor, ONLY for testing purposes
   */
  public TriggerManager(String _trackedPair) {
    trackedPair = _trackedPair;
  }

  /**
   * Receive updated values from tick data for tracked indicators
   */
  public void updateIndis() {
    TestingEnv env = strategy.env;
    //get the latest indicators values
    sma2Days = ((SMA) env.getIndicator("SMA_2DAYS_" + trackedPair)).buffer;
    sma4Hours = ((SMA) env.getIndicator("SMA_4HOURS_" + trackedPair)).buffer;
    stochs = ((Stoch) env.getIndicator("Stochs_1MIN_" + trackedPair)).buffer;

    //---------------------------------------------------------verify state for possible candidate trade
    //sma's positions
    if (sma2Days[0] > sma2Days[1] && sma2Days[1] > sma2Days[2]) //2 days tracked in hourly timeframe...
    {
      sma2DaysDirection = Direction.UP;
    } else if (sma2Days[0] < sma2Days[1] && sma2Days[1] < sma2Days[2]) {
      sma2DaysDirection = Direction.DOWN;
    } else {
      sma2DaysDirection = Direction.NONE;
    }

    if (sma4Hours[0] > sma4Hours[1] && sma4Hours[1] > sma4Hours[2]) //4 hours tracked in fivemins timeframe...
    {
      sma4HoursDirection = Direction.UP;
    } else if (sma4Hours[0] < sma4Hours[1] && sma4Hours[1] < sma4Hours[2]) {
      sma4HoursDirection = Direction.DOWN;
    } else {
      sma4HoursDirection = Direction.NONE;
    }

    //Stochastics state
    if (stochs[1][0] < 20 && stochs[0][0] > 20) {
      stochsDirection = Direction.UP;
    } else if (stochs[1][0] > 80 && stochs[0][0] < 80) {
      stochsDirection = Direction.DOWN;
    } else {
      stochsDirection = Direction.NONE;
    }

    //update UI, if strategy is running in GUI mode
    if (strategy.uiIsEnabled) {
      ui.render(this);
    }

    //debug...
//        System.out.println("sma2D: " + sma2Days[0] + " - " + sma2Days[1] + " - " + sma2Days[2] + " : " + sma2DaysDirection);
//        System.out.println("sma4H: " + sma4Hours[0] + " - " + sma4Hours[1] + " - " + sma4Hours[2] + " : " + sma4HoursDirection);
//        System.out.println("stochs: " + stochs[0][0] + " - " + stochs[1][0] + " : " + stochsDirection);
//        double[] atr = ((ATR)env.getIndicator("ATR_1HOUR_" + trackedPair)).buffer;
//        System.out.println("atr: " + atr[0]);
    //match of conditions
    if (sma2DaysDirection == Direction.UP && sma4HoursDirection == Direction.UP && stochsDirection == Direction.UP) {
      fireCandidate(AOrder.OType.BUY);
    } else if (sma2DaysDirection == Direction.DOWN && sma4HoursDirection == Direction.DOWN && stochsDirection == Direction.DOWN) {
      fireCandidate(AOrder.OType.SELL);
    }
  }

  /**
   * If a pair match all indicator values verify final filters NEWS and CORRELATION
   */
  private void fireCandidate(AOrder.OType otype) {
    strategy.handleTriggerCandidate(this, otype);
  }
}
