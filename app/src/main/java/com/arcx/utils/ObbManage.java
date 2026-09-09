package com.arcx.utils;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.mundo.MundoCore;
import com.mundo.entity.pm.InstallResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicBoolean;

import org.lsposed.lsparanoid.Obfuscate;

@Obfuscate
public class ObbManage {

    public interface Callback {
        void onResult(boolean success, String message);
    }

    private static final String BGMI_PACKAGE =
            "com.pubg.imobile";

    private static final int USER_ID = 0;

    private final Context context;

    public ObbManage(Context context) {
        this.context = context.getApplicationContext();
    }

    public void installOrFix(Callback callback) {

        try {

            if (MundoCore.get() == null) {
                callback.onResult(false, "MundoCore unavailable");
                return;
            }

            if (!MundoCore.get().isInstalled(
                    BGMI_PACKAGE,
                    USER_ID
            )) {

                InstallResult result =
                        MundoCore.get().installPackageAsUser(
                                BGMI_PACKAGE,
                                USER_ID
                        );

                if (!result.success) {
                    callback.onResult(
                            false,
                            "Install failed: " + result.msg
                    );
                    return;
                }
            }

            copyObb(callback);

        } catch (Throwable e) {

            callback.onResult(
                    false,
                    e.getMessage() == null
                            ? "Install error"
                            : e.getMessage()
            );
        }
    }

    private void copyObb(Callback callback) {

        File root =
                Environment
                        .getExternalStorageDirectory();

        File source =
                new File(
                        root,
                        "Android/obb/" + BGMI_PACKAGE
                );

        File destination =
                new File(
                        root,
                        "Sdcard/Android/obb/" +
                                BGMI_PACKAGE
                );

        if (!destination.exists()) {
            destination.mkdirs();
        }

        File[] already =
                destination.listFiles(
                        (dir, name) ->
                                name != null &&
                                name.toLowerCase()
                                        .endsWith(".obb")
                );

        if (already != null &&
                already.length > 0) {

            callback.onResult(
                    true,
                    "OBB already available"
            );
            return;
        }

        AtomicBoolean finished =
                new AtomicBoolean(false);

        Handler handler =
                new Handler(Looper.getMainLooper());

        handler.postDelayed(() -> {

            if (finished.compareAndSet(false, true)) {

                callback.onResult(
                        false,
                        "OBB copy timed out"
                );
            }

        }, 60000);

        new Thread(() -> {

            try {

                File[] sourceFiles =
                        source.listFiles(
                                (dir, name) ->
                                        name != null &&
                                        name.toLowerCase()
                                                .endsWith(".obb")
                        );

                if (sourceFiles == null ||
                        sourceFiles.length == 0) {

                    if (finished.compareAndSet(
                            false,
                            true
                    )) {

                        callback.onResult(
                                false,
                                "Source OBB not found"
                        );
                    }

                    return;
                }

                File src = sourceFiles[0];

                File dest =
                        new File(
                                destination,
                                src.getName()
                        );

                try (
                        FileChannel in =
                                new FileInputStream(src)
                                        .getChannel();

                        FileChannel out =
                                new FileOutputStream(dest)
                                        .getChannel()
                ) {

                    long size = in.size();
                    long position = 0;

                    while (position < size) {

                        long transferred =
                                in.transferTo(
                                        position,
                                        Math.min(
                                                16 * 1024 * 1024,
                                                size - position
                                        ),
                                        out
                                );

                        if (transferred <= 0) {
                            break;
                        }

                        position += transferred;
                    }
                }

                if (finished.compareAndSet(
                        false,
                        true
                )) {

                    callback.onResult(
                            true,
                            "OBB copied successfully"
                    );
                }

            } catch (Throwable e) {

                if (finished.compareAndSet(
                        false,
                        true
                )) {

                    callback.onResult(
                            false,
                            e.getMessage() == null
                                    ? "OBB copy failed"
                                    : e.getMessage()
                    );
                }
            }

        }).start();
    }

    public void launch() {

        try {

            MundoCore.get().launchApk(
                    BGMI_PACKAGE,
                    USER_ID
            );

        } catch (Throwable e) {

            Toast.makeText(
                    context,
                    "Launch failed",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}