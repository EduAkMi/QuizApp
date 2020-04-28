package com.kaltu.quizapp.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.kaltu.quizapp.R;

public class aStartFragment extends Fragment {
    private TextView startFeedbackText;

    private FirebaseAuth firebaseAuth;
    private NavController navController;

    public aStartFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_start, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();
        navController = Navigation.findNavController(view);

        startFeedbackText = view.findViewById(R.id.start_feedback);
        startFeedbackText.setText("Checking User Account...");
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            startFeedbackText.setText("Creating Account");
            firebaseAuth.signInAnonymously().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    startFeedbackText.setText("Account Created...");
                    navController.navigate(R.id.action_startFragment_to_listFragment);
                } else
                    Log.d("DEBUG", "onComplete: " + task.getException().getMessage());
            });
        } else {
            startFeedbackText.setText("Logged In...");
            navController.navigate(R.id.action_startFragment_to_listFragment);
        }
    }
}
