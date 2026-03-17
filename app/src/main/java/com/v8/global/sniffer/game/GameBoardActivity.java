package com.v8.global.sniffer.game;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import com.v8.global.sniffer.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameBoardActivity extends Activity {

    private GridView gridView;
    private CardAdapter adapter;
    private List<CardModel> cards;
    private TextView tvTimer, tvMoves, tvPairs;
    private int moves = 0;
    private int pairsFound = 0;
    private int firstPosition = -1;
    private int secondPosition = -1;
    private boolean isBusy = false;
    private CountDownTimer timer;
    private int timeLeft = 60;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_board);

        gridView = findViewById(R.id.grid_view);
        tvTimer = findViewById(R.id.tv_timer);
        tvMoves = findViewById(R.id.tv_moves);
        tvPairs = findViewById(R.id.tv_pairs);

        createCards();
        adapter = new CardAdapter(this, cards);
        gridView.setAdapter(adapter);
        
        gridView.setOnItemClickListener((parent, view, position, id) -> {
            if (!isBusy && !cards.get(position).isMatched() && position != firstPosition) {
                selectCard(position);
            }
        });

        startTimer();
        gridView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
    }

    private void createCards() {
        cards = new ArrayList<>();
        String[] emojis = {"🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼"};
        
        for (String emoji : emojis) {
            cards.add(new CardModel(emoji, false, false));
            cards.add(new CardModel(emoji, false, false));
        }
        Collections.shuffle(cards);
    }

    private void selectCard(int position) {
        moves++;
        tvMoves.setText("الحركات: " + moves);

        CardModel card = cards.get(position);
        card.setFlipped(true);
        adapter.notifyDataSetChanged();

        if (firstPosition == -1) {
            firstPosition = position;
        } else {
            secondPosition = position;
            isBusy = true;
            checkMatch();
        }
    }

    private void checkMatch() {
        CardModel card1 = cards.get(firstPosition);
        CardModel card2 = cards.get(secondPosition);

        if (card1.getImage().equals(card2.getImage())) {
            card1.setMatched(true);
            card2.setMatched(true);
            pairsFound++;
            tvPairs.setText("الأزواج: " + pairsFound + "/8");

            firstPosition = -1;
            secondPosition = -1;
            isBusy = false;

            if (pairsFound == 8) {
                gameWon();
            }
        } else {
            new Handler().postDelayed(() -> {
                card1.setFlipped(false);
                card2.setFlipped(false);
                adapter.notifyDataSetChanged();
                firstPosition = -1;
                secondPosition = -1;
                isBusy = false;
            }, 1000);
        }
    }

    private void startTimer() {
        timer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeft = (int) (millisUntilFinished / 1000);
                tvTimer.setText("الوقت: " + timeLeft + "s");
            }

            @Override
            public void onFinish() {
                gameLost("انتهى الوقت!");
            }
        }.start();
    }

    private void gameWon() {
        timer.cancel();
        
        SharedPreferences prefs = getSharedPreferences("game_prefs", MODE_PRIVATE);
        int highScore = prefs.getInt("high_score", 0);
        if (moves < highScore || highScore == 0) {
            prefs.edit().putInt("high_score", moves).apply();
            Toast.makeText(this, "🎉 رقم قياسي جديد!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "🎉 فزت!", Toast.LENGTH_LONG).show();
        }
        new Handler().postDelayed(() -> finish(), 3000);
    }

    private void gameLost(String reason) {
        Toast.makeText(this, "😢 " + reason, Toast.LENGTH_LONG).show();
        new Handler().postDelayed(() -> finish(), 2000);
    }

    @Override
    protected void onDestroy() {
        if (timer != null) timer.cancel();
        super.onDestroy();
    }
  }
