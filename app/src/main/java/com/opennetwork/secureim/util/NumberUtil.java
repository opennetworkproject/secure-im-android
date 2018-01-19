package com.opennetwork.secureim.util;

import android.telephony.PhoneNumberUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberUtil {

  private static final Pattern emailPattern = android.util.Patterns.EMAIL_ADDRESS;

  public static boolean isValidEmail(String number) {
    Matcher matcher = emailPattern.matcher(number);
    return matcher.matches();
  }

  public static boolean isValidSmsOrEmail(String number) {
    return PhoneNumberUtils.isWellFormedSmsAddress(number) || isValidEmail(number);
  }

//  public static boolean isValidSmsOrEmailOrGroup(String number) {
//    return PhoneNumberUtils.isWellFormedSmsAddress(number) ||
//        isValidEmail(number) ||
//        GroupUtil.isEncodedGroup(number);
//  }
//
//  public static String filterNumber(String number) {
//    if (number == null) return null;
//
//    int length            = number.length();
//    StringBuilder builder = new StringBuilder(length);
//
//    for (int i = 0; i < length; i++) {
//      char character = number.charAt(i);
//
//      if (Character.isDigit(character) || character == '+')
//        builder.append(character);
//    }
//
//    return builder.toString();
//  }
}
