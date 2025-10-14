package com.example.allreader.ui.activities;

import android.net.Uri;
import android.os.Bundle;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.allreader.R;
import com.example.allreader.utils.Constants;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

import java.io.InputStream;

public class ExcelReaderActivity extends AppCompatActivity {

    private TableLayout tableLayout;
    private String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_excel_reader);

        tableLayout = findViewById(R.id.tableLayout);

        filePath = getIntent().getStringExtra(Constants.EXTRA_FILE_PATH);
        String fileName = getIntent().getStringExtra(Constants.EXTRA_FILE_NAME);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(fileName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        loadExcel();
    }

    private void loadExcel() {
        try {
            Uri uri = Uri.parse(filePath);
            InputStream inputStream = getContentResolver().openInputStream(uri);

            Workbook workbook = Workbook.getWorkbook(inputStream);
            Sheet sheet = workbook.getSheet(0);

            for (int row = 0; row < sheet.getRows(); row++) {
                TableRow tableRow = new TableRow(this);

                for (int col = 0; col < sheet.getColumns(); col++) {
                    Cell cell = sheet.getCell(col, row);
                    TextView textView = new TextView(this);
                    textView.setText(cell.getContents());
                    textView.setPadding(16, 16, 16, 16);
                    textView.setTextSize(14);
                    tableRow.addView(textView);
                }

                tableLayout.addView(tableRow);
            }

            workbook.close();
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (BiffException e) {
            Toast.makeText(this, "Invalid Excel format: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error loading Excel: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
