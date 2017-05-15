package com.makadown.currencyapp.helpers;

import com.makadown.currencyapp.Constants;
import com.makadown.currencyapp.value_objects.Currency;

import org.json.JSONObject;

/**
 * Created by usuario on 13/12/2016.
 */

public class CurrencyParserHelper {

    public static Currency parseCurrency(JSONObject obj, String currencyName) {
        Currency currency = new Currency();
        currency.setBase(obj.optString(Constants.BASE));
        currency.setDate(obj.optString(Constants.DATE));
        JSONObject rateObject = obj.optJSONObject(Constants.RATES);
        if(rateObject != null) {
            currency.setRate(rateObject.optDouble(currencyName));
        }
        currency.setName(currencyName);
        return currency;
    }
}
