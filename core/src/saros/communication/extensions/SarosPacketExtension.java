package saros.communication.extensions;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Stanza;
import saros.misc.xstream.XStreamExtensionProvider;

/**
 * @JTourBusStop 1, Creating custom network messages, Stanza Extensions:
 *
 * <p>We have seen many attempts in the past where developers tried to accomplish things with the
 * existing Saros API which could not be solved because the logic behind the API was lacking of
 * information that was either never present or was available too late.
 *
 * <p>This tour explains how to create custom messages that can be used to exchange needed
 * information.
 *
 * <p>Saros uses XMPP packet extensions (data represented in XML) to exchange its data as messages.
 * This class is the base class to inherit from when creating a new packet extension.
 */
public abstract class SarosExtensionElement {

  // keep this short as it is included in every packet extension !
  public static final String VERSION = "SPXV1";

  public static final String EXTENSION_NAMESPACE = "saros";

  @XStreamAlias("v")
  @XStreamAsAttribute
  private final String version = VERSION;

  public abstract static class Provider<T extends SarosExtensionElement>
      extends XStreamExtensionProvider<T> {

    public Provider(String elementName, Class<?>... classes) {
      super(EXTENSION_NAMESPACE, elementName, classes);
    }

    @Override
    public StanzaFilter getStanzaFilter() {

      return new AndFilter(
          super.getStanzaFilter(),
          new StanzaFilter() {
            @Override
            public boolean accept(Stanza packet) {
              SarosExtensionElement extension = getPayload(packet);

              return extension != null && VERSION.equals(extension.version);
            }
          });
    }
  }
}
