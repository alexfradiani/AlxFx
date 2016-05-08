package fradiani.utils;

import fradiani.env.AOrder;
import fradiani.env.AStrategy;
import fradiani.env.ATick;
import fradiani.env.TestingEnv;
import fradiani.strategies.ALX_Common;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a strategy to compare the ticks taken from AlxFX with the ticks previously read from JForex
 */
public class TickDbgStrategy implements AStrategy {

  private TestingEnv env;
  private BufferedReader reader;
  private List<String> m_jforex;
  private List<String> m_alxfx;

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
    //open the log file to compare ticks
    try {
      reader = new BufferedReader(new FileReader("/Users/alx/AlxFx/tmp/tickslog_2012_12_28_to_2013_01_03"));
    } catch (Exception e) {
      System.err.println(e);
    }

    m_alxfx = new ArrayList<>();
    m_jforex = new ArrayList<>();
  }

  @Override
  public void onTick(ATick tick) {
    String[] tstr = ATick.getTimeAsStrings(tick.time);
    String tlstr = tstr[0] + "." + tstr[1] + "." + tstr[2] + " " + tstr[3] + ":" + tstr[4] + ":" + tstr[5] + "." + tstr[6];
    String compare = tick.pair + "," + tlstr + "," + ALX_Common.normalize(tick.bid, ALX_Common.getPipScale(tick.pair))
      + "," + ALX_Common.normalize(tick.ask, ALX_Common.getPipScale(tick.pair));

    try {
      String line = reader.readLine();

      compareTicks(line, compare);
    } catch (Exception e) {
      System.err.println(e);
    }

//        if(tick.pair.equals("EURNZD")) {
//            String[] tstr = ATick.getTimeAsStrings(tick.time);
//            String tlstr = tstr[0] + "." + tstr[1] + "." + tstr[2] + " " + tstr[3] + ":" + tstr[4] + ":" + tstr[5] + "." + tstr[6];
//            String compare = tick.pair + "," + tlstr + "," + ALX_Common.normalize(tick.bid, ALX_Common.getPipScale(tick.pair)) + 
//                "," + ALX_Common.normalize(tick.ask, ALX_Common.getPipScale(tick.pair));
//            
//            System.out.println(compare);
//        }
  }

  /**
   * compare ticks between sources
   */
  private void compareTicks(String fromjforex, String fromalxfx) {
    Boolean alxfxMatched = false;
    Boolean jforexMatched = false;

    //compare both new lines
    if (fromjforex != null && fromalxfx != null) {
      if (fromjforex.equals(fromalxfx)) {
        return;
      }
    }

    //compare new jforex line with older alxfx lines
    if (fromjforex != null) {
      if (m_alxfx.size() > 0) {
        for (int i = 0; i < m_alxfx.size(); i++) {
          if (m_alxfx.get(i).equals(fromjforex)) {  //new jforex line matchs and older alxfx line
            m_alxfx.remove(i);
            jforexMatched = true;
          }
        }
      }
    }

    //compare new alxfx line with older jforex lines
    if (fromalxfx != null) {
      if (m_jforex.size() > 0) {
        for (int i = 0; i < m_jforex.size(); i++) {
          if (m_jforex.get(i).equals(fromalxfx)) {  //new alxfx line matchs and older jforex line
            m_jforex.remove(i);
            alxfxMatched = true;
          }
        }
      }
    }

    if (alxfxMatched == false && fromalxfx != null) {
      m_alxfx.add(fromalxfx);
    }
    if (jforexMatched == false && fromjforex != null) {
      m_jforex.add(fromjforex);
    }

    if (m_alxfx.size() > 50 || m_jforex.size() > 50) //limit of inconsistencies
    {
      env.abort();
    }
  }

  @Override
  public void onMessage(AOrder.OMsg message, AOrder order) {
    //TODO...
  }

  @Override
  public void onStop() {
    System.out.println("Done comparing");
    System.out.println("AlxFx mismatchs: " + m_alxfx.size() + " JForex mismatchs: " + m_jforex.size());

    System.out.println("mismatched jforex ticks: ");
    for (int i = 0; i < m_jforex.size(); i++) {
      System.out.println(m_jforex.get(i));
    }

    System.out.println("mismatched alxfx ticks: ");
    for (int i = 0; i < m_alxfx.size(); i++) {
      System.out.println(m_alxfx.get(i));
    }

    m_alxfx.clear();
    m_jforex.clear();
  }
}
