/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.binding.freebox.internal.api.model;

import org.openhab.binding.freebox.internal.api.RequestAnnotation;

/**
 * The {@link VirtualMachineRequest} is the Java class used to map the
 * response of Virtual Machine API
 * https://dev.freebox.fr/sdk/os/vm/#
 *
 * @author Gaël L'hopital - Initial contribution
 */
@RequestAnnotation(relativeUrl = "vm/", retryAuth = true, responseClass = VirtualMachineResponse.class)
public class VirtualMachineRequest extends APIAction {

    public VirtualMachineRequest(String vmId) {
        super(vmId);
    }
}
