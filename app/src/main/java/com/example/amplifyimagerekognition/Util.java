package com.example.amplifyimagerekognition;
import android.content.Context;
import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
public class Util {

    private static Properties properties = new Properties();

    public static void initialize(Context context) throws IOException {

        AssetManager assetManager = context.getAssets();
        InputStream inputStream = assetManager.open("config.properties");
        try {
            if (inputStream != null) {
                properties.load(inputStream);
            } else {
                throw new IOException("cannot find config.properties file");
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}
