package fradiani.indicators;

import fradiani.env.ABar;
import fradiani.env.AIndicator;
import fradiani.env.TestingEnv;
import fradiani.env.TickServer;

/**
 * AVERAGE TRUE RANGE indicator AS IN
 * http://stockcharts.com/school/doku.php?st=average+true+range&id=chart_school:technical_indicators:average_true_range_atr
 */
public class ATR extends AIndicator {

  public double buffer[];  //buffer to store calculations

  public ATR(TestingEnv env, String _uid, String pair, int bufferSize, int _period, TickServer.TF timeframe) {
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
    /**
     * true range is the greater of: 1 - current high less the current low 2 - current high less the previous close 3 - current low less the previous
     * close
     */
    for (int i = buffer.length - 1; i >= 0; i--) {  //create buffer values
      double sum_tr = 0;
      for (int j = i; j < i + period; j++) {
        double tr = 0;
        if (j == trackedBars.length - 1) //oldest true-range calculated day
        {
          tr = trackedBars[j].high - trackedBars[j].low;
        } else {
          double cr = trackedBars[j].high - trackedBars[j].low;  //current range
          double hr = Math.abs(trackedBars[j].high - trackedBars[j + 1].close);  //high range
          double lr = Math.abs(trackedBars[j].low - trackedBars[j + 1].close);  //low range

          if (cr > hr && cr > lr) {
            tr = cr;
          } else if (hr > cr && hr > lr) {
            tr = hr;
          } else if (lr > cr && lr > hr) {
            tr = lr;
          }
        }

        sum_tr += tr;
      }
      buffer[i] = sum_tr / period;
    }
  }
}
