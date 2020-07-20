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
package org.openhab.binding.automower.internal.actions;

/**
 * Actions that can be executed for an automower
 *
 * @author Markus Pfleger - Initial contribution
 */
public interface IAutomowerActions {

    void resumeSchedule();

    void park(int durationMin);

    void parkUntilFurtherNotice();

    void parkUntilNextSchedule();

    void pause();

    void start(int durationMin);

}
