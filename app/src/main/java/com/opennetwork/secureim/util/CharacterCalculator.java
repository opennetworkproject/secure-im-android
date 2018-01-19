package com.opennetwork.secureim.util;

public abstract class CharacterCalculator {

  public abstract CharacterState calculateCharacters(String messageBody);

  public static class CharacterState {
    public int charactersRemaining;
    public int messagesSpent;
    public int maxMessageSize;

    public CharacterState(int messagesSpent, int charactersRemaining, int maxMessageSize) {
      this.messagesSpent       = messagesSpent;
      this.charactersRemaining = charactersRemaining;
      this.maxMessageSize      = maxMessageSize;
    }
  }
}

