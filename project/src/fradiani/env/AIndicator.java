package fradiani.env;

import fradiani.env.TickServer.TF;

/**
 * Common interface to be implemented by all Indicators
 */
public class AIndicator extends ABarListener {

  public String uid;  //unique identifier of the indicator for runtime calls

  protected ABar[] trackedBars;  //bars that the indicator is using as history
  protected int period;

  public enum INDITYPE {  //types of indicators available
    SMA,
    STOCH,
    ATR
  }

  /**
   * Generic constructor for an indicator
   *
   * @param env instance of the TestingEnvironment
   * @param _uid unique identifier string
   * @param bufferSize number of positions the indicator calculates in history
   * @param _period period for the moving average calculation
   * @param timeframe timeframe of the indicator
   */
  public AIndicator(TestingEnv env, String _uid, String pair, int bufferSize, int _period, TF timeframe) {
    super(pair, timeframe);
    this.uid = _uid;
    this.period = _period;

    //determine how many bars are needed for first buffer calculations
    System.out.println("getting initial bars for indicator: " + uid);
    ABar bars[] = env.getBars(pair, timeframe, period + bufferSize);
    System.out.println("initial bars for " + uid + " DONE");

    trackedBars = bars;  //save them for next calculations
  }

  @Override
  public void onBar(ABar bar) {
    //0 => newest ... n => older
    for (int i = trackedBars.length - 1; i > 0; i--) {  //slide by one position all previous bars
      trackedBars[i].start = trackedBars[i - 1].start;
      trackedBars[i].end = trackedBars[i - 1].end;
      trackedBars[i].open = trackedBars[i - 1].open;
      trackedBars[i].close = trackedBars[i - 1].close;
      trackedBars[i].high = trackedBars[i - 1].high;
      trackedBars[i].low = trackedBars[i - 1].low;
    }

    trackedBars[0].start = bar.start;  //assign the new bar received
    trackedBars[0].end = bar.end;
    trackedBars[0].open = bar.open;
    trackedBars[0].close = bar.close;
    trackedBars[0].high = bar.high;
    trackedBars[0].low = bar.low;
  }
}
