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
package org.openhab.binding.sleepiq.api.impl.typeadapters;

import java.time.Duration;

/**
 * Type adapter for jsr310 {@link Duration} class.
 *
 * @author Christophe Bornet
 */
public class DurationTypeAdapter extends TemporalTypeAdapter<Duration> {

  public DurationTypeAdapter() {
    super(Duration::parse);
  }
}
