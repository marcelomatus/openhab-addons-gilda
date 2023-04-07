/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.icalendar.internal.logic;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A single event.
 *
 * @author Michael Wodniok - Initial contribution
 * @author Andrew Fiddian-Green - Added support for event
 * @author Michael Wodniok - Moved CommandTag paring to separate method for doing it only on demand.
 */
@NonNullByDefault
public class Event implements Comparable<Event> {
    public final Instant end;
    public final Instant start;
    @Nullable
    public final String title;
    @Nullable
    public final String description;
    @Nullable
    public final String location;
    @Nullable
    public final String comment;
    @Nullable
    public final String contact;

    public Event(@Nullable String summary, Instant begin, Instant end, @Nullable String description,
            @Nullable String location, @Nullable String comment, @Nullable String contact) {
        this.title = summary;
        this.start = begin;
        this.end = end;
        this.description = description;
        this.location = location;
        this.comment = comment;
        this.contact = contact;

        if (description == null || description.isEmpty()) {
            return;
        }
    }

    public Event(String title, Instant start, Instant end, String description) {
        this(title, start, end, description, null, null, null);
    }

    @Override
    public String toString() {
        return "Event(title: " + this.title + ", start: " + this.start.toString() + ", end: " + this.end.toString();
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (other == null || other.getClass() != this.getClass()) {
            return false;
        }
        final Event otherEvent = (Event) other;
        return (this.title.equals(otherEvent.title) && this.start.equals(otherEvent.start)
                && this.end.equals(otherEvent.end));
    }

    @Override
    public int compareTo(Event o) {
        return start.compareTo(o.start);
    }

    /**
     * extracts the command tags from an events description.
     * 
     * @return A list of CommandTags.
     */
    public List<CommandTag> extractTags() {
        List<CommandTag> rtn = new LinkedList<CommandTag>();
        @Nullable
        String description = this.description;
        if (description != null) {
            String[] lines = description.replace("<p>", "").replace("</p>", "\n").split("\n");
            for (String line : lines) {
                CommandTag tag = CommandTag.createCommandTag(line);
                if (tag != null) {
                    rtn.add(tag);
                }
            }
        }
        return rtn;
    }
}
