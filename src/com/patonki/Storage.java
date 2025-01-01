package com.patonki;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Storage {
    //tällä hetkellä auki oleva tiedosto, oletuksella ensin auki text.txt tiedosto
    private String path = "data/text.txt";
    private File currentFile = new File(path);

    private String password = null;
    private final Timer saveInterval;

    private String ftpHost = null;
    private String ftpUsername = null;
    private String ftpPassword = null;
    private String user = null;

    private String currentContent = "";
    private boolean changesMadeSinceLastSave = false;
    private final Consumer<IOException> errorHandler;

    public Storage(Consumer<IOException> errorHandler) {
        this.errorHandler = errorHandler;
        final int[] intervalI = {0};
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                intervalI[0]++;
                if (intervalI[0] % 3 == 0) {
                    pushToFtpServer();
                }
                if (changesMadeSinceLastSave) {
                    writeToFile();

                    changesMadeSinceLastSave = false;
                }
            }
        };
        this.saveInterval = new Timer();
        this.saveInterval.scheduleAtFixedRate(timerTask, 1000, 3000);
    }
    public void onClose() {
        this.writeToFile();
        this.pushToFtpServer();
        this.saveInterval.cancel();
    }
    public synchronized boolean deleteCurrent() {
        return new File(path).delete();
    }
    public synchronized String fileNameWithoutExtension() {
        return this.path.substring(this.path.lastIndexOf('/') + 1, this.path.lastIndexOf('.'));
    }

    public synchronized void setCurrentFile(String name, String password) {
        this.password = password;
        this.path = String.format("data/%s.txt", name);
        this.currentFile = new File(this.path);

        setCurrentContent();
    }
    public synchronized boolean exists() {
        return currentFile.exists();
    }

    public void loadOptions(OptionsManager options) {
        this.ftpHost = options.getStringProperty("ftpHost");
        this.ftpUsername = options.getStringProperty("ftpUsername");
        this.ftpPassword = options.getStringProperty("ftpPassword");
        this.user = options.getStringProperty("user");

        this.syncWithFtpServer();
        setCurrentContent();
    }

    public static class DecryptionError extends IOException{ }

    private synchronized String currentContentRaw() throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(this.path)), StandardCharsets.UTF_8);
        if (this.password != null) {
            try {
                return Encryption.decrypt(content, this.password);
            } catch (Exception e) {
                throw new DecryptionError();
            }
        }
        return content;
    }
    private synchronized void writeToFile() {
        String value = currentContent;
        if (value.trim().isEmpty() && !currentFile.exists()) return;
        try {
            if (this.password != null) {
                if (this.exists()) {
                    String content = currentContentRaw();
                    if (!content.startsWith("Check")) {
                        throw new IOException("Incorrect password!");
                    }
                }
                try {
                    value = Encryption.encrypt("Check"+value, this.password);
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }
            Files.write(Paths.get(this.path), value.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            errorHandler.accept(e);
        }
    }

    private void setCurrentContent() {
        if (!this.currentFile.exists()) {
            this.currentContent = "";
            return;
        }
        try {
            String content = this.currentContentRaw();
            if (this.password != null) {
                content = content.replaceFirst("Check", "");
            }
            this.currentContent = content;
        } catch (IOException e) {
            this.errorHandler.accept(e);
        }
    }
    public synchronized String currentContent() {
        return this.currentContent;
    }

    public synchronized void write(String value) {
        if (value.equals("Unable to decrypt")) return;
        this.changesMadeSinceLastSave = true;
        this.currentContent = value;
    }

    private synchronized void unzipData(String fileName) throws IOException {
        Path zipFile = Paths.get(fileName);
        Path targetDir = Paths.get("data");
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                Path resolvedPath = targetDir.resolve(zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    Files.createDirectories(resolvedPath);
                } else {
                    Files.createDirectories(resolvedPath.getParent()); // Ensure parent directories exists
                    System.out.println("Writing " + resolvedPath);
                    try (OutputStream os = Files.newOutputStream(resolvedPath)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = zis.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }
    private synchronized void zipData(String filename) throws IOException {
        Path sourceDir = Paths.get("data");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(Paths.get(filename)));
             Stream<Path> pathStream = Files.walk(sourceDir)
        ) {
            pathStream
                    .filter(path -> !Files.isDirectory(path)) // Skip directories
                    .forEach(path -> {
                        String zipEntryName = sourceDir.relativize(path).toString();
                        try {
                            zos.putNextEntry(new ZipEntry(zipEntryName));
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
    }
    public synchronized void pushToFtpServer() {
        if (this.ftpHost == null || this.ftpPassword == null || this.ftpUsername == null || this.user == null) {
            return;
        }
        String fileName = String.format("notes_%s.zip", this.user);
        try {
            this.zipData(fileName);
            String username = URLEncoder.encode("mr@andrei.belogurov.org", "UTF-8");
            String password = URLEncoder.encode("mfw94Ce=Q5uV", "UTF-8");

            String ftpUrl = String.format(
                    "ftp://%s:%s@andrei.belogurov.org:21/%s", username, password, fileName);

            URLConnection urlConnection = new URL(ftpUrl).openConnection();
            urlConnection.setDoOutput(true);
            try (OutputStream outputStream = urlConnection.getOutputStream();
                 FileInputStream inputStream = new FileInputStream(fileName)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        }
        catch (FileNotFoundException ignored) {}
        catch (IOException e) {
            errorHandler.accept(e);
        }
    }
    public synchronized void syncWithFtpServer() {
        if (this.ftpHost == null || this.ftpPassword == null || this.ftpUsername == null || this.user == null) {
            return;
        }
        try {
            String host = URLEncoder.encode(this.ftpHost, "UTF-8");
            String user = URLEncoder.encode(this.ftpUsername, "UTF-8");
            String password = URLEncoder.encode(this.ftpPassword, "UTF-8");
            String fileName = String.format("notes_%s.zip", this.user);

            String ftpUrl = String.format(
                    "ftp://%s:%s@%s:21/%s", user, password, host, fileName);

            URLConnection urlConnection = new URL(ftpUrl).openConnection();
            InputStream inputStream = urlConnection.getInputStream();
            Files.copy(inputStream, new File(fileName).toPath(), StandardCopyOption.REPLACE_EXISTING);
            inputStream.close();

            this.unzipData(fileName);
        }
        catch (FileNotFoundException ignored) {}
        catch (IOException e) {
            errorHandler.accept(e);
        }

    }



}
