/*
 * Copyright (c) 2015, dhis2
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.hisp.dhis.android.sdk.utils.ui.adapters.rows.dataentry;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.hisp.dhis.android.sdk.R;
import org.hisp.dhis.android.sdk.controllers.Dhis2;
import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis.android.sdk.fragments.dataentry.DataEntryFragment;
import org.hisp.dhis.android.sdk.fragments.dataentry.ValidationErrorDialog;
import org.hisp.dhis.android.sdk.persistence.models.BaseValue;
import org.hisp.dhis.android.sdk.persistence.models.Event;

import java.util.ArrayList;

public final class StatusRow implements DataEntryRow {
    public static final String CLASS_TAG = StatusRow.class.getSimpleName();

    private final Event mEvent;
    private Context context;
    private boolean mHidden = false;
    private boolean editable = true;
    private StatusViewHolder holder;
    private FragmentActivity fragmentActivity;

    public StatusRow(Context context, Event event) {
        this.context = context;
        mEvent = event;
    }

    public void setFragmentActivity(FragmentActivity fragmentActivity) {
        this.fragmentActivity = fragmentActivity;
    }

    @Override
    public View getView(FragmentManager fragmentManager, LayoutInflater inflater,
                        View convertView, ViewGroup container) {
        View view;

        if (convertView != null && convertView.getTag() instanceof StatusViewHolder) {
            view = convertView;
            holder = (StatusViewHolder) view.getTag();
        } else {
            View root = inflater.inflate(
                    R.layout.listview_row_status, container, false);
            holder = new StatusViewHolder(context, root, mEvent);

            root.setTag(holder);
            view = root;
        }
        holder.onValidateButtonClickListener.setFragmentActivity(fragmentActivity);
        holder.onCompleteButtonClickListener.setActivity(fragmentActivity);

        if(!isEditable())
        {
            holder.complete.setEnabled(false);
            holder.validate.setEnabled(false);
        }
        else
        {
            holder.complete.setEnabled(true);
            holder.validate.setEnabled(true);
        }

        return view;
    }

    @Override
    public int getViewType() {
        return DataEntryRowTypes.COORDINATES.ordinal();
    }

    @Override
    public BaseValue getBaseValue() {
        return null;
    }

    @Override
    public boolean isHidden() {
        return mHidden;
    }

    @Override
    public void setHidden(boolean hidden) {
        mHidden = hidden;
    }

    @Override
    public boolean isEditable() {
        return editable;
    }

    @Override
    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    private static class StatusViewHolder {
        private final Button complete;
        private final Button validate;
        private final OnCompleteClickListener onCompleteButtonClickListener;
        private final OnValidateClickListener onValidateButtonClickListener;
        private final Event event;

        public StatusViewHolder(Context context, View view, Event event) {

            this.event = event;

            /* views */
            complete = (Button) view.findViewById(R.id.complete);
            validate = (Button) view.findViewById(R.id.validate);

            /* text watchers and click listener */
            onCompleteButtonClickListener = new OnCompleteClickListener(context, complete, this.event);
            onValidateButtonClickListener = new OnValidateClickListener(context, validate, this.event);
            complete.setOnClickListener(onCompleteButtonClickListener);
            validate.setOnClickListener(onValidateButtonClickListener);

            updateViews(event, complete, context);
        }

        public static void updateViews(Event event, Button button, Context context) {
            if(event.getStatus().equals(Event.STATUS_COMPLETED)) {
                if(context != null) {
                    button.setText(context.getString(R.string.incomplete));
                }
            } else {
                if(context != null) {
                    button.setText(context.getString(R.string.complete));
                }
            }
        }
    }

    private static class OnCompleteClickListener implements View.OnClickListener, DialogInterface.OnClickListener {
        private final Button complete;
        private final Event event;
        private final Context context;
        private Activity activity;

        public OnCompleteClickListener(Context context, Button complete, Event event) {
            this.context = context;
            this.complete = complete;
            this.event = event;
        }

        @Override
        public void onClick(View v) {
            if(activity==null) return;
            String label = event.getStatus().equals(Event.STATUS_COMPLETED) ?
                    activity.getString(R.string.incomplete) : activity.getString(R.string.complete);
            String action = event.getStatus().equals(Event.STATUS_COMPLETED) ?
                    activity.getString(R.string.incomplete_confirm) : activity.getString(R.string.complete_confirm);
            Dhis2.showConfirmDialog(activity, label, action, label, activity.getString(R.string.cancel), this);
        }

        private void setActivity(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if(event.getStatus().equals(Event.STATUS_COMPLETED)) {
                event.setStatus(Event.STATUS_ACTIVE);
            } else {
                event.setStatus(Event.STATUS_COMPLETED);
            }
            StatusViewHolder.updateViews(event, complete, context);
        }
    }

    private static class OnValidateClickListener implements View.OnClickListener {
        private final Button validate;
        private final Event event;
        private final Context context;
        private FragmentActivity fragmentActivity;

        public OnValidateClickListener(Context context, Button validate, Event event) {
            this.validate = validate;
            this.event = event;
            this.context = context;
        }

        @Override
        public void onClick(View v) {
            ArrayList<String> errors = DataEntryFragment.isEventValid(event,
                    MetaDataController.getProgramStage(event.getProgramStageId()), context);
            if (!errors.isEmpty()) {
                ValidationErrorDialog dialog = ValidationErrorDialog
                        .newInstance(errors);
                if(fragmentActivity!=null) {
                    FragmentManager fm = fragmentActivity.getSupportFragmentManager();
                    dialog.show(fm);
                }
            } else {
                ValidationErrorDialog dialog = ValidationErrorDialog
                        .newInstance(context.getString(R.string.validation_success), new ArrayList<String>());
                if(fragmentActivity!=null) {
                    FragmentManager fm = fragmentActivity.getSupportFragmentManager();
                    dialog.show(fm);
                }
            }
        }

        public void setFragmentActivity(FragmentActivity fragmentActivity) {
            this.fragmentActivity = fragmentActivity;
        }
    }
}