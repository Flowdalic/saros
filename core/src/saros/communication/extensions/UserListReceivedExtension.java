package saros.communication.extensions;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias(/* UserListStatusUpdateProcessed */ "ULSUPS")
public class UserListReceivedExtension extends SarosSessionExtensionElement {

  public static final Provider PROVIDER = new Provider();

  public UserListReceivedExtension(String sessionID) {
    super(sessionID);
  }

  public static class Provider
      extends SarosSessionExtensionElement.Provider<UserListReceivedExtension> {
    private Provider() {
      super("ulsups", UserListReceivedExtension.class);
    }
  }
}
