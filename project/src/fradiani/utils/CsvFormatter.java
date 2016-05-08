package fradiani.utils;

import fradiani.env.ATick;
import fradiani.strategies.ALX_Common;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Utility to process files obtained from TickDownloader software Convert to files needed by AlxFX TickServer.
 */
public class CsvFormatter {

  private static final String TICK_SOURCE = "C:\\TickDataDownloader\\tickdata\\";
  private static final String BASE_CSV = "E:\\shared\\BASE_CSV\\TICK_FACTORY_TEST\\";

  public CsvFormatter() {
  }

  public void process() {
    String pairs[] = ALX_Common.getTradeablePairs();
    for (String pair : pairs) {
      System.out.println("writing ticks for " + pair);
      String sourcepath = TICK_SOURCE + pair + "_tick.csv";
      try {
        BufferedReader reader = new BufferedReader(new FileReader(sourcepath));

        String line;
        String currpath = "";
        FileWriter writer = null;
        while ((line = reader.readLine()) != null) {
          String tickStr[] = line.split(",");
          String tickArr[] = ATick.getTimeAsStrings(ATick.getMillisFromCsv(tickStr[0]));

          //define the file where to store the ticks
          String savedir = BASE_CSV + pair + "\\" + tickArr[0];
          String savepath = savedir + "\\" + pair + "_" + tickArr[0] + "_" + tickArr[1] + "_" + tickArr[2] + ".csv";
          if (!currpath.equals(savepath) || writer == null) {
            System.out.println("setting destination file: " + savepath);

            //verify path existence
            File file = new File(savedir);
            if (!file.exists()) {
              file.mkdirs();
            }

            currpath = savepath;
            writer = new FileWriter(currpath, false /*append*/);
            writer.append("Tick,Bid,Ask");
          }

          writer.append('\n');
          writer.append(tickStr[0] + "," + tickStr[1] + "," + tickStr[2]);
          writer.flush();
        }

        if (writer != null) {
          writer.close();
        }
      } catch (IOException e) {
        System.err.println("TICK SOURCE INCOMPLETE. " + e);
        return;
      }
    }
  }

  public static void main(String args[]) {
    CsvFormatter formatter = new CsvFormatter();

    formatter.process();
    System.out.println("Done processing ticks!");
  }
}
