package org.openhab.binding.souliss.handler;

import org.eclipse.smarthome.core.thing.Thing;

/**
 * The {@link SoulissT65Handler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Luca Remigio - Initial contribution
 */
public class SoulissT65Handler extends SoulissT6nHandler {

    private float analogSetpointValue;

    // constructor
    public SoulissT65Handler(Thing _thing) {
        super(_thing);
    }
}
