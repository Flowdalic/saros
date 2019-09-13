package saros.communication.extensions;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("JSRq")
public class JoinSessionRequestExtension extends SarosExtensionElement {

  public static final Provider PROVIDER = new Provider();

  public static class Provider extends SarosExtensionElement.Provider<JoinSessionRequestExtension> {

    private Provider() {
      super("joinSessionRequest", JoinSessionRequestExtension.class);
    }
  }
}
