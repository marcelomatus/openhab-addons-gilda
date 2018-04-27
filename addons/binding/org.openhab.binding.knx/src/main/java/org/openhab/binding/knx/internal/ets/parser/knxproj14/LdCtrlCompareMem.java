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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * <p>
 * Java class for LdCtrlCompareMem complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="LdCtrlCompareMem">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute name="Address" type="{http://www.w3.org/2001/XMLSchema}short" />
 *       &lt;attribute name="Size" type="{http://www.w3.org/2001/XMLSchema}byte" />
 *       &lt;attribute name="InlineData" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LdCtrlCompareMem", propOrder = { "value" })
public class LdCtrlCompareMem {

    @XmlValue
    protected java.lang.String value;
    @XmlAttribute(name = "Address")
    protected Short address;
    @XmlAttribute(name = "Size")
    protected Byte size;
    @XmlAttribute(name = "InlineData")
    protected java.lang.String inlineData;

    /**
     * Gets the value of the value property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setValue(java.lang.String value) {
        this.value = value;
    }

    /**
     * Gets the value of the address property.
     *
     * @return
     *         possible object is
     *         {@link Short }
     * 
     */
    public Short getAddress() {
        return address;
    }

    /**
     * Sets the value of the address property.
     *
     * @param value
     *            allowed object is
     *            {@link Short }
     * 
     */
    public void setAddress(Short value) {
        this.address = value;
    }

    /**
     * Gets the value of the size property.
     *
     * @return
     *         possible object is
     *         {@link Byte }
     * 
     */
    public Byte getSize() {
        return size;
    }

    /**
     * Sets the value of the size property.
     *
     * @param value
     *            allowed object is
     *            {@link Byte }
     * 
     */
    public void setSize(Byte value) {
        this.size = value;
    }

    /**
     * Gets the value of the inlineData property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getInlineData() {
        return inlineData;
    }

    /**
     * Sets the value of the inlineData property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setInlineData(java.lang.String value) {
        this.inlineData = value;
    }

}
