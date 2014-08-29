package ae.threef.ocr.core;

import android.app.Application;

import java.io.File;

/**
 * Created by Hisham on 29/8/2014.
 */
public class App extends Application {

    // Tessaract OCR "constants"
    private static String tessaractFilesDirParent;
    private static String tessaractFilesDir;

    @Override
    public void onCreate() {
        super.onCreate();

        tessaractFilesDirParent = getFilesDir()
                + File.separator + "tess"
                + File.separator;

        // "tessdata" is a required directory
        tessaractFilesDir = tessaractFilesDirParent
                + "tessdata"
                + File.separator;

    }

    public static final String getTessaractFilesDirParent() {
        return tessaractFilesDirParent;
    }

    public static final String getTessaractFilesDir() {
        return tessaractFilesDir;
    }
}
