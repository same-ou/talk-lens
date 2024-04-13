package com.example.talklens.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.talklens.item.IntroItem;
import com.example.talklens.R;
import com.example.talklens.adapter.IntroAdapter;
import com.example.talklens.databinding.ActivityIntroBinding;

import java.util.ArrayList;

public class IntroActivity extends AppCompatActivity {

   ActivityIntroBinding binding;
    IntroAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityIntroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupOnboarding();
        setupIndicator();
        setCurrentIndicator(0);
        binding.introViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setCurrentIndicator(position);
            }
        });
        binding.btnOnboarding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(binding.introViewPager.getCurrentItem() + 1 < adapter.getItemCount()){
                    binding.introViewPager.setCurrentItem(binding.introViewPager.getCurrentItem() + 1);
                }else{
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    finish();
                }
            }
        });
    }

    private void setCurrentIndicator(int i) {
        int childCount = binding.indicatorLayout.getChildCount();
        for(int j=0; j<childCount; j++){
            ImageView imageView = (ImageView) binding.indicatorLayout.getChildAt(j);
            if(j==i){
                imageView.setImageResource(R.drawable.onboarding_indicator_active);
            }else{
                imageView.setImageResource(R.drawable.onboarding_indicator_inactive);
            }
        }
        if(i== adapter.getItemCount() - 1){
            binding.btnOnboarding.setText("Start");
        }else{
            binding.btnOnboarding.setText("Next");
        }
    }

    private void setupOnboarding(){
        ArrayList<IntroItem> introItems = new ArrayList<>();
        IntroItem item1 = new IntroItem();
        item1.setTitle("Welcome to TalkLens");
        item1.setDescription("Embark on a Seamless World Cup Journey.\n" +
                "Break Language Barriers and Savor Every Moment.");
        item1.setImage(R.drawable.img_world);

        IntroItem item2 = new IntroItem();
        item2.setTitle("Effortless Translation with Your Voice");
        item2.setDescription("Speak the language of the world and watch it transform.\n" +
                "Harness the power of speech recognition for instant translations on the go.");
        item2.setImage(R.drawable.img_understand);

        IntroItem item3 = new IntroItem();
        item3.setTitle("See the World through TalkLens");
        item3.setDescription("Make TalkLens your eyes, translating what you see in an instant.");
        item3.setImage(R.drawable.img_learning);

        introItems.add(item1);
        introItems.add(item2);
        introItems.add(item3);

        adapter = new IntroAdapter(introItems);
        binding.introViewPager.setAdapter(adapter);
    }
    private void setupIndicator(){
        ImageView[] indicators = new ImageView[adapter.getItemCount()];
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(8,0,8,0);
        for(int i=0; i<indicators.length; i++){
            indicators[i] = new ImageView(getApplicationContext());
            indicators[i].setImageResource(R.drawable.onboarding_indicator_inactive);
            indicators[i].setLayoutParams(layoutParams);
            binding.indicatorLayout.addView(indicators[i]);
        }
    }
}