package com.example.musicai;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class ChordEditorActivity extends BaseActivity {
    
    public static final String EXTRA_CHORD_ID = "chord_id";
    public static final int MODE_SELECT = 0;
    public static final int MODE_EDIT = 1;
    
    private int currentMode = MODE_SELECT;
    private String selectedEntryId = null;
    private boolean isOriginalEntry = false;
}
