package saros.stf.server;

import saros.context.IContainerContext;
import saros.stf.server.bot.jquery.ISelector;
import saros.stf.server.bot.jquery.JQueryHelper;
import saros.ui.browser.IBrowser;
import saros.ui.manager.BrowserManager;

/**
 * This is the base class for all remote object of the HTML GUI test framework. It contains methods
 * to acquire components from the {@link IContainerContext}
 */
public abstract class HTMLSTFRemoteObject {

  private static IContainerContext context;

  protected JQueryHelper jQueryHelper;
  protected IBrowser browser;
  protected ISelector selector;

  /**
   * This method must be called at the start of the test framework in order for future lookups to
   * succeed.
   *
   * @param context the Saros context to use for the GUI tests
   */
  public static void setContext(IContainerContext context) {
    HTMLSTFRemoteObject.context = context;
  }

  protected IContainerContext getContext() {
    return context;
  }

  /**
   * @return the {@link BrowserManager} which is required to get a certain browser and execute test
   *     code in it
   */
  protected BrowserManager getBrowserManager() {
    return getContext().getComponent(BrowserManager.class);
  }

  public void setBrowser(IBrowser browser) {
    this.browser = browser;
    this.jQueryHelper = new JQueryHelper(browser);
  }

  public void setSelector(ISelector selector) {
    this.selector = selector;
  }
}
