package saros.session;

import java.util.List;
import org.apache.log4j.Logger;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.ExtensionElement;
import saros.communication.extensions.CancelInviteExtension;
import saros.communication.extensions.CancelProjectNegotiationExtension;
import saros.communication.extensions.InvitationAcknowledgedExtension;
import saros.communication.extensions.InvitationOfferingExtension;
import saros.communication.extensions.ProjectNegotiationOfferingExtension;
import saros.negotiation.ProjectNegotiation;
import saros.negotiation.ProjectNegotiationData;
import saros.negotiation.SessionNegotiation;
import saros.net.IReceiver;
import saros.net.ITransmitter;
import saros.net.xmpp.JID;

/**
 * This class is responsible for receiving, handling, and/or forwarding specific network messages
 * (packets) to the Saros session {@linkplain SarosSessionManager manager}.
 *
 * <p><b>Restriction:</b> This class must only instantiated by the <code>SarosSessionManager</code>
 * itself.
 */
final class NegotiationStanzaListener {

  private static final Logger LOG = Logger.getLogger(NegotiationStanzaListener.class);

  private final ITransmitter transmitter;
  private final IReceiver receiver;

  private final SarosSessionManager sessionManager;

  private final SessionNegotiationObservable sessionNegotiations;
  private final ProjectNegotiationObservable projectNegotiations;

  private boolean rejectSessionNegotiationRequests;

  // TODO maybe this should be controlled by the SessionManager itself
  private final ISessionLifecycleListener sessionLifecycleListener =
      new ISessionLifecycleListener() {

        @Override
        public void sessionStarted(final ISarosSession session) {
          receiver.addStanzaListener(
              projectNegotiationRequestListener,
              ProjectNegotiationOfferingExtension.PROVIDER.getStanzaFilter(session.getID()));

          receiver.addStanzaListener(
              projectNegotiationCanceledListener,
              CancelProjectNegotiationExtension.PROVIDER.getStanzaFilter(session.getID()));
        }

        @Override
        public void sessionEnded(ISarosSession session, SessionEndReason reason) {
          receiver.removeStanzaListener(projectNegotiationRequestListener);
          receiver.removeStanzaListener(projectNegotiationCanceledListener);
        }
      };

  /*
   * ******************** Stanza Listeners START ************************
   */
  private final StanzaListener sessionNegotiationCanceledListener =
      new StanzaListener() {

        @Override
        public void processStanza(final Stanza packet) {

          final CancelInviteExtension extension = CancelInviteExtension.PROVIDER.getPayload(packet);

          if (extension == null) {
            LOG.warn("received malformed session negotiation packet from " + packet.getFrom());
            return;
          }

          sessionNegotiationCanceled(
              new JID(packet.getFrom()), extension.getNegotiationID(), extension.getErrorMessage());
        }
      };

  private final StanzaListener sessionNegotiationRequestListener =
      new StanzaListener() {

        @Override
        public void processStanza(final Stanza packet) {

          final InvitationOfferingExtension extension =
              InvitationOfferingExtension.PROVIDER.getPayload(packet);

          if (extension == null) {
            LOG.warn("received malformed session negotiation packet from " + packet.getFrom());
            return;
          }

          sessionNegotiationRequest(
              new JID(packet.getFrom()),
              extension.getNegotiationID(),
              extension.getVersion(),
              extension.getSessionID(),
              extension.getDescription());
        }
      };

  private final StanzaListener projectNegotiationCanceledListener =
      new StanzaListener() {

        @Override
        public void processStanza(Stanza packet) {

          final CancelProjectNegotiationExtension extension =
              CancelProjectNegotiationExtension.PROVIDER.getPayload(packet);

          if (extension == null) {
            LOG.warn("received malformed project negotiation packet from " + packet.getFrom());
            return;
          }

          projectNegotiationCanceled(
              new JID(packet.getFrom()), extension.getNegotiationID(), extension.getErrorMessage());
        }
      };

  private final StanzaListener projectNegotiationRequestListener =
      new StanzaListener() {

        @Override
        public void processStanza(final Stanza packet) {

          final ProjectNegotiationOfferingExtension extension =
              ProjectNegotiationOfferingExtension.PROVIDER.getPayload(packet);

          if (extension == null) {
            LOG.warn("received malformed project negotiation packet from " + packet.getFrom());
            return;
          }

          projectNegotiationRequest(
              new JID(packet.getFrom()),
              extension.getNegotiationID(),
              extension.getProjectNegotiationData());
        }
      };

  /*
   * ******************** Stanza Listeners END*******************************
   */

  NegotiationStanzaListener(
      final SarosSessionManager sessionManager,
      final SessionNegotiationObservable sessionNegotiations,
      final ProjectNegotiationObservable projectNegotiations,
      final ITransmitter transmitter,
      final IReceiver receiver) {
    this.sessionManager = sessionManager;

    this.sessionNegotiations = sessionNegotiations;
    this.projectNegotiations = projectNegotiations;
    this.transmitter = transmitter;
    this.receiver = receiver;

    init();
  }

  /**
   * Allows to reject incoming session negotiation requests.
   *
   * @param reject <code>true</code> if requests should be rejected, <code>false</code> otherwise
   */
  void setRejectSessionNegotiationRequests(final boolean reject) {
    rejectSessionNegotiationRequests = reject;
  }

  /**
   * Determines if incoming session negotiations requests are currently rejected.
   *
   * @return <code>true</code> if requests are rejected, <code>false</code> otherwise
   */
  boolean isRejectingSessionNegotiationsRequests() {
    return rejectSessionNegotiationRequests;
  }

  private void init() {
    receiver.addStanzaListener(
        sessionNegotiationCanceledListener, CancelInviteExtension.PROVIDER.getStanzaFilter());

    receiver.addStanzaListener(
        sessionNegotiationRequestListener, InvitationOfferingExtension.PROVIDER.getStanzaFilter());

    sessionManager.addSessionLifecycleListener(sessionLifecycleListener);
  }

  private void sessionNegotiationCanceled(
      final JID sender, final String sessionNegotiationID, final String errorMessage) {

    final SessionNegotiation negotiation = sessionNegotiations.get(sender, sessionNegotiationID);

    if (negotiation == null) {
      LOG.warn(
          "received session negotiation cancel from "
              + sender
              + " for a nonexisting instance with id: "
              + sessionNegotiationID);
      return;
    }

    LOG.debug(
        sender
            + " canceled session negotiation [id="
            + sessionNegotiationID
            + ", reason="
            + errorMessage
            + "]");

    negotiation.remoteCancel(errorMessage);
  }

  private void sessionNegotiationRequest(
      final JID sender,
      final String negotiationID,
      final String remoteVersion,
      final String sessionID,
      final String description) {

    LOG.info(
        "received invitation from "
            + sender
            + " [negotiation id: "
            + negotiationID
            + ", "
            + "session id: "
            + sessionID
            + ", "
            + "version: "
            + remoteVersion
            + "]");

    if (rejectSessionNegotiationRequests) {
      LOG.info("rejecting session negotiation request with id: " + negotiationID);

      /*
       * FIXME This text should be replaced with a cancel ID. This is GUI
       * logic here.
       */
      final ExtensionElement response =
          CancelInviteExtension.PROVIDER.create(
              new CancelInviteExtension(
                  negotiationID,
                  "I am already in a Saros session and so cannot accept your invitation."));

      transmitter.sendExtensionElement(sender, response);
      return;
    }

    /* *
     *
     * @JTourBusStop 6, Invitation Process:
     *
     * (3b) If the invited user (from now on referred to as "client")
     * receives an invitation (and if he is not already in a running
     * session), Saros will send an automatic response to the inviter
     * (host). Afterwards, the control is handed over to the SessionManager.
     */

    final ExtensionElement response =
        InvitationAcknowledgedExtension.PROVIDER.create(
            new InvitationAcknowledgedExtension(negotiationID));

    transmitter.sendExtensionElement(sender, response);

    /*
     * SessionManager will set rejectSessionNegotiationRequests to true in
     * this call
     */
    sessionManager.sessionNegotiationRequestReceived(
        sender, sessionID, negotiationID, remoteVersion, description);
  }

  private void projectNegotiationCanceled(
      final JID sender, final String negotiationID, final String errorMessage) {

    final ProjectNegotiation negotiation = projectNegotiations.get(sender, negotiationID);

    if (negotiation != null) {
      LOG.debug(
          sender
              + " canceled project negotiation [id="
              + negotiationID
              + ", reason="
              + errorMessage
              + "]");

      negotiation.remoteCancel(errorMessage);
    } else {
      LOG.warn(
          "received project negotiation cancel from "
              + sender
              + " for a nonexisting instance with id: "
              + negotiationID);
    }
  }

  private void projectNegotiationRequest(
      final JID sender,
      final String negotiationID,
      final List<ProjectNegotiationData> projectNegotiationData) {

    LOG.info(
        "received project negotiation from " + sender + " with negotiation id: " + negotiationID);

    sessionManager.projectNegotiationRequestReceived(sender, projectNegotiationData, negotiationID);
  }
}
