package com.example.talklens.activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.talklens.R;
import com.example.talklens.databinding.ActivityMainBinding;
import com.example.talklens.util.Language;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set up view binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        final TranslateViewModel viewModel = new ViewModelProvider(this).get(TranslateViewModel.class);

        // Set up language spinners and default selections
        final ArrayAdapter<Language> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, viewModel.getAvailableLanguages());
        binding.sourceLangSelector.setAdapter(adapter);
        binding.targetLangSelector.setAdapter(adapter);
        binding.sourceLangSelector.setSelection(adapter.getPosition(new Language("en")));
        binding.targetLangSelector.setSelection(adapter.getPosition(new Language("fr")));

        // Set up language selection listeners
        // Translate when the selection of the target language is changed
        binding.sourceLangSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setProgressText(binding.targetText);
                viewModel.sourceLang.setValue(adapter.getItem(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                binding.targetText.setText("");
            }
        });

        // Translate when the selection of the source language is changed
        binding.targetLangSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setProgressText(binding.targetText);
                viewModel.targetLang.setValue(adapter.getItem(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                binding.targetText.setText("");
            }
        });

        // Set up switch button click listener
        // swap the input and output language when the button is clicked
        binding.buttonSwitchLang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String targetText = binding.targetText.getText().toString();
                setProgressText(binding.targetText);
                int sourceLangPosition = binding.sourceLangSelector.getSelectedItemPosition();
                binding.sourceLangSelector.setSelection(binding.targetLangSelector.getSelectedItemPosition());
                binding.targetLangSelector.setSelection(sourceLangPosition);
                binding.sourceText.setText(targetText);
                viewModel.sourceText.setValue(targetText);
            }
        });

        // Set up sync toggle button listeners
        binding.buttonSyncSource.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Language language = adapter.getItem(binding.sourceLangSelector.getSelectedItemPosition());
                if (isChecked) {
                    viewModel.downloadLanguage(language);
                } else {
                    viewModel.deleteLanguage(language);
                }
            }
        });

        binding.buttonSyncTarget.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Language language = adapter.getItem(binding.targetLangSelector.getSelectedItemPosition());
                if (isChecked) {
                    viewModel.downloadLanguage(language);
                } else {
                    viewModel.deleteLanguage(language);
                }
            }
        });

        // Translate input text as it is typed
        binding.sourceText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                setProgressText(binding.targetText);
                viewModel.sourceText.postValue(s.toString());
            }
        });

        // Observe translated text changes
        viewModel.translatedText.observe(this, new Observer<TranslateViewModel.ResultOrError>() {
            @Override
            public void onChanged(TranslateViewModel.ResultOrError resultOrError) {
                if (resultOrError.error != null) {
                    binding.sourceText.setError(resultOrError.error.getLocalizedMessage());
                } else {
                    binding.targetText.setVisibility(View.VISIBLE);
                    binding.targetText.setText(resultOrError.result);
                }
            }
        });

        // Update sync toggle button states based on downloaded models list
        viewModel.availableModels.observe(this, new Observer<List<String>>() {
            @Override
            public void onChanged(List<String> translateRemoteModels) {
                String output = getString(R.string.downloaded_models_label, translateRemoteModels);
                binding.downloadedModels.setText(output);
                binding.buttonSyncSource.setChecked(!viewModel.requiresModelDownload(
                        adapter.getItem(binding.sourceLangSelector.getSelectedItemPosition()), translateRemoteModels));
                binding.buttonSyncTarget.setChecked(!viewModel.requiresModelDownload(
                        adapter.getItem(binding.targetLangSelector.getSelectedItemPosition()), translateRemoteModels));
            }
        });


        // Set up mic button click listener
        binding.micButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, VoiceActivity.class);
            intent.putExtra("sourceLanguage", adapter.getItem(binding.sourceLangSelector.getSelectedItemPosition()));
            intent.putExtra("targetLanguage", adapter.getItem(binding.targetLangSelector.getSelectedItemPosition()));
            startActivity(intent);
        });

        // set up camera button click listener
       binding.cameraBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CamActivity.class);
            startActivity(intent);
        });

        // set up conversation button click listener
        binding.convButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ConversationActivity.class);
            intent.putExtra("sourceLanguage", adapter.getItem(binding.sourceLangSelector.getSelectedItemPosition()));
            intent.putExtra("targetLanguage", adapter.getItem(binding.targetLangSelector.getSelectedItemPosition()));
            startActivity(intent);
        });

    } // OnCreate

    private void setProgressText(TextView tv) {
        binding.targetText.setVisibility(View.VISIBLE);
        tv.setText(getString(R.string.translate_progress));
    }
}