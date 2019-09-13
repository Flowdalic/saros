package saros.communication.chat.muc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.packet.ChatStateExtension;
import saros.communication.chat.IChatListener;
import saros.net.xmpp.JID;

/**
 * Handles the {@link ChatState} of a particular {@link MultiUserChat} and the propagation of
 * incoming {@link ChatState} changes. This class manages both the packet extensions and the
 * response necessary for compliance with XEP-0085.
 *
 * @author mariaspg
 * @author bkahlert
 */
class MultiUserChatStateManager {
  private static final Logger log = Logger.getLogger(MultiUserChatStateManager.class);
  protected static final Map<MultiUserChat, MultiUserChatStateManager> managers =
      new WeakHashMap<MultiUserChat, MultiUserChatStateManager>();

  /**
   * Official name for the chat states feature
   *
   * @see <a href="http://xmpp.org/registrar/namespaces.html">Jabber/XMPP Protocol Namespaces</a>
   * @see <a href="http://xmpp.org/extensions/xep-0085.html">XEP-0085: Chat State Notifications</a>
   */
  private static final String CHATSTATES_FEATURE = "http://jabber.org/protocol/chatstates";

  /**
   * Returns the {@link MultiUserChatStateManager} associated to the {@link MultiUserChat}. Creates
   * one if it does not yet exist.
   *
   * @param connection
   * @param muc
   * @return
   */
  public static MultiUserChatStateManager getInstance(
      final XMPPConnection connection, final MultiUserChat muc) {

    if (connection == null) {
      return null;
    }

    synchronized (managers) {
      MultiUserChatStateManager manager = managers.get(muc);
      if (manager == null) {
        manager = new MultiUserChatStateManager(connection, muc);
        managers.put(muc, manager);
      }
      return manager;
    }
  }

  protected XMPPConnection connection;
  protected MultiUserChat muc;
  protected ChatState lastState = null;
  protected List<IChatListener> stateListeners = new ArrayList<IChatListener>();

  /**
   * Checks every incoming {@link Message} for the {@link ExtensionElement} {@link
   * MultiUserChatStateManager#CHATSTATES_FEATURE} and notifies all {@link IChatListener}s in case
   * of a {@link ChatState} change.
   */
  protected StanzaListener incomingMessageInterceptor =
      new StanzaListener() {
        @Override
        public void processStanza(Stanza packet) {
          assert packet instanceof Message
              : "This interceptor is only intended to handle XMPP Messages";
          Message message = (Message) packet;
          ExtensionElement extension = message.getExtension(CHATSTATES_FEATURE);

          if (extension == null) {
            return;
          }

          ChatState state;
          try {
            state = ChatState.valueOf(extension.getElementName());
          } catch (Exception ex) {
            return;
          }

          log.debug(
              "Incoming Message from "
                  + message.getFrom()
                  + " with state: "
                  + message.getExtension("http://jabber.org/protocol/chatstates").getElementName());

          notifyMUCStateChanged(JID.createFromServicePerspective(message.getFrom()), state);
        }
      };

  protected MultiUserChatStateManager(XMPPConnection connection, MultiUserChat muc) {
    log.setLevel(Level.TRACE);

    this.connection = connection;
    this.muc = muc;

    // intercepting incoming messages
    this.muc.addMessageListener(incomingMessageInterceptor);

    ServiceDiscoveryManager.getInstanceFor(connection).addFeature(CHATSTATES_FEATURE);
  }

  /**
   * Sets the current state of the provided chat. This method will send an empty bodied Message
   * packet with the state attached as a {@link org.jivesoftware.smack.packet.ExtensionElement}, iff
   * the new chat state is different than the last state.
   *
   * @param newState the new state of the chat
   * @throws org.jivesoftware.smack.XMPPException when there is an error sending the message packet.
   */
  public void setState(ChatState newState) throws XMPPException {
    if (muc == null || newState == null) {
      throw new IllegalArgumentException("Arguments cannot be null.");
    }

    // last and new ChatState are the same
    if (this.lastState == newState) {
      return;
    }

    /*
     * Creates a Message with an empty body. A body is needed because the
     * Message.Type.groupchat requires one.
     */
    try {
      Message message = muc.createMessage();
      message.setBody("");
      ChatStateExtension extension = new ChatStateExtension(newState);
      message.addExtension(extension);
      muc.sendMessage(message);
    } catch (Exception e1) {
      log.debug("Couldn't send state (" + e1.getMessage() + ")");
    }

    this.lastState = newState;
  }

  /**
   * Adds a {@link IChatListener}
   *
   * @param stateListener
   */
  public void addMUCStateListener(IChatListener stateListener) {
    this.stateListeners.add(stateListener);
  }

  /**
   * Removes a {@link IChatListener}
   *
   * @param stateListener
   */
  public void removeMUCStateListener(IChatListener stateListener) {
    this.stateListeners.remove(stateListener);
  }

  /** Notify all {@link IChatListener}s about a changed chat status */
  public void notifyMUCStateChanged(JID sender, ChatState state) {
    for (IChatListener stateListener : this.stateListeners) {
      stateListener.stateChanged(sender, state);
    }
  }
}
