package com.example.criptofunhash.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvalancheTest {

    @Test
    void flipBitUsesMostSignificantBitFirst() {
        assertArrayEquals(new byte[]{(byte) 0x80, 0}, Avalanche.flipBit(new byte[]{0, 0}, 0));
        assertArrayEquals(new byte[]{0, 0x01}, Avalanche.flipBit(new byte[]{0, 0}, 15));
        byte[] original = {5};
        Avalanche.flipBit(original, 0);
        assertEquals(5, original[0]);
    }

    @Test
    void distanceCountsDifferentBits() {
        assertEquals(0, Avalanche.distance(new byte[]{1, 2}, new byte[]{1, 2}));
        assertEquals(8, Avalanche.distance(new byte[]{0x00}, new byte[]{(byte) 0xFF}));
        assertEquals(1, Avalanche.distance(new byte[]{0x10, 0}, new byte[]{0x00, 0}));
    }

    @Test
    void oneBitChangeFlipsAboutHalfTheDigest() {
        byte[] msg = "No se ganó Zamora en una hora".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        for (HashAlgorithm algorithm : Algorithms.all()) {
            Avalanche.Stats stats = Avalanche.overAllBits(algorithm, msg);
            assertEquals(msg.length * 8, stats.trials);
            assertTrue(stats.meanPercent > 45 && stats.meanPercent < 55, algorithm.displayName() + " " + stats.meanPercent);
        }
    }

    @Test
    void hexParsing() {
        assertArrayEquals(new byte[]{(byte) 0xC1, 0x5A, 0x5B, (byte) 0x9B}, Hex.parse("c1 5a 5b 9b"));
        assertArrayEquals(new byte[]{0x0F, (byte) 0xAB}, Hex.parse("0x0f,0xAB"));
        assertThrows(IllegalArgumentException.class, () -> Hex.parse("abc"));
        assertThrows(IllegalArgumentException.class, () -> Hex.parse("zz"));
        assertEquals("0000ff", Hex.word(0xFFL, 24));
    }

    @Test
    void encodingsDifferForNonAsciiCharacters() {
        byte[] utf8 = TextEncoding.UTF_8.encode("ó");
        byte[] latin1 = TextEncoding.LATIN_1.encode("ó");
        assertArrayEquals(new byte[]{(byte) 0xC3, (byte) 0xB3}, utf8);
        assertArrayEquals(new byte[]{(byte) 0xF3}, latin1);
        assertTrue(!TextEncoding.US_ASCII.canEncode("ó"));
        assertTrue(TextEncoding.LATIN_1.canEncode("ó"));
    }
}
