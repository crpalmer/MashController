package org.crpalmer.mashcontroller;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MashStatus extends AppCompatActivity {

    private final BrewBoss brewBoss = new BrewBoss();
    private DecimalInput targetTemperatureInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mash_status);

        targetTemperatureInput = new DecimalInput(R.id.targetTemperature, R.id.targetTempOkay, R.id.targetTempCancel) {
            @Override
            public void onValueChanged(double value) {
                brewBoss.setTargetTemperature(value);
                Log.e("CRP", "temperature set to " + value + " = " + brewBoss.getTargetTemperature());
            }

            @Override
            public String getCurrentValue() {
                return String.format("%.1f", brewBoss.getTargetTemperature());
            }
        };

        ToggleButton pumpOnButton = (ToggleButton) findViewById(R.id.pumpOnButton);
        pumpOnButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                try {
                    brewBoss.setPumpOn(b);
                } catch (BrewBossConnectionException e) {
                    Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private abstract class DecimalInput {
        private EditText editText;
        private ImageButton okayButton;
        private ImageButton cancelButton;
        private View top;

        public DecimalInput(int editTextId, int okayButtonId, int cancelButtonId) {
            editText = (EditText) findViewById(editTextId);
            okayButton = (ImageButton) findViewById(okayButtonId);
            cancelButton = (ImageButton) findViewById(cancelButtonId);
            top = findViewById(R.id.mashStatusTop);

            okayButton.setVisibility(View.INVISIBLE);
            cancelButton.setVisibility(View.INVISIBLE);

            editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean focussed) {
                    if (! focussed) {
                        resetValue();
                    }
                    setEditingMode(focussed);
                }
            });

            okayButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        onValueChanged(Double.valueOf(editText.getText().toString()));
                    } catch (IllegalArgumentException e) {
                        Toast.makeText(getBaseContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                    top.requestFocus();
                }
            });

            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    resetValue();
                    top.requestFocus();
                }
            });
        }

        private void setEditingMode(boolean isEditing) {
            okayButton.setVisibility(isEditing ? View.VISIBLE : View.INVISIBLE);
            cancelButton.setVisibility(isEditing ? View.VISIBLE : View.INVISIBLE);
        }

        private void resetValue() {
             editText.setText(getCurrentValue());
        }

        public abstract void onValueChanged(double value);
        public abstract String getCurrentValue();
    }
}
