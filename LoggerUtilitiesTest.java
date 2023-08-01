package com.valuephone.image.utilities;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lbalint
 * @since 1.0$
 */
class LoggerUtilitiesTest {

    @Test
    void test() {

        final Logger logger = LoggerFactory.getLogger(LoggerUtilitiesTest.class);
        logger.info("It Works!");
    }
}
