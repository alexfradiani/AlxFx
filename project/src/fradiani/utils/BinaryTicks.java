package fradiani.utils;

import fradiani.env.ATick;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectOutputStream;

/**
 * Proof-of-concept. Optimize tick structure from text file to custom binary format
 */
public class BinaryTicks {

  public static final String CSV_FILE = "";
  public static final String BINARY_FILE = "";

  /**
   * Write test
   */
  public void writeTest() {
    try {
      FileOutputStream fout = new FileOutputStream("C:\\tmp\\binary_tick");
      ObjectOutputStream oos = new ObjectOutputStream(fout);
      //read the csv file
      try {
        BufferedReader r = new BufferedReader(new FileReader("c:\\tmp\\AUDCAD_2010_05_25.csv"));
        String line = r.readLine();
        while ((line = r.readLine()) != null) {
          if (line.equals("")) {
            continue;
          }

          ATick tick = new ATick("", line);
          BinTick btk = new BinTick(tick.time, tick.bid, tick.ask);
          oos.writeObject(btk);
        }
        oos.close();
        System.out.println("file written");
      } catch (Exception e) {
        System.err.println(e);
      }
    } catch (Exception ex) {
      System.err.println(ex);
    }
  }

  /**
   * Read test
   */
  public void readTest() {

  }

  public static void main(String[] args) {
    BinaryTicks bt = new BinaryTicks();
    bt.writeTest();
  }
}
