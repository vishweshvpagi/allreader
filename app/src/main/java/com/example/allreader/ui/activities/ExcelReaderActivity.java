package com.example.allreader.ui.activities;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_excel_reader);

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Handle both internal and external intents
        Intent intent = getIntent();
        String fileName;

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

        loadExcel(fileName);
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

                // Create workbook based on file type
                Workbook workbook;
                if (fileName.endsWith(".xlsx")) {
                    workbook = new XSSFWorkbook(inputStream);  // For .xlsx
                } else {
                    workbook = new HSSFWorkbook(inputStream);  // For .xls
                }

                // Get first sheet
                Sheet sheet = workbook.getSheetAt(0);

                // Read all rows
                for (Row row : sheet) {
                    List<String> rowData = new ArrayList<>();

                    for (Cell cell : row) {
                        String cellValue = getCellValueAsString(cell);
                        rowData.add(cellValue);
                    }

                    excelData.add(rowData);
                }

                workbook.close();
                inputStream.close();

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    if (excelData.isEmpty()) {
                        Toast.makeText(this, "Excel file is empty", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    recyclerView.setVisibility(View.VISIBLE);
                    adapter = new ExcelAdapter(excelData);
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
                // Check if it's a date
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
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

    // RecyclerView Adapter
    private class ExcelAdapter extends RecyclerView.Adapter<ExcelAdapter.ViewHolder> {

        private final List<List<String>> data;

        public ExcelAdapter(List<List<String>> data) {
            this.data = data;
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
            holder.bind(rowData, position == 0);  // First row is header
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            LinearLayout rowContainer;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                rowContainer = itemView.findViewById(R.id.rowContainer);
            }

            public void bind(List<String> rowData, boolean isHeader) {
                rowContainer.removeAllViews();

                for (String cellValue : rowData) {
                    TextView textView = new TextView(itemView.getContext());
                    textView.setText(cellValue);
                    textView.setPadding(16, 16, 16, 16);
                    textView.setMinWidth(200);

                    if (isHeader) {
                        textView.setTextSize(16);
                        textView.setTypeface(null, android.graphics.Typeface.BOLD);
                        textView.setBackgroundColor(0xFFEEEEEE);
                    } else {
                        textView.setTextSize(14);
                    }

                    rowContainer.addView(textView);
                }
            }
        }
    }
}
