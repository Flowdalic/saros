package saros.net.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import org.apache.log4j.Logger;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;
import saros.annotations.Component;
import saros.net.ConnectionState;
import saros.net.DispatchThreadContext;
import saros.net.IPacketInterceptor;
import saros.net.IReceiver;
import saros.net.ITransferListener;
import saros.net.StanzaCollector;
import saros.net.StanzaCollector.CancelHook;
import saros.net.stream.StreamMode;
import saros.net.xmpp.IConnectionListener;
import saros.net.xmpp.XMPPConnectionService;

@Component(module = "net")
public class XMPPReceiver implements IReceiver, IBinaryXMPPExtensionReceiver {

  private static final Logger LOG = Logger.getLogger(XMPPReceiver.class);

  private static final int CHUNKSIZE = 16 * 1024;

  private final DispatchThreadContext dispatchThreadContext;

  private final Map<StanzaListener, StanzaFilter> listeners =
      Collections.synchronizedMap(new HashMap<StanzaListener, StanzaFilter>());

  private final CopyOnWriteArrayList<ITransferListener> transferListeners =
      new CopyOnWriteArrayList<>();

  private final CopyOnWriteArrayList<IPacketInterceptor> packetInterceptors =
      new CopyOnWriteArrayList<>();

  private XmlPullParser parser;

  private final StanzaListener smackStanzaListener =
      new StanzaListener() {

        @Override
        public void processStanza(Stanza packet) {
          XMPPReceiver.this.processStanza(packet);
        }
      };

  private final IConnectionListener connectionListener =
      new IConnectionListener() {

        @Override
        public void connectionStateChanged(XMPPConnection connection, ConnectionState state) {

          switch (state) {
            case CONNECTING:
              connection.addSyncStanzaListener(smackStanzaListener, null);
              // $FALL-THROUGH$
            case CONNECTED:
              break;
            default:
              if (connection != null) connection.removeSyncStanzaListener(smackStanzaListener);
          }
        }
      };

  public XMPPReceiver(
      DispatchThreadContext dispatchThreadContext,
      XMPPConnectionService connectionService,
      DataTransferManager dataTransferManager) {

    this.dispatchThreadContext = dispatchThreadContext;
    this.parser = new MXParser();

    connectionService.addListener(connectionListener);
    dataTransferManager.addConnectionListener(
        new IByteStreamConnectionListener() {
          @Override
          public void connectionChanged(
              final String connectionId,
              final IByteStreamConnection connection,
              final boolean incomingRequest) {
            connection.setBinaryXMPPExtensionReceiver(XMPPReceiver.this);
          }
        });
  }

  @Override
  public void addStanzaListener(StanzaListener listener, StanzaFilter filter) {
    listeners.put(listener, filter);
  }

  @Override
  public void removeStanzaListener(StanzaListener listener) {
    listeners.remove(listener);
  }

  @Override
  public void addTransferListener(final ITransferListener listener) {
    transferListeners.addIfAbsent(listener);
  }

  @Override
  public void removeTransferListener(final ITransferListener listener) {
    transferListeners.remove(listener);
  }

  @Override
  public void addPacketInterceptor(final IPacketInterceptor interceptor) {
    packetInterceptors.addIfAbsent(interceptor);
  }

  @Override
  public void removePacketInterceptor(final IPacketInterceptor interceptor) {
    packetInterceptors.remove(interceptor);
  }

  @Override
  public void processStanza(final Stanza packet) {
    dispatchThreadContext.executeAsDispatch(
        new Runnable() {
          @Override
          public void run() {
            forwardPacket(packet);
          }
        });
  }

  @Override
  public StanzaCollector createCollector(StanzaFilter filter) {
    final StanzaCollector collector =
        new StanzaCollector(
            new CancelHook() {
              @Override
              public void cancelStanzaCollector(StanzaCollector collector) {
                removeStanzaListener(collector);
              }
            },
            filter);
    addStanzaListener(collector, filter);

    return collector;
  }

  @Override
  public void receive(BinaryXMPPExtension extension) {
    dispatchThreadContext.executeAsDispatch(
        new Runnable() {

          @Override
          public void run() {

            Stanza packet = convertBinaryXMPPExtension(extension);

            if (packet != null) forwardPacket(packet);
          }
        });
  }

  /**
   * Dispatches the packet to all registered listeners.
   *
   * @sarosThread must be called from the Dispatch Thread
   */
  private void forwardPacket(Stanza packet) {
    Map<StanzaListener, StanzaFilter> copy;

    synchronized (listeners) {
      copy = new HashMap<StanzaListener, StanzaFilter>(listeners);
    }
    for (Entry<StanzaListener, StanzaFilter> entry : copy.entrySet()) {
      StanzaListener listener = entry.getKey();
      StanzaFilter filter = entry.getValue();

      if (filter == null || filter.accept(packet)) {
        try {
            listener.processStanza(packet);
        } catch (NotConnectedException | NotLoggedInException
            | InterruptedException e) {
            log.debug("processStanza() threw exception", e);
        }
      }
    }
  }

  /**
   * Deserializes the payload of an {@link BinaryXMPPExtension} back to its original {@link
   * ExtensionElement} and returns a new packet containing the deserialized packet extension.
   *
   * <p>This method is <b>not</b> thread safe and <b>must not</b> accessed by multiple threads
   * concurrently.
   */
  private Stanza convertBinaryXMPPExtension(BinaryXMPPExtension extension) {

    boolean dispatchPacket = true;

    for (IPacketInterceptor packetInterceptor : packetInterceptors)
      dispatchPacket &= packetInterceptor.receivedPacket(extension);

    if (!dispatchPacket) return null;

    if (LOG.isTraceEnabled())
      LOG.trace(
          "received binary XMPP extension: "
              + extension.getTransferDescription()
              + ", size: "
              + extension.getCompressedSize()
              + ", RX time: "
              + extension.getTransferDuration()
              + " ms ["
              + extension.getTransferMode()
              + "]");

    if (extension.getTransferDescription().compressContent()) {
      byte[] payload = extension.getPayload();
      long compressedPayloadLength = payload.length;

      try {
        payload = inflate(payload);
      } catch (IOException e) {
        LOG.error("could not decompress extension payload", e);
        return null;
      }

      extension.setPayload(compressedPayloadLength, payload);
    }

    notifyDataReceived(
        extension.getTransferMode(),
        extension.getCompressedSize(),
        extension.getUncompressedSize(),
        extension.getTransferDuration());

    TransferDescription description = extension.getTransferDescription();

    String name = description.getElementName();
    String namespace = description.getNamespace();
    // IQ provider?

    ExtensionElementProvider provider =
        (ExtensionElementProvider)
            ProviderManager.getInstance().getExtensionProvider(name, namespace);

    if (provider == null) {
      LOG.warn(
          "could not deserialize transfer object because no provider with namespace '"
              + namespace
              + "' and element name '"
              + name
              + "' is installed");
      return null;
    }

    ExtensionElement packetExtension = null;

    try {
      parser.setInput(new ByteArrayInputStream(extension.getPayload()), "UTF-8");
      /*
       * We have to skip the empty start tag because Smack expects a
       * parser that already has started parsing.
       */
      parser.next();
      packetExtension = provider.parseExtension(parser);
    } catch (Exception e) {
      LOG.error("could not deserialize transfer object payload: " + e.getMessage(), e);

      // just to be safe
      parser = new MXParser();
      return null;
    }

    Stanza packet = new Message();
    packet.setStanzaId(Stanza.ID_NOT_AVAILABLE);
    packet.setFrom(description.getSender().toString());
    packet.setTo(description.getRecipient().toString());
    packet.addExtension(packetExtension);

    return packet;
  }

  private void notifyDataReceived(
      final StreamMode mode,
      final long sizeCompressed,
      final long sizeUncompressed,
      final long duration) {

    for (final ITransferListener listener : transferListeners) {
      try {
        listener.received(mode, sizeCompressed, sizeUncompressed, duration);
      } catch (RuntimeException e) {
        LOG.error("invoking received() on listener: " + listener + " failed", e);
      }
    }
  }

  private static byte[] inflate(byte[] input) throws IOException {

    ByteArrayOutputStream bos;
    Inflater decompressor = new Inflater();

    decompressor.setInput(input, 0, input.length);
    bos = new ByteArrayOutputStream(input.length);

    byte[] buf = new byte[CHUNKSIZE];

    try {
      while (!decompressor.finished()) {
        int count = decompressor.inflate(buf);
        bos.write(buf, 0, count);
      }
      return bos.toByteArray();
    } catch (DataFormatException e) {
      throw new IOException("failed to inflate data", e);
    }
  }
}
