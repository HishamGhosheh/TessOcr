package ae.threef.ocr;

import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.Window;

import ae.threef.ocr.core.App;
import ae.threef.ocr.core.BaseActivity;
import ae.threef.ocr.tasks.TessaractDataInitTask;


public class SplashActivity extends BaseActivity
        implements TessaractDataInitTask.TessaractDataInitTaskListener {

    private static int SPLASH_DURATION = 2300;

    // To open next activity these flags must be turned to true
    private boolean filesCopied = false;
    private boolean splashEnded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_splash);

        // Initialize copying required tess files
        new TessaractDataInitTask(this, this, App.getTessaractFilesDir())
                .execute();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Schedule opening next activity
        splashHandler.postDelayed(splashRunnable, SPLASH_DURATION);
    }

    @Override
    protected void onPause() {
        // Remove scheduled task if exists
        splashHandler.removeCallbacks(splashRunnable);
        super.onPause();
    }

    @Override
    public void onDataInitFinished(boolean success) {
        if (success) {
            // Set files flag to true and request next activity
            filesCopied = true;
            nextActivity();
        } else {
            makeToast(R.string.err_tessaract_init_failed);
            finish();
        }
    }

    private void nextActivity() {
        // Return if a task is pending
        if (!filesCopied) return;
        if (!splashEnded) return;

        HomeActivity.launch(this);
        finish();
    }

    Runnable splashRunnable = new Runnable() {
        @Override
        public void run() {
            splashEnded = true;
            nextActivity();
        }
    };

    Handler splashHandler = new Handler();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.splash, menu);
        return true;
    }
}
