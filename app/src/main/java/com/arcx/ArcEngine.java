package com.arcx;

import android.app.Application;
import android.content.Context;

import com.mundo.MundoCore;
import com.mundo.activation.MundoActivate;
import com.mundo.app.configuration.ClientConfiguration;

import java.io.File;

import org.lsposed.lsparanoid.Obfuscate;

@Obfuscate
public class ArcEngine extends Application {

    static {
        try {
            System.loadLibrary("akshit");
        } catch (Throwable ignored) {
        }
    }

    private static final String TAG = "ARC-X";

    public static native String getSdkKey();

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        try {
            MundoCore.get().doAttachBaseContext(base, new ClientConfiguration() {

                @Override
                public String getHostPackageName() {
                    return base.getPackageName();
                }

                @Override
                public boolean isEnableDaemonService() {
                    return false;
                }

                @Override
                public boolean requestInstallPackage(File file) {
                    return false;
                }
            });

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            MundoCore.get().doCreate();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        try {
            String sdkKey = getSdkKey();

            if (sdkKey != null && !sdkKey.trim().isEmpty()) {
                MundoActivate.startEngine(sdkKey);
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}