package saros.ui.renderer;

import java.util.ArrayList;
import java.util.List;
import saros.HTMLUIContextFactory;
import saros.account.IAccountStoreListener;
import saros.account.XMPPAccount;
import saros.account.XMPPAccountStore;
import saros.ui.JavaScriptAPI;
import saros.ui.browser.IBrowser;

/** This class is responsible for sending the account list to the HTML UI. */
public class AccountRenderer extends Renderer {

  private List<XMPPAccount> accounts;

  /**
   * Created by PicoContainer
   *
   * @param accountStore
   * @see HTMLUIContextFactory
   */
  public AccountRenderer(XMPPAccountStore accountStore) {
    this.accounts = new ArrayList<XMPPAccount>();

    accountStore.addListener(
        new IAccountStoreListener() {
          @Override
          public void accountsChanged(List<XMPPAccount> currentAccounts) {
            update(currentAccounts);
            render();
          }
        });
  }

  private synchronized void update(List<XMPPAccount> allAccounts) {
    accounts = new ArrayList<XMPPAccount>(allAccounts);
  }

  @Override
  public synchronized void render(IBrowser browser) {
    JavaScriptAPI.updateAccounts(browser, accounts);
  }
}
