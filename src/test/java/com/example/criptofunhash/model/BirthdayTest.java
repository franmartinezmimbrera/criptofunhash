package com.example.criptofunhash.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirthdayTest {

    @Test
    void probabilityOf23PeopleIsAboutHalf() {
        assertEquals(0.507, Birthday.probability(23, 365), 0.001);
        assertEquals(0.0, Birthday.probability(1, 365), 1e-12);
        assertEquals(1.0, Birthday.probability(366, 365), 1e-12);
    }

    @Test
    void daysRoundEndsWithExactlyOneRepeat() {
        Random random = new Random(7);
        for (int i = 0; i < 200; i++) {
            Birthday.Round r = Birthday.days(random, 365);
            assertTrue(r.attempts >= 2 && r.attempts <= 366);
            assertEquals(r.attempts, r.days.length);
            assertEquals(r.days[r.first], r.days[r.second]);
            assertEquals(r.attempts - 1, r.second);
            assertEquals(r.attempts - 1, Arrays.stream(r.days).distinct().count());
        }
    }

    @Test
    void meanAttemptsMatchesTheory() {
        Random random = new Random(11);
        double sum = 0;
        int rounds = 5000;
        for (int i = 0; i < rounds; i++) {
            sum += Birthday.days(random, 365).attempts;
        }
        assertEquals(Birthday.expectedAttempts(365), sum / rounds, 0.6);
    }

    @Test
    void hashRoundFindsRealTruncatedCollision() {
        Random random = new Random(5);
        for (HashAlgorithm a : new HashAlgorithm[]{Md5.INSTANCE, Sha1.INSTANCE, Sha2Variant.SHA_256, Sha3Variant.SHA3_256}) {
            Birthday.Round r = Birthday.hash(a, 16, random);
            assertFalse(r.aborted);
            assertFalse(Arrays.equals(r.messageA, r.messageB));
            assertEquals(Birthday.prefix(r.digestA, 16), Birthday.prefix(r.digestB, 16));
            assertEquals(r.key, Birthday.prefix(r.digestA, 16));
            assertTrue(Arrays.equals(a.digest(r.messageA), r.digestA));
        }
    }

    @Test
    void prefixTakesLeadingBits() {
        assertEquals(0b1010, Birthday.prefix(new byte[]{(byte) 0xA5}, 4));
        assertEquals(0xA5F, Birthday.prefix(new byte[]{(byte) 0xA5, (byte) 0xF0}, 12));
    }
}
