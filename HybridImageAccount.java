package com.valuephone.image.security;

import io.undertow.security.idm.Account;

import java.security.Principal;
import java.util.Collections;
import java.util.Set;

/**
 * @author tcigler
 * @since 1.0
 */
public class HybridImageAccount implements Account {

    private static final long serialVersionUID = 478556034618124175L;

    private final Principal principal;
    private int types = 0;

    public final static int STATIC_USER_FLAG=1;
    public final static int USER_FLAG=2;

    HybridImageAccount(final Principal principal, int ...types ) {
        this.principal = principal;
        for (int type : types) {
            this.types|=type;
        }
    }

    @Override
    public Principal getPrincipal() {
        return principal;
    }

    @Override
    public Set<String> getRoles() {
        return Collections.emptySet();
    }

    public boolean isStaticUser(){
        return (types & STATIC_USER_FLAG) == STATIC_USER_FLAG;
    }

    public boolean isUser(){
        return (types & USER_FLAG) == USER_FLAG;
    }

    public boolean isValid(){
        return types != 0;
    }

}
