/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package com.jdimension.jlawyer.client.events;

import com.jdimension.jlawyer.dropscan.DropscanMailing;
import java.util.List;

/**
 *
 * @author jens
 */
public class DropscanStatusEvent extends Event {

    private List<DropscanMailing> mailings;

    public DropscanStatusEvent(List<DropscanMailing> mailings) {
        super(Event.TYPE_DROPSCANSTATUS);
        this.mailings = mailings;
    }

    @Override
    public boolean isUiUpdateTrigger() {
        return true;
    }

    public List<DropscanMailing> getMailings() {
        return mailings;
    }

    public void setMailings(List<DropscanMailing> mailings) {
        this.mailings = mailings;
    }
}
