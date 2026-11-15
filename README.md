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

## Stage 2: VIP and Critic customers

```
cd stage-2
javac -d out src/*.java
java -cp out Main
```

- C2 (turn 4) is a **VIP**: loses 5 satisfaction per turn and gets 10% off.
- C4 (turn 10) is a **Critic**: loses 12 satisfaction per turn and always orders the most expensive item.
- Everyone else is Regular.

Changes from Stage 1: new `VIPCustomer` and `CriticCustomer`; `ArrivalSchedule` creates them;
`Menu.getMostExpensive()` and `Money.percent()` added. `GameController`, `Waiter`, `Chef`,
`Cashier` and `ConsoleUI` are unchanged, because the new types override `Customer` methods (polymorphism).

## Stage 3: Host, Combo meals and Happy Hour

```
cd stage-3
javac -d out src/*.java
java -cp out Main
```

Each turn you now choose:
1. `Start Happy Hour now? (y/n)` - asked only until you use it once.
2. Host: customer number to seat, or `0`.
3. Waiter: `1` seat, `2` take order, `3` serve, `0` wait, then the customer number.
4. Chef: customer number whose order to cook, or `0`.

- **Host**: seats one waiting customer per turn, before the Waiter, so the Waiter is free to take an order in the same turn.
- **Combo meals**: Salad + Pasta Combo ($24.30, 3 units) and Burger + Salad Combo ($21.60, 2 units). Price is 90% of the two items, prep units are summed.
- **Happy Hour**: can be switched on once per game, lasts 2 turns. New orders get 20% off (after the VIP discount), and waiting customers recover 15 satisfaction (max 100).

Scripted runs: `java -cp out Main < tests/victory.txt` (VICTORY) and `tests/defeat.txt` (DEFEAT).

Changes from Stage 2:

| File | Change | Why |
|---|---|---|
| `Host.java` | new | Seats one queued customer per turn; reuses `Restaurant.seatCustomer()`. |
| `ComboMeal.java` | new | Extends `MenuItem`, built from two items, so `Menu`, `Order` and `Chef` use it unchanged. |
| `HappyHour.java` | new | Once-per-game, 2-turn pricing policy. |
| `GameController.java` | changed | Happy Hour step at turn start, Host step before the Waiter, `happyHour.endTurn()`. |
| `ConsoleUI.java` | changed | Happy Hour and Host prompts, Happy Hour status. |
| `Waiter.java` | changed | `takeOrder()` applies the Happy Hour discount after the customer's own discount. |
| `Restaurant.java` | changed | Owns the `HappyHour` object. |
| `Customer.java` | changed | Added `recoverSatisfaction()` (capped at 100). |
| `Main.java` | changed | Adds the two combos to the menu. |

Stage 1 and 2 rules are unchanged: `Chef`, `Cashier`, `Menu`, `Order`, the customer types and `ArrivalSchedule` are untouched.

## Stages and comparisons

| Stage | Tag |
|---|---|
| Stage 1 | [stage-1](https://github.com/nishant1122/RestaurantRush_SC2002/tree/stage-1) |
| Stage 2 | [stage-2](https://github.com/nishant1122/RestaurantRush_SC2002/tree/stage-2) |
| Stage 3 | [stage-3](https://github.com/nishant1122/RestaurantRush_SC2002/tree/stage-3) |

Compare locally: `diff -r stage-1/src stage-2/src` and `diff -r stage-2/src stage-3/src`.

## Video timestamps

| # | Segment | Time | Presenter |
|---|---|---|---|
| 1 | Game overview and the three stages | TODO | Hao Lin |
| 2 | Gameplay demonstration and key outcomes | TODO | Nishant |
| 3 | Class diagrams and evidence of stage changes | TODO | Trevor |
| 4 | Use case and sequence diagram | TODO | Jin Sheng |
| 5 | Individual contributions and key learning | TODO | Zheng Rong |

## AI history

| Member | Link |
|---|---|
| Nishant | [ai-history](https://github.com/nishant1122/RestaurantRush_SC2002/blob/Nishant/ai-history/stage-1/chat.md) |
| Yim Zheng Rong | [ai-history](https://github.com/nishant1122/RestaurantRush_SC2002/blob/Yim-Zheng-Rong/ai-history%3Astage-2/chat.md) |
| Hao Lin | [ai-history](https://github.com/nishant1122/RestaurantRush_SC2002/blob/Hao-Lin/ai-history/report/chat.md) |
| Jin Sheng | [ai-history](https://github.com/nishant1122/RestaurantRush_SC2002/blob/jinsheng/ai-history/sequence/chat.md) |
