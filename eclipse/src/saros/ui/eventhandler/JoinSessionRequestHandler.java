package saros.ui.eventhandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.preference.IPreferenceStore;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.packet.Stanza;
import saros.communication.extensions.JoinSessionRejectedExtension;
import saros.communication.extensions.JoinSessionRequestExtension;
import saros.net.IReceiver;
import saros.net.ITransmitter;
import saros.net.xmpp.JID;
import saros.preferences.EclipsePreferenceConstants;
import saros.session.ISarosSession;
import saros.session.ISarosSessionManager;
import saros.ui.util.CollaborationUtils;
import saros.ui.util.SWTUtils;

public final class JoinSessionRequestHandler {

  private static final Logger LOG = Logger.getLogger(JoinSessionRequestHandler.class);

  private final ISarosSessionManager sessionManager;

  private final ITransmitter transmitter;

  private final IReceiver receiver;

  private final IPreferenceStore preferenceStore;

  private final StanzaListener joinSessionRequestListener =
      new StanzaListener() {

        @Override
        public void processStanza(final Stanza packet) {
          SWTUtils.runSafeSWTAsync(
              LOG,
              new Runnable() {

                @Override
                public void run() {
                  handleInvitationRequest(
                      new JID(packet.getFrom()),
                      JoinSessionRequestExtension.PROVIDER.getPayload(packet));
                }
              });
        }
      };

  public JoinSessionRequestHandler(
      ISarosSessionManager sessionManager,
      ITransmitter transmitter,
      IReceiver receiver,
      IPreferenceStore preferenceStore) {
    this.sessionManager = sessionManager;
    this.transmitter = transmitter;
    this.receiver = receiver;
    this.preferenceStore = preferenceStore;

    if (Boolean.getBoolean("saros.server.SUPPORTED")) {
      this.receiver.addStanzaListener(
          joinSessionRequestListener, JoinSessionRequestExtension.PROVIDER.getStanzaFilter());
    }
  }

  private void handleInvitationRequest(JID from, JoinSessionRequestExtension extension) {

    ISarosSession session = sessionManager.getSession();

    if (session != null && !session.isHost()) return;

    if (!preferenceStore.getBoolean(EclipsePreferenceConstants.SERVER_ACTIVATED)) {
      sendRejection(from);
      return;
    }

    List<JID> list = Collections.singletonList(from);

    /*
     * Create a new session if none exists yet, add the user to the existing
     * session otherwise.
     */
    // TODO remove calls to CollaborationUtils
    if (session == null) {
      CollaborationUtils.startSession(new ArrayList<IResource>(), list);
    } else {
      CollaborationUtils.addContactsToSession(list);
    }
  }

  private void sendRejection(JID to) {
    transmitter.sendExtensionElement(
        to, JoinSessionRejectedExtension.PROVIDER.create(new JoinSessionRejectedExtension()));
  }
}
