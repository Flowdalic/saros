/*
 * DPP - Serious Distributed Pair Programming
 * (c) Freie Universität Berlin - Fachbereich Mathematik und Informatik - 2006
 * (c) Riad Djemili - 2006
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 1, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package saros.net.xmpp;

import java.io.Serializable;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;
import org.jxmpp.util.XmppStringUtils;
import saros.net.util.XMPPUtils;

/**
 * A JID which is used to identify the users of the XMPP network.
 *
 * @valueObject A JID is a value object, i.e. it is immutable!
 * @author rdjemili
 */
public class JID implements Serializable {

    private static final long serialVersionUID = 4830741516870940459L;

    private final String jid;

    /**
     * Creates the client {@link JID} on the base of a service perspective
     * {@link JID} as explained in XEP-0045.
     *
     * <p>
     * Example: A {@link MultiUserChat} participant has - from the perspective
     * of the {@link MultiUserChat} itself - the JID
     * <b>saros128280129@conference.jabber.ccc.de/bkahlert@jabber.org/Saros</b>.
     * This method would return the {@link JID} representing
     * <b>bkahlert@jabber.org/Saros</b>.
     *
     * @see <a href="http://xmpp.org/extensions/xep-0045.html#user">XEP-0045</a>
     * @param servicePerspectiveJID
     *            the XMPP address from the perspective of the service
     * @return the client JID portion
     */
    public static JID createFromServicePerspective(
        CharSequence servicePerspectiveJID) {
        return new JID(
            XmppStringUtils.parseResource(servicePerspectiveJID.toString()));
    }

    /**
     * Construct a new JID
     *
     * @param jid
     *            the JID in the format of user@host[/resource]. Resource is
     *            optional.
     */
    public JID(CharSequence jid) {
        if (jid == null)
            throw new IllegalArgumentException("jid cannot be null");

        this.jid = jid.toString();
    }

    public JID(String name, String domain) {
        if (name == null)
            throw new IllegalArgumentException("name cannot be null");

        if (domain == null)
            throw new IllegalArgumentException("domain cannot be null");

        this.jid = name + "@" + domain;
    }

    // TODO remove this method from the class
    public static boolean isValid(final JID jid) {
        return XMPPUtils.validateJID(jid);
    }

    /**
     * Checks whether the {@link #getBase() base} portion is correctly
     * formatted.
     *
     * @return
     */
    // TODO remove this method from the class
    public boolean isValid() {
        return isValid(this);
    }

    /**
     * @return the name segment of this JID.
     */
    public String getName() {
        return XmppStringUtils.parseLocalpart(jid);
    }

    /**
     * @return the JID without resource qualifier.
     */
    public String getBase() {
        return XmppStringUtils.parseBareJid(jid);
    }

    public BareJid getBareJid() {
        try {
            return JidCreate.bareFrom(jid);
        } catch (XmppStringprepException e) {
            throw new IllegalStateException(e);
        }
    }

    public EntityBareJid getEntityBareJid() {
        try {
            return JidCreate.entityBareFrom(jid);
        } catch (XmppStringprepException e) {
            throw new IllegalStateException(e);
        }
    }

    public Jid toJid() {
        try {
            return JidCreate.from(jid);
        } catch (XmppStringprepException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @return the domain segment of this JID.
     */
    public String getDomain() {
        return XmppStringUtils.parseDomain(jid);
    }

    /**
     * @return the resource segment of this JID or the empty string if there is
     *         none.
     * @see XmppStringUtils#parseResource(String)
     */
    public String getResource() {
        return XmppStringUtils.parseResource(jid);
    }

    /** Returns true if this JID does not have a resource part. */
    public boolean isBareJID() {
        return "".equals(getResource());
    }

    /** Returns true if this JID does have a resource part. */
    public boolean isResourceQualifiedJID() {
        return !isBareJID();
    }

    /** Returns the JID without any resource qualifier. */
    public JID getBareJID() {
        return new JID(getBase());
    }

    /**
     * Returns the unmodified JID this object was constructed with
     *
     * @return
     */
    public String getRAW() {
        return jid;
    }

    /**
     * @return <code>true</code> if the IDs have the same user and domain.
     *         Resource is ignored.
     */
    @Override
    public boolean equals(Object obj) {

        if (obj == null)
            return false;

        if (obj instanceof JID) {
            JID other = (JID) obj;
            return getBase().equals(other.getBase());
        }
        return false;
    }

    /**
     * Returns true if this JID and the given JID are completely identical (this
     * includes the resource unlike equals)
     */
    public boolean strictlyEquals(JID other) {
        return jid.equals(other.jid);
    }

    @Override
    public int hashCode() {
        return getBase().hashCode();
    }

    /** @return the complete string that was used to construct this object. */
    @Override
    public String toString() {
        return jid;
    }
}
