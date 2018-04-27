/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
/**
 * This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
 * See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
 * Any modifications to this file will be lost upon recompilation of the source schema.
 * Generated on: 2017.03.09 at 08:34:29 PM CET
 */

package org.openhab.binding.knx.internal.ets.parser.knxproj14;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for Resources complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="Resources">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Resource" type="{http://knx.org/xml/project/14}Resource" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Resources", propOrder = { "resource" })
public class Resources {

    @XmlElement(name = "Resource")
    protected List<Resource> resource;

    /**
     * Gets the value of the resource property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the resource property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * 
     * <pre>
     * getResource().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Resource }
     *
     *
     */
    public List<Resource> getResource() {
        if (resource == null) {
            resource = new ArrayList<Resource>();
        }
        return this.resource;
    }

}
