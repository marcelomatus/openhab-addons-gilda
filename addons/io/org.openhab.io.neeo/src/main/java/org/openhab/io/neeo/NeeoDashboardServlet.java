/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.neeo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.neeo.internal.ServiceContext;
import org.openhab.io.neeo.internal.servletservices.BrainDashboardService;
import org.openhab.io.neeo.internal.servletservices.ServletService;
import org.openhab.io.neeo.internal.servletservices.ThingDashboardService;

/**
 * This is the main dashboard servlet that will handle requests for dashboard services.
 *
 * @author Tim Roberts - Initial Contribution
 *
 */
@NonNullByDefault
public class NeeoDashboardServlet extends AbstractServlet {
    private static final long serialVersionUID = 2407461893925595039L;

    /**
     * This implementation of {@link AbstractServlet} will handle any requests from the dashboard services. Any brain
     * related services are handled by {@link BrainDashboardService} and any thing related services are handled by
     * {@link ThingDashboardService}
     *
     * @author Tim Roberts - Initial Contribution
     * @param service the non-null service
     * @param servletUrl the non-empty servletUrl
     * @param context the non-null context
     */
    NeeoDashboardServlet(NeeoService service, String servletUrl, ServiceContext context) {
        super(context, servletUrl, new ServletService[] { new BrainDashboardService(service),
                new ThingDashboardService(service, context) });
    }
}
