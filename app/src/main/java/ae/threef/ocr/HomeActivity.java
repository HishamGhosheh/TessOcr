package ae.threef.ocr;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.Date;

import ae.threef.ocr.core.BaseActivity;
import ae.threef.ocr.fragments.dialogs.DateTimePickerDialog;
import ae.threef.ocr.tasks.OcrTask;


public class HomeActivity extends BaseActivity
        implements View.OnClickListener,
        DateTimePickerDialog.DateTimePickerHost {

    private static final int REQ_CAMERA = 1;

    private static final String STATE_OUTPUT_URI = "output_uri";
    private static final String STATE_OUTPUT_PATH = "output_path";

    public static final String EXTRA_MAKE_NOTIFICATION = "ae.threef.ocr.NOTIFY";

    /**
     * Convenience method for launching this activity
     *
     * @param context Context to use
     */
    public static void launch(Context context) {
        Intent intent = new Intent(context, HomeActivity.class);
        context.startActivity(intent);
    }

    // Views
    ImageView imgPreview;
    View vPickImage;
    TextView tvOcrResult;

    // Uri used by the camera application to save the image
    Uri pickedImageUri = null;
    String pickedImagePath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Disable key guard
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_home);

        vPickImage = findViewById(R.id.vPickImage);
        imgPreview = (ImageView) findViewById(R.id.imgPreview);
        tvOcrResult = (TextView) findViewById(R.id.tvOcrResult);

        vPickImage.setOnClickListener(this);
        findViewById(R.id.vCreateAlarm).setOnClickListener(this);

        if (savedInstanceState == null) {
            if (getIntent().getBooleanExtra(EXTRA_MAKE_NOTIFICATION, false)) {
                showNotification();
            }
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(STATE_OUTPUT_URI, pickedImageUri);
        outState.putString(STATE_OUTPUT_PATH, pickedImagePath);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        pickedImageUri = savedInstanceState.getParcelable(STATE_OUTPUT_URI);
        pickedImagePath = savedInstanceState.getString(STATE_OUTPUT_PATH);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.vPickImage:
                launchCamera();
                break;
            case R.id.vCreateAlarm:
                new DateTimePickerDialog()
                        .show(getSupportFragmentManager(), "picker_dlg");
                break;
        }
    }

    private void launchCamera() {

        String path = getFilesDir() + File.separator + "OcrApp" + File.separator;
        pickedImagePath = path + File.separator + System.currentTimeMillis() + ".jpeg";
        pickedImageUri = Uri.fromFile(new File(pickedImagePath));

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Get image in intent if no sd card is available
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, pickedImageUri);

        startActivityForResult(intent, REQ_CAMERA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CAMERA && resultCode == RESULT_OK) {
            // Get uri from intent if exists
            if (data.getData() != null)
                pickedImageUri = data.getData();

            // Update some ui elements
            vPickImage.setEnabled(false);
            tvOcrResult.setText("");
            setVisibility(R.id.vLoading, View.VISIBLE);

            // Start Async OCR task
            new OcrTask(this, ocrListener, pickedImageUri)
                    .execute();
        }
    }

    // Handles the results of the OCR task
    private OcrTask.OcrTaskListener ocrListener = new OcrTask.OcrTaskListener() {
        @Override
        public void onOcrFinished(String text) {
            vPickImage.setEnabled(true);
            setVisibility(R.id.vLoading, View.GONE);

            // Display result and make it scrollable
            tvOcrResult.setMovementMethod(new ScrollingMovementMethod());
            tvOcrResult.setText(text);

            deleteImageFile();
        }

        @Override
        public void onImageLoaded(Bitmap bm) {
            // Display decoded and rotated image
            imgPreview.setImageBitmap(bm);
        }

        @Override
        public void onOcrFailed(String errorDescription) {
            vPickImage.setEnabled(true);
            setVisibility(R.id.vLoading, View.GONE);
            makeToast(errorDescription);

            tvOcrResult.setText("");
            deleteImageFile();
        }
    };

    /**
     * Safely deletes the image file produced in the OCR process
     */
    private void deleteImageFile() {
        try {
            if (pickedImagePath != null)
                new File(pickedImagePath).delete();
        } catch (Exception e) {
        }
    }

    @Override
    public void onDatePicked(DateTimePickerDialog dlg, Date date) {
        // Validate picked date

        Date now = new Date();
        if (now.after(date)) {
            makeToast(R.string.err_negative_date);
        } else {
            dlg.dismiss();
            createAlarm(date);
        }
    }

    private void createAlarm(Date date) {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);

        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra(EXTRA_MAKE_NOTIFICATION, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // We could use a BroadcastReceived here instead of an activity
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        am.set(AlarmManager.RTC_WAKEUP, date.getTime(), pi);
        makeToast(R.string.alarm_created);
        finish();
    }

    /**
     * Builds a notification to be displayed when it's time for the alarm
     */
    private void showNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        TaskStackBuilder taskBuilder = TaskStackBuilder.create(this);
        taskBuilder.addNextIntent(new Intent(this, HomeActivity.class));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setTicker(getString(R.string.its_time))
                .setContentText(getString(R.string.its_time))
                .setVibrate(new long[]{100, 200, 100, 200, 300, 1000})
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setLights(0xFFFF6600, 100, 2000);

        builder.setContentIntent(taskBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT));

        nm.notify(1, builder.build());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }
}
