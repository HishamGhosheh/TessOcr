package ae.threef.ocr.tasks;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.IOException;
import java.io.InputStream;

import ae.threef.ocr.R;
import ae.threef.ocr.core.App;

// TODO wrap task in another class and extract methods interface for multiple OCR providers

/**
 * Created by Hisham on 29/8/2014.
 */
public class OcrTask extends AsyncTask<Void, Bitmap, String> {

    public interface OcrTaskListener {
        public void onOcrFinished(String text);

        public void onImageLoaded(Bitmap bm);

        public void onOcrFailed(String errorDescription);
    }

    // Maximum image dimension, larger images will be sub-sampled
    private static final int MAX_IMAGE_DIMEN = 800;

    private OcrTaskListener listener;
    private Context context;
    private Uri imageUri;

    // Error returned if the operation fails
    private String error = null;

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        error = null;
    }

    /**
     * Extracts text from an image at the provided uri
     * @param context Context to use for content resolving
     * @param listener Listener to be notified when the process is finished
     * @param imageUri Uri to the image
     */
    public OcrTask(Context context, OcrTaskListener listener, Uri imageUri) {
        this.context = context;
        this.listener = listener;
        this.imageUri = imageUri;
    }


    @Override
    protected void onProgressUpdate(Bitmap... values) {
        super.onProgressUpdate(values);
        if (listener != null)
            listener.onImageLoaded(values[0]);
    }


    @Override
    protected String doInBackground(Void... params) {
        ContentResolver cr = context.getContentResolver();

        // Rotation needed to get the correct image
        int rotate = 0;

        // Get rotation value
        String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};
        Cursor cur = cr.query(imageUri, orientationColumn, null, null, null);
        if (cur != null && cur.moveToFirst())
            rotate = cur.getInt(cur.getColumnIndex(orientationColumn[0]));

        Bitmap bm = null;
        try {
            bm = decodeImage(cr, imageUri);
        } catch (IOException e) {
            error = context.getString(R.string.err_error_reading_image_file);
            e.printStackTrace();
        } catch (OutOfMemoryError err) {
            error = context.getString(R.string.err_bitmap_decode_failed);
        }

        if (bm != null) {
            // Rotate image as needed
            if (rotate != 0) {
                int w = bm.getWidth();
                int h = bm.getHeight();

                // Setting pre rotate
                Matrix mtx = new Matrix();
                mtx.preRotate(rotate);

                // Rotating Bitmap & convert to ARGB_8888, required by tess
                bm = Bitmap.createBitmap(bm, 0, 0, w, h, mtx, false);
            }

            bm = bm.copy(Bitmap.Config.ARGB_8888, true);

            // Send the result bm to interested listener
            publishProgress(bm);

            try {
                // Start the OCR process
                TessBaseAPI baseApi = new TessBaseAPI();
                baseApi.init(App.getTessaractFilesDirParent(), "eng");
                baseApi.setImage(bm);
                String recognizedText = baseApi.getUTF8Text();
                baseApi.end();

                return recognizedText;
            } catch (Exception e) {
                error = context.getString(R.string.err_ocr_failed_fmt, e.getMessage());
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);

        if (listener == null) return;

        if (error == null)
            listener.onOcrFinished(s);
        else
            listener.onOcrFailed(error);
    }

    private Bitmap decodeImage(ContentResolver cr, Uri uri) throws IOException {
        // First we get the image dimensions
        InputStream input = cr.openInputStream(uri);
        BitmapFactory.Options optsBounds = new BitmapFactory.Options();
        optsBounds.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(input, null, optsBounds);
        input.close();

        int maxDimen = Math.max(optsBounds.outHeight, optsBounds.outWidth);

        // We calculate the sampling ratio that will result in the image size nearest to what we want
        double ratio = (maxDimen > MAX_IMAGE_DIMEN) ? (maxDimen / MAX_IMAGE_DIMEN) : 1.0;
        ratio = getPowerOfTwoForSampleRatio(ratio);

        // Getting the bitmap
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = (int) ratio;
        input = cr.openInputStream(uri);
        Bitmap result = BitmapFactory.decodeStream(input, null, opts);

        return result;
    }

    private static int getPowerOfTwoForSampleRatio(double ratio) {
        int k = Integer.highestOneBit((int) Math.floor(ratio));
        if (k == 0) return 1;
        else return k;
    }
}
