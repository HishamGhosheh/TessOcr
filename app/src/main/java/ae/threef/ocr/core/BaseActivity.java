package ae.threef.ocr.core;

import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;

/**
 * Created by Hisham on 29/8/2014.
 */
public abstract class BaseActivity extends ActionBarActivity {

    /**
     * Convenience method to show toasts
     * @param message If passed message is an integer then it will be treated as a string resource.
     *                 Otherwise String.valueOf will be used
     */
    protected void makeToast(Object message) {
        if (message == null) return;

        String msg = "";

        if (message instanceof Integer)
            msg = getString((Integer) message);
        else
            msg = String.valueOf(message);

        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    protected void setVisibility(int viewId, int visibility) {
        findViewById(viewId).setVisibility(visibility);
    }
}
