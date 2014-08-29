package ae.threef.ocr.fragments.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.DatePicker;
import android.widget.TimePicker;

import java.util.Calendar;
import java.util.Date;

import ae.threef.ocr.R;

public class DateTimePickerDialog extends DialogFragment
        implements View.OnClickListener {

    public interface DateTimePickerHost {
        /**
         * Called when the user submits the date\time, the dialog won't be dismissed by itself, so validation can be performed.
         * @param dlg The dialog containing the pickers
         * @param date The date picked by the user
         */
        public void onDatePicked(DateTimePickerDialog dlg, Date date);
    }

    DatePicker datePicker;
    TimePicker timePicker;

    /**
     * Creates a new date & time picker dialog
     */
    public DateTimePickerDialog() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dlg_date_time_picker_dialog, container, false);

        datePicker = ((DatePicker) view.findViewById(R.id.datePicker));
        timePicker = ((TimePicker) view.findViewById(R.id.timePicker));

        view.findViewById(R.id.vOk).setOnClickListener(this);
        view.findViewById(R.id.vCancel).setOnClickListener(this);

        return view;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.vOk:
                DateTimePickerHost host = getHost();
                if (host != null) {
                    Date date = getSelectedDate();
                    host.onDatePicked(this, date);
                }

                // No dismiss

                break;

            case R.id.vCancel:
                dismiss();
                break;
        }
    }

    /**
     * Gets the selected date time combination
     * @return Date object representing the combination picked
     */
    public Date getSelectedDate() {
        Calendar cal = Calendar.getInstance();

        // Setting date
        cal.set(Calendar.YEAR, datePicker.getYear());
        cal.set(Calendar.MONTH, datePicker.getMonth());
        cal.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());

        // Setting time
        cal.set(Calendar.HOUR_OF_DAY, timePicker.getCurrentHour());
        cal.set(Calendar.MINUTE, timePicker.getCurrentMinute());
        cal.set(Calendar.SECOND, 0);

        return cal.getTime();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dlg = super.onCreateDialog(savedInstanceState);

        if (dlg != null)
            dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);

        return dlg;
    }

    private DateTimePickerHost getHost() {
        Fragment frg = getTargetFragment();
        if (frg instanceof DateTimePickerHost)
            return (DateTimePickerHost) frg;

        Activity act = getActivity();
        if (act instanceof DateTimePickerHost)
            return (DateTimePickerHost) act;

        return null;
    }
}
