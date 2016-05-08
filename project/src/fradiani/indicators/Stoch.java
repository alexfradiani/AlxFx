package fradiani.indicators;

import fradiani.env.ABar;
import fradiani.env.AIndicator;
import fradiani.env.TestingEnv;
import fradiani.env.TickServer.TF;

/**
 * STOCHASTICS indicator AS IN http://stockcharts.com/school/doku.php?id=chart_school:technical_indicators:stochastic_oscillator_fast_slow_and_full
 */
public class Stoch extends AIndicator {

  public double buffer[][];  //buffer to store calculations

  /**
   * CREATE A Stochastics indicator.
   *
   * @param env instance of the TestingEnvironment
   * @param _uid unique identifier string
   * @param pair instrument for the indicator
   * @param bufferSize number of positions the indicator calculates in history
   * @param _period period for the moving average calculation
   * @param timeframe timeframe of the indicator
   */
  public Stoch(TestingEnv env, String _uid, String pair, int bufferSize, int _period, TF timeframe) {
    super(env, _uid, pair, bufferSize + 3 /*additional slots for slow stochs formula*/, _period, timeframe);

    buffer = new double[bufferSize][2];  //two positions for %K, %D
    calculate();
  }

  @Override
  public void onBar(ABar bar) {
    super.onBar(bar);  //organize trackedBars to include the new one
//        System.out.println("received bar: " + bar.start + " to: " + bar.end);
//        System.out.println("trackedBars: ");
//        String tb = "";
//        for(int i = 0; i < trackedBars.length; i++)
//            tb += "[o: "+trackedBars[i].open+" c:" + trackedBars[i].close + " h:"+trackedBars[i].high + " l:" + 
//                trackedBars[i].low + "]";
//        System.out.println(tb);
    calculate();
  }

  /**
   * calculate indicator buffer values based on current trackedBars
   */
  private void calculate() {
    /**
     * %K = (Current Close - Lowest Low)/(Highest High - Lowest Low) * 100 %D = 3-day SMA of %K
     *
     * NOTE: Slow Stochastics: %K is the 3-period sma of the fast stoch...
     */
    for (int i = buffer.length - 1; i >= 0; i--) {  //buffer positions
      double[] fastK = new double[3];
      int kIndex = 0;
      for (int j = i; j < i + 3; j++) {  //3 fast stoch calculation
        double cc, hh = 0;
        double ll = Double.MAX_VALUE;
        //obtain current close, lowest low, highest high
        for (int k = j; k < j + period; k++) {
          if (trackedBars[k].low < ll) {
            ll = trackedBars[k].low;
          }
          if (trackedBars[k].high > hh) {
            hh = trackedBars[k].high;
          }
        }
        cc = trackedBars[j].close;

        fastK[kIndex++] = (cc - ll) / (hh - ll) * 100;
      }

      //calculate slow %K
      buffer[i][0] = buffer[i][1] = (fastK[0] + fastK[1] + fastK[2]) / 3;  // %D is not used...
    }
  }
}
