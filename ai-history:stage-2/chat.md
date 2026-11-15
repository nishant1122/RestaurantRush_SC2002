# AI Chat History: Restaurant Rush

Tool: Claude Code (Claude Opus 5.5)

---

## Q1

How should I set up Stage 2? Should I edit the Stage 1 code in place or start a new folder?

## A1

Start a new folder. Copy `stage-1/` to `stage-2/` and make every Stage 2 change there.

The brief says *"Stage 2 is independently playable and uses the same win and lose criteria."* If you edit Stage 1 in place, Stage 1 stops being runnable on its own, and markers can't compare the two. Each folder should compile and run by itself:

```bash
cd stage-2
javac -d out src/*.java
java -cp out Main < tests/victory.txt
```

### Suggested commit order
Small commits make the Git history show *how* the design was extended, which is what Stage 2 is really testing:

| Commit | What it adds |
|---|---|
| 1 | Copy `stage-1` to `stage-2`, plus any small helpers you'll need (`Money.percent`, `Menu.getMostExpensive`) |
| 2 | `VIPCustomer` |
| 3 | `CriticCustomer` |
| 4 | Register them in `ArrivalSchedule` |
| 5 | Update `tests/victory.txt` and `tests/defeat.txt` |
| 6 | README section for Stage 2 |

### What should *not* change
If the Stage 1 design is right, Stage 2 needs **no changes** to `GameController`, `Chef`, `Table`, `Order` or the turn sequence. A few small hooks in `Customer` and a one-line change in `Waiter` should be enough. If you find yourself editing the turn loop, that's a sign the variation is in the wrong place (see Q2).

---

## Q2

What does "Different behaviour must be dispatched through the customer objects, not through a chain of type checks in the turn controller" mean?

## A2

It's asking you to use **polymorphism**: the game loop should treat every customer as a plain `Customer` and call the same methods. Each subclass then supplies its own version of those methods. The loop should never ask "what kind of customer is this?" and branch on the answer.

### What's banned (type checks in the turn controller)

```java
// GameController step 5 — DON'T do this
for (Customer c : restaurant.getCustomers()) {
    if (c.isWaitingForService()) {
        if (c instanceof VIPCustomer) {
            c.setSatisfaction(c.getSatisfaction() - 5);
        } else if (c instanceof CriticCustomer) {
            c.setSatisfaction(c.getSatisfaction() - 12);
        } else {
            c.setSatisfaction(c.getSatisfaction() - 8);
        }
    }
}
```

`if (c.getType() == CustomerType.VIP)` and a `switch` on a type string count too, and so does the same kind of check inside `Cashier` or `Waiter`. All of them break the Open/Closed Principle. Adding a new customer type would mean editing every place with a branch.

### What's wanted (dispatch through the object)

Stage 1 already does this for satisfaction. Step 5 of the turn just calls:

```java
c.loseSatisfaction();   // GameController doesn't know or care what c really is
```

and `Customer.loseSatisfaction()` delegates to a hook method:

```java
satisfaction = Math.max(0, satisfaction - getSatisfactionLoss());
```

So Stage 2 only needs overrides:

```java
public class VIPCustomer extends Customer {
    @Override public int getSatisfactionLoss() { return 5; }
}
public class CriticCustomer extends Customer {
    @Override public int getSatisfactionLoss() { return 12; }
}
```

The controller loop stays exactly as it is. At runtime Java picks the right `getSatisfactionLoss()` through dynamic binding.

### Applying the same idea to the other differences

| Difference | Hook in `Customer` | Override |
|---|---|---|
| Satisfaction loss | `getSatisfactionLoss()` returns 8 | VIP 5, Critic 12 |
| Bill | `calculateBill(int basePriceCents)` returns the full price | VIP returns 90% |
| Dish choice | `chooseItem(Menu menu)` returns item *(id−1) mod 3* | Critic picks the most expensive |

### Where type-specific code *is* allowed

The one place that has to name concrete subclasses is where customers are created: `ArrivalSchedule`, with `new VIPCustomer(2)` and `new CriticCustomer(4)`. That's construction, not behaviour, so it's fine.

The bullet *"All customer types follow the same order and service flow"* points the same way. The state machine (`sitAt → placeOrder → receiveFood → finishEating → pay`) stays in the base class and isn't overridden.

**A quick self-check:** `grep -n "instanceof\|getClass" stage-2/src/*.java` should find nothing.

---

## Q3

What should the Critic's special behaviour be? The brief just says "a distinct order preference or reaction to service that you define and document."

## A3

The Critic row has two fixed rules and one open one:

| | Critic |
|---|---|
| Satisfaction loss per waiting turn | **12** (fixed) |
| Bill | **Full price** (fixed) |
| Special behaviour | **Your choice**, either an order preference *or* a reaction to service |

You need one behaviour, implemented polymorphically and explained in the README.

### Recommended: always order the most expensive item

```java
public class CriticCustomer extends Customer {
    public CriticCustomer(int id) { super(id); }

    @Override public String getType() { return "Critic"; }
    @Override public int getSatisfactionLoss() { return 12; }
    @Override public MenuItem chooseItem(Menu menu) { return menu.getMostExpensive(); }
}
```

`Menu.getMostExpensive()` is a small helper that loops over the items and keeps the one with the highest `getPriceCents()`. With the base menu that's Pasta ($18.00, 2 units).

`Waiter.takeOrder` asks the customer instead of choosing for them:

```java
MenuItem item = customer.chooseItem(restaurant.getMenu());
```

### Why this is a good choice
- **It's the simplest correct option.** It overrides a hook (`chooseItem`) that every customer already uses, so nothing else changes.
- **It creates a real trade-off for the player.** The Critic pays the most, but it also loses patience fastest (12 per turn, so it leaves after 9 waiting turns instead of 13), *and* it always orders the dish that takes 2 units to cook. Serve it first for the big bill, or risk losing it.

### Other valid choices
**Order preferences** (override `chooseItem`): always orders a signature dish, or never orders the same item as the previous customer.

**Reactions to service** (add a hook such as `onServed(int waitTurns)`, empty in `Customer`, overridden in `CriticCustomer`): a bonus if served quickly, or a reputation penalty if satisfaction is low when paying. These need a new hook that `Waiter` or `Cashier` calls for *every* customer, so they mean more code.

### What to document
Add a line like this to the README:

> **Critic customer (C4):** loses 12 satisfaction per waiting turn and pays full price. **Special behaviour:** always orders the most expensive item on the menu (`CriticCustomer.chooseItem` overrides `Customer.chooseItem`). This makes the Critic the highest-value but least patient customer.

---

## Q4

Where should the VIP 10% discount be calculated: in the Cashier or in the Customer? And how do I avoid rounding errors?

## A4

**The customer decides the price, and it's fixed when the order is placed.** The Cashier just collects whatever the order says.

### Why not in the Cashier
The obvious approach is a check in the Cashier:

```java
int bill = order.getPriceCents();
if (customer instanceof VIPCustomer) bill = bill * 9 / 10;   // DON'T
```

That's exactly the type check Q2 rules out. Every new customer type with a different bill would mean editing the Cashier.

### The polymorphic version
`Customer` gets a hook that returns the full price by default:

```java
public int calculateBill(int basePriceCents) { return basePriceCents; }
```

`VIPCustomer` overrides it:

```java
@Override
public int calculateBill(int basePriceCents) { return Money.percent(basePriceCents, 90); }
```

`Waiter.takeOrder` uses it when it creates the order:

```java
MenuItem item = customer.chooseItem(restaurant.getMenu());
int price = customer.calculateBill(item.getPriceCents());   // each customer type prices its own bill
customer.placeOrder(new Order(item, price));
```

`Cashier.collectPayment` is unchanged from Stage 1. It still charges `customer.getOrder().getPriceCents()`.

Pricing at order time has a nice side effect: the discounted price shows up as soon as the order is taken (`C2 (VIP) orders Burger for $13.50.`), so the player can see the discount working.

### Rounding
Keep money as **integer cents** everywhere, as in Stage 1. Never use `double` for money, because `0.9 * 1500` style arithmetic can give results like `1349.9999`.

Do the percentage in one integer helper that rounds half up:

```java
public static int percent(int cents, int percent) {
    return (cents * percent + 50) / 100;
}
```

| Item | Full | VIP (90%) |
|---|---|---|
| Salad | $9.00 | $8.10 |
| Burger | $15.00 | $13.50 |
| Pasta | $18.00 | $16.20 |

All three divide exactly with the base menu, but the `+ 50` keeps the result correct if a price like $9.99 is ever added.

---

## Q5

How do I add the VIP and Critic to ArrivalSchedule? Which regular arrivals should I replace?

## A5

Keep the Stage 1 timing (turns 1, 4, 7, 10, 13, 16) and replace two of the regular customers. That satisfies *"there must still be six arrivals"* and keeps the run reproducible, because the same customer type always arrives on the same turn.

```java
public class ArrivalSchedule {

    public Customer customerArrivingOn(int turn) {
        if (turn > 16 || turn % 3 != 1) {
            return null;
        }
        int id = turn / 3 + 1;
        if (id == 2) {
            return new VIPCustomer(id);
        }
        if (id == 4) {
            return new CriticCustomer(id);
        }
        return new Customer(id);
    }
}
```

The `if`s here are fine: this is a factory deciding *what to create*, not a controller deciding *how to behave* (see Q2). The method still returns plain `Customer`, so the rest of the game never sees the subclass.

### Why C2 and C4
| Slot | Arrives | Type | Reason |
|---|---|---|---|
| C1 | turn 1 | Regular | The opening matches the §3.3 worked example, so Stage 1 and Stage 2 can be compared directly. |
| **C2** | turn 4 | **VIP** | Early enough to be served and pay well within 18 turns, so the discount clearly appears in the run. |
| C3 | turn 7 | Regular | |
| **C4** | turn 10 | **Critic** | Arrives while C3 is still being served, so it actually waits in the queue. That shows off the −12 rate, and there's still time for it to pay. |
| C5, C6 | 13, 16 | Regular | |

Avoid putting the Critic at C6 (turn 16). Nobody who arrives on turn 16 can pay within 18 turns, so the Critic's behaviour would barely show.

### Make the type visible
Override `getType()` in each subclass and use it in `toString()`:

```java
@Override public String toString() { return "C" + id + " (" + getType() + ")"; }
```

Every message then shows the type, e.g. `C4 (Critic) arrives and joins the queue.`, which makes the test transcripts much easier to check.

---

## Q6

Can you design a Stage 2 input sequence that reaches Victory, using the VIP and Critic? Then I'll run it and check.

## A6

This plan is in `stage-2/tests/victory.txt`. I ran it against the Stage 2 code and it finishes with **5 paid customers, $73.50 revenue and average satisfaction 75.6**.

### Facts the plan is built on
- **Arrivals:** C1 Regular (turn 1), C2 VIP (4), C3 Regular (7), C4 Critic (10), C5 Regular (13), C6 Regular (16). There are 2 tables.
- **Food:** regular customers use *(id−1) mod 3*, so C1 Salad, C3 Pasta, C5 Burger, C6 Pasta. The VIP gets Burger at 90% ($13.50). The Critic always orders Pasta ($18.00, 2 units).
- **Input per turn:** the waiter task (`1` seat, `2` order, `3` serve, `0` wait), then the customer number if the task isn't `0`, then the chef's customer number (`0` = wait).

### Turn-by-turn plan
| Turn | Arrives | Waiter | Chef | What happens | Input |
|---|---|---|---|---|---|
| 1 | C1 Regular | seat C1 | wait | | `1 1 0` |
| 2 | | order C1 | cook C1 | C1 Salad READY | `2 1 1` |
| 3 | | serve C2 ✗, `abc` ✗, serve C1 | wait | two error messages shown, then C1 served | `3 2 abc 3 1 0` |
| 4 | C2 VIP | seat C2 | wait | C1 READY_TO_PAY | `1 2 0` |
| 5 | | order C2 | cook C2 | C2 orders Burger for **$13.50**; **C1 pays $9.00 (84)** | `2 2 2` |
| 6 | | serve C2 | wait | | `3 2 0` |
| 7 | C3 Regular | seat C3 | wait | C2 READY_TO_PAY | `1 3 0` |
| 8 | | order C3 | cook C3 (1/2) | **C2 pays $13.50 (90)** | `2 3 3` |
| 9 | | wait | cook C3 (2/2) | C3 READY | `0 3` |
| 10 | C4 Critic | serve C3 | wait | C4 queues | `3 3 0` |
| 11 | | seat C4 | wait | C3 READY_TO_PAY | `1 4 0` |
| 12 | | order C4 | cook C4 (1/2) | C4 orders **Pasta**; **C3 pays $18.00 (76)** | `2 4 4` |
| 13 | C5 Regular | seat C5 | cook C4 (2/2) | C4 READY | `1 5 4` |
| 14 | | serve C4 | wait | | `3 4 0` |
| 15 | | order C5 | cook C5 | C4 READY_TO_PAY | `2 5 5` |
| 16 | C6 Regular | serve C5 | wait | **C4 pays $18.00 (52)** | `3 5 0` |
| 17 | | seat C6 | wait | C5 READY_TO_PAY | `1 6 0` |
| 18 | | order C6 | cook C6 (1/2) | **C5 pays $15.00 (76)** | `2 6 6` |

Turn 3 deliberately includes two invalid inputs. Serving C2 fails because C2 hasn't arrived yet, and `abc` isn't a number. Both show an error and let the manager choose again without using up the waiter's task (§3.4).

### Where the customer types show up
- **VIP (C2):** pays $13.50 instead of $15.00, and finishes at **90**. It lost 5 per turn for two turns (seated, ordered). A regular customer on the same path would be at 84.
- **Critic (C4):** orders Pasta even though its id would give a regular customer Salad. It waits four turns (queued, seated, ordered, cooking) at −12 and finishes at **52**. A regular customer on the same path would finish at 68. It still pays the highest bill of the game.

### Final screen
```
Paid customers: 5 (need 4)
Revenue: $73.50 (need $45.00)
Avg satisfaction: 75.6 (need 60)
*** VICTORY! ***
```
Average: (84 + 90 + 76 + 52 + 76) / 5 = 75.6.

### Running it
From `stage-2`:
```bash
javac -d out src/*.java
java -cp out Main < tests/victory.txt
```

---

## Q7

How should the Stage 2 defeat script show the new customer types?

## A7

A defeat script only needs to fail one of the three targets, but it's more useful if it also shows the **different loss rates** side by side. `stage-2/tests/defeat.txt` does that by seating C1 and C2 and then making both staff wait for the rest of the game:

```
Turn 1:  1 1 0      seat C1, chef waits
Turn 2–3: 0 0       waiter waits, chef waits
Turn 4:  1 2 0      seat C2 (VIP), chef waits
Turn 5–18: 0 0      everyone waits
```

I ran it, and the result is:

```
>> C1 (Regular) got fed up and LEFT without paying.
>> C4 (Critic) got fed up and LEFT without paying.
Paid customers: 0 (need 4)
Revenue: $0.00 (need $45.00)
Avg satisfaction: N/A (need 60)
*** DEFEAT ***
```

### Why only those two leave
| Customer | Arrives | Loss per turn | Turns to reach 0 | Leaves on |
|---|---|---|---|---|
| C1 Regular | 1 | 8 | 13 | **turn 13** |
| C2 VIP | 4 | 5 | 20 | never (still 25 at the end) |
| C3 Regular | 7 | 8 | 13 | turn 19, too late |
| **C4 Critic** | 10 | 12 | 9 | **turn 18** |

The key point is that **C4 leaves before C3 even though C3 arrived three turns earlier**, and the VIP who was seated on turn 4 never leaves at all. That's the satisfaction-loss polymorphism visible in the output, without any type check in `GameController`.

### What to check
- The leave messages come from the same step-5 loop as in Stage 1. Only `getSatisfactionLoss()` differs.
- Satisfaction clamps at 0 rather than going negative (`Math.max(0, ...)` in `Customer.loseSatisfaction()`). 9 × 12 = 108, so without the clamp the Critic would end at −8.
- The game still runs all 18 turns and ends with DEFEAT, not a crash, even though nobody paid and the average is `N/A`.

---
