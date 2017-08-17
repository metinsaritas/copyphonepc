package com.metinsaritas.copyphone_pc;

import android.telephony.PhoneNumberUtils;

import java.util.regex.Pattern;

import static android.util.Patterns.WEB_URL;

/**
 * Created by User on 16-Aug-17.
 */

public class MyValidator {
    public static int OTHER = 0;
    public static int PHONE = 1;
    public static int URL = 2;

    public static void Validate (Copy copy) {
        String text = copy.copiedText;
        if (isPhoneNumber(text))
            copy.type = PHONE;
        else if (isUrl(text))
            copy.type = URL;
        else
            copy.type = OTHER;
    }

    public static boolean isUrl(String text) {
        return android.util.Patterns.WEB_URL.matcher(text).matches();
    }

    public static boolean isPhoneNumber (String text) {
        return PhoneNumberUtils.isGlobalPhoneNumber(text);
    }
}
