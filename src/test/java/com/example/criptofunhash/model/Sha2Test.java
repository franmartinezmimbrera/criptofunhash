package com.example.criptofunhash.model;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Sha2Test {

    private static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void sha256KnownVectors() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                Hex.of(Sha2Variant.SHA_256.digest(utf8("abc"))));
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                Hex.of(Sha2Variant.SHA_256.digest(new byte[0])));
    }

    @Test
    void sha512KnownVector() {
        assertEquals("ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a"
                        + "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f",
                Hex.of(Sha2Variant.SHA_512.digest(utf8("abc"))));
    }

    @Test
    void matchesJdkForAllLengthsAroundBlockBoundaries() throws Exception {
        Random random = new Random(1);
        for (Sha2Variant v : Sha2Variant.values()) {
            MessageDigest reference = MessageDigest.getInstance(v.javaName());
            for (int len = 0; len <= 300; len++) {
                byte[] msg = new byte[len];
                random.nextBytes(msg);
                byte[] expected = reference.digest(msg);
                assertArrayEquals(expected, v.digest(msg), v + " len=" + len);
                assertArrayEquals(expected, Sha2.trace(v, msg).digest, v + " trace len=" + len);
            }
        }
    }

    @Test
    void paddingStructure() {
        byte[] msg = utf8("Hola mundo");
        Sha2Trace t = Sha2.trace(Sha2Variant.SHA_256, msg);
        assertEquals(64, t.padded.length);
        assertEquals(1, t.blocks.size());
        assertEquals((byte) 0x80, t.padded[msg.length]);
        assertEquals(msg.length * 8, t.padded[63] & 0xFF);
        assertEquals(64 - msg.length - 1 - 8, t.zeroBytes);

        // 56 bytes obligan a un segundo bloque
        assertEquals(2, Sha2.trace(Sha2Variant.SHA_256, new byte[56]).blocks.size());
        assertEquals(1, Sha2.trace(Sha2Variant.SHA_256, new byte[55]).blocks.size());
        // SHA-512: campo de longitud de 128 bits
        assertEquals(128, Sha2.trace(Sha2Variant.SHA_512, new byte[111]).padded.length);
        assertEquals(256, Sha2.trace(Sha2Variant.SHA_512, new byte[112]).padded.length);
    }

    @Test
    void traceIsConsistent() {
        Sha2Trace t = Sha2.trace(Sha2Variant.SHA_256, utf8("Hola mundo"));
        Sha2Trace.Block b = t.blocks.get(0);
        assertEquals(64, b.w.length);
        assertEquals(0x486f6c61L, b.w[0]); // "Hola"
        assertEquals(65, b.regs.length);
        long mask = 0xFFFFFFFFL;
        for (int i = 0; i < 8; i++) {
            assertEquals((b.hIn[i] + b.regs[64][i]) & mask, b.hOut[i]);
        }
        // la nueva 'a' es T1 + T2 y la nueva 'e' es d + T1
        assertEquals((b.t1[0] + b.t2[0]) & mask, b.regs[1][0]);
        assertEquals((b.regs[0][3] + b.t1[0]) & mask, b.regs[1][4]);
    }

    @Test
    void derivedConstantsAreTheStandardOnes() {
        assertEquals(0x6a09e667L, Sha2Variant.SHA_256.initialHash()[0]);
        assertEquals(0xc1059ed8L, Sha2Variant.SHA_224.initialHash()[0]);
        assertEquals(0xcbbb9d5dc1059ed8L, Sha2Variant.SHA_384.initialHash()[0]);
        assertEquals(0x428a2f98L, Sha2Variant.SHA_256.roundConstants()[0]);
        assertEquals(0x6c44198c4a475817L, Sha2Variant.SHA_512.roundConstants()[79]);
    }
}
