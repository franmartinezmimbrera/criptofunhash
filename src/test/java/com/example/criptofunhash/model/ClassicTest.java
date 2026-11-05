package com.example.criptofunhash.model;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassicTest {

    private static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void knownVectors() {
        assertEquals("900150983cd24fb0d6963f7d28e17f72", Hex.of(Md5.INSTANCE.digest(utf8("abc"))));
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", Hex.of(Md5.INSTANCE.digest(new byte[0])));
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", Hex.of(Sha1.INSTANCE.digest(utf8("abc"))));
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", Hex.of(Sha1.INSTANCE.digest(new byte[0])));
    }

    @Test
    void matchesJdkForAllLengths() throws Exception {
        Random random = new Random(3);
        for (HashAlgorithm a : new HashAlgorithm[]{Md5.INSTANCE, Sha1.INSTANCE}) {
            MessageDigest reference = MessageDigest.getInstance(a.javaName());
            for (int len = 0; len <= 300; len++) {
                byte[] msg = new byte[len];
                random.nextBytes(msg);
                assertArrayEquals(reference.digest(msg), a.digest(msg), a.displayName() + " len=" + len);
            }
        }
    }

    @Test
    void reportsContainDigestAndSteps() {
        byte[] msg = utf8("Hola mundo");
        String md5 = Hex.of(Md5.INSTANCE.digest(msg)).toUpperCase(Locale.ROOT);
        String sha1 = Hex.of(Sha1.INSTANCE.digest(msg)).toUpperCase(Locale.ROOT);
        assertTrue(ClassicReport.md5(msg, false).contains(md5));
        assertTrue(ClassicReport.sha1(msg, false).contains(sha1));
        String md5Steps = ClassicReport.md5(msg, true);
        assertTrue(md5Steps.contains("Paso 64") && md5Steps.contains(md5));
        String sha1Steps = ClassicReport.sha1(msg, true);
        assertTrue(sha1Steps.contains("Paso 79") && sha1Steps.contains("W[79]"));
        assertTrue(ClassicReport.md5(msg, false).contains("67452301 EFCDAB89 98BADCFE 10325476"));
        assertTrue(ClassicReport.md5(msg, false).contains("01234567 89ABCDEF FEDCBA98 76543210"));
        assertTrue(ClassicReport.sha1(new byte[100_000], false).contains("se omiten los bloques"));
    }

    @Test
    void catalogIncludesObsoleteAlgorithms() {
        assertTrue(Algorithms.all().contains(Md5.INSTANCE) && Algorithms.all().contains(Sha1.INSTANCE));
        assertEquals(Sha2Variant.SHA_256, Algorithms.all().get(Algorithms.defaultIndex()));
    }
}
