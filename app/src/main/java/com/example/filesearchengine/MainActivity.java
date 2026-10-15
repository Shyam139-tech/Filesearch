package com.example.filesearchengine;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Android File Search Engine mini project.
 * Folder traversal runs on a background thread, not the Android UI thread.
 */
public class MainActivity extends Activity {
    private static final int PICK_FOLDER = 1001;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);

    private EditText queryInput;
    private TextView folderLabel;
    private TextView statusLabel;
    private TextView resultsText;
    private Button searchButton;
    private Button stopButton;
    private Uri selectedTreeUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(18));
        root.setBackgroundColor(0xFFF7F8FA);

        TextView title = new TextView(this);
        title.setText("File Search Engine");
        title.setTextSize(26);
        title.setTextColor(0xFF172554);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("Search files in a folder using background threading");
        subtitle.setTextSize(14);
        subtitle.setTextColor(0xFF475569);
        LinearLayout.LayoutParams subParams = matchWrap();
        subParams.bottomMargin = dp(18);
        root.addView(subtitle, subParams);

        Button chooseButton = new Button(this);
        chooseButton.setText("Choose folder");
        root.addView(chooseButton, matchWrap());

        folderLabel = new TextView(this);
        folderLabel.setText("No folder selected");
        folderLabel.setTextSize(13);
        folderLabel.setTextColor(0xFF334155);
        LinearLayout.LayoutParams folderParams = matchWrap();
        folderParams.topMargin = dp(6);
        folderParams.bottomMargin = dp(12);
        root.addView(folderLabel, folderParams);

        queryInput = new EditText(this);
        queryInput.setSingleLine(true);
        queryInput.setHint("Enter part of a file name");
        root.addView(queryInput, matchWrap());

        searchButton = new Button(this);
        searchButton.setText("Search");
        root.addView(searchButton, matchWrap());

        stopButton = new Button(this);
        stopButton.setText("Stop search");
        stopButton.setEnabled(false);
        root.addView(stopButton, matchWrap());

        statusLabel = new TextView(this);
        statusLabel.setText("Choose a folder to begin.");
        statusLabel.setTextSize(14);
        statusLabel.setTextColor(0xFF0F766E);
        LinearLayout.LayoutParams statusParams = matchWrap();
        statusParams.topMargin = dp(8);
        statusParams.bottomMargin = dp(8);
        root.addView(statusLabel, statusParams);

        TextView resultsHeading = new TextView(this);
        resultsHeading.setText("Search results");
        resultsHeading.setTextSize(18);
        resultsHeading.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(resultsHeading, matchWrap());

        resultsText = new TextView(this);
        resultsText.setText("Results will appear here.");
        resultsText.setTextSize(13);
        resultsText.setTextColor(0xFF111827);
        resultsText.setTextIsSelectable(true);
        resultsText.setPadding(dp(10), dp(10), dp(10), dp(10));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(resultsText);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollParams.topMargin = dp(6);
        root.addView(scroll, scrollParams);

        setContentView(root);

        chooseButton.setOnClickListener(v -> chooseFolder());
        searchButton.setOnClickListener(v -> startSearch());
        stopButton.setOnClickListener(v -> {
            stopRequested.set(true);
            stopButton.setEnabled(false);
            statusLabel.setText("Stopping search...");
        });
    }

    private void chooseFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_FOLDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FOLDER && resultCode == RESULT_OK && data != null) {
            selectedTreeUri = data.getData();
            int flags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
            try {
                getContentResolver().takePersistableUriPermission(selectedTreeUri, flags);
            } catch (SecurityException ignored) {
                // The picker may already have granted the required read access.
            }
            DocumentFile folder = DocumentFile.fromTreeUri(this, selectedTreeUri);
            folderLabel.setText(folder != null ? folder.getName() : selectedTreeUri.toString());
            statusLabel.setText("Folder selected. Enter a name and tap Search.");
        }
    }

    private void startSearch() {
        if (selectedTreeUri == null) {
            Toast.makeText(this, "Choose a folder first.", Toast.LENGTH_SHORT).show();
            return;
        }
        final String query = queryInput.getText().toString().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            queryInput.setError("Enter a file name or part of one");
            return;
        }

        stopRequested.set(false);
        searchButton.setEnabled(false);
        stopButton.setEnabled(true);
        resultsText.setText("");
        statusLabel.setText("Searching in background...");

        final Uri treeUri = selectedTreeUri;
        executor.execute(() -> {
            List<String> batch = new ArrayList<>();
            int count = searchTree(DocumentFile.fromTreeUri(this, treeUri), query, batch);
            final boolean stopped = stopRequested.get();
            mainHandler.post(() -> {
                resultsText.setText(batch.isEmpty() ? "No matching files found."
                        : joinLines(batch));
                statusLabel.setText((stopped ? "Search stopped. " : "Search complete. ")
                        + count + " matching file(s).");
                searchButton.setEnabled(true);
                stopButton.setEnabled(false);
            });
        });
    }

    private int searchTree(DocumentFile folder, String query, List<String> matches) {
        if (folder == null || stopRequested.get()) return matches.size();

        DocumentFile[] children;
        try {
            children = folder.listFiles();
        } catch (SecurityException e) {
            return matches.size();
        }

        for (DocumentFile child : children) {
            if (stopRequested.get()) break;

            if (child.isDirectory()) {
                searchTree(child, query, matches);
            } else {
                String name = child.getName();
                if (name != null && name.toLowerCase(Locale.ROOT).contains(query)) {
                    matches.add(child.getName() + "\n" + child.getUri().toString());
                    final String currentResults = joinLines(matches);
                    mainHandler.post(() -> resultsText.setText(currentResults));
                }
            }
        }
        return matches.size();
    }

    private String joinLines(List<String> items) {
        StringBuilder out = new StringBuilder();
        for (String item : items) {
            if (out.length() > 0) out.append("\n\n");
            out.append(item);
        }
        return out.toString();
    }

    @Override
    protected void onDestroy() {
        stopRequested.set(true);
        executor.shutdownNow();
        super.onDestroy();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
