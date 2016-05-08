package fradiani.env;

import fradiani.env.TickServer.TF;

/**
 * Basic logic for bar listening
 */
public class ABarListener {

  public TF timeframe;  //timeframe being tracked
  public String pair;  //instrument being tracked

  private ABar pBar = null;  //pointer bar under process

  public ABarListener(String _pair, TF _timeframe) {
    this.pair = _pair;
    this.timeframe = _timeframe;
  }

  /**
   * evaluate a new tick to verify if it triggers a new bar
   */
  public void renderTick(ATick tick) {
    if (!tick.pair.equals(pair)) //process only the respective pair
    {
      return;
    }

    if (pBar == null) {  //first time
      pBar = new ABar();

      pBar.start = ABar.truncMillisTF(tick.time, timeframe);
      pBar.end = pBar.start + ABar.tfAsMillis(timeframe);

      pBar.open = pBar.close = pBar.high = 0;
      pBar.low = Double.MAX_VALUE;
    }

    while (tick.time > pBar.end) {  //time for a new bar
      if (pBar.open != 0) {
        onBar(pBar);  //send event to handling in the strategy or indis...
      }
      pBar.open = pBar.close = pBar.high = 0;
      pBar.low = Double.MAX_VALUE;
      pBar.start = pBar.end;
      pBar.end = pBar.start + ABar.tfAsMillis(timeframe);
    }

    //adjust current bar values with this tick
    pBar.close = tick.bid;  //close is always the latest value within range
    if (pBar.open == 0) {
      pBar.open = tick.bid;
    }
    if (tick.bid > pBar.high) {
      pBar.high = tick.bid;
    }
    if (tick.bid < pBar.low) {
      pBar.low = tick.bid;
    }
  }

  public void onBar(ABar bar) {
  }  //method to be implemented in subclass
}
