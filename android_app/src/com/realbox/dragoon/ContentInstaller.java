package com.realbox.dragoon;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

final class ContentInstaller {
    static final List<String> DIRECTORIES = List.of("gfx", "patches", "resources", "android-shaders", "files");

    interface Progress {
        void update(String phase, long done, long total);
    }

    static void download(URI uri, Path target, long size, String hash, Progress progress) throws IOException {
        if (Files.isRegularFile(target) && Files.size(target) == size && digest(target).equals(hash)) return;
        Path partial = target.resolveSibling(target.getFileName() + ".part");
        try {
            for (int redirects = 0; redirects < 6; redirects++) {
                if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
                    throw new IOException("La dirección de descarga debe usar HTTPS.");
                }
                HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setRequestProperty("Accept-Encoding", "identity");
                try {
                    int code = connection.getResponseCode();
                    if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                        String location = connection.getHeaderField("Location");
                        if (location == null) throw new IOException("Redirección incompleta.");
                        uri = uri.resolve(location);
                        continue;
                    }
                    if (code != 200) throw new IOException("El servidor respondió HTTP " + code + ".");
                    if (connection.getContentLengthLong() >= 0 && connection.getContentLengthLong() != size) {
                        throw new IOException("El tamaño del paquete no coincide.");
                    }
                    MessageDigest digest = sha256();
                    long done = 0;
                    progress.update("Descargando", 0, size);
                    try (InputStream input = connection.getInputStream(); OutputStream output = Files.newOutputStream(partial)) {
                        byte[] buffer = new byte[131072];
                        for (int read; (read = input.read(buffer)) != -1;) {
                            done += read;
                            if (done > size) throw new IOException("El paquete supera el tamaño esperado.");
                            output.write(buffer, 0, read);
                            digest.update(buffer, 0, read);
                            progress.update("Descargando", done, size);
                        }
                    }
                    if (done != size || !HexFormat.of().formatHex(digest.digest()).equals(hash)) {
                        throw new IOException("La descarga está incompleta o dañada.");
                    }
                    Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    return;
                } finally {
                    connection.disconnect();
                }
            }
            throw new IOException("Demasiadas redirecciones.");
        } finally {
            Files.deleteIfExists(partial);
        }
    }

    static void unpack(Path archive, Path stage, long size, boolean keepPal, Progress progress) throws IOException {
        remove(stage);
        Files.createDirectories(stage);
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            long done = 0;
            byte[] buffer = new byte[131072];
            Set<Path> entries = new HashSet<>();
            progress.update("Instalando", 0, size);
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
                Path relative = Path.of(entry.getName());
                Path normalized = relative.normalize();
                if (relative.isAbsolute() || !relative.equals(normalized) || normalized.getNameCount() == 0
                    || !DIRECTORIES.contains(normalized.getName(0).toString())
                    || (!entry.isDirectory() && normalized.getNameCount() < 2) || !entries.add(normalized)) {
                    throw new IOException("El paquete contiene una ruta inválida.");
                }
                Path target = stage.resolve(normalized);
                boolean skip = keepPal && normalized.startsWith("files");
                if (entry.isDirectory()) {
                    if (!skip) Files.createDirectories(target);
                    continue;
                }
                if (!skip) Files.createDirectories(target.getParent());
                try (OutputStream output = skip ? OutputStream.nullOutputStream() : Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
                    for (int read; (read = zip.read(buffer)) != -1;) {
                        done += read;
                        if (done > size) throw new IOException("El contenido supera el tamaño esperado.");
                        output.write(buffer, 0, read);
                        progress.update("Instalando", done, size);
                    }
                }
            }
            if (done != size) throw new IOException("El paquete está incompleto.");
        }
    }

    static void activate(Path root, Path stage, boolean keepPal, String assetHash) throws IOException {
        Path marker = root.resolve("engine-assets.sha256");
        Files.deleteIfExists(marker);
        for (String name : DIRECTORIES) {
            if (keepPal && name.equals("files")) continue;
            Path target = root.resolve(name);
            remove(target);
            Files.move(stage.resolve(name), target, StandardCopyOption.ATOMIC_MOVE);
        }
        Path temporary = root.resolve("engine-assets.sha256.tmp");
        Files.write(temporary, assetHash.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Files.move(temporary, marker, StandardCopyOption.ATOMIC_MOVE);
    }

    private static String digest(Path path) throws IOException {
        MessageDigest digest = sha256();
        try (InputStream input = new DigestInputStream(Files.newInputStream(path), digest)) {
            input.transferTo(OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }

    static void remove(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (var paths = Files.walk(path)) {
            for (Path child : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(child);
        }
    }
}
