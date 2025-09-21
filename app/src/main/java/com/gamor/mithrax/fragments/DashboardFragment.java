package com.gamor.mithrax.fragments; // Or a UI package like com.gamor.mithrax.ui.tts

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment; // Import Fragment
import androidx.media3.common.util.UnstableApi;

import com.gamor.mithrax.R; // Assuming your R file is here

import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
// If you use ViewBinding, import it here:
// import com.gamor.mithrax.databinding.FragmentTtsSttProcessorBinding;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Objects;

public class DashboardFragment extends Fragment implements RecognitionListener {

    // For ViewBinding (recommended)
    // private FragmentTtsSttProcessorBinding binding;

    // For traditional findViewById
    private TextView statusText;
    private Button startButton;

    private TextToSpeech tts;
    private SpeechService speechService;
    private Model model;
    private boolean isListening = false;

    // It's good practice to use a static newInstance method for Fragment creation

    public DashboardFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        // If using ViewBinding:
        // binding = FragmentTtsSttProcessorBinding.inflate(inflater, container, false);
        // View view = binding.getRoot();

        // If not using ViewBinding:
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false); // Use the same layout

        // Initialize views here after inflating the layout
        statusText = view.findViewById(R.id.statusText1);
        startButton = view.findViewById(R.id.startButton1);

        // --- TTS Initialization ---
        // For TTS, Fragment needs a Context. Use requireContext() or getActivity().
        // requireContext() is generally safer as it throws an exception if the fragment is not attached.
        tts = new TextToSpeech(requireContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                if (statusText != null) statusText.setText("TTS Ready");
            } else {
                if (statusText != null) statusText.setText("TTS Initialization Failed");
            }
        });

        // --- Load Vosk model ---
        loadModel(); // This will run in a background thread

        // --- Button Click Listener ---
        startButton.setOnClickListener(v -> {
            if (isListening) {
                stopSTT();
            } else {
                startSTT();
            }
        });
        updateButtonText(); // Initial button text

        return view; // Return the inflated view
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Any logic that needs to happen after the view is created but before it's displayed
        // (e.g., setting up RecyclerView adapters, observing LiveData)
    }


    @SuppressLint("SetTextI18n")
    private void updateButtonText() {
        if (startButton == null) return; // Guard against null if view is not ready
        if (isListening) {
            startButton.setText("Stop Listening");
        } else {
            startButton.setText("Start Listening");
        }
    }

    @SuppressLint("SetTextI18n")
    private void loadModel() {
        new Thread(() -> {
            try {
                // For getFilesDir() and getAssets(), a Fragment needs a Context.
                Context context = getContext(); // Can be null if fragment is not attached yet
                if (context == null) {
                    Log.e("TtsSttProcessorFragment", "Context is null in loadModel, cannot load model.");
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (statusText != null) statusText.setText("Error: Context not available for model load");
                        });
                    }
                    return;
                }

                File modelDir = new File(context.getFilesDir(), "model");
                if (!modelDir.exists()) {
                    copyAssets("model", modelDir, context); // Pass context to copyAssets
                }

                model = new Model(modelDir.getAbsolutePath());

                // To update UI from background thread, use runOnUiThread from the Activity
                // or view.post() if view is guaranteed to be available.
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (statusText != null) statusText.setText("Model loaded");
                    });
                }

            } catch (Exception e) {
                Log.e("TtsSttProcessorFragment", "Exception during model loading", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (statusText != null) statusText.setText("Failed to load model: " + e.getMessage());
                    });
                }
            }
        }).start();
    }

    /**
     * Recursively copy asset folder to destination folder
     */
    private void copyAssets(String assetFolder, File destFolder, @NonNull Context context) throws Exception {
        String[] assets = context.getAssets().list(assetFolder);
        if (assets == null) return;

        if (!destFolder.exists() && !destFolder.mkdirs()) {
            throw new IOException("Could not create destination directory: " + destFolder.getAbsolutePath());
        }


        for (String asset : assets) {
            String assetPath = assetFolder + "/" + asset;
            File destFile = new File(destFolder, asset); // Use new File(parent, child)

            String[] subAssets = context.getAssets().list(assetPath);
            if (subAssets != null && subAssets.length > 0) { // Check if it's a directory
                copyAssets(assetPath, destFile, context);
            } else { // It's a file
                try (InputStream in = context.getAssets().open(assetPath);
                     FileOutputStream out = new FileOutputStream(destFile)) {

                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void startSTT() {
        if (model == null) {
            if (statusText != null) statusText.setText("Model not loaded yet. Cannot start STT.");
            Log.e("TtsSttProcessorFragment", "Model is null in startSTT");
            return;
        }
        try {
            Recognizer recognizer = new Recognizer(model, 16000.0f);
            speechService = new SpeechService(recognizer, 16000.0f);
            speechService.startListening(this); // 'this' is the RecognitionListener
            Log.d("STT", "Listening started by Fragment");
            if (statusText != null) statusText.setText("Listening...");
            isListening = true;
            updateButtonText();
        } catch (IOException e) {
            Log.e("TtsSttProcessorFragment", "IOException in startSTT", e);
            if (statusText != null) statusText.setText("Error starting STT: " + e.getMessage());
        }
    }

    @SuppressLint("SetTextI18n")
    @OptIn(markerClass = UnstableApi.class)
    private void stopSTT() {
        if (speechService != null) {
            speechService.stop();
            // speechService.shutdown(); // Consider if shutdown is needed here or in onDestroy
            Log.d("STT", "Listening stopped by Fragment");
        }
        isListening = false;
        updateButtonText();
    }

    // --- RecognitionListener Methods ---
    @Override
    public void onPartialResult(String hypothesis) {
        // Optional: live partial feedback
    }

    @Override
    public void onResult(String hypothesis) {
        Log.d("STT_Fragment", "Hypothesis: " + hypothesis);
        // Ensure UI updates are on the main thread
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                String recognizedText = hypothesis.replaceAll("\\{.*?\\}", "").trim();
                Log.d("STT_Fragment", "Recognized Text: " + recognizedText);

                if (statusText != null) statusText.setText(recognizedText); // Update status text

                String response = processNLP(recognizedText);
                stopSTT(); // Stop listening after getting a result
                speakResponse(response);
            });
        }
    }

    @Override
    public void onFinalResult(String hypothesis) {
        // Usually, onResult is sufficient if you don't need continuous recognition
    }

    @Override
    public void onError(Exception e) {
        Log.e("STT_Fragment_Error", "Error: " + e.getMessage(), e);
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (statusText != null) statusText.setText(String.format("STT Error: %s", e.getMessage()));
            });
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onTimeout() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (statusText != null) statusText.setText("Listening timeout");
                stopSTT(); // Stop listening on timeout
            });
        }
    }

    // --- NLP and Speak Methods (Mostly unchanged, ensure context isn't an issue if they need it) ---
    private String processNLP(String inputText) {
        String normalizedText = inputText.toLowerCase().trim();
        String response = "Sorry, I didn't understand that.";

        if (isMatch(normalizedText, new String[]{"hello", "hi", "hey", "greetings"})) {
            response = "Hi there! How can I help you today?";
        } else if (isMatch(normalizedText, new String[]{"how are you", "how's it going"})) {
            response = "I'm doing well, thank you for asking!";
        } else if (isMatch(normalizedText, new String[]{"good", "great", "fine", "okay"})) {
            if (normalizedText.contains("good morning")) {
                response = "Good morning to you too!";
            } else if (normalizedText.contains("good afternoon")) {
                response = "Good afternoon!";
            } else {
                response = "That's glad to hear!";
            }
        } else if (isMatch(normalizedText, new String[]{"bye", "goodbye", "see ya", "later"})) {
            response = "Goodbye! Have a great day!";
        } else if (normalizedText.contains("what time is it")) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("h:mm a", Locale.getDefault());
            response = "The current time is " + sdf.format(new java.util.Date());
        }

        Log.d("NLP_Fragment", "Input: '" + inputText + "', Normalized: '" + normalizedText + "', Response: '" + response + "'");
        return response;
    }

    private boolean isMatch(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private void speakResponse(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "responseId");
        } else {
            Log.e("TtsSttProcessorFragment", "TTS not initialized when trying to speak.");
        }
    }

    // --- Fragment Lifecycle for Cleanup ---
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Release resources associated with the view
        // If using ViewBinding:
        // binding = null;
        statusText = null;
        startButton = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Release Vosk and TTS resources
        if (speechService != null) {
            speechService.stop();
            speechService.shutdown(); // Ensure full shutdown
            speechService = null;
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        // model = null; // Vosk model might not need explicit release here, but good to nullify
        Log.d("TtsSttProcessorFragment", "Fragment destroyed and resources released.");
    }
}
