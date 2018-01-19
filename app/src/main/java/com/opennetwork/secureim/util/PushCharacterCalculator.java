package com.opennetwork.secureim.util;

public class PushCharacterCalculator extends CharacterCalculator {
  private static final int MAX_SIZE = 2000;
  @Override
  public CharacterState calculateCharacters(String messageBody) {
    return new CharacterState(1, MAX_SIZE - messageBody.length(), MAX_SIZE);
  }
}

