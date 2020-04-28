package com.kaltu.quizapp.fragments;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.kaltu.quizapp.models.QuestionsModel;
import com.kaltu.quizapp.QuizFragmentArgs;
import com.kaltu.quizapp.QuizFragmentDirections;
import com.kaltu.quizapp.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class dQuizFragment extends Fragment implements View.OnClickListener {
    private FirebaseFirestore firebaseFirestore;
    private String quizId, quizName;
    private NavController navController;
    private FirebaseAuth firebaseAuth;
    private String currentUserId;

    private TextView quizTitle;
    private Button optionOneBtn, optionTwoBtn, optionThreeBtn, nextBtn;
    private ImageButton closeBtn;
    private TextView questionFeedback, questionText, questionTime, questionNumber;
    private ProgressBar questionProgress;

    private List<QuestionsModel> allQuestionsList = new ArrayList<>();
    private List<QuestionsModel> questionsToAnswer = new ArrayList<>();
    private long totalQuestionsToAnswer;
    private CountDownTimer countDownTimer;

    private boolean canAnswer = false;
    private int currentQuestion = 0;

    private int correctAnswers = 0, wrongAnswers = 0, notAnswered = 0;

    public dQuizFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quiz, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);
        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() != null) {
            currentUserId = firebaseAuth.getCurrentUser().getUid();
        } else {
            // Go back to Home Page
        }

        firebaseFirestore = FirebaseFirestore.getInstance();

        quizTitle = view.findViewById(R.id.quiz_title);
        optionOneBtn = view.findViewById(R.id.quiz_option_one);
        optionTwoBtn = view.findViewById(R.id.quiz_option_two);
        optionThreeBtn = view.findViewById(R.id.quiz_option_three);
        nextBtn = view.findViewById(R.id.quiz_next_btn);
        questionFeedback = view.findViewById(R.id.quiz_question_feedback);
        questionText = view.findViewById(R.id.quiz_question);
        questionTime = view.findViewById(R.id.quiz_question_time);
        questionProgress = view.findViewById(R.id.quiz_question_progress);
        questionNumber = view.findViewById(R.id.quiz_question_number);

        quizId = QuizFragmentArgs.fromBundle(getArguments()).getQuizId();
        quizName = QuizFragmentArgs.fromBundle(getArguments()).getQuizName();
        totalQuestionsToAnswer = QuizFragmentArgs.fromBundle(getArguments()).getTotalQuestions();

        firebaseFirestore.collection("QuizList").document(quizId)
                .collection("Questions").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                allQuestionsList = task.getResult().toObjects(QuestionsModel.class);
                pickQuestions();
                loadUI();
            } else {
                quizTitle.setText("Error Loading Data");
            }
        });

        optionOneBtn.setOnClickListener(this);
        optionTwoBtn.setOnClickListener(this);
        optionThreeBtn.setOnClickListener(this);
        nextBtn.setOnClickListener(this);
    }

    private void loadUI() {
        quizTitle.setText(quizName);
        questionText.setText("Load First Question");

        enableOptions();

        loadQuestion(1);
    }

    private void loadQuestion(int questionNum) {
        questionNumber.setText(String.valueOf(questionNum));
        questionText.setText(questionsToAnswer.get(questionNum - 1).getQuestion());

        optionOneBtn.setText(questionsToAnswer.get(questionNum - 1).getOption_a());
        optionTwoBtn.setText(questionsToAnswer.get(questionNum - 1).getOption_b());
        optionThreeBtn.setText(questionsToAnswer.get(questionNum - 1).getOption_c());

        canAnswer = true;
        currentQuestion = questionNum;

        startTimer(questionNum);
    }

    private void startTimer(int questionNumber) {
        Long timeToAnswer = questionsToAnswer.get(questionNumber - 1).getTimer();
        questionTime.setText(timeToAnswer.toString());

        questionProgress.setVisibility(View.VISIBLE);

        countDownTimer = new CountDownTimer(timeToAnswer * 1000, 10) {
            @Override
            public void onTick(long millisUntilFinished) {
                questionTime.setText(String.valueOf(millisUntilFinished / 1000));

                Long percent = millisUntilFinished / (timeToAnswer * 10);
                questionProgress.setProgress(percent.intValue());
            }

            @Override
            public void onFinish() {
                canAnswer = false;

                questionFeedback.setText("Time Up! No answer was submitted.");
                questionFeedback.setTextColor(getResources().getColor(R.color.colorPrimary));
                notAnswered++;
                showNextBtn();
            }
        };

        countDownTimer.start();
    }

    private void enableOptions() {
        optionOneBtn.setVisibility(View.VISIBLE);
        optionTwoBtn.setVisibility(View.VISIBLE);
        optionThreeBtn.setVisibility(View.VISIBLE);

        optionOneBtn.setEnabled(true);
        optionTwoBtn.setEnabled(true);
        optionThreeBtn.setEnabled(true);

        questionFeedback.setVisibility(View.INVISIBLE);
        nextBtn.setVisibility(View.INVISIBLE);
        nextBtn.setEnabled(false);
    }

    private void pickQuestions() {
        for (int i = 0; i < totalQuestionsToAnswer; i++) {
            int randomNumber = getRandomInteger(allQuestionsList.size(), 0);
            questionsToAnswer.add(allQuestionsList.get(randomNumber));
            allQuestionsList.remove(randomNumber);
        }
    }

    public static int getRandomInteger(int maximum, int minimum) {
        return ((int) (Math.random() * (maximum - minimum))) + minimum;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.quiz_option_one:
                verifyAnswer(optionOneBtn);
                break;
            case R.id.quiz_option_two:
                verifyAnswer(optionTwoBtn);
                break;
            case R.id.quiz_option_three:
                verifyAnswer(optionThreeBtn);
                break;
            case R.id.quiz_next_btn:
                if (currentQuestion == totalQuestionsToAnswer) {
                    submitResults();
                } else {
                    currentQuestion++;
                    loadQuestion(currentQuestion);
                    resetOptions();
                }
                break;
        }
    }

    private void submitResults() {
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("correct", correctAnswers);
        resultMap.put("wrong", wrongAnswers);
        resultMap.put("unanswered", notAnswered);
        firebaseFirestore.collection("QuizList").document(quizId).collection("Results")
                .document(currentUserId).set(resultMap).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuizFragmentDirections.ActionQuizFragmentToResultFragment action = QuizFragmentDirections.actionQuizFragmentToResultFragment();
                        action.setQuizId(quizId);
                        navController.navigate(action);
                    } else {
                        quizTitle.setText(task.getException().getMessage());
                    }
                });
    }

    private void resetOptions() {
        optionOneBtn.setBackground(getResources().getDrawable(R.drawable.outline_light_btn_bg, null));
        optionTwoBtn.setBackground(getResources().getDrawable(R.drawable.outline_light_btn_bg, null));
        optionThreeBtn.setBackground(getResources().getDrawable(R.drawable.outline_light_btn_bg, null));

        optionOneBtn.setTextColor(getResources().getColor(R.color.colorLightText));
        optionTwoBtn.setTextColor(getResources().getColor(R.color.colorLightText));
        optionThreeBtn.setTextColor(getResources().getColor(R.color.colorLightText));

        questionFeedback.setVisibility(View.INVISIBLE);
        nextBtn.setVisibility(View.INVISIBLE);
        nextBtn.setEnabled(false);
    }

    private void verifyAnswer(Button selectedAnswerBtn) {
        if (canAnswer) {
            selectedAnswerBtn.setTextColor(getResources().getColor(R.color.colorDark));

            if (questionsToAnswer.get(currentQuestion - 1).getAnswer().equals(selectedAnswerBtn.getText())) {
                correctAnswers++;
                selectedAnswerBtn.setBackground(getResources().getDrawable(R.drawable.correct_answer_btn_bg));

                questionFeedback.setText("Correct Answer");
                questionFeedback.setTextColor(getResources().getColor(R.color.colorPrimary));
            } else {
                wrongAnswers++;
                selectedAnswerBtn.setBackground(getResources().getDrawable(R.drawable.wrong_answer_btn_bg));

                questionFeedback.setText("Wrong Answer \n Correct Answer: " + questionsToAnswer.get(currentQuestion - 1).getAnswer());
                questionFeedback.setTextColor(getResources().getColor(R.color.colorPrimary));
            }
            canAnswer = false;

            countDownTimer.cancel();

            showNextBtn();
        }
    }

    private void showNextBtn() {
        if (currentQuestion == totalQuestionsToAnswer) {
            nextBtn.setText("Submit Results");
        }
        questionFeedback.setVisibility(View.VISIBLE);
        nextBtn.setVisibility(View.VISIBLE);
        nextBtn.setEnabled(true);
    }
}
