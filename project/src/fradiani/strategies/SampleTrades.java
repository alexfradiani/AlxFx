package fradiani.strategies;

import fradiani.env.AOrder;
import fradiani.env.AOrder.OType;
import fradiani.env.AStrategy;
import fradiani.env.ATick;
import fradiani.env.TestingEnv;

/**
 * TESTING strategy...
 */
public class SampleTrades implements AStrategy {

  TestingEnv env;
  AOrder order;
  int order_count;

  @Override
  public void onSettingEnv(TestingEnv environment) {
    env = environment;

    //set pairs and periods for the test
    env.setPairs(ALX_Common.getTradeablePairs());
    env.setPeriods(ALX_Common.periodStart, ALX_Common.periodEnd);  //FROM - TO
    env.setInitialCapital(ALX_Common.equity);

    order = null;
    order_count = 0;
  }

  @Override
  public void onStart() {
    //TODO...
  }

  @Override
  public void onTick(ATick tick) {
    if (order == null) {  //create first order
      order = env.createOrder(
        "test order",
        tick.pair,
        AOrder.OType.BUY,
        tick.ask,
        tick.bid - 10 * ALX_Common.getPipValue(tick.pair), //SL
        tick.ask + 10 * ALX_Common.getPipValue(tick.pair), //TP
        0.01
      );

      System.out.println(formatTradeAction());
      order_count++;
    } else if (order.status == AOrder.OStatus.CLOSED && order_count < 4) {
      OType otype = order.type == OType.BUY ? OType.SELL : OType.BUY;
      double price = otype == OType.BUY ? tick.ask : tick.bid;
      double sl, tp;
      if (otype == OType.BUY) {
        sl = tick.bid - 10 * ALX_Common.getPipValue(tick.pair);  //SL
        tp = tick.ask + 10 * ALX_Common.getPipValue(tick.pair);  //TP
      } else {
        sl = tick.ask + 10 * ALX_Common.getPipValue(tick.pair);  //SL
        tp = tick.bid - 10 * ALX_Common.getPipValue(tick.pair);  //TP
      }
      order = env.createOrder(
        "test order" + order_count,
        tick.pair,
        otype,
        price,
        sl,
        tp,
        0.01
      );

      System.out.println(formatTradeAction());
      order_count++;
    }
  }

  @Override
  public void onMessage(AOrder.OMsg message, AOrder order) {
    if (message == AOrder.OMsg.CLOSED) {
      System.out.println(formatTradeAction());
    }
  }

  @Override
  public void onStop() {
    System.out.println("Strategy Ended.");
  }

  /**
   * string with fields for reporting order info
   */
  private String formatTradeAction() {
    String open = ALX_Common.getReadableTime(order.open_time);
    String close = "";
    if (order.close_time != 0) {
      close = " close:" + ALX_Common.getReadableTime(order.close_time);
    }

    String str = "Order " + order.label + ": " + order.pair + " " + order.type.toString()
      + " " + String.valueOf(order.size) + " lots open:" + open + close;

    return str;
  }
}
