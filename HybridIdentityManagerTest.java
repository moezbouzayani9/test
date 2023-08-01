package com.valuephone.image.security;

import io.undertow.security.idm.Credential;
import io.undertow.security.idm.PasswordCredential;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HybridIdentityManagerTest {

    String id="id";
    String pass="asdf";
    String hashedPass="e9c2a1e5b64dd294ef58fa7811418da0c93f792b3a9fe99fc99a2216983497f4";
    String hashedOldPass="912ec803b2ce49e4a541068d495ab570";
    String hashedNewPass="QBsJ6rPAE9TKVJIruAK+yP1TGBkrCnXyAdizcnQpCA+zN1kavT5ERTuVRVW3oIEuEIHDm3QCk/dl6ucx9aZe0Q==";
    Credential credential = new PasswordCredential(pass.toCharArray());

    @Test
    public void testUsersVerify(){
        HybridIdentityManager identManager = new HybridIdentityManager(null, id, hashedPass);

        assertNotNull(identManager.innerVerify(new ImageUserPrincipal(1, id, "type"), false, id));
        assertNotNull(identManager.innerVerify(null, true, null));
        assertNull(identManager.innerVerify(null, false, null));

        assertTrue(identManager.verifyStaticCredential(id, credential));
        assertTrue(identManager.verifyCredential(hashedOldPass, credential));
        assertTrue(identManager.verifyCredential(hashedNewPass, credential));
    }
}
