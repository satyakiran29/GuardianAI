package com.android.sheguard.ui.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.android.sheguard.R;
import com.android.sheguard.util.ThemeUtil;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;

public class AppearanceFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_appearance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireContext().getSharedPreferences(ThemeUtil.PREFS_NAME, Context.MODE_PRIVATE);

        ImageView btnBack = view.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                try {
                    Navigation.findNavController(v).navigateUp();
                } catch (Exception e) {
                    if (getActivity() != null) {
                        getActivity().onBackPressed();
                    }
                }
            });
        }

        MaterialCardView cardDefault = view.findViewById(R.id.card_theme_default);
        MaterialCardView cardBlue = view.findViewById(R.id.card_theme_blue);
        MaterialCardView cardViolet = view.findViewById(R.id.card_theme_violet);
        MaterialCardView cardEmerald = view.findViewById(R.id.card_theme_emerald);

        String currentTheme = prefs.getString(ThemeUtil.KEY_THEME_COLOR, ThemeUtil.COLOR_DEFAULT);
        updateThemeSelection(currentTheme, cardDefault, cardBlue, cardViolet, cardEmerald);

        View.OnClickListener onThemeClick = v -> {
            String newTheme;
            int id = v.getId();
            if (id == R.id.card_theme_blue) {
                newTheme = ThemeUtil.COLOR_BLUE;
            } else if (id == R.id.card_theme_violet) {
                newTheme = ThemeUtil.COLOR_VIOLET;
            } else if (id == R.id.card_theme_emerald) {
                newTheme = ThemeUtil.COLOR_EMERALD;
            } else {
                newTheme = ThemeUtil.COLOR_DEFAULT;
            }

            if (!newTheme.equals(prefs.getString(ThemeUtil.KEY_THEME_COLOR, ThemeUtil.COLOR_DEFAULT))) {
                prefs.edit().putString(ThemeUtil.KEY_THEME_COLOR, newTheme).apply();
                updateThemeSelection(newTheme, cardDefault, cardBlue, cardViolet, cardEmerald);
                if (getActivity() != null) {
                    getActivity().recreate();
                }
            }
        };

        if (cardDefault != null) cardDefault.setOnClickListener(onThemeClick);
        if (cardBlue != null) cardBlue.setOnClickListener(onThemeClick);
        if (cardViolet != null) cardViolet.setOnClickListener(onThemeClick);
        if (cardEmerald != null) cardEmerald.setOnClickListener(onThemeClick);

        RadioGroup rgThemeMode = view.findViewById(R.id.rg_theme_mode);
        RadioButton rbSystem = view.findViewById(R.id.rb_system);
        RadioButton rbLight = view.findViewById(R.id.rb_light);
        RadioButton rbDark = view.findViewById(R.id.rb_dark);

        String currentMode = prefs.getString(ThemeUtil.KEY_THEME_MODE, ThemeUtil.MODE_SYSTEM);
        if (ThemeUtil.MODE_LIGHT.equals(currentMode)) {
            if (rbLight != null) rbLight.setChecked(true);
        } else if (ThemeUtil.MODE_DARK.equals(currentMode) || ThemeUtil.MODE_AMOLED.equals(currentMode)) {
            if (rbDark != null) rbDark.setChecked(true);
        } else {
            if (rbSystem != null) rbSystem.setChecked(true);
        }

        if (rgThemeMode != null) {
            rgThemeMode.setOnCheckedChangeListener((group, checkedId) -> {
                String newMode;
                if (checkedId == R.id.rb_light) {
                    newMode = ThemeUtil.MODE_LIGHT;
                } else if (checkedId == R.id.rb_dark) {
                    newMode = ThemeUtil.MODE_DARK;
                } else {
                    newMode = ThemeUtil.MODE_SYSTEM;
                }

                if (!newMode.equals(prefs.getString(ThemeUtil.KEY_THEME_MODE, ThemeUtil.MODE_SYSTEM))) {
                    prefs.edit().putString(ThemeUtil.KEY_THEME_MODE, newMode).apply();
                    ThemeUtil.applyNightMode(newMode);
                    if (getActivity() != null) {
                        getActivity().recreate();
                    }
                }
            });
        }

        MaterialSwitch switchAppleGlass = view.findViewById(R.id.switch_apple_glass);
        MaterialCardView cardAppleGlass = view.findViewById(R.id.card_apple_glass);
        if (switchAppleGlass != null) {
            switchAppleGlass.setChecked(prefs.getBoolean(ThemeUtil.KEY_APPLE_GLASS, false));
            if (cardAppleGlass != null) {
                cardAppleGlass.setOnClickListener(v -> switchAppleGlass.toggle());
            }
            switchAppleGlass.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked != prefs.getBoolean(ThemeUtil.KEY_APPLE_GLASS, false)) {
                    prefs.edit().putBoolean(ThemeUtil.KEY_APPLE_GLASS, isChecked).apply();
                    if (getActivity() != null) {
                        getActivity().recreate();
                    }
                }
            });
        }

        View layoutPureBlackSection = view.findViewById(R.id.layout_pure_black_section);
        boolean isNight = ThemeUtil.isNightMode(requireContext());
        if (layoutPureBlackSection != null) {
            layoutPureBlackSection.setVisibility(isNight ? View.VISIBLE : View.GONE);
        }

        MaterialSwitch switchPureBlack = view.findViewById(R.id.switch_pure_black);
        MaterialCardView cardPureBlack = view.findViewById(R.id.card_pure_black);
        if (switchPureBlack != null) {
            switchPureBlack.setChecked(prefs.getBoolean(ThemeUtil.KEY_PURE_BLACK, false));
            if (cardPureBlack != null) {
                cardPureBlack.setOnClickListener(v -> switchPureBlack.toggle());
            }
            switchPureBlack.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked != prefs.getBoolean(ThemeUtil.KEY_PURE_BLACK, false)) {
                    prefs.edit().putBoolean(ThemeUtil.KEY_PURE_BLACK, isChecked).apply();
                    if (getActivity() != null) {
                        getActivity().recreate();
                    }
                }
            });
        }
    }

    private void updateThemeSelection(String selectedTheme,
                                      MaterialCardView cardDefault,
                                      MaterialCardView cardBlue,
                                      MaterialCardView cardViolet,
                                      MaterialCardView cardEmerald) {
        if (!isAdded() || getContext() == null) return;
        int transparent = ContextCompat.getColor(requireContext(), android.R.color.transparent);
        if (cardDefault != null) {
            cardDefault.setStrokeColor(ThemeUtil.COLOR_DEFAULT.equals(selectedTheme)
                    ? ContextCompat.getColor(requireContext(), R.color.accent_pink)
                    : transparent);
        }
        if (cardBlue != null) {
            cardBlue.setStrokeColor(ThemeUtil.COLOR_BLUE.equals(selectedTheme)
                    ? ContextCompat.getColor(requireContext(), R.color.accent_blue)
                    : transparent);
        }
        if (cardViolet != null) {
            cardViolet.setStrokeColor(ThemeUtil.COLOR_VIOLET.equals(selectedTheme)
                    ? ContextCompat.getColor(requireContext(), R.color.accent_violet)
                    : transparent);
        }
        if (cardEmerald != null) {
            cardEmerald.setStrokeColor(ThemeUtil.COLOR_EMERALD.equals(selectedTheme)
                    ? ContextCompat.getColor(requireContext(), R.color.accent_emerald)
                    : transparent);
        }
    }
}
