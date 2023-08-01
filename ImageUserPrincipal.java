package com.valuephone.image.security;

import java.io.Serializable;
import java.security.Principal;

/**
 * @author tcigler
 * @since 1.0
 */
public class ImageUserPrincipal implements Principal, Serializable {

    private static final long serialVersionUID = 8793129129643246555L;

    private final int userId;
    private final String username;
    private final String clientType;

    public ImageUserPrincipal(int userId, String username, final String clientType) {

        this.userId = userId;
        this.username = username;

        this.clientType = clientType;
    }

    @Override
    public String getName() {
        return username;
    }

    public int getUserId() {
        return userId;
    }

    public String getClientType() {
        return clientType;
    }

}
