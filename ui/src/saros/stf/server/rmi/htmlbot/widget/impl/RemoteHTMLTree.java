package saros.stf.server.rmi.htmlbot.widget.impl;

import java.rmi.RemoteException;
import saros.stf.server.HTMLSTFRemoteObject;
import saros.stf.server.bot.jquery.ISelector.Selector;
import saros.stf.server.rmi.htmlbot.widget.IRemoteHTMLTree;

public final class RemoteHTMLTree extends HTMLSTFRemoteObject implements IRemoteHTMLTree {

  private static final RemoteHTMLTree INSTANCE = new RemoteHTMLTree();

  public static RemoteHTMLTree getInstance() {
    return INSTANCE;
  }

  @Override
  public void check(String title) throws RemoteException {
    if (isChecked(title) == false) {
      Selector nodeSelector = new Selector("span[title=\"" + title + "\"]");
      jQueryHelper.clickOnSelection(nodeSelector);
    }
  }

  @Override
  public void uncheck(String title) throws RemoteException {
    if (isChecked(title)) {
      Selector nodeSelector = new Selector("span[title=\"" + title + "\"]");
      jQueryHelper.clickOnSelection(nodeSelector);
    }
  }

  @Override
  public boolean isChecked(String title) throws RemoteException {
    Selector nodeSelector = new Selector("span[title=\"" + title + "\"]");
    Object checked =
        browser.evaluate(
            String.format(
                "return %s.prev().hasClass('rc-tree-checkbox-checked')",
                nodeSelector.getStatement()));
    return checked instanceof Boolean ? ((Boolean) checked).booleanValue() : false;
  }
}
