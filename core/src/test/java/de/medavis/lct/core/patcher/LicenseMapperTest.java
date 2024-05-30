package de.medavis.lct.core.patcher;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LicenseMapperTest {

    @Test
    void create() {

        LicenseMapper mapper = LicenseMapper.create();

        assertTrue(mapper.mapIdByPURL("TriTraTrullalla").isEmpty());
        assertTrue(mapper.mapIdByUrl("TriTraTrullalla").isEmpty());
        assertTrue(mapper.patchId("TriTraTrullalla").isEmpty());
        assertTrue(mapper.patchName("TriTraTrullalla").isEmpty());

        assertEquals("Apache-2.0", mapper.mapIdByPURL("pkg:maven/com.github.kenglxn.qrgen/core@2.6.0?type=jar").get());
        assertEquals("CC0-1.0", mapper.mapIdByUrl("https://creativecommons.org/publicdomain/zero/1.0/").get());
        assertEquals("MIT", mapper.patchId("Lesser General Public License (LGPL)").get());
        assertEquals("Apache License 2.0", mapper.patchName("Apache License, 2.0").get());

        mapper.validateRules(SpdxLicenseManager.create(null));

    }
}