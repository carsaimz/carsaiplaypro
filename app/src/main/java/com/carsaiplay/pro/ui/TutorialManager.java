package com.carsaiplay.pro.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.carsaiplay.pro.R;

import java.util.Arrays;
import java.util.List;

public class TutorialManager {
    private Activity activity;
    private View tutorialView;
    private int currentStep = 0;
    private SharedPreferences prefs;

    private static final String PREF_TUTORIAL_SHOWN = "tutorial_shown";

    private final List<TutorialStep> tutorialSteps =
            Arrays.asList(
                    new TutorialStep(
                            R.drawable.tutorial_1,
                            "Bem-vindo ao CarsaiPlay!",
                            "Aqui você encontrará diversos sites de streaming organizados por categorias. Clique em 'Próximo' para aprender a usar."),
                    new TutorialStep(
                            R.drawable.tutorial_2,
                            "Escolha uma Categoria",
                            "Use o menu dropdown no topo para ver todas as categorias disponíveis."),
                    new TutorialStep(
                            R.drawable.tutorial_3,
                            "Navegue pelas Categorias",
                            "Toque em qualquer categoria para ver todos os sites disponíveis nela."),
                    new TutorialStep(
                            R.drawable.tutorial_4,
                            "Explore os Sites",
                            "Escolha qualquer site da lista para acessar seus conteúdos."),
                    new TutorialStep(
                            R.drawable.tutorial_5,
                            "Menu Principal",
                            "Toque nos três traços na barra de navegação para acessar o menu com suporte e gerenciamento de conta."),
                    new TutorialStep(
                            R.drawable.tutorial_6,
                            "Opções do Menu",
                            "Aqui você encontra todas as opções para gerenciar sua conta e obter suporte quando precisar."));

    public TutorialManager(Activity activity) {
        this.activity = activity;
        this.prefs = activity.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
    }

    public void showTutorialIfNeeded() {
        if (!prefs.getBoolean(PREF_TUTORIAL_SHOWN, false)) {
            showTutorial();
        }
    }

    public void showTutorial() {
        LayoutInflater inflater = LayoutInflater.from(activity);
        tutorialView = inflater.inflate(R.layout.tutorial_overlay, null);

        Dialog tutorialDialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
        tutorialDialog.setContentView(tutorialView);
        tutorialDialog.setCancelable(false);

        Button btnNext = tutorialView.findViewById(R.id.btnNext);
        Button btnSkip = tutorialView.findViewById(R.id.btnSkip);

        setupDotIndicators();
        updateTutorialContent();

        btnNext.setOnClickListener(
                v -> {
                    if (currentStep < tutorialSteps.size() - 1) {
                        currentStep++;
                        updateTutorialContent();
                    } else {
                        completeTutorial();
                        tutorialDialog.dismiss();
                    }
                });

        btnSkip.setOnClickListener(
                v -> {
                    completeTutorial();
                    tutorialDialog.dismiss();
                });

        tutorialDialog.show();
    }

    private void setupDotIndicators() {
        LinearLayout dotsLayout = tutorialView.findViewById(R.id.dotsIndicator);
        dotsLayout.removeAllViews(); // Limpa indicadores existentes

        for (int i = 0; i < tutorialSteps.size(); i++) {
            ImageView dot = new ImageView(activity);
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(8, 0, 8, 0);
            dot.setLayoutParams(params);
            dot.setImageResource(R.drawable.dot_indicator);
            dotsLayout.addView(dot);
        }
    }

    private void updateTutorialContent() {
        TutorialStep step = tutorialSteps.get(currentStep);

        ImageView tutorialImage = tutorialView.findViewById(R.id.tutorialImage);
        TextView tutorialTitle = tutorialView.findViewById(R.id.tutorialTitle);
        TextView tutorialDescription = tutorialView.findViewById(R.id.tutorialDescription);
        Button btnNext = tutorialView.findViewById(R.id.btnNext);

        tutorialImage.setImageResource(step.imageResId);
        tutorialTitle.setText(step.title);
        tutorialDescription.setText(step.description);

        if (currentStep == tutorialSteps.size() - 1) {
            btnNext.setText("Começar");
        } else {
            btnNext.setText("Próximo");
        }

        updateDotIndicators();
    }

    private void updateDotIndicators() {
        LinearLayout dotsLayout = tutorialView.findViewById(R.id.dotsIndicator);
        for (int i = 0; i < dotsLayout.getChildCount(); i++) {
            ImageView dot = (ImageView) dotsLayout.getChildAt(i);
            dot.setSelected(i == currentStep);
        }
    }

    private void completeTutorial() {
        prefs.edit().putBoolean(PREF_TUTORIAL_SHOWN, true).apply();
    }

    private static class TutorialStep {
        int imageResId;
        String title;
        String description;

        TutorialStep(int imageResId, String title, String description) {
            this.imageResId = imageResId;
            this.title = title;
            this.description = description;
        }
    }
}
