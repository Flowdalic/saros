package saros.test.fakes.net;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Stanza;
import saros.net.IReceiver;
import saros.net.StanzaCollector;
import saros.net.StanzaCollector.CancelHook;

class NonThreadedReceiver implements IReceiver {

  private Map<StanzaListener, StanzaFilter> listeners =
      new ConcurrentHashMap<StanzaListener, StanzaFilter>();

  @Override
  public void addStanzaListener(StanzaListener listener, StanzaFilter filter) {
    listeners.put(listener, filter);
  }

  @Override
  public void removeStanzaListener(StanzaListener listener) {
    listeners.remove(listener);
  }

  @Override
  public void processStanza(Stanza packet) {
    for (Entry<StanzaListener, StanzaFilter> entry : listeners.entrySet()) {
      StanzaListener listener = entry.getKey();
      StanzaFilter filter = entry.getValue();

      if (filter == null || filter.accept(packet)) {
        listener.processStanza(packet);
      }
    }
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
}
