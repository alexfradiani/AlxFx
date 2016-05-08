package fradiani.env;

/**
 * Common interface to be implemented by all Strategies
 */
public interface AStrategy {
  //Environment initialization, setting variables

  public void onSettingEnv(TestingEnv environment);
  //Strategy started

  public void onStart();
  //every time a tick is obtained from the server

  public void onTick(ATick tick);
  //when an order action is executed. eg: order closed.

  public void onMessage(AOrder.OMsg message, AOrder order);
  //Strategy is ended

  public void onStop();
}
