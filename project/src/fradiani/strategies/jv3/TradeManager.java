package fradiani.strategies.jv3;

import fradiani.env.TestingEnv;
import fradiani.env.AOrder;
import fradiani.env.ATick;
import fradiani.indicators.ATR;
import fradiani.strategies.ALX_Common;
import fradiani.strategies.jv3.JV3_UI.LABELSTYLES;

/**
 * Management of trade execution and money
 */
public class TradeManager {

  private final TriggerManager trigger;  //the trigger associated to this Trade Manager

  public static int SLIPPAGE = 10;

  //Money management
  double lotSize;

  //Define possible states for orders
  public enum O_Phase {
    PREORDER,
    ORDER
  };

  public enum PreorderState {
    CLEARED, //default state when there's no order, waiting for action
    FILTERED_BY_NEWS_NOTLOADED, //news have not been loading, wait to process any orders
    FILTERED_BY_NEWS_NEAR, //order filtered, news are close in time
    FILTERED_BY_CORRELATION_UPDATING, //correlation is beign updated for subscribed instruments
    FILTERED_BY_CORRELATION, //filtered by correlation between pairs
    FILTERED_BY_PREDAY_TIME  //time filter set by the trigger randomly
  };
  public O_Phase ophase;
  public PreorderState pstate;

  private AOrder order;  //the actual order of this trade manager

  /**
   * Constructor
   */
  public TradeManager(TriggerManager _trigger) {
    trigger = _trigger;

    lotSize = 0.001; //amount in millions   0.001 -> 0.01 lot

    ophase = O_Phase.PREORDER; //waiting for action
    pstate = PreorderState.CLEARED;

    order = null;
  }

  /**
   * Receives and order execution request and manages its life-cycle
   */
  public void processOrder(TriggerManager trigger, AOrder.OType otype) {
    pstate = PreorderState.CLEARED;
    ophase = O_Phase.ORDER;

    TestingEnv env = trigger.strategy.env;
    ATick tick = env.getLastTick(trigger.trackedPair);

    //get the ATR for defining SL and TP
    ATR atr = (ATR) env.getIndicator("ATR_24HOURS_" + trigger.trackedPair);

    int point = ALX_Common.getPipScale(trigger.trackedPair);
    double sl;
    double tp;
    double price;
    if (otype == AOrder.OType.BUY) {
      sl = tick.bid - atr.buffer[0];
      sl = ALX_Common.normalize(sl, point);
      tp = tick.ask + atr.buffer[0];
      tp = ALX_Common.normalize(tp, point);
      price = tick.ask;
    } else {
      sl = tick.bid + atr.buffer[0];
      sl = ALX_Common.normalize(sl, point);
      tp = tick.ask - atr.buffer[0];
      tp = ALX_Common.normalize(tp, point);
      price = tick.bid;
    }

    //send order to TestingEnvironment
    int nOrders = ++(trigger.strategy.nOrders);
    order = env.createOrder(
      "No_" + String.valueOf(nOrders), //label
      trigger.trackedPair,
      otype,
      price,
      sl,
      tp,
      lotSize
    );

    //add to traded pairs, but remove if order is rejected
    trigger.strategy.todayTradedPairs.add(trigger.trackedPair);
  }

  /**
   * Handle when the order status changes
   */
  public void onOrderMessage(AOrder.OMsg message, AOrder order) {
    if (message == AOrder.OMsg.OPENED) {  //order has been taken
      //order is active
      String msg = formatTradeAction();
      System.out.println(msg);
      if (trigger.strategy.uiIsEnabled) {
        trigger.ui.renderTradeAction(msg, LABELSTYLES.ACTIVE_TRADE);
      }

      trigger.strategy.bmark.countOrder(order);  //order benchmark
    } else if (message == AOrder.OMsg.CLOSED) {  //order closed OK
      //order has been closed
      String msg = formatTradeAction();
      System.out.println(msg);
      if (trigger.strategy.uiIsEnabled) {
        trigger.ui.renderTradeAction(msg);
      }

      ophase = O_Phase.PREORDER;
      pstate = PreorderState.CLEARED;

      this.order = null;
    }
  }

  public AOrder getOrder() {
    return order;
  }

  public void forceClose() {
    if (order != null) {
      trigger.strategy.env.closeOrder(order);
    }
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
