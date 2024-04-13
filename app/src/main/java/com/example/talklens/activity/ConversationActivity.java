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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.talklens.adapter.MessageAdapter;
import com.example.talklens.databinding.ActivityConversationBinding;
import com.example.talklens.item.Message;
import com.example.talklens.util.Language;
import com.example.talklens.util.SenderType;

import java.util.ArrayList;
import java.util.List;

public class ConversationActivity extends AppCompatActivity {
    private ActivityConversationBinding binding;
    private SpeechRecognizer mSpeechRecognizer;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private TranslateViewModel translateViewModel;
    private List<Message> messageList;
    private MessageAdapter messageAdapter;
    private SenderType currentUser = SenderType.USER1;
    Language sourceLanguage;
    Language targetLanguage;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        binding = ActivityConversationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestAudioPermission();
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeechRecognizer.setRecognitionListener(new VoiceRecognitionListener());

        // get the intent data
        sourceLanguage = (Language) getIntent().getSerializableExtra("sourceLanguage");
        targetLanguage =(Language) getIntent().getSerializableExtra("targetLanguage");

        binding.sourceLang.setText(sourceLanguage.getDisplayName());
        binding.targetLang.setText(targetLanguage.getDisplayName());



        // setup recyclerView
        binding.rvConversation.setLayoutManager(new LinearLayoutManager(this));
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        binding.rvConversation.setAdapter(messageAdapter);

       binding.btnUser1.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                currentUser = SenderType.USER1;
                startSpeechRecognition(sourceLanguage);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                stopSpeechRecognition();
            }
            return true;
        });

        binding.btnUser2.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                currentUser = SenderType.USER2;
                startSpeechRecognition(targetLanguage);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                stopSpeechRecognition();
            }
            return true;
        });

        binding.ivBack.setOnClickListener(v -> {
            onBackPressed();
        });

        translateViewModel = new ViewModelProvider(this).get(TranslateViewModel.class);
        translateViewModel.getTranslatedText().observe(this, s -> {
            if (s.error != null) {
                Toast.makeText(ConversationActivity.this, "Translation Error", Toast.LENGTH_SHORT).show();
            } else {
                addMessage(new Message(s.result, currentUser));
            }
        });

    } // onCreate

    private void addMessage(Message message) {
        Log.d("ConversationActivity", "addMessage: " + message.getMessage());
        if (message.getMessage().isEmpty()) {
            return;
        }
        messageList.add(message);
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        binding.rvConversation.smoothScrollToPosition(messageList.size() - 1);
        translateViewModel.sourceText.setValue("");
    } // addMessage

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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.i("ConversationActivity", "Permission denied by user");
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
            Log.d("VoiceRecognitionListener", "onReadyForSpeech");
        }
        @Override
        public void onBeginningOfSpeech() {
            Log.d("VoiceRecognitionListener", "onBeginningOfSpeech");
        }
        @Override
        public void onRmsChanged(float rmsdB) {
            Log.d("VoiceRecognitionListener", "onRmsChanged");
        }
        @Override
        public void onBufferReceived(byte[] buffer) {
            Log.d("VoiceRecognitionListener", "onBufferReceived");
        }
        @Override
        public void onEndOfSpeech() {
            Log.d("VoiceRecognitionListener", "onEndOfSpeech");
        }
        @Override
        public void onError(int error) {
            Log.d("VoiceRecognitionListener", "onError");
        }
        @Override
        public void onResults(Bundle results) {
            Log.d("VoiceRecognitionListener", "onResults");
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            // check rhe current user
            if (currentUser == SenderType.USER1) {
                // get the result
                if (matches != null) {
                    String text = matches.get(0);
                    translateViewModel.setSourceLang(sourceLanguage);
                    translateViewModel.setTargetLang(targetLanguage);
                    translateViewModel.sourceText.setValue(text);
                }
            } else {
                // get the result
                if (matches != null) {
                    String text = matches.get(0);
                    translateViewModel.setSourceLang(targetLanguage);
                    translateViewModel.setTargetLang(sourceLanguage);
                    translateViewModel.sourceText.setValue(text);
                }
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            Log.d("VoiceRecognitionListener", "onPartialResults");
        }
        @Override
        public void onEvent(int eventType, Bundle params) {
            Log.d("VoiceRecognitionListener", "onEvent");
        }
    } // VoiceRecognitionListener
}