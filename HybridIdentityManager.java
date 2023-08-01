package com.valuephone.image.security;

import com.valuephone.image.utilities.DatabaseManager;
import com.valuephone.image.utilities.SecurityUtil;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import lombok.extern.slf4j.Slf4j;

import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
public class HybridIdentityManager implements IdentityManager {

    private static final String SALT = "S9mkgIr6";
    private final DatabaseManager serverDatabaseManager;
    private String staticUsername;
    private String staticPasswordHash;


    public HybridIdentityManager(DatabaseManager serverDatabaseManager, String staticUsername, String staticPasswordHash) {
        this.serverDatabaseManager = serverDatabaseManager;
        this.staticUsername = staticUsername;
        this.staticPasswordHash = staticPasswordHash;
    }

    @Override
    public Account verify(Account account) {
        return account;
    }

    @Override
    public Account verify(String id, Credential credential) {
        return innerVerify(getPrincipalFromDB(id, credential), verifyStaticCredential(id, credential), id);
    }

    protected Account innerVerify(Principal dbPrincipal, boolean staticVerifyResult, String id){
        HybridImageAccount account = new HybridImageAccount(
                    dbPrincipal != null ? dbPrincipal : ()->id,
                staticVerifyResult ? HybridImageAccount.STATIC_USER_FLAG : 0,
                dbPrincipal == null ? 0 : HybridImageAccount.USER_FLAG);

        return account.isValid() ? account : null;
    }

    protected Principal getPrincipalFromDB(String id, Credential credential){
        try (
                Connection connection = serverDatabaseManager.getConnection();
                final PreparedStatement preparedStatement = connection.prepareStatement("SELECT id, username, password_hash, client_type FROM image_users_vw WHERE username = ?")
        ) {

            preparedStatement.setString(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {

                if (!resultSet.next()) {
                    return null;
                }

                final String passwordHash = resultSet.getString("password_hash");

                if (verifyCredential(passwordHash, credential)) {

                    final ImageUserPrincipal principal = new ImageUserPrincipal(
                            resultSet.getInt("id"),
                            resultSet.getString("username"),
                            resultSet.getString("client_type")
                    );


                    return principal;

                } else {
                    return null;
                }

            }

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException("SQL Exception!");
        }
    }

    @Override
    public Account verify(Credential credential) {
        return null;
    }

    protected boolean verifyCredential(String passwordHash, Credential credential) {
        if (credential instanceof PasswordCredential) {
            char[] passwordCharArray = ((PasswordCredential) credential).getPassword();

            if (passwordCharArray.length > 0) {
                final String password = new String(passwordCharArray);

                return SecurityUtil.isPasswordHashValid(passwordHash, password) || // Standard users
                        SecurityUtil.encodePassword(password).equals(passwordHash) // Mobile registrations
                        ;
            } else {
                return false;
            }
        }
        return false;
    }

    protected boolean verifyStaticCredential(String principalName, Credential credential) {
        if (credential instanceof PasswordCredential) {
            char[] providedPass = ((PasswordCredential) credential).getPassword();

            final String saltedPassword = SALT + String.valueOf(providedPass);

            final String hashedPassword = SecurityUtil.sha256sum(saltedPassword);

            return this.staticUsername.equals(principalName) && this.staticPasswordHash.equals(hashedPassword);
        }
        return false;
    }

}
