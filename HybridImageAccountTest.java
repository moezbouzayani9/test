package com.valuephone.image.security;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HybridImageAccountTest {

    @Test
    public void testAccountTypesAndValidity(){
        HybridImageAccount account = new HybridImageAccount(null, 0);
        assertFalse(account.isValid());

        account = new HybridImageAccount(null, HybridImageAccount.USER_FLAG);
        assertFalse(account.isStaticUser());
        assertTrue(account.isUser());
        assertTrue(account.isValid());

        account = new HybridImageAccount(null, HybridImageAccount.STATIC_USER_FLAG);
        assertTrue(account.isStaticUser());
        assertFalse(account.isUser());
        assertTrue(account.isValid());

        account = new HybridImageAccount(null, HybridImageAccount.USER_FLAG, HybridImageAccount.STATIC_USER_FLAG);
        assertTrue(account.isStaticUser());
        assertTrue(account.isUser());
        assertTrue(account.isValid());
    }
}
