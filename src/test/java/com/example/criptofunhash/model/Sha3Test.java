package com.example.criptofunhash.model;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Sha3Test {

    @Test
    void sha3_256KnownVectors() {
        assertEquals("3a985da74fe225b2045c172d6bd390bd855f086e3e9d525b46bfe24511431532",
                Hex.of(Sha3Variant.SHA3_256.digest("abc".getBytes(StandardCharsets.UTF_8))));
        assertEquals("a7ffc6f8bf1ed76651c14756a061d662f580ff4de43b49fa82d80a4b80f8434a",
                Hex.of(Sha3Variant.SHA3_256.digest(new byte[0])));
    }

    @Test
    void matchesJdkForAllLengthsAroundRateBoundaries() throws Exception {
        Random random = new Random(2);
        for (Sha3Variant v : Sha3Variant.values()) {
            MessageDigest reference = null;
            try {
                reference = MessageDigest.getInstance(v.javaName());
            } catch (java.security.NoSuchAlgorithmException e) {
                // SHA-3 no existe en el JDK hasta Java 9: sin referencia no se puede comparar.
                Assumptions.assumeTrue(false, v.javaName() + " no disponible en este JDK");
            }
            for (int len = 0; len <= 450; len++) {
                byte[] msg = new byte[len];
                random.nextBytes(msg);
                byte[] expected = reference.digest(msg);
                assertArrayEquals(expected, v.digest(msg), v + " len=" + len);
                assertArrayEquals(expected, Sha3.trace(v, msg).digest, v + " trace len=" + len);
            }
        }
    }

    @Test
    void ratesAndCapacities() {
        assertEquals(144, Sha3Variant.SHA3_224.rateBytes());
        assertEquals(136, Sha3Variant.SHA3_256.rateBytes());
        assertEquals(104, Sha3Variant.SHA3_384.rateBytes());
        assertEquals(72, Sha3Variant.SHA3_512.rateBytes());
        assertEquals(512, Sha3Variant.SHA3_256.capacityBits());
    }

    @Test
    void keccakConstants() {
        assertEquals(0x0000000000000001L, Keccak.roundConstant(0));
        assertEquals(0x0000000000008082L, Keccak.roundConstant(1));
        assertEquals(0x8000000080008008L, Keccak.roundConstant(23));
        assertEquals(0, Keccak.rotationOffset(0, 0));
        assertEquals(1, Keccak.rotationOffset(1, 0));
        assertEquals(36, Keccak.rotationOffset(0, 1));
        assertEquals(14, Keccak.rotationOffset(4, 4));
    }

    @Test
    void paddingIsPad10star1WithDomainSuffix() {
        Sha3Trace t = Sha3.trace(Sha3Variant.SHA3_256, new byte[]{1, 2, 3});
        assertEquals(136, t.padded.length);
        assertEquals((byte) 0x06, t.padded[3]);
        assertEquals((byte) 0x80, t.padded[135]);
        // si falta solo un byte, el sufijo y el 1 final comparten byte: 0x86
        Sha3Trace one = Sha3.trace(Sha3Variant.SHA3_256, new byte[135]);
        assertEquals((byte) 0x86, one.padded[135]);
        assertEquals(1, one.blocks.size());
        assertEquals(2, Sha3.trace(Sha3Variant.SHA3_256, new byte[136]).blocks.size());
    }

    @Test
    void permutationTraceEndsAtFinalState() {
        Sha3Trace t = Sha3.trace(Sha3Variant.SHA3_256, new byte[]{42});
        Sha3Trace.Block b = t.blocks.get(0);
        assertArrayEquals(b.after, b.permutation.output());
        assertArrayEquals(b.afterXor, b.permutation.stateAt(0));
        assertArrayEquals(b.permutation.iota[23], b.permutation.stateAt(120));
        assertArrayEquals(b.permutation.chi[5], b.permutation.stateAt(5 * 5 + 4));
    }
}
