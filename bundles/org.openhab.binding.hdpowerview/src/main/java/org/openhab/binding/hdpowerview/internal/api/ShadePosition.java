/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.hdpowerview.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.PercentType;

/**
 * The position of a shade, as returned by the HD Power View HUB
 *
 * @author Andy Lintner - Initial contribution
 * @author Andrew Fiddian-Green - Added support for secondary rail positions
 * 
 */
@NonNullByDefault
public class ShadePosition {

    /*-
     * Specifies the position of the shade. Top-down shades are in the same
     * coordinate space as bottom-up shades. Shade position values for top-down
     * shades would be reversed for bottom-up shades. For example, since 65535 is
     * the open value for a bottom-up shade, it is the closed value for a top-down
     * shade. The top-down/bottom-up shade is different in that instead of the top
     * and bottom rail operating in one coordinate space like the top-down and the
     * bottom-up, it operates in two where the top (middle) rail closed value is 0
     * and the bottom (primary) rail closed position is also 0 and fully open for
     * both is 65535
     * 
     * The position element can take on multiple states depending on the family of
     * shade under control.
     *
     * The position1 element will only ever show one type of posKind1: either 1 or
     * 3; this is because the shade cannot physically exist with both shade and vane
     * open (to any degree).
     *
     * Therefore one can make the assumption that if there is a non-zero position1
     * while posKind1 == 1, then position1 == 0 for the implied posKind1 == 3. The
     * range of integer values for position1 depends on posKind1:
     *
     *    0..65535 if posKind1 == 1 
     *    0..32767 if posKind1 == 3
     * 
     * Shade fully up: (top-down: open, bottom-up: closed)
     *    posKind1: 1
     *    position1: 65535
     *
     * Shade and vane fully down: (top-down: closed, bottom-up: open) 
     *    posKind1: 1
     *    position1: 0
     *    
     * ALTERNATE: Shade and vane fully down: (top-down: closed, bottom-up: open)
     *    posKind1: 3
     *    position1: 0
     *
     * Shade fully down (closed) and vane fully up (open):
     *    posKind1: 3
     *    position1: 32767
     */

    private static final int MAX_SHADE = 65535;
    private static final int MAX_VANE = 32767;

    private int posKind1;
    private int position1;

    /*
     * here we have to use Integer objects rather than just int primitives because
     * these are secondary optional position elements in the JSON payload, so the
     * GSON de-serializer might leave them as null
     */
    private @Nullable Integer posKind2 = null;
    private @Nullable Integer position2 = null;

    public static ShadePosition create(ShadePositionKind kind, int position) {
        return new ShadePosition(kind, position, null);
    }

    public static ShadePosition create(ShadePositionKind kind, int primaryPos, @Nullable Integer secondaryPos) {
        return new ShadePosition(kind, primaryPos, secondaryPos);
    }

    ShadePosition(ShadePositionKind primaryKind, int primaryPos, @Nullable Integer secondaryPos) {
        switch (primaryKind) {
            case PRIMARY:
                this.position1 = mulDiv(100 - primaryPos, MAX_SHADE, 100);
                this.posKind1 = primaryKind.getKey();
                break;
            case VANE:
                this.position1 = mulDiv(primaryPos, MAX_VANE, 100);
                this.posKind1 = primaryKind.getKey();
                break;
            case SECONDARY:
                this.position1 = 0;
                this.posKind1 = ShadePositionKind.PRIMARY.getKey();
        }
        if (secondaryPos == null) {
            this.position2 = null;
            this.posKind2 = null;
        } else {
            this.position2 = Integer.valueOf(mulDiv(100 - secondaryPos.intValue(), MAX_SHADE, 100));
            this.posKind2 = Integer.valueOf(ShadePositionKind.SECONDARY.getKey());
        }
    }

    private int mulDiv(int value, int multiplicator, int divisor) {
        return (value * multiplicator) / divisor;
    }

    public PercentType getPercent(ShadePositionKind kind) {
        switch (kind) {
            case PRIMARY:
                switch (posKind1) {
                    case 1:
                        return new PercentType(100 - mulDiv(position1, 100, MAX_SHADE));
                    case 3:
                        return PercentType.HUNDRED;
                }
                break;
            case VANE:
                switch (posKind1) {
                    case 1:
                        return PercentType.ZERO;
                    case 3:
                        return new PercentType(mulDiv(position1, 100, MAX_VANE));
                }
                break;
            case SECONDARY:
                Integer posKind2 = this.posKind2;
                Integer position2 = this.position2;
                if (posKind2 != null && position2 != null) {
                    switch (posKind2.intValue()) {
                        case 1:
                            return new PercentType(100 - mulDiv(position2.intValue(), 100, MAX_SHADE));
                        case 3:
                            return PercentType.HUNDRED;
                    }
                }
        }
        return PercentType.ZERO;
    }

    public ShadePositionKind getPosKind() {
        return ShadePositionKind.get(posKind1);
    }

    public ShadePosition copyPrimaryFrom(ShadePosition position) {
        this.position1 = position.position1;
        this.posKind1 = position.posKind1;
        return this;
    }
}
