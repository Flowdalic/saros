package saros.net;

import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Stanza;

public interface IReceiver {

  /**
   * @JTourBusStop 5, Architecture Overview, Network Layer - Receiver:
   *
   * <p>(...) And this Interface is the main entrance point for receiving them.
   *
   * <p>For a more detailed look on how Activities work see the "Activity sending"-Tour.
   */

  /**
   * Adds the given listener to the list of listeners notified when a new packet arrives.
   *
   * <p>Will only pass those packets to the listener that are accepted by the given filter or all
   * packets if no filter is given.
   *
   * @param listener The listener to pass packets to.
   * @param filter The filter to use when trying to identify packets that should be passed to the
   *     listener. If <code>null</code> all packets are passed to the listener.
   */
  public void addStanzaListener(StanzaListener listener, StanzaFilter filter);

  /**
   * Removes the given listener from the list of listeners.
   *
   * @param listener the listener to remove
   */
  public void removeStanzaListener(StanzaListener listener);

  /**
   * Dispatches the given packet to all registered packet listeners.
   *
   * @param packet the packet to dispatch
   */
  public void processStanza(Stanza packet);

  /**
   * Installs a {@linkplain StanzaCollector collector}. Use this method instead of {@link
   * #addStanzaListener} if the logic is using a polling mechanism.
   *
   * @param filter a filter that packets must match to be added to the collector.
   * @return a {@linkplain StanzaCollector collector} which <b>must</b> be canceled if it is no
   *     longer used
   * @see StanzaCollector#cancel()
   */
  public StanzaCollector createCollector(StanzaFilter filter);

  public default void addTransferListener(ITransferListener listener) {
    // NOP
  }

  public default void removeTransferListener(ITransferListener listener) {
    // NOP
  }

  public default void addPacketInterceptor(IPacketInterceptor interceptor) {
    // NOP
  }

  public default void removePacketInterceptor(IPacketInterceptor interceptor) {
    // NOP
  }
}
