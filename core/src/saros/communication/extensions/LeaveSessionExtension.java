package saros.communication.extensions;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias(/* SessionLeave */ "SNLV")
public class LeaveSessionExtension extends SarosSessionExtensionElement {

  public static final Provider PROVIDER = new Provider();

  public LeaveSessionExtension(String sessionID) {
    super(sessionID);
  }

  public static class Provider extends SarosSessionExtensionElement.Provider<LeaveSessionExtension> {
    private Provider() {
      super("snlv", LeaveSessionExtension.class);
    }
  }
}
