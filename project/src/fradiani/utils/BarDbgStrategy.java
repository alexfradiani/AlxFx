package fradiani.utils;

import fradiani.env.ABar;
import fradiani.env.AOrder;
import fradiani.env.AStrategy;
import fradiani.env.ATick;
import fradiani.env.TestingEnv;
import fradiani.env.TickServer.TF;
import fradiani.strategies.ALX_Common;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * helper class for debugging purposes Compare bars from jforex to alxfx
 */
public class BarDbgStrategy implements AStrategy {

  private TestingEnv env;
  private List<String> m_jforex;
  private List<String> m_alxfx;

  private class Stamp {

    String pair;
    long last_1min_time;
    long last_5min_time;
    long last_15min_time;
    long last_30min_time;
    long last_1hour_time;
  }
  Stamp[] stamps;

  @Override
  public void onSettingEnv(TestingEnv environment) {
    //set environment parameters
    environment.setPairs(ALX_Common.getTradeablePairs());
    environment.setPeriods(ALX_Common.periodStart, ALX_Common.periodEnd);  //FROM - TO
    environment.setInitialCapital(ALX_Common.equity);

    env = environment;
  }

  @Override
  public void onStart() {
    m_alxfx = new ArrayList<>();
    m_jforex = new ArrayList<>();

    for (String pair : ALX_Common.getTradeablePairs()) {
      ABar[] b1min = env.getBars(pair, TF.T_1MIN, 480);
      ABar[] b5min = env.getBars(pair, TF.T_5MIN, 480);
      ABar[] b15min = env.getBars(pair, TF.T_15MIN, 480);
      ABar[] b30min = env.getBars(pair, TF.T_30MIN, 480);
      ABar[] b1hour = env.getBars(pair, TF.T_1HOUR, 480);

      for (ABar b : b1min) {
        m_alxfx.add(pair + "," + b.start + "," + b.end + "," + b.open + "," + b.close + "," + b.high + "," + b.low);
      }
      for (ABar b : b5min) {
        m_alxfx.add(pair + "," + b.start + "," + b.end + "," + b.open + "," + b.close + "," + b.high + "," + b.low);
      }
      for (ABar b : b15min) {
        m_alxfx.add(pair + "," + b.start + "," + b.end + "," + b.open + "," + b.close + "," + b.high + "," + b.low);
      }
      for (ABar b : b30min) {
        m_alxfx.add(pair + "," + b.start + "," + b.end + "," + b.open + "," + b.close + "," + b.high + "," + b.low);
      }
      for (ABar b : b1hour) {
        m_alxfx.add(pair + "," + b.start + "," + b.end + "," + b.open + "," + b.close + "," + b.high + "," + b.low);
      }
    }

    //initialize stamps
//        stamps = new Stamp[28];
//        String[] pairs = ALX_Common.getTradeablePairs();
//        for(int i = 0, len = stamps.length; i < len; i++) {
//            stamps[i] = new Stamp();
//            stamps[i].pair = pairs[i];
//            stamps[i].last_1min_time = 0;
//            stamps[i].last_5min_time = 0;
//            stamps[i].last_15min_time = 0;
//            stamps[i].last_30min_time = 0;
//            stamps[i].last_1hour_time = 0;
//        }
    readJForexBars();
  }

  @Override
  public void onTick(ATick tick) {
//        if(tick.time - env.getTestStartingTime() < 1 * 60 * 1000 /*one min*/)  //offset...
//            return;
//        
//        //verify if a bar can be tracked
//        for(Stamp stamp : stamps)
//            if(stamp.pair.equals(tick.pair)) {  //this is the pair with the tick
//                if(tick.time - stamp.last_1min_time >= 1 * 60 * 1000 /*one min*/) {
//                    //create a new bar entry
//                    ABar[] bars  = env.getBars(tick.pair, TF.T_1MIN, 2);
//                    String str = tick.pair + "," + bars[0].start + "," + bars[0].end + "," + bars[0].open + "," + 
//                        bars[0].close + "," + bars[0].high + "," + bars[0].low;
//                    m_alxfx.add(str);
//                    //System.out.println("added " + str);
//                    stamp.last_1min_time = bars[0].start;
//                }
//                
//                if(tick.time - stamp.last_5min_time >= 5 * 60 * 1000 /*five min*/) {
//                    //create a new bar entry
//                    ABar[] bars  = env.getBars(tick.pair, TF.T_5MIN, 2);
//                    String str = tick.pair + "," + bars[0].start + "," + bars[0].end + "," + bars[0].open + "," + 
//                        bars[0].close + "," + bars[0].high + "," + bars[0].low;
//                    m_alxfx.add(str);
//                    //System.out.println("added " + str);
//                    stamp.last_1min_time = bars[0].start;
//                }
//                
//                if(tick.time - stamp.last_15min_time >= 15 * 60 * 1000 /*15 min*/) {
//                    //create a new bar entry
//                    ABar[] bars  = env.getBars(tick.pair, TF.T_15MIN, 2);
//                    String str = tick.pair + "," + bars[0].start + "," + bars[0].end + "," + bars[0].open + "," + 
//                        bars[0].close + "," + bars[0].high + "," + bars[0].low;
//                    m_alxfx.add(str);
//                    //System.out.println("added " + str);
//                    stamp.last_15min_time = bars[0].start;
//                }
//                
//                if(tick.time - stamp.last_30min_time >= 30 * 60 * 1000 /*30 min*/) {
//                    //create a new bar entry
//                    ABar[] bars  = env.getBars(tick.pair, TF.T_30MIN, 2);
//                    String str = tick.pair + "," + bars[0].start + "," + bars[0].end + "," + bars[0].open + "," + 
//                        bars[0].close + "," + bars[0].high + "," + bars[0].low;
//                    m_alxfx.add(str);
//                    //System.out.println("added " + str);
//                    stamp.last_30min_time = bars[0].start;
//                }
//                
//                if(tick.time - stamp.last_1hour_time >= 60 * 60 * 1000 /*hour*/) {
//                    //create a new bar entry
//                    ABar[] bars  = env.getBars(tick.pair, TF.T_1HOUR, 2);
//                    String str = tick.pair + "," + bars[0].start + "," + bars[0].end + "," + bars[0].open + "," + 
//                        bars[0].close + "," + bars[0].high + "," + bars[0].low;
//                    m_alxfx.add(str);
//                    //System.out.println("added " + str);
//                    stamp.last_1hour_time = bars[0].start;
//                }
//            }
  }

  @Override
  public void onMessage(AOrder.OMsg message, AOrder order) {
    //TODO
  }

  @Override
  public void onStop() {
    compareBars();

    System.out.println("strategy stopped. total incidences: " + m_alxfx.size());
    System.out.println("mismatched jforex bars: ");
    for (int i = 0; i < m_jforex.size(); i++) {
      System.out.println(m_jforex.get(i));
    }

    System.out.println("mismatched alxfx bars: ");
    for (int i = 0; i < m_alxfx.size(); i++) {
      System.out.println(m_alxfx.get(i));
    }
  }

  private void readJForexBars() {
    //open the log file to compare ticks
    try {
      BufferedReader reader = new BufferedReader(
        new FileReader("/Users/alx/AlxFx/tmp/barslog"));
      String line;
      while ((line = reader.readLine()) != null) {
        m_jforex.add(line);
      }
      reader.close();
    } catch (Exception e) {
      System.err.println(e);
    }
  }

  private void compareBars() {
    for (int i = 0; i < m_alxfx.size(); i++) {
      System.out.println("compare: " + i + " of: " + m_alxfx.size());
      Boolean matched = false;
      for (int j = 0; j < m_jforex.size(); j++) {
        if (barProximity(m_alxfx.get(i), m_jforex.get(j))) {
          m_jforex.remove(j--);
          matched = true;
        }
      }
      if (matched) {
        m_alxfx.remove(i--);
      }

      if (i > 100) {
        return;
      }
    }

//        if(m_alxfx.size() > 50)
//            env.abort();
  }

  private Boolean barProximity(String strA, String strB) {
    String[] bA = strA.split(",");
    String[] bB = strB.split(",");

    if (!bA[0].equals(bB[0])) //different pairs
    {
      return false;
    }

    if (!bA[1].equals(bB[1])) //different start time
    {
      return false;
    }

    if (!bA[2].equals(bB[2])) //different end time
    {
      return false;
    }

    //open, close, high, low
    double openA = Double.parseDouble(bA[3]);
    double openB = Double.parseDouble(bB[3]);
    if (Math.abs(openA - openB) > 10 * ALX_Common.getPipValue(bA[0])) {
      return false;
    }

    double closeA = Double.parseDouble(bA[4]);
    double closeB = Double.parseDouble(bB[4]);
    if (Math.abs(closeA - closeB) > 10 * ALX_Common.getPipValue(bA[0])) {
      return false;
    }

    double highA = Double.parseDouble(bA[5]);
    double highB = Double.parseDouble(bB[5]);
    if (Math.abs(highA - highB) > 10 * ALX_Common.getPipValue(bA[0])) {
      return false;
    }

    double lowA = Double.parseDouble(bA[6]);
    double lowB = Double.parseDouble(bB[6]);
    if (Math.abs(lowA - lowB) > 10 * ALX_Common.getPipValue(bA[0])) {
      return false;
    }

    return true;
  }
}
