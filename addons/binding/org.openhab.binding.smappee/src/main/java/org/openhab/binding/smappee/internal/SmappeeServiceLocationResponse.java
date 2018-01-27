/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.smappee.internal;

/**
 * Where is the Smappee located ? The response
 *
 * @author Niko Tanghe - Initial contribution
 */
public class SmappeeServiceLocationResponse {
    public String appname;

    public SmappeeServiceLocation[] serviceLocations;
}

// Example JSON received from the Smappee API :
// {
// "appName": "MyFirstApp",
// "serviceLocations": [
// {
// "serviceLocationId": 123456,
// "name": "Home"
// }
// ]
// }
