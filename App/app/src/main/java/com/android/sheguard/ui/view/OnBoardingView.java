package com.android.sheguard.ui.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.viewpager2.widget.ViewPager2;

import com.android.sheguard.databinding.ViewOnboardingPageBinding;
import com.android.sheguard.ui.activity.LoginRegisterActivity;
import com.android.sheguard.ui.adapter.OnBoardingPagerAdapter;
import com.android.sheguard.ui.core.Transform;
import com.android.sheguard.ui.entity.OnBoardingPage;

@SuppressWarnings("unused")
public class OnBoardingView extends FrameLayout {

    private static ViewOnboardingPageBinding binding;
    private int numberOfPages;

    public OnBoardingView(Context context) {
        this(context, null);
    }

    public OnBoardingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OnBoardingView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public OnBoardingView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context, attrs, defStyleAttr, defStyleRes);
    }

    public static void navigateToPrevSlide() {
        if (binding == null || binding.slider == null) return;
        int prevSlidePos = binding.slider.getCurrentItem() - 1;
        if (prevSlidePos < 0) {
            throw new IllegalArgumentException("Can't navigate to previous slide, because current slide is first");
        }
        binding.slider.setCurrentItem(prevSlidePos, true);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        LayoutInflater inflater = LayoutInflater.from(context);
        binding = ViewOnboardingPageBinding.inflate(inflater, this, true);

        numberOfPages = OnBoardingPage.values().length;

        setUpSlider(binding);
        addingButtonsClickListeners(binding);
    }

    private void setUpSlider(ViewOnboardingPageBinding binding) {
        binding.slider.setAdapter(new OnBoardingPagerAdapter());
        binding.slider.setPageTransformer(Transform::setParallaxTransformation);

        binding.slider.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) binding.nextBtn.getLayoutParams();
                if (position == numberOfPages - 1) {
                    binding.skipBtn.setVisibility(View.GONE);
                    binding.btnTopSkip.setVisibility(View.GONE);
                    params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    binding.nextBtn.setLayoutParams(params);
                    binding.nextBtn.setText("Get started  →");
                } else {
                    binding.skipBtn.setVisibility(View.VISIBLE);
                    binding.btnTopSkip.setVisibility(View.VISIBLE);
                    params.width = (int) (140 * getResources().getDisplayMetrics().density);
                    binding.nextBtn.setLayoutParams(params);
                    binding.nextBtn.setText("Next  →");
                }
            }
        });

        binding.pageIndicator.attachTo(binding.slider);
    }

    private void addingButtonsClickListeners(ViewOnboardingPageBinding binding) {
        binding.nextBtn.setOnClickListener(view -> {
            int currentItem = binding.slider.getCurrentItem();
            if (currentItem < numberOfPages - 1) {
                binding.slider.setCurrentItem(currentItem + 1, true);
            } else {
                setFirstTimeLaunchToFalse();
            }
        });

        binding.skipBtn.setOnClickListener(view -> setFirstTimeLaunchToFalse());
        binding.btnTopSkip.setOnClickListener(view -> setFirstTimeLaunchToFalse());
        binding.startBtn.setOnClickListener(view -> setFirstTimeLaunchToFalse());
    }

    private void setFirstTimeLaunchToFalse() {
        getContext().startActivity(new Intent(getContext(), LoginRegisterActivity.class));
        if (getContext() instanceof Activity) {
            ((Activity) getContext()).finish();
        }
    }
}