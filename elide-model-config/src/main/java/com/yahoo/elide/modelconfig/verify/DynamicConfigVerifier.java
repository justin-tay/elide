/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.verify;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.util.Base64;

/**
 * Util class to Verify model tar.gz file's RSA signature with available public key in key store.
 */
public class DynamicConfigVerifier {

    /**
     * Main Method to Verify Signature of Model Tar file.
     * @param args : expects 3 arguments.
     */
    public static void main(String[] args) {

        Options options = prepareOptions();

        try {
            CommandLine cli = new DefaultParser().parse(options, args);

            if (cli.hasOption("help")) {
                printHelp(options);
                return;
            }
            if (!cli.hasOption("tarFile") || !cli.hasOption("signatureFile") || !cli.hasOption("publicKeyName")) {
                printHelp(options);
                System.err.println("Missing required option");
                System.exit(1);
            }

            String modelTarFile = cli.getOptionValue("tarFile");
            String signatureFile = cli.getOptionValue("signatureFile");
            String publicKeyName = cli.getOptionValue("publicKeyName");

            if (verify(modelTarFile, signatureFile, getPublicKey(publicKeyName))) {
                System.out.println("Successfully Validated " + modelTarFile);
            } else {
                System.err.println("Could not verify " + modelTarFile + " with details provided");
                System.exit(2);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(3);
        }
    }

    /**
     * Verify signature of tar.gz.
     * @param file : file containing content
     * @param signatureFile : file containing signature
     * @param publicKey : public key name
     * @return whether the file can be verified by given key and signature
     * @throws NoSuchAlgorithmException If no Provider supports a Signature implementation for the SHA256withRSA
     *         algorithm.
     * @throws InvalidKeyException If the {@code publicKey} is invalid.
     * @throws SignatureException If Signature object is not initialized properly.
     * @throws IOException If there is an issue reading the files
     * @throws FileNotFoundException If the files cannot be found
     */
    public static boolean verify(String file, String signatureFile, PublicKey publicKey)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, FileNotFoundException,
            IOException {
        Signature publicSignature = Signature.getInstance("SHA256withRSA");
        publicSignature.initVerify(publicKey);
        byte[] buffer = new byte[4096];
        try (InputStream inputStream = new FileInputStream(file)) {
            int read = 0;
            while ((read = inputStream.read(buffer)) != -1) {
                publicSignature.update(buffer, 0, read);
            }
        }
        byte[] signatureBytes = Base64.getDecoder().decode(Files.readString(Path.of(signatureFile)));
        return publicSignature.verify(signatureBytes);
    }

    /**
     * Retrieve public key from Key Store.
     * @param keyName : name of the public key
     * @return publickey
     */
    private static PublicKey getPublicKey(String keyName) throws KeyStoreException {
        PublicKey publicKey = null;
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        Certificate cert = keyStore.getCertificate(keyName);
        publicKey = cert.getPublicKey();
        return publicKey;
    }

    /**
     * Define Arguments.
     */
    private static final Options prepareOptions() {
        Options options = new Options();
        options.addOption(new Option("h", "help", false, "Print a help message and exit."));
        options.addOption(new Option("t", "tarFile", true, "Path of the tar.gz file"));
        options.addOption(new Option("s", "signatureFile", true, "Path of the file containing the signature"));
        options.addOption(new Option("p", "publicKeyName", true, "Name of public key in keystore"));
        return options;
    }

    /**
     * Print Help.
     */
    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(
                "java -cp <Jar File> com.yahoo.elide.modelconfig.verify.DynamicConfigVerifier",
                options);
    }
}
