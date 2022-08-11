/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
/*
 * Imported from https://github.com/google-gson/typeadapters/tree/master/jsr310/src
 * and repackaged to avoid the default package.
 */
package org.openhab.binding.lametrictime.api.common.impl.typeadapters.imported;

import java.time.Period;

/**
 * Type adapter for jsr310 {@link Period} class.
 *
 * @author Christophe Bornet
 */
public class PeriodTypeAdapter extends TemporalTypeAdapter<Period> {

  public PeriodTypeAdapter() {
    super(Period::parse);
  }
}
