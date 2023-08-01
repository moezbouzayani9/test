package com.valuephone.image.security;

import io.undertow.security.idm.Account;

import java.security.Principal;
import java.util.Collections;
import java.util.Set;

/**
 * @author tcigler
 * @since 1.0
 */
public class SimpleImageAccount implements Account {

    private static final long serialVersionUID = 478556034618124175L;

    private final ImageUserPrincipal principal;

    SimpleImageAccount(final ImageUserPrincipal principal) {
        this.principal = principal;
    }

    @Override
    public Principal getPrincipal() {
        return principal;
    }

    @Override
    public Set<String> getRoles() {
        return Collections.emptySet();
    }
}
