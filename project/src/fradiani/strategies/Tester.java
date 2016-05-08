package fradiani.strategies;

import fradiani.env.ABar;
import fradiani.env.AIndicator;
import fradiani.env.AOrder;
import fradiani.env.AStrategy;
import fradiani.env.ATick;
import fradiani.env.TestingEnv;
import fradiani.env.TickServer.TF;
import fradiani.indicators.SMA;

/**
 * tester strategy Only for checking TestingEnv and TickServer functionality
 */
public class Tester implements AStrategy {

  private TestingEnv env;  //access to the testing environment

  @Override
  public void onSettingEnv(TestingEnv environment) {
    env = environment;
    //set pairs and periods for the test
    env.setPairs(ALX_Common.getTradeablePairs());
    env.setPeriods(ALX_Common.periodStart, ALX_Common.periodEnd);  //FROM - TO
  }

  @Override
  public void onStart() {
    //DEBUG getBars
    //testGetBars();

    env.setIndiListener(AIndicator.INDITYPE.SMA, "sma_5min", "EURUSD", 3, 48, TF.T_1MIN);
  }

  @Override
  public void onTick(ATick tick) {
    /*
        //output ticks
        String[] tStr = tick.getTimeAsStrings();
        System.out.println("Tester tick " + tick.pair + " " + tStr[0] + "." + tStr[1] + "." + 
            tStr[2] + " " + tStr[3] + ":" + tStr[4] + ":" + tStr[5] + "." + tStr[6] + " Bid: " + tick.bid + " Ask: " + tick.ask);
     */

    SMA sma = (SMA) env.getIndicator("sma_5min");
  }

  @Override
  public void onMessage(AOrder.OMsg message, AOrder order) {
  }

  @Override
  public void onStop() {
    System.out.println("Tester on stop...");
  }

  /**
   * get bars method, for DEBUGGING purposes
   */
  public void testGetBars() {
    ABar[] lastBars = env.getBars("EURUSD", TF.T_5MIN, 10);
    for (ABar bar : lastBars) {
      String start = ALX_Common.getReadableTime(bar.start);
      String end = ALX_Common.getReadableTime(bar.end);
      double open = ALX_Common.normalize(bar.open, 5);
      double close = ALX_Common.normalize(bar.close, 5);
      double high = ALX_Common.normalize(bar.high, 5);
      double low = ALX_Common.normalize(bar.low, 5);

      System.out.println("bar [start: " + start + " end: " + end
        + "] O: " + open + " C: " + close + " H: " + high + " L: " + low);
    }
  }
}
