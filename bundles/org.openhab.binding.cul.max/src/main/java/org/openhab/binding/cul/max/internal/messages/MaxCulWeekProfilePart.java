/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.cul.max.internal.messages;

import java.util.ArrayList;
import java.util.List;

import org.openhab.binding.cul.max.internal.messages.constants.MaxCulWeekdays;

/**
 * @author Johannes Goehr (johgoe) - Initial contribution
 */
public class MaxCulWeekProfilePart {

    private List<MaxCulWeekProfileControlPoint> weekProfileControlPoints = new ArrayList<MaxCulWeekProfileControlPoint>();

    private MaxCulWeekdays day;

    public void setDay(MaxCulWeekdays day) {
        this.day = day;
    }

    public MaxCulWeekdays getDay() {
        return day;
    }

    public void addControlPoint(MaxCulWeekProfileControlPoint weekProfileControlPoint) {
        weekProfileControlPoints.add(weekProfileControlPoint);
    }

    public List<MaxCulWeekProfileControlPoint> getControlPoints() {
        return weekProfileControlPoints;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getDay()).append("\r\n");
        String time_prof_str = "00:00";
        String temp_prof_str = "";
        List<MaxCulWeekProfileControlPoint> controlPoints = getControlPoints();
        for (int k = 0; k < controlPoints.size(); k++) {
            MaxCulWeekProfileControlPoint controlPoint = controlPoints.get(k);
            time_prof_str += String.format("-%02d:%02d", controlPoint.getHour(), controlPoint.getMin());
            temp_prof_str += String.format("%2.1f °C", controlPoint.getTemperature());
            if (k < controlPoints.size() - 1) {
                time_prof_str += "  /  " + String.format("%02d:%02d", controlPoint.getHour(), controlPoint.getMin());
                temp_prof_str += "  /  ";
            }
        }
        sb.append(time_prof_str).append("\r\n");
        sb.append(temp_prof_str);
        return sb.toString();
    }
}
