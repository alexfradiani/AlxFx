/**
 * Developed by Alexander Fradiani
 * designed to work with server-side script that return news from ff calendar
 *
 */
package fradiani.strategies.jv3;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import fradiani.strategies.ALX_Common;
import java.util.ArrayList;

/**
 * NEWS Reporter
 */
public class NReporter {
  //http and json main classes

  static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
  static final JsonFactory JSON_FACTORY = new JacksonFactory();

  public NReporter () {
    listeners = new ArrayList<>();
  }

  /**
   *
   * Interface to receive the news object
   */
  public interface CatchNews {

    public abstract void onNews(NewsEntry[] newsList);
  }

  /**
   * class for a news entry
   */
  public class NewsEntry {

    public String time; //hour:minute of the news (if hour-specific)
    public String currency; //currency affected by this news
    public String event; //description of the event
  }

  /**
   * URL for news request
   */
  public class NewsURL extends GenericUrl {

    public NewsURL(String encodedUrl) {
      super(encodedUrl);
    }
  }

  /**
   * List of listeners that will receive news asynchronously
   */
  public ArrayList<CatchNews> listeners;

  /**
   * Get the news of the parameters specified. From local server FF crawler
   *
   * @param day the day 1-31
   * @param month month 1-12
   * @param year year of the news
   * @return triggers CatchNews event for all listeners. sends NewsEntryList list of news for that date
   * @throws Exception
   */
  public void getNews(final int day, final int month, final int year) throws Exception {
    /**
     * NOTE!! for debugging purposes the method is now synchronous
     */

    /**
     * Asynchronous method to load news from server
     */
    class AsyncNewsLoad {

      public void run() {
        HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
          @Override
          public void initialize(HttpRequest request) {
            request.setParser(new JsonObjectParser(JSON_FACTORY));
          }
        });

        String date = day + "/" + month + "/" + year;
        NewsURL nurl = new NewsURL("http://" + ALX_Common.LOCAL_SERVER
          + "/stocks_scripts/panel/NewsCrawler/getFFCalendar/" + date);
        Boolean success = false;
        while (!success) {
          try {
            HttpRequest request = requestFactory.buildGetRequest(nurl);
            HttpResponse response = request.execute();
            String str = response.parseAsString();

            JsonElement json = new JsonParser().parse(str);
            JsonArray arr = json.getAsJsonObject().getAsJsonArray("news");
            NewsEntry[] loadedNews = new Gson().fromJson(arr, NewsEntry[].class);

            //FIRE EVENT with news
            for (CatchNews cn : listeners) {
              cn.onNews(loadedNews);
            }
            success = true;
          } catch (Exception e) {
            System.err.println("error loading news " + e.toString());
            success = false;
          }
        }
      }
    }

//        Thread t = new Thread(new AsyncNewsLoad());
//        t.start();
    AsyncNewsLoad nl = new AsyncNewsLoad();
    nl.run();
  }

  /**
   * Validate the time of a news entry return array with hh and mm or null if it's not a valid time. e.g:"Tentative" or other values
   */
  public static String[] validateNewsTime(NewsEntry n, NewsEntry[] currentNews) {
    String[] hhmm = null;

    if (n.time.contains(":")) //valid time field
    {
      hhmm = n.time.split(":");
    } else { //verify if it haves the same time of a previous news row
      String[] prevTime = null;
      for (NewsEntry _n : currentNews) {
        if (_n.time.contains(":")) {
          prevTime = _n.time.split(":");
        } else if (_n.event.equals(n.event)) {
          hhmm = prevTime;

          break;
        }
      }
    }

    return hhmm;
  }

  /**
   * main method for local testing
   */
  public static void main(String[] args) {
    System.out.println("testing news reporter...");

    class ReportCatcher implements CatchNews {

      @Override
      public void onNews(NewsEntry[] newsList) {
        for (NewsEntry n : newsList) {
          System.out.println("time: " + n.time);
          System.out.println("currency: " + n.currency);
          System.out.println("event: " + n.event);
        }
      }
    }

    try {
      NReporter nreporter = new NReporter();
      ReportCatcher catcher = new ReportCatcher();
      nreporter.listeners.add(catcher);
      nreporter.getNews(1, 11, 2015);

      System.out.println("loading news...");
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}
