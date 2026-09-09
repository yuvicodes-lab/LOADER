package com.arcx.utils;

import android.content.Context;
import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.lsposed.lsparanoid.Obfuscate;

@Obfuscate
public class FileTask extends AsyncTask<String, Integer, Boolean> {

    public interface Callback {
        void onComplete(boolean success);
    }

    public interface ProgressListener {
        void onProgress(int progress);
    }

    private final Context context;
    private final Callback callback;

    private ProgressListener progressListener;

    private String errorMessage = "";

    private static final String FINAL_NAME = "libbgmi.so";

    public FileTask(Context context, Callback callback) {
        this.context = context.getApplicationContext();
        this.callback = callback;
    }

    public void setProgressListener(ProgressListener listener) {
        this.progressListener = listener;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public static native String Link();

    @Override
    protected Boolean doInBackground(String... params) {

        File root = new File(context.getFilesDir(), "loader");
        File finalFile = new File(root, FINAL_NAME);

        File zipFile = new File(
                context.getFilesDir(),
                "arcx_loader_temp.zip"
        );

        try {

            if (!root.exists() && !root.mkdirs()) {
                errorMessage = "Unable to create loader directory";
                return false;
            }

            /*
             * Requirement:
             * Existing libbgmi.so must be deleted BEFORE download.
             */
            if (finalFile.exists() && !finalFile.delete()) {
                errorMessage = "Unable to delete old libbgmi.so";
                return false;
            }

            /*
             * Remove stale temporary ZIP.
             */
            if (zipFile.exists()) {
                zipFile.delete();
            }

            /*
             * URL comes ONLY from native Link().
             */
            String downloadUrl;

            try {
                downloadUrl = Link();
            } catch (Throwable e) {
                errorMessage = "Unable to get download URL";
                return false;
            }

            /*
             * Empty URL is allowed.
             * Login has already succeeded, so simply skip download.
             */
            if (downloadUrl == null ||
                    downloadUrl.trim().isEmpty()) {

                return true;
            }

            downloadUrl = downloadUrl.trim();

            /*
             * Download ZIP.
             */
            if (!downloadZip(downloadUrl, zipFile)) {
                return false;
            }

            /*
             * Extract any .so from ZIP and rename to
             * libbgmi.so.
             */
            if (!extractSo(zipFile, root, finalFile)) {
                return false;
            }

            /*
             * Temporary ZIP is no longer needed.
             */
            if (zipFile.exists()) {
                zipFile.delete();
            }

            /*
             * Final validation.
             */
            if (!finalFile.exists() || finalFile.length() <= 0) {
                errorMessage = "libbgmi.so was not created";
                return false;
            }

            return true;

        } catch (Throwable e) {

            errorMessage = e.getMessage();

            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = e.getClass().getSimpleName();
            }

            if (zipFile.exists()) {
                zipFile.delete();
            }

            return false;
        }
    }

    private boolean downloadZip(String urlString, File destination) {

        HttpURLConnection connection = null;

        try {

            URL url = new URL(urlString);

            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setInstanceFollowRedirects(true);
            connection.setUseCaches(false);

            int responseCode = connection.getResponseCode();

            if (responseCode < 200 || responseCode >= 300) {
                errorMessage = "HTTP " + responseCode;
                return false;
            }

            long total = connection.getContentLengthLong();

            InputStream input =
                    new BufferedInputStream(connection.getInputStream());

            FileOutputStream fos =
                    new FileOutputStream(destination);

            BufferedOutputStream output =
                    new BufferedOutputStream(fos);

            byte[] buffer = new byte[8192];

            long downloaded = 0;
            int count;

            while ((count = input.read(buffer)) != -1) {

                output.write(buffer, 0, count);

                downloaded += count;

                if (total > 0) {
                    int progress =
                            (int) ((downloaded * 100L) / total);

                    publishProgress(progress);
                }
            }

            output.flush();
            output.close();
            input.close();

            return destination.exists() &&
                    destination.length() > 0;

        } catch (Throwable e) {

            errorMessage = e.getMessage();

            if (errorMessage == null) {
                errorMessage = "Download failed";
            }

            return false;

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private boolean extractSo(
            File zip,
            File outputDirectory,
            File finalFile
    ) {

        ZipInputStream zis = null;

        try {

            zis = new ZipInputStream(
                    new BufferedInputStream(
                            new FileInputStream(zip)
                    )
            );

            ZipEntry entry;

            byte[] buffer = new byte[8192];

            boolean found = false;

            while ((entry = zis.getNextEntry()) != null) {

                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }

                String name = entry.getName();

                /*
                 * Accept any .so filename.
                 */
                if (name != null &&
                        name.toLowerCase().endsWith(".so")) {

                    File tempSo = new File(
                            outputDirectory,
                            ".arcx_extract.so"
                    );

                    FileOutputStream fos =
                            new FileOutputStream(tempSo);

                    BufferedOutputStream output =
                            new BufferedOutputStream(fos);

                    int count;

                    while ((count = zis.read(buffer)) != -1) {
                        output.write(buffer, 0, count);
                    }

                    output.flush();
                    output.close();

                    /*
                     * Rename extracted SO to exact final name.
                     */
                    if (finalFile.exists()) {
                        finalFile.delete();
                    }

                    if (!tempSo.renameTo(finalFile)) {

                        /*
                         * Rename may fail across filesystems.
                         * Do a safe copy fallback.
                         */
                        FileInputStream in =
                                new FileInputStream(tempSo);

                        FileOutputStream out =
                                new FileOutputStream(finalFile);

                        byte[] copyBuffer = new byte[8192];

                        int read;

                        while ((read = in.read(copyBuffer)) != -1) {
                            out.write(copyBuffer, 0, read);
                        }

                        out.flush();
                        out.close();
                        in.close();

                        tempSo.delete();
                    }

                    found = true;

                    zis.closeEntry();
                    break;
                }

                zis.closeEntry();
            }

            return found &&
                    finalFile.exists() &&
                    finalFile.length() > 0;

        } catch (Throwable e) {

            errorMessage = e.getMessage();

            if (errorMessage == null) {
                errorMessage = "ZIP extraction failed";
            }

            return false;

        } finally {

            if (zis != null) {
                try {
                    zis.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {

        if (progressListener != null &&
                values != null &&
                values.length > 0) {

            progressListener.onProgress(values[0]);
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {

        if (callback != null) {
            callback.onComplete(success);
        }
    }
}