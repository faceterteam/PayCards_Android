package cards.pay.sample.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class IntroActivity extends AppCompatActivity {

    private Toolbar mToolbar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        mToolbar = findViewById(R.id.toolbar);
        setupToolbar();

        findViewById(R.id.button_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToCardDetails();
            }
        });

    }

    private void setupToolbar() {
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToCardDetails();
            }
        });
    }

    private void goToCardDetails() {
        Intent intent = new Intent(this, CardDetailsActivity.class);
        startActivity(intent);
        finish();
    }

}
