package com.example.criptofunhash.model;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportTest {

    private static byte[] random(int n) {
        byte[] b = new byte[n];
        new Random(n).nextBytes(b);
        return b;
    }

    @Test
    void walkGivesSameDigestAsDirectComputation() {
        for (int len : new int[]{0, 1, 55, 56, 64, 135, 136, 137, 1000}) {
            byte[] msg = random(len);
            for (Sha2Variant v : Sha2Variant.values()) {
                assertArrayEquals(v.digest(msg), Sha2.digestOf(v, Sha2.walk(v, msg, i -> false, b -> { })), v + " " + len);
            }
            for (Sha3Variant v : Sha3Variant.values()) {
                assertArrayEquals(v.digest(msg), Sha3.digestOf(v, Sha3.walk(v, msg, i -> false, b -> { })), v + " " + len);
            }
        }
    }

    @Test
    void walkOnlyVisitsTracedBlocks() {
        final int[] visited = {0};
        Sha2.walk(Sha2Variant.SHA_256, random(1000), i -> i % 5 == 0, b -> visited[0]++);
        assertTrue(visited[0] == 4); // 16 bloques: 0, 5, 10, 15
        assertTrue(Sha2.blockCount(Sha2Variant.SHA_256, 1000) == 16);
    }

    @Test
    void sha2ReportContainsDigestAndSteps() {
        byte[] msg = "Hola mundo".getBytes(StandardCharsets.UTF_8);
        String digest = Hex.of(Sha2Variant.SHA_256.digest(msg)).toUpperCase(Locale.ROOT);
        String blocks = Sha2Report.build(Sha2Variant.SHA_256, msg, false);
        String steps = Sha2Report.build(Sha2Variant.SHA_256, msg, true);
        assertTrue(blocks.contains(digest));
        assertTrue(steps.contains(digest));
        assertTrue(blocks.contains("Procesamiento del Bloque 1"));
        assertTrue(steps.contains("Ronda 63"));
        assertTrue(steps.contains("W[63]"));
        assertTrue(!blocks.contains("Ronda 63"));
        assertTrue(blocks.contains("486F6C61")); // "Hola"
    }

    @Test
    void sha3ReportContainsDigestAndRounds() {
        byte[] msg = "Hola mundo".getBytes(StandardCharsets.UTF_8);
        String digest = Hex.of(Sha3Variant.SHA3_256.digest(msg)).toUpperCase(Locale.ROOT);
        assertTrue(Sha3Report.build(Sha3Variant.SHA3_256, msg, false).contains(digest));
        String steps = Sha3Report.build(Sha3Variant.SHA3_256, msg, true);
        assertTrue(steps.contains(digest));
        assertTrue(steps.contains("Ronda 23"));
    }

    @Test
    void reportsHandleEmptyAndHugeMessages() {
        for (Sha2Variant v : Sha2Variant.values()) {
            assertTrue(Sha2Report.build(v, new byte[0], false).contains("Procesamiento del Bloque 1"));
        }
        String big = Sha2Report.build(Sha2Variant.SHA_256, random(100_000), false);
        assertTrue(big.contains("se omiten los bloques"));
        assertTrue(big.length() < 400_000);
        String big3 = Sha3Report.build(Sha3Variant.SHA3_256, random(100_000), false);
        assertTrue(big3.contains("se omiten los bloques"));
    }
}
