/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.openhab.binding.seneye.service;

/**
 * The result of a seneye readout - Light intensity in Par
 *
 * @author Niko Tanghe
 */

public class SeneyeDeviceReadingPar {
    public double curr;
    public SeneyeDeviceReadingAdvice[] advices;
}
