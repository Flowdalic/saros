package saros.communication.extensions;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Stanza;

public abstract class InvitationExtension extends SarosExtensionElement {

  @XStreamAlias("nid")
  @XStreamAsAttribute
  protected final String negotiationID;

  public InvitationExtension(String negotiationID) {
    this.negotiationID = negotiationID;
  }

  public String getNegotiationID() {
    return negotiationID;
  }

  public abstract static class Provider<T extends InvitationExtension>
      extends SarosExtensionElement.Provider<T> {

    public Provider(String elementName, Class<?>... classes) {
      super(elementName, classes);
    }

    public StanzaFilter getStanzaFilter(final String invitationID) {

      return new AndFilter(
          super.getStanzaFilter(),
          new StanzaFilter() {
            @Override
            public boolean accept(Stanza packet) {
              InvitationExtension extension = getPayload(packet);

              if (extension == null) return false;

              return invitationID.equals(extension.getNegotiationID());
            }
          });
    }
  }
}
