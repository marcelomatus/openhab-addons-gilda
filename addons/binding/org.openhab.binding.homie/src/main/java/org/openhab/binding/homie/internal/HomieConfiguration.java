/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.homie.internal;

import java.util.Dictionary;

/**
 * Class that holds the OpenHAB Homie Configuration
 * 
 * @author Michael Kolb - Initial contribution
 *
 */
public class HomieConfiguration {

    private String baseTopic;

    private String brokerUrl;

    public HomieConfiguration(Dictionary<String, Object> properties) {
        brokerUrl = (String) properties.get("mqttbrokerurl");
        baseTopic = (String) properties.get("basetopic");
    }

    public String getBaseTopic() {
        return baseTopic;
    }

    public void setBaseTopic(String baseTopic) {
        this.baseTopic = baseTopic;
    }

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public void setBrokerUrl(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

}
