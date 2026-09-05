package com.android.sheguard.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.sheguard.R;
import com.android.sheguard.databinding.ViewOnboardingPageItemBinding;
import com.android.sheguard.ui.entity.OnBoardingPage;

public class OnBoardingPagerAdapter extends RecyclerView.Adapter<OnBoardingPagerAdapter.PagerViewHolder> {

    private final OnBoardingPage[] onBoardingPageList;

    public OnBoardingPagerAdapter() {
        this.onBoardingPageList = OnBoardingPage.values();
    }

    @NonNull
    @Override
    public PagerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ViewOnboardingPageItemBinding binding = ViewOnboardingPageItemBinding.inflate(inflater, parent, false);
        return new PagerViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PagerViewHolder holder, int position) {
        holder.bind(onBoardingPageList[position], position);
    }

    @Override
    public int getItemCount() {
        return onBoardingPageList.length;
    }

    public static class PagerViewHolder extends RecyclerView.ViewHolder {

        private final ViewOnboardingPageItemBinding binding;

        public PagerViewHolder(ViewOnboardingPageItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(OnBoardingPage onBoardingPage, int position) {
            Context context = binding.getRoot().getContext();
            Resources res = context.getResources();

            binding.titleTv.setText(res.getString(onBoardingPage.getTitleResource()));
            binding.descTV.setText(res.getString(onBoardingPage.getDescriptionResource()));
            binding.img.setImageResource(onBoardingPage.getLogoResource());

            if (position == 0) {
                // Slide 1: Protection & Peace of Mind
                binding.subTitleTv.setText(Html.fromHtml("Safety by your side,\n<font color='#FF2E93'>wherever you go</font>", Html.FROM_HTML_MODE_LEGACY));
                binding.layoutSlideFeatures4.setVisibility(View.VISIBLE);
                binding.layoutSlideFeatures3.setVisibility(View.GONE);

            } else if (position == 1) {
                // Slide 2: Built for Real Moments
                binding.subTitleTv.setText(Html.fromHtml("Discreet triggers\n<font color='#FF2E93'>that act in seconds</font>", Html.FROM_HTML_MODE_LEGACY));
                binding.layoutSlideFeatures4.setVisibility(View.GONE);
                binding.layoutSlideFeatures3.setVisibility(View.VISIBLE);

                binding.ivFeature31.setImageResource(R.drawable.ic_shield_heart_vector);
                binding.tvFeature31.setText("Power Button\nEmergency SOS");

                binding.ivFeature32.setImageResource(R.drawable.ic_settings_gear_vector);
                binding.tvFeature32.setText("Shake &\nVoice Triggers");

                binding.ivFeature33.setImageResource(R.drawable.ic_people_group_vector);
                binding.tvFeature33.setText("Instant Safe\nCheck-in");

            } else {
                // Slide 3: Never Walk Alone
                binding.subTitleTv.setText(Html.fromHtml("Your trusted circle\n<font color='#FF2E93'>is always with you</font>", Html.FROM_HTML_MODE_LEGACY));
                binding.layoutSlideFeatures4.setVisibility(View.GONE);
                binding.layoutSlideFeatures3.setVisibility(View.VISIBLE);

                binding.ivFeature31.setImageResource(R.drawable.ic_people_group_vector);
                binding.tvFeature31.setText("Trusted\nGuardians");

                binding.ivFeature32.setImageResource(R.drawable.ic_book_open_vector);
                binding.tvFeature32.setText("24/7 Helpline\nDesk");

                binding.ivFeature33.setImageResource(R.drawable.ic_heart_pink_vector);
                binding.tvFeature33.setText("Live Trip\nSharing");
            }
        }
    }
}
