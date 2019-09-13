package saros.communication.extensions;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Stanza;

/**
 * @JTourBusStop 2, Creating custom network messages, Session Stanza Extensions:
 *
 * <p>As you see we also have another base class to inherit from if you want to use messages that
 * should only be processed during a running Saros session.
 */
public abstract class SarosSessionExtensionElement extends SarosExtensionElement {

  @XStreamAlias("sid")
  @XStreamAsAttribute
  protected final String sessionID;

  protected SarosSessionExtensionElement(String sessionID) {
    this.sessionID = sessionID;
  }

  public String getSessionID() {
    return sessionID;
  }

  public abstract static class Provider<T extends SarosSessionExtensionElement>
      extends SarosExtensionElement.Provider<T> {

    public Provider(String elementName, Class<?>... classes) {
      super(elementName, classes);
    }

    public StanzaFilter getStanzaFilter(final String sessionID) {

      return new AndFilter(
          super.getStanzaFilter(),
          new StanzaFilter() {
            @Override
            public boolean accept(Stanza packet) {
              SarosSessionExtensionElement extension = getPayload(packet);

              if (extension == null) return false;

              return sessionID.equals(extension.getSessionID());
            }
          });
    }
  }
}
