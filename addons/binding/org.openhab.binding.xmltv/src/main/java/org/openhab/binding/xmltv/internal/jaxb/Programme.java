/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.xmltv.internal.jaxb;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Java class for a programme XML element
 *
 * @author Gaël L'hopital - Initial contribution
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
@NonNullByDefault
public class Programme {
    private static final DateFormat XMLTV_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss Z");

    @XmlElement(name = "title", required = true)
    protected List<WithLangType> titles = new ArrayList<>();

    @XmlElement(name = "category")
    protected List<WithLangType> categories = new ArrayList<>();

    @XmlElement(name = "icon")
    protected List<Icon> icons = new ArrayList<>();

    @XmlAttribute(required = true)
    private String start = "";

    @XmlAttribute
    private String stop = "";

    @XmlAttribute(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String channel = "";

    public List<WithLangType> getTitles() {
        return this.titles;
    }

    public List<WithLangType> getCategories() {
        return this.categories;
    }

    public Instant getProgrammeStart() {
        long epoch = iso860DateToEpoch(start);
        return Instant.ofEpochMilli(epoch);
    }

    public Instant getProgrammeStop() {
        long epoch = iso860DateToEpoch(stop);
        return Instant.ofEpochMilli(epoch);
    }

    private long iso860DateToEpoch(String date) {
        try {
            Date formatted = XMLTV_DATE_FORMAT.parse(date);
            return formatted.getTime();
        } catch (Exception e) {
            return 0;
        }
    }

    public List<Icon> getIcons() {
        return this.icons;
    }

    public String getChannel() {
        return channel;
    }

}
