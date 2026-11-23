package com.clinic.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Saves paid-bill receipts as plain .txt files, one file per invoice
 * (for example receipts/Receipt_INV-0001.txt).
 *
 * The receipts folder sits next to bills.txt and the other data files.
 * It lives in this package because TxtUtil is package-private.
 */
public class ReceiptFileHandler {
    public static final String FOLDER_NAME = "receipts";

    private final Path folder;

    public ReceiptFileHandler() {
        this(TxtUtil.defaultDataDir().resolve(FOLDER_NAME));
    }

    public ReceiptFileHandler(Path folder) {
        this.folder = folder;
    }

    public Path getFolder() { return folder; }

    /** Where the receipt for this invoice is (or would be) saved. */
    public Path fileFor(String invoiceId) {
        return folder.resolve("Receipt_" + safeName(invoiceId) + ".txt");
    }

    public boolean exists(String invoiceId) {
        return Files.exists(fileFor(invoiceId));
    }

    /**
     * Saves the receipt text to receipts/Receipt_&lt;invoiceId&gt;.txt.
     * An existing receipt for the same invoice is overwritten.
     *
     * @return the file that was written
     */
    public Path save(String invoiceId, String receiptText) throws IOException {
        return saveTo(fileFor(invoiceId), receiptText);
    }

    /** Saves the receipt text to any chosen .txt path (adds .txt if it is missing). */
    public Path saveTo(Path target, String receiptText) throws IOException {
        if (receiptText == null || receiptText.trim().isEmpty()) {
            throw new IOException("The receipt is empty, nothing to save.");
        }
        Path file = target;
        if (!file.getFileName().toString().toLowerCase().endsWith(".txt")) {
            file = file.resolveSibling(file.getFileName() + ".txt");
        }
        TxtUtil.writeAtomically(file, Arrays.asList(receiptText.split("\\R", -1)));
        return file;
    }

    /** Reads a saved receipt back, or returns null when it has not been saved yet. */
    public String load(String invoiceId) throws IOException {
        return TxtUtil.readOrNull(fileFor(invoiceId));
    }

    /** Keeps only characters that are safe in a file name. */
    private static String safeName(String s) {
        String clean = s == null ? "" : s.trim().replaceAll("[^A-Za-z0-9._-]", "_");
        return clean.isEmpty() ? "unknown" : clean;
    }
}