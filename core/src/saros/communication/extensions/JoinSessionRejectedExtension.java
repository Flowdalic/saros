package saros.communication.extensions;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("JSRj")
public class JoinSessionRejectedExtension extends SarosExtensionElement {

  public static final Provider PROVIDER = new Provider();

  public static class Provider extends SarosExtensionElement.Provider<JoinSessionRejectedExtension> {

    private Provider() {
      super("joinRequestRejected", JoinSessionRejectedExtension.class);
    }
  }
}
