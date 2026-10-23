package com.chargingw;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 48, 32, 24);
        TextView title = new TextView(this);
        title.setText("Charging W\n\nFréquence souhaitée (secondes)");
        title.setTextSize(20);
        layout.addView(title);
        EditText field = new EditText(this);
        field.setInputType(InputType.TYPE_CLASS_NUMBER);
        field.setSingleLine(true);
        field.setText(String.valueOf(getSharedPreferences("prefs", 0).getInt("seconds", 900)));
        layout.addView(field);
        TextView note = new TextView(this);
        note.setText("Mode économie d'énergie : minimum effectif 900 s (15 min). Android peut retarder les mesures. Branchement et débranchement déclenchent une mise à jour.");
        layout.addView(note);
        Button save = new Button(this);
        save.setText("Enregistrer");
        layout.addView(save);
        setContentView(layout);
        save.setOnClickListener(v -> {
            try {
                int seconds = Integer.parseInt(field.getText().toString());
                if (seconds < 1 || seconds > 86400) throw new IllegalArgumentException();
                getSharedPreferences("prefs", 0).edit().putInt("seconds", seconds).apply();
                ChargeService.start(this);
                Toast.makeText(this, "Enregistré (minimum effectif : 900 s)", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                field.setError("Saisir un entier entre 1 et 86400");
            }
        });
        ChargeService.start(this);
    }
}
