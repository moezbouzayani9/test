package com.valuephone.image.handler;

import com.valuephone.image.security.HybridImageAccount;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.security.Principal;

/**
 * @author tcigler
 * @since 1.0
 */
public abstract class HybridAuthenticatedWrapperHandler implements HttpHandler {

    private static HybridImageAccount getUserAccount(final HttpServerExchange exchange) {
        final SecurityContext securityContext = exchange.getSecurityContext();

        final Account account = securityContext.getAuthenticatedAccount();

        if(account == null){
            throw new SecurityException("unauthorized");
        }
        if (!(account instanceof HybridImageAccount)) {
            throw new SecurityException("Authenticated account not recognized");
        }

        return (HybridImageAccount) account;
    }

    @Override
    public final void handleRequest(final HttpServerExchange exchange) throws Exception {

        HybridImageAccount account = getUserAccount(exchange);

        handleRequestAuthenticated(exchange, account.getPrincipal(), account);

    }

    protected abstract void handleRequestAuthenticated(final HttpServerExchange exchange, Principal principal, HybridImageAccount account) throws Exception;

}
