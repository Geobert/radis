package fr.geobert.radis.ui.editor;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import com.actionbarsherlock.app.SherlockFragment;
import fr.geobert.radis.R;
import fr.geobert.radis.data.Account;
import fr.geobert.radis.data.Operation;
import fr.geobert.radis.data.ScheduledOperation;
import fr.geobert.radis.db.AccountTable;

public class ScheduleEditorFragment extends SherlockFragment implements OnTransfertCheckedChangeListener {
    ScheduledOperation mCurrentSchOp;
    private DatePicker mEndDatePicker;
    private Spinner mAccountSpinner;
    private Spinner mPeriodicitySpinner;
    private EditText mCustomPeriodicityVal;
    private Spinner mCustomPeriodicityUnit;
    private View mCustomPeriodicityCont;
    private CheckBox mEndDateCheck;
    private ScheduledOperationEditor mActivity = null;
    private OpEditFragmentAccessor mOpEditFragment;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.scheduling_edit, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mActivity = (ScheduledOperationEditor) getSherlockActivity();
        mOpEditFragment = mActivity;
        initViewReferences();
        initViewBehavior();
        populateFields();
    }

    private void initViewReferences() {
        mAccountSpinner = (Spinner) mActivity.findViewById(R.id.account_choice);
        mPeriodicitySpinner = (Spinner) mActivity.findViewById(R.id.periodicity_choice);
        mCustomPeriodicityCont = mActivity.findViewById(R.id.custom_periodicity);
        mCustomPeriodicityVal = (EditText) mActivity.findViewById(R.id.custom_periodicity_value);
        mCustomPeriodicityUnit = (Spinner) mActivity.findViewById(R.id.custom_periodicity_choice);
        mEndDatePicker = (DatePicker) mActivity.findViewById(R.id.edit_end_date);
        mEndDateCheck = (CheckBox) mActivity.findViewById(R.id.end_date_check);
    }

    void populateFields() {
        if (mActivity == null) {
            return;
        }
        ScheduledOperation op = mCurrentSchOp;
        mActivity.mCurrentOp = op;
        mCustomPeriodicityVal.setText(mCurrentSchOp.mPeriodicity == 0 ? ""
                : Integer.toString(mCurrentSchOp.mPeriodicity));
        poputatePeriodicitySpinner();
        populateCustomPeriodicitySpinner();
        populateAccountSpinner(((CommonOpEditor) getActivity()).getAccountManager().getAllAccountsCursor());
        if (mCurrentSchOp.getEndDate() > 0) {
            mEndDateCheck.setChecked(true);
            mEndDatePicker.setEnabled(true);
            mEndDatePicker.updateDate(op.getEndYear(), op.getEndMonth(),
                    op.getEndDay());
        } else {
            mEndDateCheck.setChecked(false);
            mEndDatePicker.setEnabled(false);
        }
    }

    private void initViewBehavior() {
        mCustomPeriodicityVal
                .setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (hasFocus) {
                            ((EditText) v).selectAll();
                        }
                    }
                });

        mEndDatePicker.setEnabled(false);

        mEndDateCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                mEndDatePicker.setEnabled(isChecked);
                mEndDatePicker.clearFocus();
            }
        });
        mEndDateCheck.setChecked(false);
    }

    private void populateCustomPeriodicitySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                mActivity, R.array.periodicity_custom_choices,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCustomPeriodicityUnit.setAdapter(adapter);

        int unit = mCurrentSchOp.mPeriodicityUnit;
        if (unit >= ScheduledOperation.CUSTOM_DAILY_PERIOD) {
            mCustomPeriodicityUnit.setSelection(unit
                    - ScheduledOperation.CUSTOM_DAILY_PERIOD);
        }
    }

    private void setCustomPeriodicityVisibility(final boolean isVisible) {
        mCustomPeriodicityCont.setVisibility(isVisible ? View.VISIBLE
                : View.GONE);
    }

    @Override
    public void onTransfertCheckedChanged(boolean isChecked) {
        mAccountSpinner.setEnabled(!isChecked);
        Account acc0 = (Account) mAccountSpinner.getItemAtPosition(0);
        if (acc0 != null) {
            if (isChecked) {
                acc0.mName = getString(R.string.defined_by_transfert);
                mAccountSpinner.setSelection(0);
            } else {
                acc0.mName = getString(R.string.choose_account);
            }
        }
    }

    private void poputatePeriodicitySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                mActivity, R.array.periodicity_choices,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPeriodicitySpinner.setAdapter(adapter);
        mPeriodicitySpinner
                .setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                                               View view, int pos, long id) {
                        setCustomPeriodicityVisibility(pos == (parent
                                .getAdapter().getCount() - 1));
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                    }
                });
        int unit = mCurrentSchOp.mPeriodicityUnit;
        if (unit < ScheduledOperation.CUSTOM_DAILY_PERIOD) {
            mPeriodicitySpinner.setSelection(unit);
        } else {
            mPeriodicitySpinner.setSelection(ScheduledOperation.CUSTOM_DAILY_PERIOD);
        }
    }

    private void populateAccountSpinner(Cursor c) {
        if (c.moveToFirst()) {
            SimpleCursorAdapter adapter = new SimpleCursorAdapter(mActivity,
                    android.R.layout.simple_spinner_item, c, new String[]{AccountTable.KEY_ACCOUNT_NAME},
                    new int[]{android.R.id.text1}, SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mAccountSpinner.setAdapter(adapter);
            if (mCurrentSchOp.mAccountId != 0 || mActivity.mCurAccountId != 0) {
                int pos = 1;
                while (pos < adapter.getCount()) {
                    long id = adapter.getItemId(pos);
                    if (id == mCurrentSchOp.mAccountId || id == mActivity.mCurAccountId) {
                        mAccountSpinner.setSelection(pos);
                        break;
                    } else {
                        pos++;
                    }
                }
            }
            final boolean isTransfert = mOpEditFragment.isTransfertChecked();
            mAccountSpinner.setEnabled(!isTransfert);
            if (isTransfert) {
                mAccountSpinner.setSelection(mOpEditFragment.getSrcAccountSpinnerIdx());
            }
        }
    }

    protected void fillOperationWithInputs(Operation operation) {
        if (mActivity == null) {
            return;
        }
        ScheduledOperation op = (ScheduledOperation) operation;
        if (!mActivity.isTransfertChecked()) {
            Cursor c = (Cursor) mAccountSpinner.getSelectedItem();
            op.mAccountId = c.getLong(0);
        }
        final boolean isCustom = mPeriodicitySpinner.getSelectedItemPosition() == (mPeriodicitySpinner
                .getAdapter().getCount() - 1);
        if (!isCustom) {
            op.mPeriodicity = 1;
            op.mPeriodicityUnit = mPeriodicitySpinner.getSelectedItemPosition();
        } else {
            String periodicity = mCustomPeriodicityVal.getText().toString();
            try {
                op.mPeriodicity = Integer.parseInt(periodicity);
            } catch (NumberFormatException e) {
                StringBuffer b = new StringBuffer();
                for (char c : periodicity.toCharArray()) {
                    if ("0123456789".contains(String.valueOf(c))) {
                        b.append(c);
                    }
                }
                try {
                    op.mPeriodicity = Integer.parseInt(b.toString());
                    mCustomPeriodicityVal.setText(b);
                } catch (NumberFormatException e2) {
                    op.mPeriodicity = 0;
                }
            }
            op.mPeriodicityUnit =
                    mCustomPeriodicityUnit.getSelectedItemPosition() + ScheduledOperation.CUSTOM_DAILY_PERIOD;
        }
        if (mEndDatePicker.isEnabled()) {
            DatePicker dp = mEndDatePicker;
            dp.clearChildFocus(mActivity.getCurrentFocus());
            op.setEndDay(dp.getDayOfMonth());
            op.setEndMonth(dp.getMonth());
            op.setEndYear(dp.getYear());
        } else {
            op.mEndDate.clear();
        }
    }

    boolean isFormValid(StringBuilder errMsg) {
        ScheduledOperation op = mCurrentSchOp;
        boolean res = true;
        if (mPeriodicitySpinner == null) { //
            return res;
        }
        if (mPeriodicitySpinner.getSelectedItemPosition() == (mPeriodicitySpinner
                .getAdapter().getCount() - 1)) {
            try {
                Integer.parseInt(mCustomPeriodicityVal.getText().toString());
            } catch (NumberFormatException e) {
                if (errMsg.length() > 0) {
                    errMsg.append("\n");
                }
                errMsg.append(getString(R.string.periodicity_must_be_num));
                res = false;
            }
        }
        if (res) {
            fillOperationWithInputs(op);
            final boolean hasEnd = op.getEndDate() > 0;
            if (hasEnd && (op.getDate() > op.getEndDate())) {
                if (errMsg.length() > 0) {
                    errMsg.append("\n");
                }
                errMsg.append(getString(R.string.end_date_incorrect));
                res = false;
            }
            if ((op.mPeriodicityUnit >= ScheduledOperation.CUSTOM_DAILY_PERIOD)
                    && op.mPeriodicity <= 0) {
                if (errMsg.length() > 0) {
                    errMsg.append("\n");
                }
                errMsg.append(getString(R.string.periodicity_must_be_greater_0));
                res = false;
            }
        }
        if (!mActivity.isTransfertChecked()) {
            Cursor cursor = (Cursor) mAccountSpinner.getSelectedItem();
            final long accountId = cursor.getLong(0);
            if (accountId == 0) {
                if (errMsg.length() > 0) {
                    errMsg.append("\n");
                }
                errMsg.append(getString(R.string.sch_no_choosen_account));
                res = false;
            }
        }
        return res;
    }

    @Override
    public void onPause() {
        super.onPause();
        fillOperationWithInputs(mActivity.mCurrentOp);
    }
}
