/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.lgthinq.api;

import static org.openhab.binding.lgthinq.internal.LGThinqBindingConstants.GATEWAY_URL;
import static org.openhab.binding.lgthinq.internal.LGThinqBindingConstants.THINQ_CONNECTION_DATA_FILE;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import org.openhab.binding.lgthinq.errors.*;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The {@link TokenManager} Principal facade to manage all token handles
 *
 * @author Nemer Daud - Initial contribution
 */
public class TokenManager {
    private static final int EXPIRICY_TOLERANCE_SEC = 60;
    private final OauthLgEmpAuthenticator oAuthAuthenticator;
    private TokenResult tokenCached;
    private static final TokenManager instance;
    static {
        instance = new TokenManager();
    }

    private TokenManager() {
        oAuthAuthenticator = OauthLgEmpAuthenticator.getInstance();
    }

    public static TokenManager getInstance() {
        return instance;
    }

    public boolean isTokenExpired(TokenResult token) {
        Calendar c = Calendar.getInstance();
        c.setTime(token.getGeneratedTime());
        c.add(Calendar.SECOND, token.getExpiresIn() - EXPIRICY_TOLERANCE_SEC);
        Date expiricyDate = c.getTime();
        return expiricyDate.before(new Date());
    }

    public TokenResult refreshToken(String bridgeName, TokenResult currentToken) throws RefreshTokenException {
        try {
            TokenResult token = oAuthAuthenticator.doRefreshToken(currentToken);
            new ObjectMapper().writeValue(new File(getConfigDataFileName(bridgeName)), token);
            return token;
        } catch (IOException e) {
            throw new RefreshTokenException("Error refreshing LGThinq token", e);
        }
    }

    private String getConfigDataFileName(String bridgeName) {
        return String.format(THINQ_CONNECTION_DATA_FILE, bridgeName);
    }

    public boolean isOauthTokenRegistered(String bridgeName) {
        File tokenFile = new File(getConfigDataFileName(bridgeName));
        // TODO - check if the file content is valid.
        return tokenFile.isFile();
    }

    public TokenResult oauthFirstRegistration(String bridgeName, String language, String country, String username,
            String password)
            throws LGGatewayException, PreLoginException, AccountLoginException, TokenException, IOException {
        Gateway gw;
        OauthLgEmpAuthenticator.PreLoginResult preLogin;
        OauthLgEmpAuthenticator.LoginAccountResult accountLogin;
        TokenResult token;
        UserInfo userInfo;
        try {
            gw = oAuthAuthenticator.discoverGatewayConfiguration(GATEWAY_URL, language, country);
        } catch (Exception ex) {
            throw new LGGatewayException("Error trying to discovery the LG Gateway Setting for the region informed",
                    ex);
        }
        try {
            preLogin = oAuthAuthenticator.preLoginUser(gw, username, password);
        } catch (Exception ex) {
            throw new PreLoginException("Error doing pre-login of the user in the Emp LG Server", ex);
        }
        try {
            accountLogin = oAuthAuthenticator.loginUser(gw, preLogin);
        } catch (Exception ex) {
            throw new AccountLoginException("Error doing user's account login on the Emp LG Server", ex);
        }
        try {
            token = oAuthAuthenticator.getToken(gw, accountLogin);
        } catch (Exception ex) {
            throw new TokenException("Error getting Token", ex);
        }
        try {
            userInfo = oAuthAuthenticator.getUserInfo(token);
            token.setUserInfo(userInfo);
            token.setGatewayInfo(gw);
        } catch (Exception ex) {
            throw new TokenException("Error getting UserInfo from Token", ex);
        }

        // persist the token information generated in file
        new ObjectMapper().writeValue(new File(getConfigDataFileName(bridgeName)), token);
        return token;
    }

    public TokenResult getValidRegisteredToken(String bridgeName) throws IOException, RefreshTokenException {
        if (tokenCached == null) {
            tokenCached = new ObjectMapper().readValue(new File(getConfigDataFileName(bridgeName)), TokenResult.class);
        }
        if (isTokenExpired(tokenCached)) {
            tokenCached = refreshToken(bridgeName, tokenCached);
        }
        return tokenCached;
    }

    /**
     * Remove the toke file registered for the bridge. Must be called only if the bridge is removed
     */
    public void cleanupTokenRegistry(String bridgeName) {
        File f = new File(getConfigDataFileName(bridgeName));
        if (f.isFile()) {
            f.delete();
        }
    }
}
