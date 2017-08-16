package com.metinsaritas.copyphone_pc;

/**
 * Created by User on 16-Aug-17.
 */

public class MyValidator {
    public static int OTHER = 0;
    public static int PHONE = 1;
    public static int LINK = 2;

    public static void Validate (Copy copy) {
        copy.type = PHONE;
    }
}
