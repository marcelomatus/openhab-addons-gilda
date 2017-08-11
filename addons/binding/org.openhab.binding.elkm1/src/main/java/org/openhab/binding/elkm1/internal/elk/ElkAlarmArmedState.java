/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.elkm1.internal.elk;

/**
 * Tracks the armed state of the area.
 *
 * @author David Bennett - Initial Contribution
 */
public enum ElkAlarmArmedState {
    Disarmed,
    ArmedAway,
    ArmedStay,
    ArmedStayInstant,
    ArmedToNight,
    ArmedToNightInstant,
    ArmedToVacation
}