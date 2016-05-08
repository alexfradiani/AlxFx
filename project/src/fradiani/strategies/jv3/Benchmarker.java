package fradiani.strategies.jv3;

import fradiani.env.AOrder;
import fradiani.strategies.ALX_Common;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/*
 * Benchmarker
 * Helper class to generate statistics of trades performance
 */
public class Benchmarker {

  private static final int MAX_TEST_DAYS = 300;
  private static final int MAX_DAY_TRADES = 20;

  private class DailyOrders {

    private final int count[];
    private final double stoplosses[][];
    private int index;
    private int sl_index;

    public DailyOrders(int ndays, int orders_per_day) {
      count = new int[ndays];
      stoplosses = new double[ndays][orders_per_day];

      for (int i = 0; i < ndays; i++) {
        count[i] = 0;
        for (int j = 0; j < orders_per_day; j++) {
          stoplosses[i][j] = 0.0;
        }
      }

      index = 0;
      sl_index = 0;
    }

    public void addToCurrentDay(double sl) {
      count[index] += 1;
      stoplosses[index][sl_index] = sl;

      sl_index++;
    }

    public void addToNewDay(double sl) {
      index++;
      count[index] += 1;

      stoplosses[index][0] = sl;
      sl_index = 1;
    }

    public int sumOrders() {
      int sum = 0;

      for (int i = 0; i <= index; i++) {
        sum += count[i];
      }

      return sum;
    }

    public double getAvgSL() {
      double sl_acum = 0.0;
      int sl_counter = 0;

      for (int i = 0; i <= index; i++) {
        int _sl_index = 0;
        while (stoplosses[i][_sl_index] > 0.0) {
          sl_acum += stoplosses[i][_sl_index];
          sl_counter++;

          sl_index++;
        }
      }

      return sl_acum / sl_counter;
    }

    public int getOrderCount() {
      return index + 1;
    }
  }

  private final DailyOrders dailyCounter;
  int today;

  /**
   * Constructor
   */
  public Benchmarker(int _today) {
    dailyCounter = new DailyOrders(MAX_TEST_DAYS, MAX_DAY_TRADES);

    today = _today;
  }

  /**
   * Constructor for lotSimulation function
   */
  public Benchmarker() {
    dailyCounter = null;
    this.today = 0;
  }

  /**
   * add order count
   */
  public void countOrder(AOrder order) {
    //determine the day of the order
    long created = order.open_time;
    GregorianCalendar gc = new GregorianCalendar();
    gc.setTime(new Date(created));

    //add to current or next day counter
    int orderDay = gc.get(Calendar.DAY_OF_MONTH);
    double slDiffPoints = Math.abs(order.open_price - order.sl) / ALX_Common.getPipValue(order.pair);
    if (orderDay != this.today) {
      dailyCounter.addToNewDay(slDiffPoints);
      this.today = orderDay;
    } else {
      dailyCounter.addToCurrentDay(slDiffPoints);
    }
  }

  /**
   * This report should help to determine the default lot-size for orders based on back-testing results
   *
   * general formula: lotsize * (average daily orders)*(average SL points) = (max available risk)
   */
  public void totalizeStats() {
    double avgOrders = dailyCounter.sumOrders() / dailyCounter.getOrderCount();
    double avgSL = dailyCounter.getAvgSL();

    System.out.println("Benchmark: [avg orders x day: " + avgOrders + " avg SL: " + avgSL + "]");
  }

  /**
   * Calculations of a possible lot size with a specific capital-margin
   *
   * use-of-leverage = margin / equity margin = exposure / leverage exposure = lotsize * contractsize * (conversion-price)
   *
   * as in https://www.dukascopy.com/swiss/english/forex/forex_trading_accounts/margin/
   *
   */
  public double lotSimulation() {
    //values for the simulation
    double equity = 1000.0;  //TOTAL AVAILABLE CAPITAL 
    double leverage = 300;  //LEVERAGE
    double u_o_l = .6;  //maximum acceptable use-of-leverage

    double trades_per_day = 10.0;  //averaged trades for a day of operation
    double sl_points = 0.00150; //averaged stop-loss acceptable for trades

    double contract = 100000 * trades_per_day;
    double lots = (u_o_l * leverage * equity) / (contract * (sl_points + 1));

    return lots;
  }

  public static void main(String args[]) {
    Benchmarker bmark = new Benchmarker();

    System.out.println("Simulating LOT SIZE for specific capital, leverage.");
    System.out.println("lot size: " + bmark.lotSimulation());
  }
}
