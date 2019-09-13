package saros.communication.extensions;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("PONG")
public class PongExtension extends SarosSessionExtensionElement {

  public static final Provider PROVIDER = new Provider();

  public PongExtension(String sessionID) {
    super(sessionID);
  }

  public static class Provider extends SarosSessionExtensionElement.Provider<PongExtension> {

    private Provider() {
      super("pong", PongExtension.class);
    }
  }
}
