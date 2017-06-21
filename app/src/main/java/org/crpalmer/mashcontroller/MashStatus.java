package org.crpalmer.mashcontroller;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MashStatus extends AppCompatActivity implements BrewBossStateChangeListener {

    private final BrewBoss brewBoss = new BrewBoss();
    private View top;
    private TextView connectionStatus;
    private BrewButton pumpButton;
    private BrewButton heaterButton;
    private EditText actualTemperature;
    private DecimalInput targetTemperature;
    private DecimalInput heaterPower;
    private OnCheckedChangeListener brewModeListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup radioGroup, int id) {
            boolean automatic = id == R.id.automaticMode;
            brewBoss.setAutomaticMode(automatic);
            heaterPower.setEnabled(!automatic);
            targetTemperature.setEnabled(automatic);
        }
    };

    private static String formatTemperature(double temperature) {
        return formatTemperature(temperature, true);
    }

    private static String formatTemperature(double temperature, boolean pretty) {
        String result = String.format("%.1f", temperature);
        if (pretty && result.endsWith(".0")) {
            return result.substring(0, result.length() - 2);
        } else {
            return result;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mash_status);

        top = findViewById(R.id.mashStatusTop);
        top.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean focussed) {
                if (focussed) {
                    hideSoftKeyboard(top);
                }
            }
        });

        connectionStatus = (TextView) findViewById(R.id.connectionStatus);
        actualTemperature = (EditText) findViewById(R.id.actualTemperature);

        targetTemperature = new DecimalInput(R.id.targetTemperature, R.id.targetTempOkay, R.id.targetTempCancel) {
            @Override
            public void onValueChanged(double value) {
                brewBoss.setTargetTemperature(value);
            }

            @Override
            public String getCurrentValue() {
                return formatTemperature(brewBoss.getTargetTemperature());
            }
        };

        heaterPower = new DecimalInput(R.id.heaterPower, R.id.heaterPowerOkay, R.id.heaterPowerCancel) {
            @Override
            public void onValueChanged(double value) throws BrewBossConnectionException {
                brewBoss.setHeaterPower((int) Math.round(value));
            }

            @Override
            public String getCurrentValue() {
                return formatTemperature(brewBoss.getHeaterPower());
            }
        };

        pumpButton = new BrewButton(R.id.pumpOnButton) {
            @Override
            public void changeState(boolean newState) throws BrewBossConnectionException {
                brewBoss.setPumpOn(newState);
            }
        };

        heaterButton = new BrewButton(R.id.heaterOnButton) {
            @Override
            public void changeState(boolean newState) throws BrewBossConnectionException {
                brewBoss.setHeaterOn(newState);
            }
        };

        // Do brew mode last because it changes other views
        RadioGroup brewMode = (RadioGroup) findViewById(R.id.brewMode);
        brewMode.setOnCheckedChangeListener(brewModeListener);
        brewMode.check(brewBoss.isAutomaticMode() ? R.id.automaticMode : R.id.manualMode);

        brewBoss.addStateChangeListener(this);
    }

    private void hideSoftKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onConnectionStateChanged(boolean connected) {
        connectionStatus.setTextColor(connected ? Color.GREEN : Color.RED);
        connectionStatus.setText(getString(connected ? R.string.connection_status_connected : R.string.connection_status_disconnected));
    }

    @Override
    public void onHeaterChanged(boolean on, int power) {
        heaterButton.setVisualState(on);
        heaterPower.resetValue();
    }

    @Override
    public void onPumpChanged(boolean on) {
        pumpButton.setVisualState(on);
    }

    @Override
    public void onTemperatureChanged(double temperature) {
        actualTemperature.setText(formatTemperature(temperature));
    }

    private abstract class DecimalInput {
        private EditText editText;
        private ImageButton okayButton;
        private ImageButton cancelButton;

        public DecimalInput(int editTextId, int okayButtonId, int cancelButtonId) {
            editText = (EditText) findViewById(editTextId);
            okayButton = (ImageButton) findViewById(okayButtonId);
            cancelButton = (ImageButton) findViewById(cancelButtonId);

            okayButton.setVisibility(View.INVISIBLE);
            cancelButton.setVisibility(View.INVISIBLE);

            editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean focussed) {
                    if (!focussed) {
                        resetValue();
                    }
                    okayButton.setVisibility(focussed ? View.VISIBLE : View.INVISIBLE);
                    cancelButton.setVisibility(focussed ? View.VISIBLE : View.INVISIBLE);
                }
            });
            editText.setText(getCurrentValue());

            okayButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        onValueChanged(Double.valueOf(editText.getText().toString()));
                    } catch (IllegalArgumentException | BrewBossConnectionException e) {
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

        private void resetValue() {
            editText.setText(getCurrentValue());
        }

        private void setEnabled(boolean enabled) {
            editText.setFocusable(enabled);
            editText.setFocusableInTouchMode(enabled);
            editText.setBackgroundColor(enabled ? Color.TRANSPARENT : Color.LTGRAY);
        }

        public abstract void onValueChanged(double value) throws BrewBossConnectionException;

        public abstract String getCurrentValue();
    }

    private abstract class BrewButton implements View.OnClickListener {
        private Button button;
        private boolean currentState;

        BrewButton(int buttonId) {
            button = (Button) findViewById(buttonId);
            button.setOnClickListener(this);
        }

        public void setVisualState(boolean enabled) {
            button.setBackgroundColor(enabled ? Color.GREEN : Color.YELLOW);
            button.setText(getString(enabled ? R.string.status_on : R.string.status_off));
            currentState = enabled;
        }

        @Override
        public void onClick(View view) {
            try {
                changeState(!currentState);
            } catch (BrewBossConnectionException e) {
                Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        }

        public abstract void changeState(boolean newState) throws BrewBossConnectionException;
    }
}