/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.openhab.binding.km200.internal;

import java.util.HashMap;

/**
 * The KM200CommObject representing a service on a device with its all capabilities
 *
 * @author Markus Eckhardt - Initial contribution
 *
 */

public class KM200CommObject {
    private Integer readable;
    private Integer writeable;
    private Integer recordable;
    private Integer virtual;
    private Boolean updated;
    private String parent;
    private String fullServiceName;
    private String serviceType;
    private String jsonData;
    private Object value;
    private Object valueParameter;

    /* Device services */
    public HashMap<String, KM200CommObject> serviceTreeMap;
    KM200CommObject parentObject;

    public KM200CommObject(String fullServiceName, String serviceType, Integer readable, Integer writeable,
            Integer recordable, Integer virtual, String parent, KM200CommObject parentObject) {
        serviceTreeMap = new HashMap<String, KM200CommObject>();
        this.fullServiceName = fullServiceName;
        this.serviceType = serviceType;
        this.readable = readable;
        this.writeable = writeable;
        this.recordable = recordable;
        this.virtual = virtual;
        this.parent = parent;
        this.parentObject = parentObject;
        updated = false;
    }

    /* Sets */
    public void setValue(Object val) {
        value = val;
    }

    public void setUpdated(Boolean updt) {
        updated = updt;
    }

    public void setValueParameter(Object val) {
        valueParameter = val;
    }

    public void setJSONData(String data) {
        jsonData = data;
    }

    /* gets */
    public Integer getReadable() {
        return readable;
    }

    public Integer getWriteable() {
        return writeable;
    }

    public Integer getRecordable() {
        return recordable;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getFullServiceName() {
        return fullServiceName;
    }

    public Object getValue() {
        return value;
    }

    public Object getValueParameter() {
        return valueParameter;
    }

    public String getParent() {
        return parent;
    }

    public Integer getVirtual() {
        return virtual;
    }

    public Boolean getUpdated() {
        return updated;
    }

    public String getJSONData() {
        return jsonData;
    }
}
