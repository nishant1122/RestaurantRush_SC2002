Welcome to Restaurant Rush

A turn-based restaurant management game played in the command line. You are the manager:
each turn you decide what the Waiter and Chef do.

## How to play (Stage 1)

```
cd stage-1
javac -d out src/*.java
java -cp out Main
```

Each turn you choose:
1. Waiter: `1` seat, `2` take order, `3` serve, `0` wait, then the customer number.
2. Chef: customer number whose order to cook, or `0`.

**Win** after turn 18: at least 4 paid customers AND revenue at least $45 AND average satisfaction of paid customers at least 60.

Scripted runs: `java -cp out Main < tests/victory.txt` (VICTORY) and `tests/defeat.txt` (DEFEAT).