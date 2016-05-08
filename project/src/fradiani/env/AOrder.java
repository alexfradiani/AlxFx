package fradiani.env;

/**
 * Model of an order
 */
public class AOrder {

  /**
   * Order Stop Loss
   */
  public double sl;
  /**
   * Order Take Profit
   */
  public double tp;

  public long open_time;  //order opening time
  public long close_time;  //order closing time
  public double open_price;  //opening price of the order
  public double close_price;  //closing price of the order
  public double size;  //lot size
  public String pair;  //pair of the order
  public String label;  //user-defined identification of the order
  public OType type;  //BUY or SELL

  public double commission;
  public OStatus status;
  public double profit;

  /**
   * Types of Orders
   */
  public static enum OType {
    BUY,
    SELL
  }

  /**
   * Status of an order
   */
  public static enum OStatus {
    OPENED,
    CLOSED
  }

  /**
   * Order Messages sent from the TestingEnvironment to the strategy
   */
  public static enum OMsg {
    OPENED,
    CLOSED
  }

  public AOrder() {
    close_time = 0;
    close_price = 0;
    label = "";

    sl = tp = 0;
  }
}
