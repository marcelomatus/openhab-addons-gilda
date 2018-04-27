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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for ApplicationProgram complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="ApplicationProgram">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="Static" type="{http://knx.org/xml/project/14}Static" minOccurs="0"/>
 *         &lt;element name="Dynamic" type="{http://knx.org/xml/project/14}Dynamic" minOccurs="0"/>
 *       &lt;/all>
 *       &lt;attribute name="Id" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="ApplicationNumber" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="ApplicationVersion" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="ProgramType" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="MaskVersion" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="LoadProcedureStyle" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="PeiType" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="HelpFile" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="DefaultLanguage" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="DynamicTableManagement" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Linkable" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="MinEtsVersion" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="PreEts4Style" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="NonRegRelevantDataVersion" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Hash" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="ConvertedFromPreEts4Data" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Broken" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="IPConfig" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="AdditionalAddressesCount" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="DownloadInfoIncomplete" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="CreatedFromLegacySchemaVersion" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="VisibleDescription" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="OriginalManufacturer" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="HelpTopic" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ApplicationProgram", propOrder = {

})
public class ApplicationProgram {

    @XmlElement(name = "Static")
    protected Static _static;
    @XmlElement(name = "Dynamic")
    protected Dynamic dynamic;
    @XmlAttribute(name = "Id")
    protected java.lang.String id;
    @XmlAttribute(name = "Name")
    protected java.lang.String name;
    @XmlAttribute(name = "ApplicationNumber")
    protected java.lang.String applicationNumber;
    @XmlAttribute(name = "ApplicationVersion")
    protected java.lang.String applicationVersion;
    @XmlAttribute(name = "ProgramType")
    protected java.lang.String programType;
    @XmlAttribute(name = "MaskVersion")
    protected java.lang.String maskVersion;
    @XmlAttribute(name = "LoadProcedureStyle")
    protected java.lang.String loadProcedureStyle;
    @XmlAttribute(name = "PeiType")
    protected java.lang.String peiType;
    @XmlAttribute(name = "HelpFile")
    protected java.lang.String helpFile;
    @XmlAttribute(name = "DefaultLanguage")
    protected java.lang.String defaultLanguage;
    @XmlAttribute(name = "DynamicTableManagement")
    protected java.lang.String dynamicTableManagement;
    @XmlAttribute(name = "Linkable")
    protected java.lang.String linkable;
    @XmlAttribute(name = "MinEtsVersion")
    protected java.lang.String minEtsVersion;
    @XmlAttribute(name = "PreEts4Style")
    protected java.lang.String preEts4Style;
    @XmlAttribute(name = "NonRegRelevantDataVersion")
    protected java.lang.String nonRegRelevantDataVersion;
    @XmlAttribute(name = "Hash")
    protected java.lang.String hash;
    @XmlAttribute(name = "ConvertedFromPreEts4Data")
    protected java.lang.String convertedFromPreEts4Data;
    @XmlAttribute(name = "Broken")
    protected java.lang.String broken;
    @XmlAttribute(name = "IPConfig")
    protected java.lang.String ipConfig;
    @XmlAttribute(name = "AdditionalAddressesCount")
    protected java.lang.String additionalAddressesCount;
    @XmlAttribute(name = "DownloadInfoIncomplete")
    protected java.lang.String downloadInfoIncomplete;
    @XmlAttribute(name = "CreatedFromLegacySchemaVersion")
    protected java.lang.String createdFromLegacySchemaVersion;
    @XmlAttribute(name = "VisibleDescription")
    protected java.lang.String visibleDescription;
    @XmlAttribute(name = "OriginalManufacturer")
    protected java.lang.String originalManufacturer;
    @XmlAttribute(name = "HelpTopic")
    protected java.lang.String helpTopic;

    /**
     * Gets the value of the static property.
     *
     * @return
     *         possible object is
     *         {@link Static }
     * 
     */
    public Static getStatic() {
        return _static;
    }

    /**
     * Sets the value of the static property.
     *
     * @param value
     *            allowed object is
     *            {@link Static }
     * 
     */
    public void setStatic(Static value) {
        this._static = value;
    }

    /**
     * Gets the value of the dynamic property.
     *
     * @return
     *         possible object is
     *         {@link Dynamic }
     * 
     */
    public Dynamic getDynamic() {
        return dynamic;
    }

    /**
     * Sets the value of the dynamic property.
     *
     * @param value
     *            allowed object is
     *            {@link Dynamic }
     * 
     */
    public void setDynamic(Dynamic value) {
        this.dynamic = value;
    }

    /**
     * Gets the value of the id property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setId(java.lang.String value) {
        this.id = value;
    }

    /**
     * Gets the value of the name property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setName(java.lang.String value) {
        this.name = value;
    }

    /**
     * Gets the value of the applicationNumber property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getApplicationNumber() {
        return applicationNumber;
    }

    /**
     * Sets the value of the applicationNumber property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setApplicationNumber(java.lang.String value) {
        this.applicationNumber = value;
    }

    /**
     * Gets the value of the applicationVersion property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getApplicationVersion() {
        return applicationVersion;
    }

    /**
     * Sets the value of the applicationVersion property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setApplicationVersion(java.lang.String value) {
        this.applicationVersion = value;
    }

    /**
     * Gets the value of the programType property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getProgramType() {
        return programType;
    }

    /**
     * Sets the value of the programType property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setProgramType(java.lang.String value) {
        this.programType = value;
    }

    /**
     * Gets the value of the maskVersion property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getMaskVersion() {
        return maskVersion;
    }

    /**
     * Sets the value of the maskVersion property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setMaskVersion(java.lang.String value) {
        this.maskVersion = value;
    }

    /**
     * Gets the value of the loadProcedureStyle property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getLoadProcedureStyle() {
        return loadProcedureStyle;
    }

    /**
     * Sets the value of the loadProcedureStyle property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setLoadProcedureStyle(java.lang.String value) {
        this.loadProcedureStyle = value;
    }

    /**
     * Gets the value of the peiType property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getPeiType() {
        return peiType;
    }

    /**
     * Sets the value of the peiType property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setPeiType(java.lang.String value) {
        this.peiType = value;
    }

    /**
     * Gets the value of the helpFile property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getHelpFile() {
        return helpFile;
    }

    /**
     * Sets the value of the helpFile property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setHelpFile(java.lang.String value) {
        this.helpFile = value;
    }

    /**
     * Gets the value of the defaultLanguage property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getDefaultLanguage() {
        return defaultLanguage;
    }

    /**
     * Sets the value of the defaultLanguage property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setDefaultLanguage(java.lang.String value) {
        this.defaultLanguage = value;
    }

    /**
     * Gets the value of the dynamicTableManagement property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getDynamicTableManagement() {
        return dynamicTableManagement;
    }

    /**
     * Sets the value of the dynamicTableManagement property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setDynamicTableManagement(java.lang.String value) {
        this.dynamicTableManagement = value;
    }

    /**
     * Gets the value of the linkable property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getLinkable() {
        return linkable;
    }

    /**
     * Sets the value of the linkable property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setLinkable(java.lang.String value) {
        this.linkable = value;
    }

    /**
     * Gets the value of the minEtsVersion property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getMinEtsVersion() {
        return minEtsVersion;
    }

    /**
     * Sets the value of the minEtsVersion property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setMinEtsVersion(java.lang.String value) {
        this.minEtsVersion = value;
    }

    /**
     * Gets the value of the preEts4Style property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getPreEts4Style() {
        return preEts4Style;
    }

    /**
     * Sets the value of the preEts4Style property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setPreEts4Style(java.lang.String value) {
        this.preEts4Style = value;
    }

    /**
     * Gets the value of the nonRegRelevantDataVersion property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getNonRegRelevantDataVersion() {
        return nonRegRelevantDataVersion;
    }

    /**
     * Sets the value of the nonRegRelevantDataVersion property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setNonRegRelevantDataVersion(java.lang.String value) {
        this.nonRegRelevantDataVersion = value;
    }

    /**
     * Gets the value of the hash property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getHash() {
        return hash;
    }

    /**
     * Sets the value of the hash property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setHash(java.lang.String value) {
        this.hash = value;
    }

    /**
     * Gets the value of the convertedFromPreEts4Data property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getConvertedFromPreEts4Data() {
        return convertedFromPreEts4Data;
    }

    /**
     * Sets the value of the convertedFromPreEts4Data property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setConvertedFromPreEts4Data(java.lang.String value) {
        this.convertedFromPreEts4Data = value;
    }

    /**
     * Gets the value of the broken property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getBroken() {
        return broken;
    }

    /**
     * Sets the value of the broken property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setBroken(java.lang.String value) {
        this.broken = value;
    }

    /**
     * Gets the value of the ipConfig property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getIPConfig() {
        return ipConfig;
    }

    /**
     * Sets the value of the ipConfig property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setIPConfig(java.lang.String value) {
        this.ipConfig = value;
    }

    /**
     * Gets the value of the additionalAddressesCount property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getAdditionalAddressesCount() {
        return additionalAddressesCount;
    }

    /**
     * Sets the value of the additionalAddressesCount property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setAdditionalAddressesCount(java.lang.String value) {
        this.additionalAddressesCount = value;
    }

    /**
     * Gets the value of the downloadInfoIncomplete property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getDownloadInfoIncomplete() {
        return downloadInfoIncomplete;
    }

    /**
     * Sets the value of the downloadInfoIncomplete property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setDownloadInfoIncomplete(java.lang.String value) {
        this.downloadInfoIncomplete = value;
    }

    /**
     * Gets the value of the createdFromLegacySchemaVersion property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getCreatedFromLegacySchemaVersion() {
        return createdFromLegacySchemaVersion;
    }

    /**
     * Sets the value of the createdFromLegacySchemaVersion property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setCreatedFromLegacySchemaVersion(java.lang.String value) {
        this.createdFromLegacySchemaVersion = value;
    }

    /**
     * Gets the value of the visibleDescription property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getVisibleDescription() {
        return visibleDescription;
    }

    /**
     * Sets the value of the visibleDescription property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setVisibleDescription(java.lang.String value) {
        this.visibleDescription = value;
    }

    /**
     * Gets the value of the originalManufacturer property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getOriginalManufacturer() {
        return originalManufacturer;
    }

    /**
     * Sets the value of the originalManufacturer property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setOriginalManufacturer(java.lang.String value) {
        this.originalManufacturer = value;
    }

    /**
     * Gets the value of the helpTopic property.
     *
     * @return
     *         possible object is
     *         {@link java.lang.String }
     * 
     */
    public java.lang.String getHelpTopic() {
        return helpTopic;
    }

    /**
     * Sets the value of the helpTopic property.
     *
     * @param value
     *            allowed object is
     *            {@link java.lang.String }
     * 
     */
    public void setHelpTopic(java.lang.String value) {
        this.helpTopic = value;
    }

}
