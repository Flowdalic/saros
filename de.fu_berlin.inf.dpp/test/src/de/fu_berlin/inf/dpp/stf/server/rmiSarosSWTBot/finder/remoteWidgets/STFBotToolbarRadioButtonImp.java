package de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarRadioButton;

public class STFBotToolbarRadioButtonImp extends AbstractRmoteWidget implements
    STFBotToolbarRadioButton {

    private static transient STFBotToolbarRadioButtonImp self;

    private SWTBotToolbarRadioButton toolbarRadioButton;

    /**
     * {@link STFBotButtonImp} is a singleton, but inheritance is possible.
     */
    public static STFBotToolbarRadioButtonImp getInstance() {
        if (self != null)
            return self;
        self = new STFBotToolbarRadioButtonImp();
        return self;
    }

    public void setSwtBotToolbarRadioButton(
        SWTBotToolbarRadioButton toolbarRadioButton) {
        this.toolbarRadioButton = toolbarRadioButton;
    }

    /**************************************************************
     * 
     * exported functions
     * 
     **************************************************************/

    /**********************************************
     * 
     * actions
     * 
     **********************************************/
}
