package fradiani.strategies;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Common functions to all strategies
 */
public class ALX_Common {

  public static final String periodStart = "2013.05.01 00:00:00";  //START TIME OF THE TEST
  public static final String periodEnd = "2013.05.04 00:00:00";  //END TIME OF THE TEST
  public static final double equity = 1000;  //initial capital for a test

  public static final String LOCAL_SERVER = "alx-os.local";  //server for ForexFactory Calendar crawler

  public static final String strategyName = "fradiani.strategies.JV3_DMAStochs";  //Strategy of the test

  public static enum Direction {
    UP,
    DOWN,
    NONE
  }

  //list of recognized currencies
  public static final String[] recognizedCurrencies = {"AUD", "CAD", "CHF", "CNY", "EUR", "GBP", "JPY", "NZD", "USD", "ALL"};

  /**
   * Pairs to be traded in the strategy
   */
  public static String[] getTradeablePairs() {
    String[] pairs = {
      "AUDCAD", "AUDCHF", "AUDJPY", "AUDNZD", "AUDUSD",
      "CADCHF", "CADJPY",
      "CHFJPY",
      "EURAUD", "EURCAD", "EURCHF", "EURGBP", "EURJPY", "EURNZD", "EURUSD",
      "GBPAUD", "GBPCAD", "GBPCHF", "GBPJPY", "GBPNZD", "GBPUSD",
      "NZDCAD", "NZDCHF", "NZDJPY", "NZDUSD", "USDCAD", "USDCHF", "USDJPY"
    };

//        String[] pairs = {"EURUSD"};
    return pairs;
  }

  /**
   * get a string with a valid year.month.day hh:mm:ss for a time
   */
  public static String getReadableTime(long time) {
    Date dtime = new Date(time);

    SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
    df.setTimeZone(TimeZone.getTimeZone("GMT"));

    return df.format(dtime);
  }

  /**
   * Normalize double value to decimal places necessary for execution in the platform
   */
  public static double normalize(double value, int places) {
    return new BigDecimal(value).setScale(places, RoundingMode.HALF_UP).doubleValue();
  }

  /**
   * Return the value of a pip for a given Pair
   */
  public static double getPipValue(String pair) {
    String[] currencies = splitPairCurrencies(pair);

    //only JPY related pairs have less decimal points
    if (currencies[0].equals("JPY") || currencies[1].equals("JPY")) {
      return 0.001;
    } else {
      return 0.00001;
    }
  }

  /**
   * Return the number of decimal places used by a pair pips
   */
  public static int getPipScale(String pair) {
    String[] currencies = splitPairCurrencies(pair);

    //only JPY related pairs have less decimal points
    if (currencies[0].equals("JPY") || currencies[1].equals("JPY")) {
      return 3;
    } else {
      return 5;
    }
  }

  public static String[] splitPairCurrencies(String pair) {
    String[] currs = new String[2];
    currs[0] = pair.substring(0, 3);
    currs[1] = pair.substring(3, 6);

    return currs;
  }

  /**
   * Used for testing comparisons need a predetermined hour and minutes for the preDay filters
   */
  public static int[] getWiredPreDay(String forIns) {
    int[] hm = new int[2];

    if (forIns.equals("AUDCAD")) {
      hm[0] = 8;
      hm[1] = 15;
    } else if (forIns.equals("AUDCHF")) {
      hm[0] = 10;
      hm[1] = 40;
    } else if (forIns.equals("AUDJPY")) {
      hm[0] = 9;
      hm[1] = 10;
    } else if (forIns.equals("AUDNZD")) {
      hm[0] = 2;
      hm[1] = 5;
    } else if (forIns.equals("AUDUSD")) {
      hm[0] = 1;
      hm[1] = 30;
    } else if (forIns.equals("CADCHF")) {
      hm[0] = 1;
      hm[1] = 50;
    } else if (forIns.equals("CADJPY")) {
      hm[0] = 7;
      hm[1] = 5;
    } else if (forIns.equals("CHFJPY")) {
      hm[0] = 6;
      hm[1] = 6;
    } else if (forIns.equals("EURAUD")) {
      hm[0] = 3;
      hm[1] = 30;
    } else if (forIns.equals("EURCAD")) {
      hm[0] = 6;
      hm[1] = 20;
    } else if (forIns.equals("EURCHF")) {
      hm[0] = 7;
      hm[1] = 30;
    } else if (forIns.equals("EURGBP")) {
      hm[0] = 8;
      hm[1] = 40;
    } else if (forIns.equals("EURJPY")) {
      hm[0] = 9;
      hm[1] = 50;
    } else if (forIns.equals("EURNZD")) {
      hm[0] = 2;
      hm[1] = 20;
    } else if (forIns.equals("EURUSD")) {
      hm[0] = 1;
      hm[1] = 10;
    } else if (forIns.equals("GBPAUD")) {
      hm[0] = 2;
      hm[1] = 20;
    } else if (forIns.equals("GBPCAD")) {
      hm[0] = 3;
      hm[1] = 30;
    } else if (forIns.equals("GBPCHF")) {
      hm[0] = 4;
      hm[1] = 40;
    } else if (forIns.equals("GBPJPY")) {
      hm[0] = 5;
      hm[1] = 50;
    } else if (forIns.equals("GBPNZD")) {
      hm[0] = 6;
      hm[1] = 5;
    } else if (forIns.equals("GBPUSD")) {
      hm[0] = 7;
      hm[1] = 10;
    } else if (forIns.equals("NZDCAD")) {
      hm[0] = 5;
      hm[1] = 50;
    } else if (forIns.equals("NZDCHF")) {
      hm[0] = 1;
      hm[1] = 52;
    } else if (forIns.equals("NZDJPY")) {
      hm[0] = 2;
      hm[1] = 21;
    } else if (forIns.equals("NZDUSD")) {
      hm[0] = 9;
      hm[1] = 11;
    } else if (forIns.equals("USDCAD")) {
      hm[0] = 8;
      hm[1] = 20;
    } else if (forIns.equals("USDCHF")) {
      hm[0] = 9;
      hm[1] = 30;
    } else if (forIns.equals("USDJPY")) {
      hm[0] = 10;
      hm[1] = 40;
    }

    return hm;
  }
}
