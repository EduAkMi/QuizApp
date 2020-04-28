package com.kaltu.quizapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.kaltu.quizapp.DetailsFragmentArgs;
import com.kaltu.quizapp.DetailsFragmentDirections;
import com.kaltu.quizapp.models.QuizListModel;
import com.kaltu.quizapp.viewModels.QuizListViewModel;
import com.kaltu.quizapp.R;

import java.util.List;

public class cDetailsFragment extends Fragment implements View.OnClickListener {
    private NavController navController;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;
    private QuizListViewModel quizListViewModel;
    private int position;
    private String quizId, quizName;
    private int totalQuestions = 0;

    private ImageView detailsImage;
    private TextView detailsTitle, detailsDesc, detailsDiff, detailsQuestions, detailsScore;
    private Button detailsStartBtn;

    public cDetailsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);
        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            // Go back
        }
        firebaseFirestore = FirebaseFirestore.getInstance();

        position = DetailsFragmentArgs.fromBundle(getArguments()).getPosition();

        detailsImage = view.findViewById(R.id.details_image);
        detailsTitle = view.findViewById(R.id.details_title);
        detailsDesc = view.findViewById(R.id.details_desc);
        detailsDiff = view.findViewById(R.id.details_difficulty);
        detailsQuestions = view.findViewById(R.id.details_questions);
        detailsScore = view.findViewById(R.id.details_score_text);

        detailsStartBtn = view.findViewById(R.id.details_start_btn);
        detailsStartBtn.setOnClickListener(this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        quizListViewModel = new ViewModelProvider(getActivity()).get(QuizListViewModel.class);
        quizListViewModel.getQuizListModelData().observe(getViewLifecycleOwner(), new Observer<List<QuizListModel>>() {
            @Override
            public void onChanged(List<QuizListModel> quizListModels) {
                detailsTitle.setText(quizListModels.get(position).getName());
                detailsDesc.setText(quizListModels.get(position).getDesc());
                detailsDiff.setText(quizListModels.get(position).getLevel());
                detailsQuestions.setText(quizListModels.get(position).getQuestions() + "");

                Glide.with(getContext()).load(quizListModels.get(position).getImage())
                        .centerCrop().placeholder(R.drawable.placeholder_image)
                        .into(detailsImage);

                quizId = quizListModels.get(position).getQuiz_id();
                quizName = quizListModels.get(position).getName();
                totalQuestions = quizListModels.get(position).getQuestions();

                loadResultsData();
            }
        });
    }

    private void loadResultsData() {
        firebaseFirestore.collection("QuizList").document(quizId).collection("Results")
                .document(firebaseAuth.getCurrentUser().getUid()).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    Long correct = document.getLong("correct");
                    Long wrong = document.getLong("wrong");
                    Long missed = document.getLong("unanswered");

                    long total = correct + wrong + missed;
                    long percent = (correct * 100) / total;

                    detailsScore.setText(percent + "%");
                } else {
                    // Document doesn't exist and should stay N/A
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.details_start_btn:
                DetailsFragmentDirections.ActionDetailsFragmentToQuizFragment action = DetailsFragmentDirections.actionDetailsFragmentToQuizFragment();
                action.setTotalQuestions(totalQuestions);
                action.setQuizId(quizId);
                action.setQuizName(quizName);
                navController.navigate(action);
                break;
        }
    }
}
