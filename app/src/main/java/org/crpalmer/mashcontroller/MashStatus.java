package org.crpalmer.mashcontroller;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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

import java.io.File;
import java.io.FileNotFoundException;

public class MashStatus extends AppCompatActivity implements BrewBossStateChangeListener {
    static final int CONNECTION_STATE_CHANGED_MSG = 1;
    static final int HEATER_CHANGED_MSG = 2;
    static final int PUMP_CHANGED_MSG = 3;
    static final int TEMPERATURE_CHANGED_MSG = 4;
    static final int TARGET_TEMPERATURE_CHANGED_MSG = 5;

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
            setAutomaticMode(id == R.id.automaticMode);
        }
    };

    private void setAutomaticMode(boolean automatic) {
        brewBoss.setAutomaticMode(automatic);
        heaterPower.setEnabled(!automatic);
        targetTemperature.setEnabled(automatic);
    }

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

        App.requestPermissions(this);

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

        try {
            brewBoss.loadBrewXml(new File("/sdcard/Download/a-beer-only-chris-and-carrie-could-love.xml"));
        } catch (XmlException | FileNotFoundException e) {
            App.toastException(e);
        }
    }

    private void hideSoftKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onConnectionStateChanged(boolean connected) {
        handler.sendMessage(handler.obtainMessage(CONNECTION_STATE_CHANGED_MSG));
    }

    @Override
    public void onHeaterChanged(boolean on, int power) {
        handler.sendMessage(handler.obtainMessage(HEATER_CHANGED_MSG));
    }

    @Override
    public void onPumpChanged(boolean on) {
        handler.sendMessage(handler.obtainMessage(PUMP_CHANGED_MSG));
    }

    @Override
    public void onTemperatureChanged(double temperature) {
        handler.sendMessage(handler.obtainMessage(TEMPERATURE_CHANGED_MSG));
    }

    @Override
    public void onTargetTemperatureChanged(double targetTemperature) {
        handler.sendMessage(handler.obtainMessage(TARGET_TEMPERATURE_CHANGED_MSG));
    }


    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CONNECTION_STATE_CHANGED_MSG:
                    boolean isConnected = brewBoss.isConnected();
                    connectionStatus.setTextColor(isConnected ? Color.GREEN : Color.RED);
                    connectionStatus.setText(getString(isConnected ? R.string.connection_status_connected : R.string.connection_status_disconnected));
                    break;
                case HEATER_CHANGED_MSG:
                    heaterButton.setVisualState(brewBoss.isHeaterOn());
                    heaterPower.resetValue();
                    break;
                case PUMP_CHANGED_MSG:
                    pumpButton.setVisualState(brewBoss.isPumpOn());
                    break;
                case TEMPERATURE_CHANGED_MSG:
                    actualTemperature.setText(formatTemperature(brewBoss.getTemperature()));
                    break;
                case TARGET_TEMPERATURE_CHANGED_MSG:
                    targetTemperature.resetValue();
                    break;
            }
        }
    };

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
                        App.toastException(e);
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
                App.toastException(e);
            }
        }

        public abstract void changeState(boolean newState) throws BrewBossConnectionException;
    }
}