/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.km200.internal;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The KM200Device representing the device with its all capabilities
 *
 * @author Markus Eckhardt - Initial contribution
 *
 */

public class KM200Device {

    private static final Logger logger = LoggerFactory.getLogger(KM200Device.class);

    /* valid IPv4 address of the KMxxx. */
    protected String ip4Address = null;

    /* The gateway password which is provided on the type sign of the KMxxx. */
    protected String gatewayPassword = null;

    /* The private password which has been defined by the user via EasyControl. */
    protected String privatePassword = null;

    /* The returned device charset for communication */
    protected String charSet = null;

    /* Needed keys for the communication */
    protected byte[] cryptKeyInit = null;
    protected byte[] cryptKeyPriv = null;

    /* Buderus_MD5Salt */
    protected byte[] MD5Salt = null;

    /* Device services */
    public HashMap<String, KM200CommObject> serviceTreeMap = null;

    /* Device services blacklist */
    List<String> blacklistMap = null;
    /* List of virtual services */
    List<KM200CommObject> virtualList = null;

    /* Is the first INIT done */
    protected Boolean inited = false;

    public KM200Device() {
        serviceTreeMap = new HashMap<String, KM200CommObject>();
        blacklistMap = new ArrayList<String>();
        blacklistMap.add("/gateway/firmware");
        virtualList = new ArrayList<KM200CommObject>();
    }

    public Boolean isConfigured() {
        return StringUtils.isNotBlank(ip4Address) && cryptKeyPriv != null;
    }

    /**
     * This function creates the private key from the MD5Salt, the device and the private password
     *
     * @author Markus Eckhardt
     *
     * @since 1.9.0
     */
    private void recreateKeys() {
        if (StringUtils.isNotBlank(gatewayPassword) && StringUtils.isNotBlank(privatePassword) && MD5Salt != null) {
            byte[] MD5_K1 = null;
            byte[] MD5_K2_Init = null;
            byte[] MD5_K2_Private = null;
            byte[] bytesOfGatewayPassword = null;
            byte[] bytesOfPrivatePassword = null;
            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                logger.error("No such algorithm, MD5: {}", e.getMessage());
                return;
            }

            /* First half of the key: MD5 of (GatewayPassword . Salt) */
            try {
                bytesOfGatewayPassword = gatewayPassword.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                logger.error("No such encoding, UTF-8: {}", e.getMessage());
                return;
            }
            byte[] CombParts1 = new byte[bytesOfGatewayPassword.length + MD5Salt.length];
            System.arraycopy(bytesOfGatewayPassword, 0, CombParts1, 0, bytesOfGatewayPassword.length);
            System.arraycopy(MD5Salt, 0, CombParts1, bytesOfGatewayPassword.length, MD5Salt.length);
            MD5_K1 = md.digest(CombParts1);

            /* Second half of the key: - Initial: MD5 of ( Salt) */
            MD5_K2_Init = md.digest(MD5Salt);

            /* Second half of the key: - private: MD5 of ( Salt . PrivatePassword) */
            try {
                bytesOfPrivatePassword = privatePassword.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                logger.error("No such encoding, UTF-8: {}", e.getMessage());
                return;
            }
            byte[] CombParts2 = new byte[bytesOfPrivatePassword.length + MD5Salt.length];
            System.arraycopy(MD5Salt, 0, CombParts2, 0, MD5Salt.length);
            System.arraycopy(bytesOfPrivatePassword, 0, CombParts2, MD5Salt.length, bytesOfPrivatePassword.length);
            MD5_K2_Private = md.digest(CombParts2);

            /* Create Keys */
            cryptKeyInit = new byte[MD5_K1.length + MD5_K2_Init.length];
            System.arraycopy(MD5_K1, 0, cryptKeyInit, 0, MD5_K1.length);
            System.arraycopy(MD5_K2_Init, 0, cryptKeyInit, MD5_K1.length, MD5_K2_Init.length);

            cryptKeyPriv = new byte[MD5_K1.length + MD5_K2_Private.length];
            System.arraycopy(MD5_K1, 0, cryptKeyPriv, 0, MD5_K1.length);
            System.arraycopy(MD5_K2_Private, 0, cryptKeyPriv, MD5_K1.length, MD5_K2_Private.length);

        }
    }

    // getter
    public String getIP4Address() {
        return ip4Address;
    }

    public String getGatewayPassword() {
        return gatewayPassword;
    }

    public String getPrivatePassword() {
        return privatePassword;
    }

    public byte[] getCryptKeyInit() {
        return cryptKeyInit;
    }

    public byte[] getCryptKeyPriv() {
        return cryptKeyPriv;
    }

    public String getCharSet() {
        return charSet;
    }

    public Boolean getInited() {
        return inited;
    }

    /**
     * This function prepares a list of all on the device available services with its capabilities
     *
     */
    public void listAllServices() {
        if (serviceTreeMap != null) {
            logger.info("##################################################################");
            logger.info("List of avalible services");
            logger.info("readable;writeable;recordable;virtual;type;service;value;allowed;min;max;unit");
            printAllServices(serviceTreeMap);
            logger.info("##################################################################");
        }

    }

    /**
     * This function outputs a ";" separated list of all on the device available services with its capabilities
     *
     * @param actTreeMap
     */

    public void printAllServices(HashMap<String, KM200CommObject> actTreeMap) {
        if (actTreeMap != null) {
            for (KM200CommObject object : actTreeMap.values()) {
                if (object != null) {
                    String val = "", type, valPara = "";
                    logger.debug("List type: {} service: {}", object.getServiceType(), object.getFullServiceName());
                    type = object.getServiceType();
                    if (type == null) {
                        type = new String();
                    }
                    if ("stringValue".equals(type) || "floatValue".equals(type)) {
                        val = object.getValue().toString();
                        if (object.getValueParameter() != null) {
                            if ("stringValue".equals(type)) {
                                // Type is definitely correct here
                                @SuppressWarnings("unchecked")
                                List<String> valParas = (List<String>) object.getValueParameter();
                                for (int i = 0; i < valParas.size(); i++) {
                                    if (i > 0) {
                                        valPara += "|";
                                    }
                                    valPara += valParas.get(i);
                                }
                                valPara += ";;;";
                            }
                            if ("floatValue".equals(type)) {
                                // Type is definitely correct here
                                @SuppressWarnings("unchecked")
                                List<Object> valParas = (List<Object>) object.getValueParameter();
                                valPara += ";";
                                valPara += valParas.get(0);
                                valPara += ";";
                                valPara += valParas.get(1);
                                valPara += ";";
                                if (valParas.size() == 3) {
                                    valPara += valParas.get(2);
                                }
                            }
                        } else {
                            valPara += ";;;";
                        }
                    } else {
                        val = "";
                        valPara = ";";
                    }
                    logger.info("{};{};{};{};{};{};{};{}", object.getReadable().toString(),
                            object.getWriteable().toString(), object.getRecordable().toString(),
                            object.getVirtual().toString(), type, object.getFullServiceName(), val, valPara);
                    printAllServices(object.serviceTreeMap);
                }
            }
        }

    }

    /**
     * This function resets the update state on all service objects
     *
     * @param actTreeMap
     */
    public void resetAllUpdates(HashMap<String, KM200CommObject> actTreeMap) {
        if (actTreeMap != null) {
            for (KM200CommObject stmObject : actTreeMap.values()) {
                if (stmObject != null) {
                    stmObject.setUpdated(false);
                    resetAllUpdates(stmObject.serviceTreeMap);
                }
            }
        }
    }

    /**
     * This function checks whether a service is available
     *
     * @param service
     */
    public Boolean containsService(String service) {
        String[] servicePath = service.split("/");
        KM200CommObject object = null;
        int len = servicePath.length;
        if (len == 0) {
            return false;
        }
        if (!serviceTreeMap.containsKey(servicePath[1])) {
            return false;
        } else {
            if (len == 2) {
                return true;
            }
            object = serviceTreeMap.get(servicePath[1]);
        }
        for (int i = 2; i < len; i++) {
            if (object.serviceTreeMap.containsKey(servicePath[i])) {
                object = object.serviceTreeMap.get(servicePath[i]);
                continue;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * This function return the KM200CommObject of a service
     *
     * @param service
     */
    public KM200CommObject getServiceObject(String service) {
        String[] servicePath = service.split("/");
        KM200CommObject object = null;
        int len = servicePath.length;
        if (len == 0) {
            return null;
        }
        if (!serviceTreeMap.containsKey(servicePath[1])) {
            return null;
        } else {
            object = serviceTreeMap.get(servicePath[1]);
            if (len == 2) {
                return object;
            }
        }
        for (int i = 2; i < len; i++) {
            if (object.serviceTreeMap.containsKey(servicePath[i])) {
                object = object.serviceTreeMap.get(servicePath[i]);
                continue;
            } else {
                return null;
            }
        }
        return object;
    }

    // setter
    public void setIP4Address(String ip) {
        ip4Address = ip;
    }

    public void setGatewayPassword(String password) {
        gatewayPassword = password;
        recreateKeys();
    }

    public void setPrivatePassword(String password) {
        privatePassword = password;
        recreateKeys();
    }

    public void setMD5Salt(String salt) {
        MD5Salt = DatatypeConverter.parseHexBinary(salt);
        recreateKeys();
    }

    public void setCryptKeyPriv(String key) {
        cryptKeyPriv = DatatypeConverter.parseHexBinary(key);
    }

    public void setCharSet(String charset) {
        charSet = charset;
    }

    public void setInited(Boolean Init) {
        inited = Init;
    }

}
