package com.novyr.callfilter.ui.loglist;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import com.novyr.callfilter.ContactFinder;
import com.novyr.callfilter.R;
import com.novyr.callfilter.formatter.LogDateFormatter;
import com.novyr.callfilter.formatter.LogMessageFormatter;
import com.novyr.callfilter.permissions.PermissionChecker;
import com.novyr.callfilter.ui.rulelist.RuleListActivity;
import com.novyr.callfilter.viewmodel.LogViewModel;

public class LogListActivity extends AppCompatActivity {
    private RecyclerView mLogList;
    private LogViewModel mLogViewModel;
    private Snackbar mPermissionNotice;
    private PermissionChecker mPermissionChecker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_log_list);

        mLogList = findViewById(R.id.log_list);

        mLogViewModel = new ViewModelProvider(this).get(LogViewModel.class);

        final ContactFinder contactFinder = new ContactFinder(this);
        final LogListMenuHandler menuHandler = new LogListMenuHandler(
                this,
                contactFinder,
                mLogViewModel
        );

        final LogListAdapter adapter = new LogListAdapter(
                this,
                new LogMessageFormatter(getResources(), contactFinder),
                new LogDateFormatter(),
                menuHandler
        );

        mLogList.setAdapter(adapter);
        mLogList.setLayoutManager(new LinearLayoutManager(this));

        final TextView emptyView = findViewById(R.id.empty_view);

        mLogViewModel.findAll().observe(this, entities -> {
            adapter.setEntities(entities);

            if (adapter.getItemCount() > 0) {
                mLogList.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
            } else {
                mLogList.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            }
        });

        mPermissionChecker = new PermissionChecker(this, errors -> {
            if (mPermissionNotice != null) {
                mPermissionNotice.dismiss();
                mPermissionNotice = null;
            }

            if (errors.size() < 1) {
                return;
            }

            StringBuilder errorMessage = new StringBuilder();
            for (int i = 0; i < errors.size(); i++) {
                if (errorMessage.length() > 0) {
                    errorMessage.append("\n");
                }
                errorMessage.append(errors.get(i));
            }

            mPermissionNotice = Snackbar.make(mLogList, errorMessage, Snackbar.LENGTH_INDEFINITE);
            mPermissionNotice
                    .setAction(
                            R.string.permission_notice_retry,
                            view -> mPermissionChecker.onStart()
                    )
                    .show();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        mPermissionChecker.onStart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_log_viewer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_rules) {
            startActivity(new Intent(this, RuleListActivity.class));
            return true;
        } else if (itemId == R.id.action_clear_log) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_clear_logs_title)
                    .setMessage(R.string.dialog_clear_logs_message)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setPositiveButton(
                            R.string.yes,
                            (dialog, whichButton) -> mLogViewModel.deleteAll()
                    )
                    .setNegativeButton(R.string.no, null)
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        mPermissionChecker.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        mPermissionChecker.onActivityResult(requestCode, resultCode, data);
    }
}
