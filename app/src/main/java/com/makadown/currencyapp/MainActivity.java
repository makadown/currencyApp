package com.makadown.currencyapp;

import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.makadown.currencyapp.adapters.CurrencyAdapter;
import com.makadown.currencyapp.database.CurrencyDatabaseAdapter;
import com.makadown.currencyapp.database.CurrencyTableHelper;
import com.makadown.currencyapp.receivers.CurrencyReceiver;
import com.makadown.currencyapp.services.CurrencyService;
import com.makadown.currencyapp.utils.AlarmUtils;
import com.makadown.currencyapp.utils.LogUtils;
import com.makadown.currencyapp.utils.NotificationUtils;
import com.makadown.currencyapp.utils.SharedPreferencesUtils;
import com.makadown.currencyapp.value_objects.Currency;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity
        implements CurrencyReceiver.Receiver {

    private String mBaseCurrency = Constants.CURRENCY_CODES[30];
    private String mTargetCurrency = "CAD"; // Constants.CURRENCY_CODES[0];
    private CurrencyTableHelper mCurrencyTableHelper;

    private static final String TAG = MainActivity.class.getName();
    private int mServiceRepetition = AlarmUtils.REPEAT.REPEAT_EVERY_MINUTE.ordinal();

    private CoordinatorLayout mLogLayout;
    private boolean mIsLogVisible = true;

    private ListView mBaseCurrencyList;
    private ListView mTargetCurrencyList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resetDownloads();
        //initCurrencies();
        initDB();
        initToolBar();
        initSpinner();
        initCurrencyList();
        //showLogs();

        mLogLayout = (CoordinatorLayout) findViewById( R.id.log_layout );

    }

    @Override
    protected void onResume() {
        super.onResume();
        mServiceRepetition = SharedPreferencesUtils.getServiceRepetition(this);
        retrieveCurrencyExchangeRate();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtils.setLogListener(null);
    }

    @Override
    public void onReceiveResult(int resultCode, final Bundle resultData) {
        switch(resultCode) {
            case Constants.STATUS_RUNNING:
                LogUtils.log(TAG, "Currency Service Running!");
                break;

            case Constants.STATUS_FINISHED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Currency currencyParcel = resultData.getParcelable(Constants.RESULT);
                        if(currencyParcel != null) {
                            String message = "Currency: " + currencyParcel.getBase() + " - " +
                                    currencyParcel.getName() + ": " + currencyParcel.getRate();
                            LogUtils.log(TAG, message);
                            long id = mCurrencyTableHelper.insertCurrency(currencyParcel);
                            Currency currency = currencyParcel;
                            try {
                                currency = mCurrencyTableHelper.getCurrency(id);
                            } catch(SQLException e) {
                                e.printStackTrace();
                                LogUtils.log(TAG, "Currency retrieval has failed");
                            }
                            if(currency != null) {
                                String dbMessage = "Currency (DB): " + currency.getBase() + " - " +
                                        currency.getName() + ": " + currency.getRate();
                                LogUtils.log(TAG, dbMessage);
                                NotificationUtils.showNotificationMessage(getApplicationContext(),
                                        "Currency Exchange Rate", dbMessage);
                            }

                            if(NotificationUtils.isAppInBackground(MainActivity.this)) {
                                int numDownloads = SharedPreferencesUtils.getNumDownloads(getApplicationContext());
                                SharedPreferencesUtils.updateNumDownloads(getApplicationContext(), ++numDownloads);
                                if(numDownloads == Constants.MAX_DOWNLOADS) {
                                    LogUtils.log(TAG, "Max downloads for the background processing has been reached.");
                                    mServiceRepetition = AlarmUtils.REPEAT.REPEAT_EVERY_DAY.ordinal();
                                    retrieveCurrencyExchangeRate();
                                }
                            }
                        }
                    }
                });
                break;

            case Constants.STATUS_ERROR:
                String error = resultData.getString(Intent.EXTRA_TEXT);
                LogUtils.log(TAG, error);
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        }
    }

    private void initDB() {
        CurrencyDatabaseAdapter currencyDatabaseAdapter = new CurrencyDatabaseAdapter(this);
        mCurrencyTableHelper = new CurrencyTableHelper(currencyDatabaseAdapter);
    }

    private void initToolBar(){
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar );
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
    }

    private void initSpinner()
    {
        final Spinner spinner = (Spinner) findViewById(R.id.time_frequency);
        spinner.setSaveEnabled(true);
        spinner.setSelection(SharedPreferencesUtils.getServiceRepetition(this), false);
        spinner.post(new Runnable() {
            @Override
            public void run() {
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        SharedPreferencesUtils.updateServiceRepetition(MainActivity.this, position);
                        mServiceRepetition = position;
                        if (position>=AlarmUtils.REPEAT.values().length)
                        {
                            AlarmUtils.stopService();
                        }
                        else
                        {
                            retrieveCurrencyExchangeRate();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
            }
        });
    }



    private void initCurrencyList()
    {
        mBaseCurrencyList = (ListView) findViewById(R.id.base_currency_list);
        mTargetCurrencyList = (ListView) findViewById( R.id.target_currency_list );

        CurrencyAdapter baseCurrencyAdapter = new CurrencyAdapter(this);
        CurrencyAdapter targetCurrencyAdapter = new CurrencyAdapter(this);

        int baseCurrencyIndex = retrieveIndexOf(mBaseCurrency);
        int targetCurrencyIndex = retrieveIndexOf(mTargetCurrency);

        mBaseCurrencyList.setItemChecked(baseCurrencyIndex);
        mTargetCurrencyList.setItemChecked(targetCurrencyIndex);

        mBaseCurrencyList.setSelection(baseCurrencyIndex);
        mTargetCurrencyList.setSelection(targetCurrencyIndex);

    }

    private int retrieveIndexOf(String currency)
    {
        return Arrays.asList(Constants.CURRENCY_CODES).indexOf(currency);
    }






























    private void retrieveCurrencyExchangeRate() {
        CurrencyReceiver receiver = new CurrencyReceiver(new Handler());
        receiver.setReceiver(this);
        Intent intent = new Intent(Intent.ACTION_SYNC, null, getApplicationContext(), CurrencyService.class);
        intent.setExtrasClassLoader(CurrencyService.class.getClassLoader());

        Bundle bundle = new Bundle();
        String url = Constants.CURRENCY_URL + mBaseCurrency;
        bundle.putString(Constants.URL, url);
        bundle.putParcelable(Constants.RECEIVER, receiver);
        bundle.putInt(Constants.REQUEST_ID, Constants.REQUEST_ID_NUM);
        bundle.putString(Constants.CURRENCY_NAME, mTargetCurrency);
        bundle.putString(Constants.CURRENCY_BASE, mBaseCurrency);
        intent.putExtra(Constants.BUNDLE, bundle);
//        startService(intent);
        AlarmUtils.startService(this, intent,
                AlarmUtils.REPEAT.values()[mServiceRepetition]);
    }

    private void resetDownloads() {
        SharedPreferencesUtils.updateNumDownloads(this, 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}