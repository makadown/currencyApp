package com.makadown.currencyapp.receivers;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

/**
 * Created by usuario on 22/02/2017.
 */

public class CurrencyReceiver extends ResultReceiver
{
    private Receiver mReceiver;

    public CurrencyReceiver(Handler handler) {
        super(handler);
    }

    public void setReceiver(Receiver mReceiver) {
        this.mReceiver = mReceiver;
    }

    public interface Receiver {
        void onReceiveResult(int resultCode, Bundle resultData);
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        if(mReceiver != null) {
            mReceiver.onReceiveResult(resultCode, resultData);
        }
    }
}
