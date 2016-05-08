package fradiani.env;

import fradiani.strategies.ALX_Common;
import java.util.ArrayList;
import java.util.List;

/**
 * General logger used to track events that will be presented in the strategy's report
 */
public class ALogger {

  public class LogEvent {

    String when;
    String type;
    String text;
  }

  private final List<LogEvent> log;

  public ALogger() {
    log = new ArrayList<LogEvent>();
  }

  public void write(long when, String type, String text) {
    LogEvent ev = new LogEvent();
    ev.when = ALX_Common.getReadableTime(when);
    ev.type = type;
    ev.text = text;

    log.add(ev);
  }

  public LogEvent[] getEvents() {
    LogEvent[] array = log.toArray(new LogEvent[log.size()]);

    return array;
  }
}
