package fradiani.indicators;

import fradiani.env.ABar;
import fradiani.env.AIndicator;
import fradiani.env.TestingEnv;
import fradiani.env.TickServer.TF;

/**
 * SIMPLE MOVING AVERAGE indicator AS IN http://stockcharts.com/school/doku.php?id=chart_school:technical_indicators:moving_averages
 */
public class SMA extends AIndicator {

  public double buffer[];  //buffer to store calculations

  /**
   * CREATE A Simple Moving Average indicator. only using median price = (high - low) / 2
   *
   * @param env instance of the TestingEnvironment
   * @param _uid unique identifier string
   * @param pair instrument for the indicator
   * @param bufferSize number of positions the indicator calculates in history
   * @param _period period for the moving average calculation
   * @param timeframe timeframe of the indicator
   */
  public SMA(TestingEnv env, String _uid, String pair, int bufferSize, int _period, TF timeframe) {
    super(env, _uid, pair, bufferSize, _period, timeframe);

    buffer = new double[bufferSize];
    calculate();
  }

  @Override
  public void onBar(ABar bar) {
    super.onBar(bar);  //organize trackedBars to include the new one
    calculate();
  }

  /**
   * calculate indicator buffer values based on current trackedBars
   */
  private void calculate() {
    for (int i = buffer.length - 1; i >= 0; i--) {  //loop on buffer positions for calculations of each shift
      double sum = 0;
      for (int j = i; j < i + period; j++) {
        sum += (trackedBars[j].high + trackedBars[j].low) / 2;
      }

      double avg = sum / period;

      buffer[i] = avg;  //set new value
    }
  }
}
