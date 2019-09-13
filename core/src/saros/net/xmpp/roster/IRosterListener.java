package saros.net.xmpp.roster;

import java.util.Collection;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterListener;
import org.jxmpp.jid.Jid;

public interface IRosterListener extends RosterListener {

    @Override
    public default void entriesAdded(Collection<Jid> addresses) {
        // NOP
    }

    @Override
    public default void entriesUpdated(Collection<Jid> addresses) {
        // NOP
    }

    @Override
    public default void entriesDeleted(Collection<Jid> addresses) {
        // NOP
    }

    @Override
    public default void presenceChanged(Presence presence) {
        // NOP
    }

    public default void rosterChanged(Roster roster) {
        // NOP
    }
}
