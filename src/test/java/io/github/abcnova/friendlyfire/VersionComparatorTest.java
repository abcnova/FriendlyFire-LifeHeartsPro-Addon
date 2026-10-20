package io.github.abcnova.friendlyfire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VersionComparatorTest {
    @Test
    void comparesNumericReleaseSegments() {
        assertTrue(VersionComparator.compare("1.1.10", "1.1.9") > 0);
        assertTrue(VersionComparator.compare("1.2.0", "1.1.2") > 0);
        assertEquals(0, VersionComparator.compare("v1.2.0", "1.2"));
    }

    @Test
    void readsTheVersionFromTheModrinthVersionResponse() {
        String response = "[{\"name\":\"FriendlyFire\",\"version_number\":\"1.2.0\",\"files\":[]}]";
        assertEquals("1.2.0", UpdateChecker.parseLatestVersion(response));
        assertNull(UpdateChecker.parseLatestVersion("[]"));
        assertNull(UpdateChecker.parseLatestVersion(null));
    }
}
