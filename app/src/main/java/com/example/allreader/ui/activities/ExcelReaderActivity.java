package com.example.allreader.ui.activities;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.allreader.R;
import com.example.allreader.utils.Constants;
import com.example.allreader.utils.PreferencesHelper;
import com.example.allreader.utils.RecentFilesManager;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ExcelReaderActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private Uri excelUri;
    private ExcelAdapter adapter;
    private boolean isNightMode = false;
    private boolean isFullscreen = false;
    private String fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_excel_reader);

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Intent intent = getIntent();

        if (intent.getData() != null) {
            excelUri = intent.getData();
            fileName = getFileNameFromUri(excelUri);
        } else {
            String filePath = intent.getStringExtra(Constants.EXTRA_FILE_PATH);
            fileName = intent.getStringExtra(Constants.EXTRA_FILE_NAME);

            if (filePath != null) {
                excelUri = Uri.parse(filePath);
            } else {
                Toast.makeText(this, "No Excel file to open", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(fileName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Load preferences
        isNightMode = PreferencesHelper.isNightMode(this);
        isFullscreen = PreferencesHelper.isFullscreenMode(this);

        if (isFullscreen) {
            enterFullscreen();
        }

        if (isNightMode) {
            applyNightMode();
        }

        // Add to recent files
        if (excelUri != null && fileName != null) {
            long fileSize = getFileSize(excelUri);
            RecentFilesManager.addRecentFile(this, fileName, excelUri.toString(),
                    fileName.endsWith(".xlsx") ? "XLSX" : "XLS", fileSize);
        }

        loadExcel(fileName);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_excel_reader, menu);

        MenuItem nightModeItem = menu.findItem(R.id.action_night_mode);
        nightModeItem.setTitle(isNightMode ? "Day Mode" : "Night Mode");

        MenuItem fullscreenItem = menu.findItem(R.id.action_fullscreen);
        fullscreenItem.setTitle(isFullscreen ? "Exit Fullscreen" : "Fullscreen");

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_night_mode) {
            toggleNightMode();
            return true;
        } else if (id == R.id.action_fullscreen) {
            toggleFullscreen();
            return true;
        } else if (id == R.id.action_share) {
            shareDocument();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void toggleNightMode() {
        isNightMode = !isNightMode;
        PreferencesHelper.setNightMode(this, isNightMode);
        applyNightMode();
        invalidateOptionsMenu();
    }

    private void applyNightMode() {
        if (isNightMode) {
            recyclerView.setBackgroundColor(0xFF1E1E1E);
            getWindow().getDecorView().setBackgroundColor(0xFF1E1E1E);
            Toast.makeText(this, "Night mode ON", Toast.LENGTH_SHORT).show();
        } else {
            recyclerView.setBackgroundColor(0xFFFFFFFF);
            getWindow().getDecorView().setBackgroundColor(0xFFF5F5F5);
            Toast.makeText(this, "Night mode OFF", Toast.LENGTH_SHORT).show();
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void toggleFullscreen() {
        if (isFullscreen) {
            exitFullscreen();
        } else {
            enterFullscreen();
        }
    }

    private void enterFullscreen() {
        isFullscreen = true;
        PreferencesHelper.setFullscreenMode(this, true);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        invalidateOptionsMenu();
        Toast.makeText(this, "Fullscreen mode", Toast.LENGTH_SHORT).show();
    }

    private void exitFullscreen() {
        isFullscreen = false;
        PreferencesHelper.setFullscreenMode(this, false);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (getSupportActionBar() != null) {
            getSupportActionBar().show();
        }

        invalidateOptionsMenu();
        Toast.makeText(this, "Fullscreen off", Toast.LENGTH_SHORT).show();
    }

    private void shareDocument() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        shareIntent.putExtra(Intent.EXTRA_STREAM, excelUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share Spreadsheet"));
    }

    private long getFileSize(Uri uri) {
        try {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                long size = cursor.getLong(sizeIndex);
                cursor.close();
                return size;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = "Spreadsheet.xlsx";
        try {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileName;
    }

    private void loadExcel(String fileName) {
        if (excelUri == null) {
            Toast.makeText(this, "Invalid Excel file", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        new Thread(() -> {
            List<List<String>> excelData = new ArrayList<>();
            int maxColumns = 0;

            try {
                InputStream inputStream = getContentResolver().openInputStream(excelUri);
                if (inputStream == null) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Cannot open Excel file", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                Workbook workbook;
                if (fileName.endsWith(".xlsx")) {
                    workbook = new XSSFWorkbook(inputStream);
                } else {
                    workbook = new HSSFWorkbook(inputStream);
                }

                Sheet sheet = workbook.getSheetAt(0);

                for (Row row : sheet) {
                    List<String> rowData = new ArrayList<>();
                    int lastCellNum = row.getLastCellNum();

                    for (int i = 0; i < lastCellNum; i++) {
                        Cell cell = row.getCell(i);
                        String cellValue = getCellValueAsString(cell);
                        rowData.add(cellValue);
                    }

                    maxColumns = Math.max(maxColumns, rowData.size());
                    excelData.add(rowData);
                }

                for (List<String> row : excelData) {
                    while (row.size() < maxColumns) {
                        row.add("");
                    }
                }

                workbook.close();
                inputStream.close();

                int finalMaxColumns = maxColumns;
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    if (excelData.isEmpty()) {
                        Toast.makeText(this, "Excel file is empty", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    recyclerView.setVisibility(View.VISIBLE);
                    adapter = new ExcelAdapter(excelData, finalMaxColumns);
                    recyclerView.setAdapter(adapter);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error reading Excel: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        CellType cellType = cell.getCellType();

        switch (cellType) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double value = cell.getNumericCellValue();
                    if (value == (long) value) {
                        return String.valueOf((long) value);
                    } else {
                        return String.valueOf(value);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getCellFormula();
                }
            case BLANK:
                return "";
            default:
                return "";
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private class ExcelAdapter extends RecyclerView.Adapter<ExcelAdapter.ViewHolder> {

        private final List<List<String>> data;
        private final int columnCount;
        private final List<HorizontalScrollView> scrollViews = new ArrayList<>();
        private boolean isScrolling = false;

        public ExcelAdapter(List<List<String>> data, int columnCount) {
            this.data = data;
            this.columnCount = columnCount;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_excel_row, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            List<String> rowData = data.get(position);
            holder.bind(rowData, position);

            if (!scrollViews.contains(holder.scrollView)) {
                scrollViews.add(holder.scrollView);
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        @Override
        public void onViewRecycled(@NonNull ViewHolder holder) {
            super.onViewRecycled(holder);
            scrollViews.remove(holder.scrollView);
        }

        private void syncScroll(HorizontalScrollView source, int scrollX) {
            if (isScrolling) return;

            isScrolling = true;
            for (HorizontalScrollView scrollView : scrollViews) {
                if (scrollView != source) {
                    scrollView.scrollTo(scrollX, 0);
                }
            }
            isScrolling = false;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            HorizontalScrollView scrollView;
            LinearLayout rowContainer;
            TextView rowNumberView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                scrollView = itemView.findViewById(R.id.horizontalScrollView);
                rowContainer = itemView.findViewById(R.id.rowContainer);
                rowNumberView = itemView.findViewById(R.id.rowNumber);

                scrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    syncScroll((HorizontalScrollView) v, scrollX);
                });
            }

            public void bind(List<String> rowData, int rowIndex) {
                rowContainer.removeAllViews();

                rowNumberView.setText(String.valueOf(rowIndex + 1));
                rowNumberView.setBackgroundResource(R.drawable.cell_border);

                boolean isHeader = rowIndex == 0;

                for (int i = 0; i < rowData.size(); i++) {
                    String cellValue = rowData.get(i);
                    TextView textView = new TextView(itemView.getContext());
                    textView.setText(cellValue);
                    textView.setPadding(24, 20, 24, 20);
                    textView.setMinWidth(200);
                    textView.setMaxWidth(400);
                    textView.setSingleLine(false);
                    textView.setBackgroundResource(R.drawable.cell_border);

                    if (isHeader) {
                        textView.setTextSize(14);
                        textView.setTypeface(null, android.graphics.Typeface.BOLD);
                        textView.setTextColor(0xFF000000);
                        textView.setBackgroundResource(R.drawable.header_cell_border);
                        rowNumberView.setBackgroundResource(R.drawable.header_cell_border);
                    } else {
                        textView.setTextSize(13);
                        textView.setTextColor(isNightMode ? 0xFFE0E0E0 : 0xFF333333);
                    }

                    rowContainer.addView(textView);
                }
            }
        }
    }
}
