package com.skdev.guardianai.dashboard;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.skdev.guardianai.R;
import com.skdev.guardianai.data.SafetyModelEngine;

import java.util.Locale;

/**
 * Interactive Explainable AI Activity for simulating the mathematical regression formula.
 */
public class ExplainableAiActivity extends AppCompatActivity {

    private TextView tvScoreLarge;
    private TextView tvRiskBadge;
    private TextView tvRecommendation;
    private TextView tvAttrBase, tvAttrLighting, tvAttrPolice, tvAttrCrime;
    private TextView tvSliderLightLbl, tvSliderPoliceLbl, tvSliderCrimeLbl;
    private SeekBar sbLighting, sbPolice, sbCrime;

    private double currentLighting = 8.5;
    private double currentPoliceDist = 1.5;
    private int currentCrimeCount = 15;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.skdev.guardianai.utils.LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explainable_ai);

        initViews();
        recalculatePrediction();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back_xai);
        btnBack.setOnClickListener(v -> finish());

        tvScoreLarge = findViewById(R.id.tv_xai_score_large);
        tvRiskBadge = findViewById(R.id.tv_xai_risk_badge);
        tvRecommendation = findViewById(R.id.tv_xai_recommendation);

        tvAttrBase = findViewById(R.id.tv_attr_base);
        tvAttrLighting = findViewById(R.id.tv_attr_lighting);
        tvAttrPolice = findViewById(R.id.tv_attr_police);
        tvAttrCrime = findViewById(R.id.tv_attr_crime);

        tvSliderLightLbl = findViewById(R.id.tv_xai_slider_light_lbl);
        tvSliderPoliceLbl = findViewById(R.id.tv_xai_slider_police_lbl);
        tvSliderCrimeLbl = findViewById(R.id.tv_xai_slider_crime_lbl);

        sbLighting = findViewById(R.id.sb_xai_lighting);
        sbPolice = findViewById(R.id.sb_xai_police);
        sbCrime = findViewById(R.id.sb_xai_crime);

        sbLighting.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentLighting = Math.max(1.0, progress / 10.0);
                tvSliderLightLbl.setText(String.format(Locale.US, "Street Lighting Score: %.1f / 10", currentLighting));
                recalculatePrediction();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        sbPolice.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentPoliceDist = Math.max(0.2, progress / 10.0);
                tvSliderPoliceLbl.setText(String.format(Locale.US, "Police Station Distance: %.1f km", currentPoliceDist));
                recalculatePrediction();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        sbCrime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentCrimeCount = progress;
                tvSliderCrimeLbl.setText(String.format(Locale.US, "Historical Crime Count: %d incidents", currentCrimeCount));
                recalculatePrediction();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void recalculatePrediction() {
        SafetyModelEngine.SafetyPrediction pred = SafetyModelEngine.evaluateSafety(
                currentLighting, currentPoliceDist, currentCrimeCount
        );

        tvScoreLarge.setText(String.format(Locale.US, "%.3f", pred.score));
        tvScoreLarge.setTextColor(Color.parseColor(pred.riskLevel.getColorHex()));
        tvRiskBadge.setText(pred.riskLevel.getLabel().toUpperCase(Locale.ROOT));
        tvRiskBadge.setTextColor(Color.parseColor(pred.riskLevel.getColorHex()));
        tvRecommendation.setText(pred.riskLevel.getRecommendation());

        if (pred.riskLevel == SafetyModelEngine.RiskLevel.LOW) {
            tvRiskBadge.setBackgroundResource(R.drawable.badge_safe);
        } else if (pred.riskLevel == SafetyModelEngine.RiskLevel.MEDIUM) {
            tvRiskBadge.setBackgroundResource(R.drawable.badge_moderate);
        } else if (pred.riskLevel == SafetyModelEngine.RiskLevel.HIGH) {
            tvRiskBadge.setBackgroundResource(R.drawable.badge_high);
        } else {
            tvRiskBadge.setBackgroundResource(R.drawable.badge_critical);
        }

        tvAttrBase.setText(String.format(Locale.US, "• Base Model Intercept: +%.4f (Baseline constant)", SafetyModelEngine.BASE_INTERCEPT));
        tvAttrLighting.setText(String.format(Locale.US, "• Street Lighting (+0.0436 × %.1f): +%.4f (Protective Factor)", currentLighting, pred.lightingContribution));
        tvAttrPolice.setText(String.format(Locale.US, "• Police Distance (-0.0587 × %.1f km): %.4f (Hazard Factor)", currentPoliceDist, pred.policeDistImpact));
        tvAttrCrime.setText(String.format(Locale.US, "• Crime History (-0.0074 × %d): %.4f (Hazard Factor)", currentCrimeCount, pred.crimeCountImpact));
    }
}
