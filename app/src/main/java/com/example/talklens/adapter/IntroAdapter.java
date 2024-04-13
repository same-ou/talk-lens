package com.example.talklens.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.talklens.item.IntroItem;
import com.example.talklens.R;

import java.util.List;

public class IntroAdapter extends RecyclerView.Adapter<IntroAdapter.IntroViewHolder>{
    private List<IntroItem> introItemList;

    public IntroAdapter(List<IntroItem> introItemList) {
        this.introItemList = introItemList;
    }

    @NonNull
    @Override
    public IntroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_onboarding_screen, parent, false);
        return new IntroViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IntroViewHolder holder, int position) {
        holder.setOnboardingData(introItemList.get(position));
    }

    @Override
    public int getItemCount() {
        return introItemList.size();
    }

    public class IntroViewHolder extends RecyclerView.ViewHolder {
        private ImageView introImageView;
        private TextView introTitle;
        private TextView introDescription;

        public IntroViewHolder(@NonNull View itemView) {
            super(itemView);
            introImageView = itemView.findViewById(R.id.pageImage);
            introTitle = itemView.findViewById(R.id.pageTitle);
            introDescription = itemView.findViewById(R.id.pageDescription);
        }
        public void setOnboardingData(IntroItem introItem) {
            introImageView.setImageResource(introItem.getImage());
            introTitle.setText(introItem.getTitle());
            introDescription.setText(introItem.getDescription());
        }
    }
}
