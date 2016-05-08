package fradiani.utils;

import java.io.Serializable;

class BinTick implements Serializable {

  long time;
  double bid;
  double ask;

  public BinTick(long _time, double _bid, double _ask) {
    this.time = _time;
    this.bid = _bid;
    this.ask = _ask;
  }
}
