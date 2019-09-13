package saros.test.fakes.net;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Stanza;
import saros.net.IReceiver;
import saros.net.StanzaCollector;
import saros.net.StanzaCollector.CancelHook;
import saros.util.NamedThreadFactory;

public class ThreadedReceiver implements IReceiver {
  private Map<StanzaListener, StanzaFilter> listeners =
      new ConcurrentHashMap<StanzaListener, StanzaFilter>();

  private final ExecutorService executor =
      Executors.newSingleThreadExecutor(new NamedThreadFactory("ThreadedReceiver", false));

  public void stop() {
    executor.shutdown();
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
  public void processStanza(final Stanza packet) {
    for (Entry<StanzaListener, StanzaFilter> entry : listeners.entrySet()) {
      final StanzaListener listener = entry.getKey();
      final StanzaFilter filter = entry.getValue();

      if (filter == null || filter.accept(packet)) {
        listener.processStanza(packet);

        executor.submit(
            new Runnable() {

              @Override
              public void run() {
                try {
                  listener.processStanza(packet);
                } catch (Throwable t) {
                  t.printStackTrace();
                }
              }
            });
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
