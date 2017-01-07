package com.braindump.md;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MarkdownViewerActivity extends AppCompatActivity {

    private ParseMarkdownTask parseMarkdownTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_markdown_viewer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data != null) {
            String fileName = readFileName(data);
            if (fileName != null && actionBar != null) {
                actionBar.setTitle(fileName);
            }
            parseMarkdownTask = new ParseMarkdownTask();
            parseMarkdownTask.execute(data);
        } else {
            displayErrorAndExit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (parseMarkdownTask != null && !parseMarkdownTask.isCancelled()) {
            parseMarkdownTask.cancel(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String readFileName(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            return null;
        }

        String result = null;
        try {
            if (cursor.moveToFirst()) {
                result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    private void displayErrorAndExit() {
        View view = findViewById(R.id.container_markdown_viewer);
        Snackbar
            .make(view, R.string.error_no_markdown_received, Snackbar.LENGTH_SHORT)
            .addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                @Override
                public void onDismissed(Snackbar transientBottomBar, int event) {
                    finish();
                }
            })
            .show();
    }

    private class ParseMarkdownTask extends AsyncTask<Uri, Void, String> {
        @Override
        protected String doInBackground(Uri... params) {
            String markdown = null;
            try (InputStream inputStream = getContentResolver().openInputStream(params[0])) {
                if (inputStream != null) {
                    try (
                        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                        BufferedReader reader = new BufferedReader(inputStreamReader)
                    ) {
                        Parser parser = Parser.builder().build();
                        HtmlRenderer renderer = HtmlRenderer.builder().build();
                        markdown = renderer.render(parser.parseReader(reader));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return markdown;
        }

        protected void onPostExecute(String markdown) {
            if (markdown != null) {
                HtmlTextView htmlTextView = (HtmlTextView) findViewById(R.id.rendered_markdown);
                htmlTextView.setHtml(markdown);
            } else {
                displayErrorAndExit();
            }
        }
    }
}