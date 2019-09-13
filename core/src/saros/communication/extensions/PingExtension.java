package saros.communication.extensions;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("PING")
public class PingExtension extends SarosSessionExtensionElement {

  public static final Provider PROVIDER = new Provider();

  public PingExtension(String sessionID) {
    super(sessionID);
  }

  public static class Provider extends SarosSessionExtensionElement.Provider<PingExtension> {

    private Provider() {
      super("ping", PingExtension.class);
    }
  }
}
