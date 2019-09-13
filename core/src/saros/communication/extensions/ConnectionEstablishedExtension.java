package saros.communication.extensions;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias(/* XMPPConnectionEstablishedExtension */ "COES")
public class XMPPConnectionEstablishedExtension extends InvitationExtension {

  public static final Provider PROVIDER = new Provider();

  public XMPPConnectionEstablishedExtension(String invitationID) {
    super(invitationID);
  }

  public static class Provider
      extends InvitationExtension.Provider<ConnectionEstablishedExtension> {
    private Provider() {
      super("coes", XMPPConnectionEstablishedExtension.class);
    }
  }
}
