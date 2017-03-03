/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.02.17 at 06:25:15 PM CET 
//


package org.openhab.ui.cometvisu.internal.config.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for infoaction complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="infoaction"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="layout" type="{}layout" minOccurs="0"/&gt;
 *         &lt;element name="label" type="{}label" minOccurs="0"/&gt;
 *         &lt;element name="widgetinfo" type="{}widgetinfo"/&gt;
 *         &lt;element name="widgetaction" type="{}widgetaction"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "infoaction", propOrder = {
    "layout",
    "label",
    "widgetinfo",
    "widgetaction"
})
public class Infoaction {

    protected Layout layout;
    protected Label label;
    @XmlElement(required = true)
    protected Widgetinfo widgetinfo;
    @XmlElement(required = true)
    protected Widgetaction widgetaction;

    /**
     * Gets the value of the layout property.
     * 
     * @return
     *     possible object is
     *     {@link Layout }
     *     
     */
    public Layout getLayout() {
        return layout;
    }

    /**
     * Sets the value of the layout property.
     * 
     * @param value
     *     allowed object is
     *     {@link Layout }
     *     
     */
    public void setLayout(Layout value) {
        this.layout = value;
    }

    /**
     * Gets the value of the label property.
     * 
     * @return
     *     possible object is
     *     {@link Label }
     *     
     */
    public Label getLabel() {
        return label;
    }

    /**
     * Sets the value of the label property.
     * 
     * @param value
     *     allowed object is
     *     {@link Label }
     *     
     */
    public void setLabel(Label value) {
        this.label = value;
    }

    /**
     * Gets the value of the widgetinfo property.
     * 
     * @return
     *     possible object is
     *     {@link Widgetinfo }
     *     
     */
    public Widgetinfo getWidgetinfo() {
        return widgetinfo;
    }

    /**
     * Sets the value of the widgetinfo property.
     * 
     * @param value
     *     allowed object is
     *     {@link Widgetinfo }
     *     
     */
    public void setWidgetinfo(Widgetinfo value) {
        this.widgetinfo = value;
    }

    /**
     * Gets the value of the widgetaction property.
     * 
     * @return
     *     possible object is
     *     {@link Widgetaction }
     *     
     */
    public Widgetaction getWidgetaction() {
        return widgetaction;
    }

    /**
     * Sets the value of the widgetaction property.
     * 
     * @param value
     *     allowed object is
     *     {@link Widgetaction }
     *     
     */
    public void setWidgetaction(Widgetaction value) {
        this.widgetaction = value;
    }

}
