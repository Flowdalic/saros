package saros.ui.model.roster;

import org.eclipse.core.runtime.IAdapterFactory;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import saros.net.xmpp.JID;

/**
 * Provides adapters for {@link Roster} entities which are provided by {@link
 * RosterContentProvider}.
 *
 * <p>E.g. let's you adapt {@link RosterGroupElement} to {@link RosterGroup} with
 *
 * <pre>
 * RosterGroup rosterGroup = (RosterGroup) rosterGroupElement.getAdapter(RosterGroup.class);
 *
 * if (rosterGroup != null)
 *     return true;
 * else
 *     return false;
 * </pre>
 *
 * <p><b>IMPORTANT:</b> If you update this class, please also update the extension <code>
 * org.eclipse.core.runtime.adapters</code> in <code>plugin.xml</code>!<br>
 * Eclipse needs to know which object can be adapted to which type.
 *
 * @author bkahlert
 */
public class RosterAdapterFactory implements IAdapterFactory {

  @Override
  public Class<?>[] getAdapterList() {
    return new Class[] {RosterGroup.class, RosterEntry.class, JID.class};
  }

  @Override
  public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
    if (adaptableObject instanceof RosterGroupElement) {
      if (adapterType == RosterGroup.class) {
        return adapterType.cast(((RosterGroupElement) adaptableObject).getRosterGroup().getClass());
      }
    }

    if (adaptableObject instanceof RosterEntryElement) {
      if (adapterType == RosterEntry.class)
        return adapterType.cast(((RosterEntryElement) adaptableObject).getRosterEntry());
      if (adapterType == JID.class)
        return adapterType.cast(((RosterEntryElement) adaptableObject).getJID());
    }

    return null;
  }
}
