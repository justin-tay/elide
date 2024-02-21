/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.verify;

import static com.github.stefanbirkner.systemlambda.SystemLambda.catchSystemExit;
import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErr;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.Base64;

public class DynamicConfigVerifierTest {

    private static KeyPair kp;
    private static String signature;
    private static final String TAR_FILE_PATH = "src/test/resources/configs.tar.gz";
    private static final String SIGNATURE_FILE_PATH = "src/test/resources/configs.tar.gz.sig";
    private static final String INVALID_TAR_FILE_PATH = "src/test/resources/invalid.tar.gz";

    @BeforeAll
    public static void setUp() throws Exception {
        createTarGZ();
        kp = generateKeyPair();
        signature = sign(TAR_FILE_PATH, kp.getPrivate());
        Files.writeString(Path.of(SIGNATURE_FILE_PATH), signature);
        Files.writeString(Path.of(INVALID_TAR_FILE_PATH), "invalid-signature");
    }

    @AfterAll
    public static void after() {
        try {
            Files.deleteIfExists(Paths.get(TAR_FILE_PATH));
        } catch (IOException e) {
            // Do nothing
        }
        try {
            Files.deleteIfExists(Paths.get(SIGNATURE_FILE_PATH));
        } catch (IOException e) {
            // Do nothing
        }
        try {
            Files.deleteIfExists(Paths.get(INVALID_TAR_FILE_PATH));
        } catch (IOException e) {
            // Do nothing
        }
    }

    @Test
    public void testValidSignature() throws Exception {
        assertTrue(DynamicConfigVerifier.verify(TAR_FILE_PATH, SIGNATURE_FILE_PATH, kp.getPublic()));
    }

    @Test
    public void testInvalidSignature() throws Exception {
        assertFalse(DynamicConfigVerifier.verify(INVALID_TAR_FILE_PATH, SIGNATURE_FILE_PATH, kp.getPublic()));
    }

    @Test
    public void testHelpArguments() {
        assertDoesNotThrow(() -> DynamicConfigVerifier.main(new String[] { "-h" }));
        assertDoesNotThrow(() -> DynamicConfigVerifier.main(new String[] { "--help" }));
    }

    @Test
    public void testNoArguments() throws Exception {
        String error = tapSystemErr(() -> {
            int exitStatus = catchSystemExit(() -> DynamicConfigVerifier.main(null));
            assertEquals(1, exitStatus);
        });

        assertTrue(error.startsWith("Missing required option"));
    }

    @Test
    public void testOneEmptyArguments() throws Exception {
        String error = tapSystemErr(() -> {
            int exitStatus = catchSystemExit(() -> DynamicConfigVerifier.main(new String[] { "" }));
            assertEquals(1, exitStatus);
        });

        assertTrue(error.startsWith("Missing required option"));
    }

    @Test
    public void testMissingArgumentValue() throws Exception {
        String error = tapSystemErr(() -> {
            int exitStatus = catchSystemExit(() -> DynamicConfigVerifier.main(new String[] { "--tarFile" }));
            assertEquals(3, exitStatus);
        });

        assertTrue(error.startsWith("Missing argument for option"));

        error = tapSystemErr(() -> {
            int exitStatus = catchSystemExit(() -> DynamicConfigVerifier.main(new String[] { "-t" }));
            assertEquals(3, exitStatus);
        });

        assertTrue(error.startsWith("Missing argument for option"));
    }

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, new SecureRandom());
        return generator.generateKeyPair();
    }

    private static String sign(String file, PrivateKey privateKey) throws Exception {
        byte[] buffer = new byte[4096];
        Signature privateSignature = Signature.getInstance("SHA256withRSA");
        privateSignature.initSign(privateKey);
        try (InputStream inputStream = new FileInputStream(file)) {
            int read = 0;
            while ((read = inputStream.read(buffer)) != -1) {
                privateSignature.update(buffer, 0, read);
            }
        }
        byte[] signature = privateSignature.sign();
        return Base64.getEncoder().encodeToString(signature);
    }

    private static void createTarGZ() throws FileNotFoundException, IOException {
        try (TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(new GzipCompressorOutputStream(
                new BufferedOutputStream(new FileOutputStream(new File(TAR_FILE_PATH)))))) {
            String configPath = "src/test/resources/validator/";
            addFileToTarGz(tarOutputStream, configPath, "");
        }
    }

    private static void addFileToTarGz(TarArchiveOutputStream tOut, String path, String base) throws IOException {
        File f = new File(path);
        String entryName = base + f.getName();
        TarArchiveEntry tarEntry = new TarArchiveEntry(f, entryName);
        tOut.putArchiveEntry(tarEntry);

        if (f.isFile()) {
            try (InputStream inputStream = new FileInputStream(f)) {
                inputStream.transferTo(tOut);
            }
            tOut.closeArchiveEntry();
        } else {
            tOut.closeArchiveEntry();
            File[] children = f.listFiles();
            if (children != null) {
                for (File child : children) {
                    addFileToTarGz(tOut, child.getAbsolutePath(), entryName + "/");
                }
            }
        }
    }
}
