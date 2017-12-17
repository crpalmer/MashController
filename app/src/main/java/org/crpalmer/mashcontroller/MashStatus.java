package org.crpalmer.mashcontroller;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;

public class MashStatus extends AppCompatActivity implements BrewStateChangeListener {
    static final int CONNECTION_STATE_CHANGED_MSG = 1;
    static final int HEATER_CHANGED_MSG = 2;
    static final int PUMP_CHANGED_MSG = 3;
    static final int TEMPERATURE_CHANGED_MSG = 4;
    static final int TARGET_TEMPERATURE_CHANGED_MSG = 5;
    static final int STEP_START_MSG = 6;
    static final int STEP_TICK_MSG = 7;

    private final BrewController brewController = new BrewController();
    private View top;
    private TextView connectionStatus;
    private BrewButton pumpButton;
    private BrewButton heaterButton;
    private TextView actualTemperature;
    private DecimalInput targetTemperature;
    private DecimalInput heaterPower;
    private TextView stepTimer;
    private TextView stepDescription;

    private OnCheckedChangeListener brewModeListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup radioGroup, int id) {
            setAutomaticMode(id == R.id.automaticMode);
        }
    };

    private void setAutomaticMode(boolean automatic) {
        brewController.setAutomaticMode(automatic);
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
        actualTemperature = (TextView) findViewById(R.id.actualTemperature);

        targetTemperature = new DecimalInput(R.id.targetTemperature, R.string.target_temperature) {
            @Override
            public void onValueChanged(double value) {
                brewController.setTargetTemperature(value);
            }

            @Override
            public String getCurrentValue() {
                return formatTemperature(brewController.getTargetTemperature());
            }
        };

        heaterPower = new DecimalInput(R.id.heaterPower, R.string.heater) {
            @Override
            public void onValueChanged(double value) throws BrewBossConnectionException {
                brewController.setHeaterPower((int) Math.round(value));
            }

            @Override
            public String getCurrentValue() {
                return formatTemperature(brewController.getHeaterPower());
            }
        };

        pumpButton = new BrewButton(R.id.pumpOnButton) {
            @Override
            public void changeState(boolean newState) throws BrewBossConnectionException {
                brewController.setPumpOn(newState);
            }
        };

        heaterButton = new BrewButton(R.id.heaterOnButton) {
            @Override
            public void changeState(boolean newState) throws BrewBossConnectionException {
                brewController.setHeaterOn(newState);
            }
        };

        stepTimer = (TextView) findViewById(R.id.stepTimer);
        stepDescription = (TextView) findViewById(R.id.stepDescription);

        // Do brew mode last because it changes other views
        RadioGroup brewMode = (RadioGroup) findViewById(R.id.brewMode);
        brewMode.setOnCheckedChangeListener(brewModeListener);
        brewMode.check(brewController.isAutomaticMode() ? R.id.automaticMode : R.id.manualMode);

        brewController.addStateChangeListener(this);

        try {
            // brewController.loadBrewXml(new File("/sdcard/Download/a-beer-only-chris-and-carrie-could-love.xml"));
            brewController.loadBrewXml(new File("/sdcard/test-brew.xml"));
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
    public void onStepStart(int num, String description) {
        handler.sendMessage(handler.obtainMessage(STEP_START_MSG, String.format("%d: %s", num, description)));
    }

    @Override
    public void onStepTick(int secondsLeft) {
        handler.sendMessage(handler.obtainMessage(STEP_TICK_MSG, String.format("%02d:%02d", secondsLeft / 60, secondsLeft % 60)));
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
                    boolean isConnected = brewController.isConnected();
                    connectionStatus.setTextColor(isConnected ? Color.GREEN : Color.RED);
                    connectionStatus.setText(getString(isConnected ? R.string.connection_status_connected : R.string.connection_status_disconnected));
                    break;
                case HEATER_CHANGED_MSG:
                    heaterButton.setVisualState(brewController.isHeaterOn());
                    heaterPower.resetValue();
                    break;
                case PUMP_CHANGED_MSG:
                    pumpButton.setVisualState(brewController.isPumpOn());
                    break;
                case TEMPERATURE_CHANGED_MSG:
                    actualTemperature.setText(formatTemperature(brewController.getTemperature()));
                    break;
                case TARGET_TEMPERATURE_CHANGED_MSG:
                    targetTemperature.resetValue();
                    break;
                case STEP_START_MSG:
                    stepDescription.setText((String) msg.obj);
                    stepTimer.setText("--:--");
                    break;
                case STEP_TICK_MSG:
                    stepTimer.setText((String) msg.obj);
                    break;
            }
        }
    };

    private abstract class DecimalInput {
        private TextView value;
        private String title;

        public DecimalInput(int valueId, int titleStringId) {
            value = (TextView) findViewById(valueId);
            title = getString(titleStringId);

            value.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean b) {
                    if (b) {
                        top.requestFocus();
                    }
                }
            });
            value.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MashStatus.this);
                    builder.setTitle(title);
                    final EditText input = new EditText(MashStatus.this);
                    input.setInputType(InputType.TYPE_CLASS_NUMBER);
                    input.setRawInputType(Configuration.KEYBOARD_12KEY);
                    float pixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics());
                    input.setWidth((int) pixels);
                    input.setText(getCurrentValue());
                    builder.setView(input);
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            try {
                                onValueChanged(Double.valueOf(input.getText().toString()));
                            } catch (IllegalArgumentException | BrewBossConnectionException e) {
                                App.toastException(e);
                            }
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            resetValue();
                        }
                    });
                    final AlertDialog dialog = builder.create();
                    dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                        }
                    });
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                    dialog.show();
                    input.requestFocus();
                }
            });
            value.setText(getCurrentValue());
        }

        private void resetValue() {
            value.setText(getCurrentValue());
        }

        private void setEnabled(boolean enabled) {
            value.setClickable(enabled);
            value.setBackgroundColor(enabled ? Color.TRANSPARENT : Color.LTGRAY);
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