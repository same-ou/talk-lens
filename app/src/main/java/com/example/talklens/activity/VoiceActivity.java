package com.example.talklens.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.talklens.databinding.ActivityVoiceBinding;
import com.example.talklens.util.Language;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceActivity extends AppCompatActivity {

    private ActivityVoiceBinding binding;
    private SpeechRecognizer mSpeechRecognizer;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private TranslateViewModel translateViewModel;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVoiceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeechRecognizer.setRecognitionListener(new VoiceRecognitionListener());
        requestAudioPermission();

        binding.ivBack.setOnClickListener(v -> {
            // Navigate back to the MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            // Finish the current activity
            finish();
        });

        // get the intent data
        Language sourceLanguage = (Language) getIntent().getSerializableExtra("sourceLanguage");
        Language targetLanguage =(Language) getIntent().getSerializableExtra("targetLanguage");

        // set the source and target language
        binding.sourceLang.setText(sourceLanguage.getDisplayName());
        binding.targetLang.setText(targetLanguage.getDisplayName());
        translateViewModel = new ViewModelProvider(this).get(TranslateViewModel.class);

        // set the source and target language
        translateViewModel.setSourceLang(sourceLanguage);
        translateViewModel.setTargetLang(targetLanguage);

        translateViewModel.getTranslatedText().observe(this, s -> {
            if (s.error != null) {
                Toast.makeText(VoiceActivity.this, "Translation Error", Toast.LENGTH_SHORT).show();
            } else {
                binding.etVoice.setText(s.result);
            }
        });

        binding.micButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Start speech recognition
                    startSpeechRecognition(sourceLanguage);
                    return true;
                case MotionEvent.ACTION_UP:
                    // Stop speech recognition
                    stopSpeechRecognition();
                    return true;
            }
            return false;
        });
    } // onCreate

    private void requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION
            );
        }
    } // requestAudioPermission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            // Check if the permission was granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can proceed with speech recognition

            } else {
                // Permission denied, handle accordingly (e.g., show a message to the user)
                Toast.makeText(this, "Permission denied. Speech recognition will not work.", Toast.LENGTH_SHORT).show();
            }
        }
    } // onRequestPermissionsResult
    private void startSpeechRecognition(Language sourceLanguage) {

        // Start speech recognition
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, sourceLanguage.getCode());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");

        try {
           // Toast.makeText(this, "Listening...", Toast.LENGTH_SHORT).show();
            mSpeechRecognizer.startListening(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Speech recognition not supported on this device", Toast.LENGTH_SHORT).show();
        }
    } // startSpeechRecognition
    private void stopSpeechRecognition() {
        // Stop speech recognition
        // Toast.makeText(this, "Stopped listening", Toast.LENGTH_SHORT).show();
        mSpeechRecognizer.stopListening();
    } // stopSpeechRecognition
    private class VoiceRecognitionListener implements RecognitionListener {

        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.d("VoiceActivity", "onReadyForSpeech");
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.d("VoiceActivity", "onBeginningOfSpeech");
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            Log.d("VoiceActivity", "onRmsChanged");
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            Log.d("VoiceActivity", "onBufferReceived");
        }

        @Override
        public void onEndOfSpeech() {
            Log.d("VoiceActivity", "onEndOfSpeech");
        }

        @Override
        public void onError(int error) {
            Log.d("VoiceActivity", "onError");
        }

        @Override
        public void onResults(Bundle results) {
            // Called when speech recognition is successful
            // Retrieve the recognized text
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && matches.size() > 0) {
                String recognizedText = matches.get(0);
                binding.tvVoice.setText(recognizedText);
                translateViewModel.sourceText.setValue(recognizedText);
            }
            else {
                Toast.makeText(VoiceActivity.this, "No speech was recognized", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            ArrayList<String> partialMatches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (partialMatches != null && !partialMatches.isEmpty()) {
                String partialText = partialMatches.get(0);
                // Do something with the partial result, like updating your UI
                updateUIWithPartialText(partialText);
            }
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
            Log.d("VoiceActivity", "onEvent");
        }
    } // VoiceRecognitionListener
    private void updateUIWithPartialText(String partialText) {
        // Update the UI with the partial text
        runOnUiThread(() -> {
            binding.tvVoice.setText(partialText);
        });
    } // updateUIWithPartialText
}// class