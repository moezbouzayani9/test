package com.valuephone.image.security;

import com.valuephone.image.utilities.SecurityUtil;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;

import java.security.Principal;
import java.util.Collections;
import java.util.Set;

/**
 * @author tcigler
 * @since 1.0
 */
public class SingleIdentityManager implements IdentityManager {

    private final String username;
    private static final String SALT = "S9mkgIr6";
    private final String password;

    public SingleIdentityManager(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public Account verify(Account account) {
        return account;
    }

    @Override
    public Account verify(String id, Credential credential) {
        Account account = getAccount(id);
        if (account != null && verifyCredential(account, credential)) {
            return account;
        }

        return null;
    }

    @Override
    public Account verify(Credential credential) {
        return null;
    }

    private boolean verifyCredential(Account account, Credential credential) {
        if (credential instanceof PasswordCredential) {
            char[] providedPass = ((PasswordCredential) credential).getPassword();
            final String principalName = account.getPrincipal().getName();

            final String saltedPassword = SALT + String.valueOf(providedPass);

            final String hashedPassword = SecurityUtil.sha256sum(saltedPassword);

            return this.username.equals(principalName) && this.password.equals(hashedPassword);
        }
        return false;
    }

    private Account getAccount(final String id) {
        if (username.equals(id)) {
            return new Account() {

                private final Principal principal = () -> id;

                @Override
                public Principal getPrincipal() {
                    return principal;
                }

                @Override
                public Set<String> getRoles() {
                    return Collections.emptySet();
                }

            };
        }
        return null;
    }


}
