package com.valuephone.image.handler;

import com.valuephone.image.security.ImageUserPrincipal;
import com.valuephone.image.security.SimpleImageAccount;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * @author tcigler
 * @since 1.0
 */
public abstract class AuthenticatedUserWrapperHandler implements HttpHandler {
    private static ImageUserPrincipal getUserPrincipal(final HttpServerExchange exchange) {
        final SecurityContext securityContext = exchange.getSecurityContext();

        final Account account = securityContext.getAuthenticatedAccount();

        if (!(account instanceof SimpleImageAccount)) {
            throw new SecurityException("Authenticated account not recognized");
        }

        return (ImageUserPrincipal) account.getPrincipal();
    }

    @Override
    public final void handleRequest(final HttpServerExchange exchange) throws Exception {

        handleRequestAuthenticated(exchange, getUserPrincipal(exchange));

    }

    protected abstract void handleRequestAuthenticated(final HttpServerExchange exchange, ImageUserPrincipal principal) throws Exception;

}
