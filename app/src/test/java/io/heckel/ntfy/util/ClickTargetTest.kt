package io.heckel.ntfy.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClickTargetTest {
    @Test fun usableLinksAreFollowed() {
        assertEquals("hermes://chat/lead-dev/api_1#42", clickTarget(" hermes://chat/lead-dev/api_1#42 "))
        assertEquals("https://example.com/x", clickTarget("https://example.com/x"))
    }

    @Test fun blankOrBrokenOpensTheTopic() {
        assertNull(clickTarget(""))
        assertNull(clickTarget("not a link"))
        assertNull(clickTarget("hermes:"))
    }
}
