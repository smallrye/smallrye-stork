package io.smallrye.stork.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NoAcceptableServiceInstanceFoundExceptionTest {

    @Test
    void canCreateNoAcceptableServiceInstanceFoundException() {
        NoAcceptableServiceInstanceFoundException exception = new NoAcceptableServiceInstanceFoundException("missing");
        Assertions.assertEquals("missing", exception.getMessage());
        Assertions.assertNull(exception.getCause());

        Exception cause = new ArithmeticException("boom");
        exception = new NoAcceptableServiceInstanceFoundException("missing", cause);
        Assertions.assertEquals("missing", exception.getMessage());
        Assertions.assertEquals(cause, exception.getCause());
    }

}