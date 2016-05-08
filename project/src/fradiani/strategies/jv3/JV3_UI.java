package fradiani.strategies.jv3;

import fradiani.strategies.ALX_Common.Direction;
import fradiani.strategies.JV3_DMAStochs;
import fradiani.strategies.jv3.NReporter.NewsEntry;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

/**
 *
 * USER INTERFACE MANAGEMENT for ALX JV2
 */
public class JV3_UI extends JFrame {

  private JLabel header;  //header of the frame with current time & date OF THE RUNNING TEST
  private JTextPane newsPane; //text to write the news
  private JLabel correlatorLabel; //info of the correlator
  private JLabel accLabel; //info of the account ballance, lot size, etc

  public enum LABELSTYLES {
    NORMAL, ACTIVE_TRADE
  };

  /**
   * Class to group UI elements of the triggers
   */
  public class TriggerUI {

    private JLabel lblSmaLong; //label for the long SMA values
    private JLabel lblSmaLongDirection; //icon for UP or DOWN direction

    private JLabel lblSmaShort; //label for the short SMA values
    private JLabel lblSmaShortDirection; //icon for UP or DOWN direction

    private JLabel lblStochs; //label for the stochastics values
    private JLabel lblStochsDirection; //icon for UP or DOWN direction

    private JLabel lblTradeAction; //description of last trading action for that trigger

    private final java.awt.Color greenColor;
    private final java.awt.Color whiteColor;
    private final java.awt.Color redColor;
    private final java.awt.Color greyColor;
    private final java.awt.Color blackColor;

    /**
     * Constructor
     */
    public TriggerUI() {
      greenColor = new java.awt.Color(21, 217, 21);
      whiteColor = new java.awt.Color(255, 255, 255);
      redColor = new java.awt.Color(255, 35, 35);
      greyColor = new java.awt.Color(204, 204, 204);
      blackColor = new java.awt.Color(0, 0, 0);
    }

    /**
     * render indicator and fields for the trigger
     */
    public void render(TriggerManager trigger) {
      //SMA long-term values
      double sma[] = trigger.sma2Days;
      NumberFormat df = DecimalFormat.getInstance();
      df.setMinimumFractionDigits(5);
      df.setMaximumFractionDigits(5);
      lblSmaLong.setText("SMA 2D: " + df.format(sma[2]) + ", " + df.format(sma[1]) + ", " + df.format(sma[0]));

      //icon for long-term direction
      if (trigger.sma2DaysDirection == Direction.UP) {
        lblSmaLongDirection.setBackground(greenColor);
        lblSmaLongDirection.setForeground(whiteColor);
        lblSmaLongDirection.setText("UP");
      } else if (trigger.sma2DaysDirection == Direction.DOWN) {
        lblSmaLongDirection.setBackground(redColor);
        lblSmaLongDirection.setForeground(whiteColor);
        lblSmaLongDirection.setText("DN");
      } else {
        lblSmaLongDirection.setBackground(greyColor);
        lblSmaLongDirection.setForeground(blackColor);
        lblSmaLongDirection.setText("NN");
      }

      //SMA short-term values
      sma = trigger.sma4Hours;
      lblSmaShort.setText("SMA 4H: " + df.format(sma[2]) + ", " + df.format(sma[1]) + ", " + df.format(sma[0]));

      //icon for short-term direction
      if (trigger.sma4HoursDirection == Direction.UP) {
        lblSmaShortDirection.setBackground(greenColor);
        lblSmaShortDirection.setForeground(whiteColor);
        lblSmaShortDirection.setText("UP");
      } else if (trigger.sma4HoursDirection == Direction.DOWN) {
        lblSmaShortDirection.setBackground(redColor);
        lblSmaShortDirection.setForeground(whiteColor);
        lblSmaShortDirection.setText("DN");
      } else {
        lblSmaShortDirection.setBackground(greyColor);
        lblSmaShortDirection.setForeground(blackColor);
        lblSmaShortDirection.setText("NN");
      }

      //Stochastics value
      double stochs = trigger.stochs[0][0];
      df.setMinimumIntegerDigits(2);
      df.setMinimumFractionDigits(1);
      df.setMaximumFractionDigits(1);
      lblStochs.setText("Stochs: " + df.format(stochs));

      //stochastic direction icon
      if (trigger.stochsDirection == Direction.UP) {
        lblStochsDirection.setBackground(greenColor);
        lblStochsDirection.setForeground(whiteColor);
        lblStochsDirection.setText("UP");
      } else if (trigger.stochsDirection == Direction.DOWN) {
        lblStochsDirection.setBackground(redColor);
        lblStochsDirection.setForeground(whiteColor);
        lblStochsDirection.setText("DN");
      } else {
        lblStochsDirection.setBackground(greyColor);
        lblStochsDirection.setForeground(blackColor);
        lblStochsDirection.setText("NN");
      }
    }

    /**
     * Render text for the trade action field
     */
    public void renderTradeAction(String msg) {
      //check style
      lblTradeAction.setOpaque(false);
      lblTradeAction.setForeground(blackColor);

      lblTradeAction.setText(msg);
    }

    /**
     * Render text for the trade action field with a specific style
     */
    public void renderTradeAction(String msg, LABELSTYLES style) {
      if (style == LABELSTYLES.ACTIVE_TRADE) {
        lblTradeAction.setOpaque(true);
        lblTradeAction.setBackground(Color.yellow);
        lblTradeAction.setForeground(redColor);
        lblTradeAction.setText(msg + " ACTIVE");
      }
    }
  }

  /**
   * Constructor
   */
  public JV3_UI(JV3_DMAStochs _strategy, String name, Set<TriggerManager> triggers) {
    super(name);

    initComponents(triggers);
    this.renderAccInfo(_strategy.env.getBallance());
  }

  /**
   * Alternative Constructor, ONLY for testing purposes
   */
  public JV3_UI(String name, Set<TriggerManager> triggers) {
    super(name);

    initComponents(triggers);
  }

  /**
   * Initialize UI elements
   */
  private void initComponents(Set<TriggerManager> triggers) {
    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

    this.getContentPane().setLayout(new BoxLayout((this.getContentPane()), BoxLayout.Y_AXIS));
    FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);

    //header with date & time label
    JPanel headerPanel = new JPanel();
    headerPanel.setLayout(flowLayout);
    JLabel lblHeader = new JLabel();
    lblHeader.setText("Tracked time: ");
    headerPanel.add(lblHeader);
    this.getContentPane().add(headerPanel);
    header = lblHeader;

    //container and layout
    JPanel triggersPanel = new JPanel();
    triggersPanel.setLayout(new GridLayout(0, 2));

    this.getContentPane().add(triggersPanel);
    //Create a TriggerUI for each of the triggers
    for (TriggerManager trigger : triggers) {
      JPanel triggerContainer = new JPanel();
      triggerContainer.setLayout(new BoxLayout(triggerContainer, BoxLayout.Y_AXIS));
      triggerContainer.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
      TriggerUI ui = new TriggerUI();

      JPanel panel = new JPanel();
      panel.setLayout(flowLayout);

      //"tracked pair" text
      JLabel lbl = new JLabel();
      lbl.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 11));
      lbl.setText("Pair:");
      panel.add(lbl);

      //Pair symbol
      lbl = new JLabel();
      lbl.setFont(new java.awt.Font("Tahoma", Font.BOLD, 11));
      lbl.setText(trigger.trackedPair);
      panel.add(lbl);

      //label for the SMA long-term
      lbl = new JLabel();
      lbl.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 11));
      lbl.setText("SMA 2D: 0.0, 0.0, 0.0");
      ui.lblSmaLong = lbl;
      panel.add(lbl);

      //UP or DOWN indicator for SMA long-term
      lbl = new JLabel();
      lbl.setFont(new java.awt.Font("Tahoma", Font.BOLD, 11));
      lbl.setBackground(new java.awt.Color(204, 204, 204)); //GRAY
      lbl.setOpaque(true);
      lbl.setText("NN");
      ui.lblSmaLongDirection = lbl;
      panel.add(lbl);

      //label for the SMA short-term
      lbl = new JLabel();
      lbl.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 11));
      lbl.setText("SMA 4H: 0.0, 0.0, 0.0");
      ui.lblSmaShort = lbl;
      panel.add(lbl);

      //UP or DOWN indicator for SMA short-term
      lbl = new JLabel();
      lbl.setFont(new java.awt.Font("Tahoma", Font.BOLD, 11));
      lbl.setBackground(new java.awt.Color(204, 204, 204)); //GRAY
      lbl.setOpaque(true);
      lbl.setText("NN");
      ui.lblSmaShortDirection = lbl;
      panel.add(lbl);

      //label for the stochastics value
      lbl = new JLabel();
      lbl.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 11));
      lbl.setText("Stochs: 0.0");
      ui.lblStochs = lbl;
      panel.add(lbl);

      //UP or DOWN indicator for STOCHASTICS
      lbl = new JLabel();
      lbl.setFont(new java.awt.Font("Tahoma", Font.BOLD, 11));
      lbl.setBackground(new java.awt.Color(204, 204, 204)); //GRAY
      lbl.setOpaque(true);
      lbl.setText("NN");
      ui.lblStochsDirection = lbl;
      panel.add(lbl);

      //create a new line
      triggerContainer.add(panel);
      panel = new JPanel();
      panel.setLayout(flowLayout);

      //label for trading action reporting
      lbl = new JLabel();
      lbl.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 11));
      lbl.setForeground(new java.awt.Color(128, 127, 127));
      lbl.setText("Last trade action: -");
      ui.lblTradeAction = lbl;
      panel.add(lbl);

      triggerContainer.add(panel);
      triggersPanel.add(triggerContainer);
      trigger.ui = ui;  //assign the created ui to the trigger
    }

    //News text pane
    JTextPane txtPane = new JTextPane();
    txtPane.setEditable(false);
    txtPane.setContentType("text/html");
    txtPane.setText("<html><b>News being monitored this day:</b>");
    JScrollPane scroll = new JScrollPane(txtPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    this.getContentPane().add(scroll);
    newsPane = txtPane;

    //correlator text
    JPanel correlatorPanel = new JPanel();
    correlatorPanel.setLayout(flowLayout);
    JLabel lbl = new JLabel();
    correlatorPanel.add(lbl);
    this.getContentPane().add(correlatorPanel);
    this.correlatorLabel = lbl;

    //Account stats
    JPanel accPanel = new JPanel();
    accPanel.setLayout(flowLayout);
    lbl = new JLabel();
    accPanel.add(lbl);
    this.getContentPane().add(accPanel);
    this.accLabel = lbl;

    pack();
  }

  /**
   * header line with current date and time being tracked
   */
  public void renderHeader(long time) {
    Date now = new Date(time);

    SimpleDateFormat df = new SimpleDateFormat("E yyyy.MM.dd 'at' HH:mm:ss");
    df.setTimeZone(TimeZone.getTimeZone("GMT"));
    this.header.setText("Tracked time: " + df.format(now));
  }

  /**
   * update values of news text
   */
  public void renderNews(NewsEntry[] newsList) {
    String txt = "<html><b>News being monitored this day:</b><br/>";
    for (NewsEntry n : newsList) {
      String hhmm[] = NReporter.validateNewsTime(n, newsList);
      String str_hhmm = "";
      if (hhmm != null) {
        str_hhmm = hhmm[0] + ":" + hhmm[1];
      }
      txt += "<b>" + str_hhmm + " - " + n.currency + ": </b> " + n.event + "<br/>";
    }

    newsPane.setText(txt);
  }

  /**
   * correlator file last update if needs update change label color
   */
  public void renderCorrelator(Correlator correlator, Boolean isOutdated) throws Exception {
    String crrlText = "Correlator file: " + correlator.getLastUpdate();

    if (isOutdated) {
      correlatorLabel.setOpaque(true);
      correlatorLabel.setBackground(Color.YELLOW);
      correlatorLabel.setForeground(Color.RED);
      crrlText += "- UPDATE NEEDED";
    } else {
      correlatorLabel.setOpaque(false);
      correlatorLabel.setForeground(Color.BLACK);
    }

    correlatorLabel.setText(crrlText);
  }

  /**
   * render account label for account info
   */
  public final void renderAccInfo(double ballance) {
    accLabel.setText("Acc Equity: " + ballance);
  }
}
