package fr.geobert.radis.ui;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import fr.geobert.radis.MainActivity;
import fr.geobert.radis.R;
import fr.geobert.radis.data.Operation;
import fr.geobert.radis.db.DbContentProvider;
import fr.geobert.radis.db.InfoTables;
import fr.geobert.radis.db.OperationTable;
import fr.geobert.radis.tools.CorrectCommaWatcher;
import fr.geobert.radis.tools.Formater;
import fr.geobert.radis.tools.InfoAdapter;
import fr.geobert.radis.tools.MyAutoCompleteTextView;
import fr.geobert.radis.tools.QuickAddTextWatcher;
import fr.geobert.radis.tools.Tools;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class QuickAddController {
    private MyAutoCompleteTextView mQuickAddThirdParty;
    private EditText mQuickAddAmount;
    private ImageButton mQuickAddButton;
    private QuickAddTextWatcher mQuickAddTextWatcher;
    private CorrectCommaWatcher mCorrectCommaWatcher;
    private MainActivity mActivity;
    private long mAccountId = 0;
    private LinearLayout mLayout;

    public QuickAddController(MainActivity activity, View container) {
        mActivity = activity;
        mLayout = (LinearLayout) container.findViewById(R.id.quick_add_layout);
        mQuickAddThirdParty = (MyAutoCompleteTextView) container.findViewById(R.id.quickadd_third_party);
        mQuickAddAmount = (EditText) container.findViewById(R.id.quickadd_amount);
        mQuickAddButton = (ImageButton) container.findViewById(R.id.quickadd_validate);
        mQuickAddThirdParty.setNextFocusDownId(R.id.quickadd_amount);
        mCorrectCommaWatcher = new CorrectCommaWatcher(Formater.getSumFormater().getDecimalFormatSymbols()
                .getDecimalSeparator(), mQuickAddAmount).setAutoNegate(true);

        mQuickAddTextWatcher = new QuickAddTextWatcher(mQuickAddThirdParty, mQuickAddAmount, mQuickAddButton);

        QuickAddController.setQuickAddButEnabled(mQuickAddButton, false);
        mQuickAddButton.post(new Runnable() {
            private void adjustImageButton(ImageButton btn) {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) btn.getLayoutParams();
                params.bottomMargin = 3;
                params.height = mQuickAddThirdParty.getMeasuredHeight();
                btn.setLayoutParams(params);
            }

            @Override
            public void run() {
                if (Build.VERSION.SDK_INT < 11) {
                    adjustImageButton(mQuickAddButton);
                }
            }
        });
    }

    public static void setQuickAddButEnabled(ImageButton but, boolean b) {
        but.setEnabled(b);
    }

    public void setAccount(long accountId) {
        mAccountId = accountId;
        setEnabled(accountId != 0);
    }

    public void initViewBehavior() {
        mQuickAddThirdParty.setAdapter(new InfoAdapter(mActivity, DbContentProvider.THIRD_PARTY_URI,
                InfoTables.KEY_THIRD_PARTY_NAME, true));

        mQuickAddAmount.addTextChangedListener(mCorrectCommaWatcher);

        mQuickAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    quickAddOp();
                } catch (Exception e) {
                    Tools.popError(mActivity, e.getMessage(), null);
                    e.printStackTrace();
                }
            }
        });
        mQuickAddButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                showDatePicker();
                return true;
            }
        });
        QuickAddController.setQuickAddButEnabled(mQuickAddButton, false);

        mQuickAddThirdParty.addTextChangedListener(mQuickAddTextWatcher);
        mQuickAddAmount.addTextChangedListener(mQuickAddTextWatcher);
        mQuickAddAmount.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,
                                          KeyEvent event) {
                try {
                    if (mQuickAddButton.isEnabled()) {
                        quickAddOp();
                    } else {
                        Tools.popError(mActivity, mActivity.getString(R.string.quickadd_fields_not_filled), null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
    }

    private void showDatePicker() {
        GregorianCalendar today = new GregorianCalendar();
        DatePickerDialog dialog = new DatePickerDialog(mActivity, new DatePickerDialog.OnDateSetListener() {
            private int alreadyFired = -1;

            @Override
            public void onDateSet(DatePicker datePicker, int y, int m, int d) {
                // workaround known android bug
                if (alreadyFired % 2 == 0) {
                    GregorianCalendar date = new GregorianCalendar(y, m, d);
                    try {
                        quickAddOp(date);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                alreadyFired++;
            }
        }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
        dialog.setTitle(R.string.op_date);
        dialog.setButton(DatePickerDialog.BUTTON_NEGATIVE, mActivity.getString(android.R.string.cancel),
                (DialogInterface.OnClickListener) null);
        dialog.show();
    }

    private void quickAddOp() throws Exception {
        quickAddOp(null);
    }

    private void quickAddOp(GregorianCalendar date) throws Exception {
        Operation op = new Operation();
        if (date != null) {
            Tools.clearTimeOfCalendar(date);
            op.setDate(date.getTimeInMillis());
        }
        op.setmThirdParty(mQuickAddThirdParty.getText().toString());
        op.setSumStr(mQuickAddAmount.getText().toString());
        assert (mAccountId != 0);
        if (OperationTable.createOp(mActivity, op, mAccountId) > -1) {
            mActivity.updateDisplay(null);
        }

        mQuickAddAmount.setText("");
        mQuickAddThirdParty.setText("");
        InputMethodManager mgr = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(mQuickAddAmount.getWindowToken(), 0);
        mCorrectCommaWatcher.setAutoNegate(true);
        mQuickAddAmount.clearFocus();
        mQuickAddThirdParty.clearFocus();
    }

    public void clearFocus() {
        mQuickAddThirdParty.clearFocus();
        InputMethodManager imm = (InputMethodManager) mActivity
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mQuickAddThirdParty.getWindowToken(), 0);
    }

    public void getFocus() {
        mQuickAddThirdParty.requestFocus();
    }

    public void setAutoNegate(boolean autoNeg) {
        mCorrectCommaWatcher.setAutoNegate(autoNeg);
    }

    public void setEnabled(boolean enabled) {
        mQuickAddThirdParty.setEnabled(enabled);
        mQuickAddAmount.setEnabled(enabled);
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putCharSequence("third_party", mQuickAddThirdParty.getText());
        outState.putCharSequence("amount", mQuickAddAmount.getText());
    }

    public void onRestoreInstanceState(Bundle state) {
        mQuickAddThirdParty.setText(state.getCharSequence("third_party"));
        mQuickAddAmount.setText(state.getCharSequence("amount"));
    }

    public void setVisibility(int visibility) {
        mLayout.setVisibility(visibility);
    }

    public boolean isVisible() {
        return mLayout.getVisibility() == View.VISIBLE;
    }
}
