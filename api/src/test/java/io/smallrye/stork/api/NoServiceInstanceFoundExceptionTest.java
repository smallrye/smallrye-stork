package io.smallrye.stork.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NoServiceInstanceFoundExceptionTest {

    @Test
    void canCreateNoServiceInstanceFoundException() {
        NoServiceInstanceFoundException exception = new NoServiceInstanceFoundException("missing");
        Assertions.assertEquals("missing", exception.getMessage());
        Assertions.assertNull(exception.getCause());

        Exception cause = new ArithmeticException("boom");
        exception = new NoServiceInstanceFoundException("missing", cause);
        Assertions.assertEquals("missing", exception.getMessage());
        Assertions.assertEquals(cause, exception.getCause());
    }

}