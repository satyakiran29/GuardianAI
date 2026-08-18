package com.skdev.guardianai.voice;

import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import com.skdev.guardianai.data.SafetyLocation;
import com.skdev.guardianai.data.SafetyModelEngine;

import java.util.Locale;

/**
 * Text-To-Speech (TTS) Voice Feedback Engine for destination safety assessments and SOS confirmations.
 */
public class VoiceFeedbackManager {

    private static final String TAG = "VoiceFeedbackManager";
    private static VoiceFeedbackManager instance;
    private TextToSpeech tts;
    private boolean isReady = false;
    private final Context context;

    private VoiceFeedbackManager(Context context) {
        this.context = context.getApplicationContext();
        initTts();
    }

    public static synchronized VoiceFeedbackManager getInstance(Context context) {
        if (instance == null) {
            instance = new VoiceFeedbackManager(context);
        }
        return instance;
    }

    private void initTts() {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Language US not supported, falling back to default");
                    tts.setLanguage(Locale.getDefault());
                }
                tts.setPitch(1.05f);
                tts.setSpeechRate(1.0f);
                isReady = true;
                Log.i(TAG, "TTS initialized successfully");
            } else {
                Log.e(TAG, "TTS Initialization failed: " + status);
            }
        });
    }

    public void speak(String text) {
        if (!isReady || tts == null) {
            Log.w(TAG, "TTS not ready yet: " + text);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "guardian_tts_" + System.currentTimeMillis());
        } else {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    /**
     * Announces safety level of chosen destination locality using TTS.
     */
    public void announceDestinationSafety(SafetyLocation location) {
        if (location == null) return;
        SafetyModelEngine.SafetyPrediction pred = location.getPrediction();
        if (pred == null) {
            speak("Destination " + location.getArea() + " selected.");
            return;
        }

        String lang = com.skdev.guardianai.utils.LocaleHelper.getLanguage(context);
        String speech;

        if (com.skdev.guardianai.utils.LocaleHelper.LANG_HINDI.equals(lang)) {
            if (pred.riskLevel == SafetyModelEngine.RiskLevel.LOW) {
                speech = String.format(Locale.US, "गंतव्य %s सुरक्षित है। सुरक्षा स्कोर %d प्रतिशत है। स्ट्रीट लाइटिंग %.1f है। हरा सुरक्षित मार्ग चुना गया है।",
                        location.getArea(), pred.getScorePercentage(), pred.lightingScore);
            } else if (pred.riskLevel == SafetyModelEngine.RiskLevel.MEDIUM) {
                speech = String.format(Locale.US, "गंतव्य %s मध्यम जोखिम वाला क्षेत्र है। सुरक्षा स्कोर %d प्रतिशत है। मुख्य सड़कों का उपयोग करें।",
                        location.getArea(), pred.getScorePercentage());
            } else if (pred.riskLevel == SafetyModelEngine.RiskLevel.HIGH) {
                speech = String.format(Locale.US, "सावधान: गंतव्य %s उच्च जोखिम वाला क्षेत्र है। सुरक्षा स्कोर %d प्रतिशत है। केवल सुरक्षित हरे मार्ग का अनुसरण करें।",
                        location.getArea(), pred.getScorePercentage());
            } else {
                speech = String.format(Locale.US, "खतरे की चेतावनी: गंतव्य %s गंभीर खतरे वाला क्षेत्र है। एसओएस मोड स्टैंडबाय पर है।",
                        location.getArea());
            }
        } else if (com.skdev.guardianai.utils.LocaleHelper.LANG_TELUGU.equals(lang)) {
            if (pred.riskLevel == SafetyModelEngine.RiskLevel.LOW) {
                speech = String.format(Locale.US, "గమ్యస్థానం %s సురక్షితమైనదిగా గుర్తించబడింది. రక్షణ స్కోరు %d శాతం. వీధి దీపాల సూచిక %.1f. ఆకుపచ్చ సురక్షిత మార్గాన్ని అనుసరించండి.",
                        location.getArea(), pred.getScorePercentage(), pred.lightingScore);
            } else if (pred.riskLevel == SafetyModelEngine.RiskLevel.MEDIUM) {
                speech = String.format(Locale.US, "గమ్యస్థానం %s మధ్యస్థ ప్రమాదకర ప్రాంతం. రక్షణ స్కోరు %d శాతం. జాగ్రత్తగా ప్రయాణించండి.",
                        location.getArea(), pred.getScorePercentage());
            } else if (pred.riskLevel == SafetyModelEngine.RiskLevel.HIGH) {
                speech = String.format(Locale.US, "హెచ్చరిక: గమ్యస్థానం %s అధిక ముప్పు ఉన్న ప్రాంతం. రక్షణ స్కోరు %d శాతం మాత్రమే. సురక్షిత ఆకుపచ్చ మార్గాన్ని ఎంచుకోండి.",
                        location.getArea(), pred.getScorePercentage());
            } else {
                speech = String.format(Locale.US, "తీవ్ర ప్రమాద హెచ్చరిక: గమ్యస్థానం %s తీవ్రమైన ముప్పు కలిగిన ప్రాంతం. అత్యవసర SOS అందుబాటులో ఉంది.",
                        location.getArea());
            }
        } else {
            if (pred.riskLevel == SafetyModelEngine.RiskLevel.LOW) {
                speech = String.format(Locale.US,
                        "Destination %s evaluated as Safe. Safety score is %d percent. Street lighting index is %.1f. Police station is %.1f kilometers away. Green safe route calculated.",
                        location.getArea(), pred.getScorePercentage(), pred.lightingScore, pred.policeDistanceKm);
            } else if (pred.riskLevel == SafetyModelEngine.RiskLevel.MEDIUM) {
                speech = String.format(Locale.US,
                        "Destination %s evaluated as Moderate Risk. Safety score is %d percent. Lighting index is %.1f. Please prefer well-lit main avenues.",
                        location.getArea(), pred.getScorePercentage(), pred.lightingScore);
            } else if (pred.riskLevel == SafetyModelEngine.RiskLevel.HIGH) {
                speech = String.format(Locale.US,
                        "Caution: Destination %s evaluated as High Risk. Safety score is %d percent. Poor street lighting and higher incident frequency detected. Follow the safest green route.",
                        location.getArea(), pred.getScorePercentage());
            } else {
                speech = String.format(Locale.US,
                        "Danger Warning: Destination %s is classified as Critical Hazard. Safety score is only %d percent. Emergency SOS mode is on standby.",
                        location.getArea(), pred.getScorePercentage());
            }
        }

        speak(speech);
    }

    /**
     * Announces SOS activation confirmation in active language.
     */
    public void announceSosTriggered(int contactCount) {
        String lang = com.skdev.guardianai.utils.LocaleHelper.getLanguage(context);
        String msg;
        if (com.skdev.guardianai.utils.LocaleHelper.LANG_HINDI.equals(lang)) {
            msg = "आपातकालीन एसओएस सक्रिय हो गया है! तेज सायरन चालू है और आपका लाइव जीपीएस स्थान आपातकालीन संपर्कों को भेज दिया गया है।";
        } else if (com.skdev.guardianai.utils.LocaleHelper.LANG_TELUGU.equals(lang)) {
            msg = "అత్యవసర SOS ప్రారంభించబడింది! హై-వాల్యూమ్ సైరన్ మోగుతోంది మరియు మీ ప్రత్యక్ష GPS లొకేషన్ కాంటాక్ట్‌లకు పంపబడింది.";
        } else {
            msg = "Emergency SOS activated! High-volume siren enabled. Emergency alert and live GPS location dispatched to "
                    + contactCount + " emergency contacts.";
        }
        speak(msg);
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            isReady = false;
        }
    }
}
