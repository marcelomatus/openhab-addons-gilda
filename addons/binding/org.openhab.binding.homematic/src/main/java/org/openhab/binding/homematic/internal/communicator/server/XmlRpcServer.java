/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.homematic.internal.communicator.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.openhab.binding.homematic.internal.common.HomematicConfig;
import org.openhab.binding.homematic.internal.communicator.message.RpcRequest;
import org.openhab.binding.homematic.internal.communicator.message.XmlRpcRequest;
import org.openhab.binding.homematic.internal.communicator.message.XmlRpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Reads a XML-RPC message and handles the method call.
 *
 * @author Gerhard Riegler - Initial contribution
 */
public class XmlRpcServer implements RpcServer {
    private final Logger logger = LoggerFactory.getLogger(XmlRpcServer.class);

    private static final String XML_EMPTY_STRING = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n<methodResponse><params><param><value></value></param></params></methodResponse>";
    private static final String XML_EMPTY_ARRAY = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n<methodResponse><params><param><value><array><data></data></array></value></param></params></methodResponse>";
    private static final String XML_EMPTY_EVENT_LIST = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n<methodResponse><params><param><value><array><data><value>event</value></data></array></value></param></params></methodResponse>";

    private Server xmlRpcHTTPD;
    private HomematicConfig config;
    private RpcResponseHandler<String> rpcResponseHander;
    private ResponseHandler jettyResponseHandler = new ResponseHandler();

    public XmlRpcServer(RpcEventListener listener, HomematicConfig config) {
        this.config = config;
        this.rpcResponseHander = new RpcResponseHandler<String>(listener) {

            /**
             * {@inheritDoc}
             */
            @Override
            protected String getEmptyStringResult() {
                return XML_EMPTY_STRING;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected String getEmptyEventListResult() {
                return XML_EMPTY_EVENT_LIST;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected String getEmptyArrayResult() {
                return XML_EMPTY_ARRAY;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected RpcRequest<String> createRpcRequest() {
                return new XmlRpcRequest(null, XmlRpcRequest.TYPE.RESPONSE);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() throws IOException {
        logger.debug("Initializing XML-RPC server at port {}", config.getXmlCallbackPort());

        xmlRpcHTTPD = new Server(config.getXmlCallbackPort());
        xmlRpcHTTPD.setHandler(jettyResponseHandler);

        try {
            xmlRpcHTTPD.start();
            if (logger.isTraceEnabled()) {
                xmlRpcHTTPD.dumpStdErr();
            }
        } catch (Exception e) {
            throw new IOException("Jetty start failed", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        if (xmlRpcHTTPD != null) {
            logger.debug("Stopping XML-RPC server");
            try {
                xmlRpcHTTPD.stop();
            } catch (Exception ex) {
                logger.error("{}", ex.getMessage(), ex);
            }
        }
    }

    /**
     * Response handler for Jetty implementing a XML-RPC server
     *
     * @author Martin Herbst
     */
    private class ResponseHandler extends AbstractHandler {

        /**
         * {@inheritDoc}
         */
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            response.setContentType("text/xml;charset=ISO-8859-1");
            response.setStatus(HttpServletResponse.SC_OK);
            final PrintWriter respWriter = response.getWriter();
            try {
                XmlRpcResponse xmlResponse = new XmlRpcResponse(request.getInputStream(), config.getEncoding());
                if (logger.isTraceEnabled()) {
                    logger.trace("Server parsed XmlRpcMessage:\n{}", xmlResponse);
                }
                final String returnValue = rpcResponseHander.handleMethodCall(xmlResponse.getMethodName(),
                        xmlResponse.getResponseData());
                if (logger.isTraceEnabled()) {
                    logger.trace("Server XmlRpcResponse:\n{}", returnValue);
                }
                respWriter.println(returnValue);
            } catch (SAXException | ParserConfigurationException ex) {
                logger.error("{}", ex.getMessage(), ex);
                respWriter.println(XML_EMPTY_STRING);
            }
            baseRequest.setHandled(true);
        }
    }

}
