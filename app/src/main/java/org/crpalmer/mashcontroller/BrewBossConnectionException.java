package org.crpalmer.mashcontroller;

import android.util.Log;

/**
 * Created by crpalmer on 6/17/17.
 */

public class BrewBossConnectionException extends Exception {
    BrewBossConnectionException(String tag, String message, Exception cause) {
        super(message, cause);
        Log.e(tag, message + ": " + cause.getLocalizedMessage());
    }
}
