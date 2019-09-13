package saros.communication.extensions;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("SSRq")
public class SessionStatusRequestExtension extends SarosExtensionElement {

  public static final Provider PROVIDER = new Provider();

  public static class Provider
      extends SarosExtensionElement.Provider<SessionStatusRequestExtension> {
    private Provider() {
      super("sessionStatusRequest", SessionStatusRequestExtension.class);
    }
  }
}
