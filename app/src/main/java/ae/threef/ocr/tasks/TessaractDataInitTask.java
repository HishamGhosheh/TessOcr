package ae.threef.ocr.tasks;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Hisham on 29/8/2014.
 */

/**
 * Copies the required tessaract files from the assets folder to a place more accessible
 */
public class TessaractDataInitTask extends AsyncTask<Void, Void, Boolean> {

    // State file name, existence of this files means all files are in place
    private static final String STATE_FILE = "success";

    // Destination to copy files to
    private final String destinationPath;
    private final Context context;
    private final TessaractDataInitTaskListener listener;

    public interface TessaractDataInitTaskListener {
        public void onDataInitFinished(boolean success);
    }

    /**
     * Copies all assets files under "tessdata" to the provided location. does nothing if files already exist
     *
     * @param context Context used to access assets
     * @param listener Listener to be notified when the task has finished its work
     * @param destinationPath Location to copy the files to
     */
    public TessaractDataInitTask(Context context, TessaractDataInitTaskListener listener, String destinationPath) {
        this.context = context;
        this.listener = listener;
        this.destinationPath = destinationPath;
    }

    @Override
    protected Boolean doInBackground(Void... params) {

        boolean result = false;

        // Fix destination path if needed
        String dest = destinationPath;
        if (!dest.endsWith(File.separator)) dest += File.separator;

        // Check if files already exist
        File stateFile = new File(dest, STATE_FILE);

        if (stateFile.exists())
            return true;


        String[] files;

        AssetManager am = context.getAssets();
        try {
            // Make folders for the destination path
            File destDir = new File(dest);
            destDir.mkdirs();

            files = am.list("tessdata");

            // Loop through asset files
            for (String file : files) {
                InputStream in = am.open("tessdata/" + file);
                OutputStream out = new FileOutputStream(destDir + File.separator + file);

                byte[] buffer = new byte[4 * 1024];
                int len;

                while ((len = in.read(buffer)) > 0)
                    out.write(buffer, 0, len);

                in.close();
                out.close();
            }

            // All files copied successfully, we only need to write the state file
            stateFile.createNewFile();
            result = true;
        } catch (IOException e) {
        }

        return result;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);

        if (listener != null)
            listener.onDataInitFinished(success);
    }
}
