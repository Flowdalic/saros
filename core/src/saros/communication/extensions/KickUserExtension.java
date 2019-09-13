package saros.communication.extensions;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias(/* SessionKickUser */ "SNKU")
public class KickUserExtension extends SarosSessionExtensionElement {

  public static final Provider PROVIDER = new Provider();

  public KickUserExtension(String sessionID) {
    super(sessionID);
  }

  public static class Provider extends SarosSessionExtensionElement.Provider<KickUserExtension> {
    private Provider() {
      super("snku", KickUserExtension.class);
    }
  }
}
