package com.clinic.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Properties;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Local admin credentials for this desktop demo. */
public final class AdminCredentials {
    private static final Path FILE = Paths.get(System.getProperty("user.home"), ".clinic-system", "admin.properties");
    private static final String DEFAULT_EMAIL = "admin@clinic.com";
    private static final String DEFAULT_PASSWORD = "admin123";
    private AdminCredentials() {}

    public static synchronized String getEmail() { return load().getProperty("email", DEFAULT_EMAIL); }

    private static final String DEFAULT_DISPLAY_NAME = "Admin";
    private static final int MAX_DISPLAY_NAME = 30;

    /** Name shown in the sidebar and account menu. Defaults to "Admin". */
    public static synchronized String getDisplayName() {
        String n = load().getProperty("displayName", DEFAULT_DISPLAY_NAME).trim();
        return n.isEmpty() ? DEFAULT_DISPLAY_NAME : n;
    }

    public static synchronized void setDisplayName(String name) throws IOException {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) throw new IllegalArgumentException("Display name cannot be empty.");
        if (n.length() > MAX_DISPLAY_NAME) throw new IllegalArgumentException("Display name must be " + MAX_DISPLAY_NAME + " characters or fewer.");
        Properties p = load();
        p.setProperty("displayName", n);
        Files.createDirectories(FILE.getParent());
        try (OutputStream out = Files.newOutputStream(FILE)) { p.store(out, "Clinic System admin credentials"); }
    }

    public static synchronized boolean authenticate(String email, char[] password) {
        Properties p = load();
        if (email == null || password == null) return false;
        if (!p.getProperty("email", DEFAULT_EMAIL).equalsIgnoreCase(email.trim())) return false;
        String saltText = p.getProperty("salt");
        String hashText = p.getProperty("hash");
        if (saltText == null || hashText == null) {
            return DEFAULT_PASSWORD.equals(new String(password));
        }
        try {
            byte[] salt = Base64.getDecoder().decode(saltText);
            byte[] expected = Base64.getDecoder().decode(hashText);
            return MessageDigest.isEqual(expected, hash(password, salt));
        } catch (Exception ex) { return false; }
    }

    public static synchronized void change(String currentEmail, char[] currentPassword, String newEmail, char[] newPassword) throws Exception {
        if (!authenticate(currentEmail, currentPassword)) throw new IllegalArgumentException("Current email or password is incorrect.");
        String email = newEmail == null ? "" : newEmail.trim();
        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) throw new IllegalArgumentException("Enter a valid email address.");
        if (newPassword == null || newPassword.length < 8) throw new IllegalArgumentException("New password must be at least 8 characters.");
        byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
        Properties p = load();   // keep other saved settings (e.g. display name)
        p.setProperty("email", email);
        p.setProperty("salt", Base64.getEncoder().encodeToString(salt));
        p.setProperty("hash", Base64.getEncoder().encodeToString(hash(newPassword, salt)));
        Files.createDirectories(FILE.getParent());
        try (OutputStream out = Files.newOutputStream(FILE)) { p.store(out, "Clinic System admin credentials"); }
    }

    private static Properties load() {
        Properties p = new Properties();
        if (Files.exists(FILE)) {
            try (InputStream in = Files.newInputStream(FILE)) { p.load(in); }
            catch (IOException ignored) { }
        }
        return p;
    }

    private static byte[] hash(char[] password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password, salt, 120000, 256);
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    }
}