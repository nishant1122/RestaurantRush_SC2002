# AI Chat History: Restaurant Rush

Tool: Claude Code (Claude Opus 5.5)

---

## Q1

In the worked example (3.3), why can't the waiter serve the Pasta on turn 3 even though it's READY?

## A1

The Pasta isn't READY yet at the moment the waiter acts. It only becomes READY later in turn 3.

The answer comes from step 3 of the turn sequence (§3.2, "Kitchen and serving"):

> *The Waiter acts before the Chef. When an order's last preparation unit is done, it becomes READY. A dish can be served only if it was already READY when the Waiter acts, so a dish finished this turn is served next turn at the earliest.*

Here is turn 3 in order:

1. **The waiter acts first.** The Pasta has 2 preparation units and only unit 1 is done (from turn 2), so it is still in preparation. There is nothing to serve.
2. **The chef acts next.** The chef does unit 2 of 2, and the order becomes READY.
3. **Turn end.** The customer is seated but hasn't been served, so they lose 8 satisfaction (84 → 76).

On turn 4 the waiter acts again, and this time the order was already READY, so it can be served.

The table in §3.3 says "READY" for turn 3 because that is the order's state at the end of the turn. When the waiter acted, it wasn't READY yet.

**Why it matters:** this rule costs one turn of waiting, which is the extra −8 on turn 3. The same pattern shows up in other steps. A customer who becomes READY_TO_PAY pays the next turn at the earliest, and the chef can work on an order the waiter took that same turn only because the waiter acts first. Your code should check whether an order is READY when the waiter acts, before the chef works on it. If it doesn't, your output won't match the example.

---

## Q2

If a customer is served on turn 4, which turn do they become READY_TO_PAY, and which turn can they pay? Walk through the timing.

## A2

They become **READY_TO_PAY on turn 5** and can **pay on turn 6** at the earliest. This matches the §3.3 example.

### Turn 4: served
- **Step 3 (Kitchen and serving):** The waiter serves the READY order, and it becomes SERVED.
- **Step 4 (Eating and payment):** Nothing happens yet. The eating rule only covers a customer *"served on an **earlier** turn,"* and this customer was served this turn.
- **Step 5 (Waiting):** They don't lose satisfaction, because they have now been served. They stay at 76.

### Turn 5: finishes eating
- **Step 4:** They were served on turn 4, which is an earlier turn. So they *"finish one full turn of eating and become READY_TO_PAY."*
- The cashier also runs in step 4, but it can't charge them yet: *"A customer who became READY_TO_PAY this turn pays next turn at the earliest."*
- **Step 5:** No loss, because customers who are ready to pay lose nothing. Still 76.

### Turn 6: pays
- **Step 4:** They were already READY_TO_PAY when the turn started, so the cashier can collect $18.00. The customer and the order become PAID, the table is freed, and satisfaction 76 is recorded for the average.

### The pattern
| Event on turn *N* | Next state, earliest |
|---|---|
| Order finishes cooking (READY) | Served on *N+1* |
| Served | READY_TO_PAY on *N+1* |
| READY_TO_PAY | Paid on *N+1* |

Each state change takes effect one turn later. That's why the turn-16 arrival can never pay within 18 turns (§3.4).

**One thing to watch:** turn 6 is the *earliest* they can pay, not a guarantee. The cashier handles at most one READY_TO_PAY customer per turn, oldest first. If another customer was already waiting to pay, this one slides to turn 7. They still don't lose satisfaction while they wait.

---

## Q3

Should satisfaction-loss logic live in Customer or in GameController? What are the tradeoffs?

## A3

Split it: **Customer decides how much satisfaction drops, GameController decides when it drops.** Your stage-3 code already does most of this.

### What you have now
- **`Customer`** owns the satisfaction value, the loss rate (`getSatisfactionLoss()`: 8, overridden to 5 in VIP and 12 in Critic), the clamp at 0 (`loseSatisfaction()`, `Customer.java:76`), and whether the customer counts as waiting (`isWaitingForService()`).
- **`GameController`** owns the step-5 timing (`GameController.java:72-81`). It runs at turn end, after serving and eating, and only for customers who are still waiting.

### Why each piece belongs where it is
| Concern | Home | Reason |
|---|---|---|
| Rate per customer type | Customer subclasses | Polymorphism. Adding a new customer type means one new subclass and no changes to the controller (Open–Closed). |
| Clamp at 0 and the satisfaction field | Customer | Information Expert and encapsulation. No other class can push satisfaction out of the 0–100 range. |
| Which states lose satisfaction | Customer (`isWaitingForService`) | It depends only on the customer's own state. |
| When in the turn it happens | GameController | Turn order is a rule about the game, not about a customer. The served-on-turn-4 example from earlier only works if this runs after step 4. |

### If everything lived in GameController
- **For:** all the turn rules sit in one file, which is easy to trace.
- **Against:** you'd need `if (c instanceof VIPCustomer) ... else if (c instanceof CriticCustomer)` checks. That breaks OCP, the controller grows into a god class, and something outside Customer would have to reach in and set satisfaction directly, which breaks encapsulation. Markers for this assignment look for exactly these problems.

### If everything lived in Customer
- **For:** maximum encapsulation.
- **Against:** a customer would need to know where the game is in the turn, or it would update itself on its own schedule. Timing rules would then be spread across entity classes and harder to check against §3.2.

### One small improvement
The controller still asks and then decides, as in `if (c.getSatisfaction() == 0) c.leaveUnhappy();`. That's a mild Tell-Don't-Ask violation, and the rule "reaching 0 means leaving" is a customer rule. You could fold it into Customer:

```java
/** Applies end-of-turn waiting loss. Returns true if the customer left. */
public boolean endTurnWaiting() {
    if (!isWaitingForService()) return false;
    loseSatisfaction();
    if (satisfaction == 0) { leaveUnhappy(); return true; }
    return false;
}
```

The controller then just runs the loop and prints the message. Your current design is already defensible, though, so this change is optional polish. What matters more is that the report explains the split: Customer handles how much, and the controller handles when.

---

## Q4

Should CustomerState and OrderStatus be enums? What would a state transition method look like?

## A4

Yes, and they already are. `CustomerState.java` and `OrderStatus.java` are both enums, which is the right choice. The weakness is in how the states change. Every transition is a plain assignment, so nothing stops an illegal move:

- `Order.markServed()` works on a PREPARING order, which would let the turn-3 Pasta be served.
- `Order.prepareOneUnit()` keeps counting even after the order is READY or CANCELLED.
- `Customer.pay()` works on someone who is still EATING, which skips the READY_TO_PAY turn.
- `Customer.leaveUnhappy()` works on someone who already PAID.

Your Waiter, Cashier and Restaurant code probably checks for these before calling. Even so, the entity itself should refuse a transition that can never be valid.

### 1. Put the transition rules in the enum

```java
public enum OrderStatus {
    PREPARING, READY, SERVED, PAID, CANCELLED;

    /** Legal moves in the order life cycle. */
    public boolean canMoveTo(OrderStatus next) {
        switch (this) {
            case PREPARING: return next == READY  || next == CANCELLED;
            case READY:     return next == SERVED || next == CANCELLED; // customer can leave before it's served
            case SERVED:    return next == PAID;
            default:        return false;                               // PAID and CANCELLED are final
        }
    }
}
```

```java
public enum CustomerState {
    WAITING, SEATED, ORDERED, EATING, READY_TO_PAY, PAID, LEFT;

    public boolean canMoveTo(CustomerState next) {
        switch (this) {
            case WAITING:      return next == SEATED  || next == LEFT;
            case SEATED:       return next == ORDERED || next == LEFT;
            case ORDERED:      return next == EATING  || next == LEFT;
            case EATING:       return next == READY_TO_PAY;   // served customers never leave
            case READY_TO_PAY: return next == PAID;
            default:           return false;                  // PAID and LEFT are final
        }
    }

    public boolean isWaitingForService() {
        return this == WAITING || this == SEATED || this == ORDERED;
    }
}
```

I used a `switch` instead of storing an `EnumSet` of next states in the enum constructor. An enum constructor can't refer to constants declared after it, so the switch is simpler. I also moved `isWaitingForService()` into the enum, because it depends only on the state.

### 2. Route every change through one private method

```java
// Customer.java
private void moveTo(CustomerState next) {
    if (!state.canMoveTo(next)) {
        throw new IllegalStateException(name + " cannot go from " + state + " to " + next);
    }
    state = next;
}

public void sitAt(Table table)    { moveTo(CustomerState.SEATED); this.table = table; table.seat(this); }
public void placeOrder(Order o)   { moveTo(CustomerState.ORDERED); this.order = o; }
public void receiveFood(int turn) { moveTo(CustomerState.EATING); order.markServed(); servedTurn = turn; }
public void finishEating()        { moveTo(CustomerState.READY_TO_PAY); }
public void pay()                 { moveTo(CustomerState.PAID); order.markPaid(); leaveTable(); }
```

`Order` follows the same pattern: `markServed()` calls `moveTo(OrderStatus.SERVED)`. `prepareOneUnit()` should first throw unless the status is PREPARING. Call `moveTo` first in each method, so a failed check leaves the object unchanged.

### 3. Keep two kinds of error separate
- **`IllegalStateException` in the entity** is a safety net for bugs. It should never fire in normal play.
- **The §3.4 rule that an invalid choice must explain why it failed and let the manager choose again** belongs earlier, in Waiter or Restaurant. Check the condition there and throw a readable `IllegalArgumentException`, like your existing `"No free table..."` message, so the UI can show it without using up the staff task.

### Why not the State pattern (a class per state)?
It's the textbook OO answer, but your states carry no data or behaviour of their own. Only the allowed transitions differ between them. Seven classes per life cycle would add complexity for no real gain. In the report, you can say you considered the State pattern and chose an enum with a transition table because the states differ only in which moves are legal.

I can apply this to the stage-3 files (`CustomerState`, `OrderStatus`, `Customer`, `Order`) and re-run the victory and defeat test scripts to confirm the output doesn't change, if you'd like.

---

## Q5

How do I handle non-numeric input with Scanner without crashing or skipping a line?

## A5

Your `ConsoleUI.readInt` (`ConsoleUI.java:114`) already uses the pattern that avoids both problems: read the whole line, then parse it.

```java
String line = scanner.nextLine().trim();
try {
    return Integer.parseInt(line);
} catch (NumberFormatException e) {
    System.out.println("  !! '" + line + "' is not a number.");
}
```

### Why the usual approach breaks

**1. Crashing.** `scanner.nextInt()` throws `InputMismatchException` when the user types `abc`. Catching it isn't enough, because the bad token stays in the buffer. The next `nextInt()` reads `abc` again and the loop never ends unless you discard the token with `scanner.next()`.

**2. Skipping a line.** `nextInt()` reads `3` but leaves the `\n` behind. The next `nextLine()` returns that empty leftover right away, so a prompt like your `askHappyHour()` would look skipped.

Reading with `nextLine()` every time fixes both. Each call consumes exactly one full line, newline included, and a bad line is discarded the moment you read it. Rule of thumb: **don't mix `nextInt()`/`next()` with `nextLine()` on the same Scanner. Use `nextLine()` everywhere and parse the result yourself.**

Your other two safeguards are also right:
- **`hasNextLine()` before reading:** without it, end of input from a piped test file throws `NoSuchElementException`.
- **A single shared `Scanner`:** creating a new `Scanner(System.in)` in each method can lose input that the previous one had already buffered, and closing one closes `System.in`.

### Optional improvements

**Range checking in the same loop.** Right now `readInt` accepts any integer, such as `-5` or `99`. Range checks are input format, not game rules, so this stays inside the UI layer:

```java
private int readInt(String prompt, int min, int max) {
    while (true) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) { System.out.println("(no more input - choosing 0)"); return 0; }
        String line = scanner.nextLine().trim();
        System.out.println(line);
        try {
            int n = Integer.parseInt(line);
            if (n >= min && n <= max) return n;
            System.out.println("  !! Enter a number from " + min + " to " + max + ".");
        } catch (NumberFormatException e) {
            System.out.println("  !! '" + line + "' is not a number.");
        }
    }
}
```

Keep the line between UI and domain clear. **The UI checks whether the input is a number in the menu's range. The domain checks whether the move is legal.** "Table 2 is occupied" or "that order isn't READY" should still come back from Waiter or Restaurant as an exception message, which the UI shows before prompting again (§3.4).

**The echo.** `System.out.println(line)` makes piped test runs readable, but in an interactive terminal every input appears twice. If that bothers you, you could echo only when `System.console() == null`, which is true when input is piped. Your test transcripts depend on the echo, though, so leave it alone if the expected outputs are compared line by line.

---

## Q6

How do I tag a commit as stage-1 and push the tag?

## A6

The `stage-1`, `stage-2` and `stage-3` tags already exist in your local repo, on the right commits. They haven't reached GitHub yet, because the push fails before it gets there.

### Current state
| Tag | Commit | Message |
|---|---|---|
| `stage-1` | `4094ac2` | Stage 1: basic restaurant operation |
| `stage-2` | `ccd87cc` | Stage 2: VIP and Critic customers |
| `stage-3` | `30bfea9` | Stage 3: Host, ComboMeal and Happy Hour |

When I asked `origin` for its tags, GitHub replied **"Repository not found"**. Either the repo `nishant1122/SC2002_Project_Restaurant_Rush` doesn't exist under that exact name, or it's private and git isn't logged in to an account with access. GitHub shows "not found" in both cases.

### The general commands
```bash
git tag -a stage-1 -m "Stage 1: basic restaurant operation"           # tags the current commit (HEAD)
git tag -a stage-1 4094ac2 -m "Stage 1: basic restaurant operation"   # tags a specific commit
git push origin stage-1                                               # push one tag
git push origin --tags                                                # push every tag
```

A plain `git push` does **not** send tags, so you have to push them explicitly.

`-a` creates an *annotated* tag, which records who made it, when, and a message. Your current tags are *lightweight*, meaning just a name pointing at a commit. Either kind satisfies "tagged in GitHub", but annotated tags are the usual choice for releases. To convert one:

```bash
git tag -a -f stage-1 4094ac2 -m "Stage 1: basic restaurant operation"
```

Run this only before pushing. Changing a tag after teammates have fetched it causes confusion.

### What to do next
1. **Check the repo.** Open `https://github.com/nishant1122/SC2002_Project_Restaurant_Rush` in a browser while logged in. If the name is different, fix the remote with `git remote set-url origin <correct-url>`. If the repo doesn't exist, create it on GitHub. You'll also need to push your branch (`git push -u origin <branch>`) so the tagged commits are there.
2. **If it's private, log in.** Run `! gh auth login` (or let Git Credential Manager prompt you), then test with `git ls-remote origin`.
3. **Push the tags:** `git push origin --tags`
4. **Verify:** `git ls-remote --tags origin` should list all three. They'll also appear under **Tags** on the repo page.

I haven't pushed anything. Once the remote works, I can push the branch and the tags for you.

---

## Q7

Help me design an input sequence that reliably reaches Victory (≥4 paid, ≥$45, avg ≥60). Then I'll run it and check.

## A7

I designed this for **Stage 1**, using your controller's input order and food rule. It should finish with **5 paid customers, $66.00 revenue and average satisfaction 79.2**, well above all three targets. I haven't run it, so the checkpoints below are what to compare against.

### Facts the plan is built on
- **Food choice:** customer *n* gets menu item *(n−1) mod 3*. So C1 Salad (1 unit), C2 Burger (1), C3 Pasta (2), C4 Salad, C5 Burger, C6 Pasta.
- **Arrivals:** turns 1, 4, 7, 10, 13, 16. There are 2 tables.
- **Input per turn:** the waiter task (`1` seat, `2` order, `3` serve, `0` wait), then the customer number if the task isn't `0`, then the chef's customer number (`0` = wait).

### Strategy
- **Seat a customer the turn they arrive.** Waiting in the queue costs −8 just like sitting unserved, so there's nothing to gain by delaying.
- **Take the order and have the chef cook it in the same turn.** The waiter acts before the chef, so the chef can start on an order taken that turn. A 1-unit dish is then READY at the end of that turn.
- **Serve on the next turn.** A dish finished this turn can't be served until the next one. A 1-unit customer is served 2 turns after arriving and finishes at **84**. Pasta needs one extra turn and finishes at **76**.
- **From turn 10 on, the waiter is one turn behind schedule.** Each new arrival comes on the same turn as a serve. Serving first gets the existing customer to the cashier sooner, and the newcomer loses only one extra 8 points in the queue.

### Turn-by-turn plan
| Turn | Arrives | Waiter | Chef | What happens | Input lines |
|---|---|---|---|---|---|
| 1 | C1 Salad | seat C1 | wait | | `1 1 0` |
| 2 | | order C1 | cook C1 | C1 READY | `2 1 1` |
| 3 | | serve C1 | wait | | `3 1 0` |
| 4 | C2 Burger | seat C2 | wait | C1 READY_TO_PAY | `1 2 0` |
| 5 | | order C2 | cook C2 | **C1 pays $9 (84)** | `2 2 2` |
| 6 | | serve C2 | wait | | `3 2 0` |
| 7 | C3 Pasta | seat C3 | wait | C2 READY_TO_PAY | `1 3 0` |
| 8 | | order C3 | cook C3 (1/2) | **C2 pays $15 (84)** | `2 3 3` |
| 9 | | wait | cook C3 (2/2) | C3 READY, but too late to serve this turn | `0 3` |
| 10 | C4 Salad | serve C3 | wait | C4 waits in the queue (92) | `3 3 0` |
| 11 | | seat C4 | wait | C3 READY_TO_PAY | `1 4 0` |
| 12 | | order C4 | cook C4 | **C3 pays $18 (76)** | `2 4 4` |
| 13 | C5 Burger | serve C4 | wait | | `3 4 0` |
| 14 | | seat C5 | wait | C4 READY_TO_PAY | `1 5 0` |
| 15 | | order C5 | cook C5 | **C4 pays $9 (76)** | `2 5 5` |
| 16 | C6 Pasta | serve C5 | wait | | `3 5 0` |
| 17 | | seat C6 | wait | C5 READY_TO_PAY | `1 6 0` |
| 18 | | order C6 | cook C6 | **C5 pays $15 (76)** | `2 6 6` |

On turn 9 the waiter only takes a single `0`, with no customer number, so that turn has two lines. Seating C6 is optional, since the turn-16 arrival can never pay. It's there to show the order being taken and to confirm nothing breaks. There's always a free table when one is needed, because each table frees up when its customer pays.

### Expected final screen
```
Paid customers: 5 (need 4)
Revenue: $66.00 (need $45.00)
Avg satisfaction: 79.2 (need 60)     ← (84+84+76+76+76)/5; the exact format depends on formatAverage
*** VICTORY! ***
```

### Running it
I saved the input as one value per line in `stage1-victory-plan.txt`.

From `RestaurantRush/stage-1`:
```bash
javac -d out src/*.java
java -cp out Main < stage1-victory-plan.txt
```

**What to check:**
- No `!!` error lines should appear.
- Payments should land on turns 5, 8, 12, 15 and 18.
- C3 should show `READY` at the end of turn 9 and be served on turn 10.

If a payment comes a turn early or late, the most likely cause is the order of the step-4 checks for paying and finishing eating.

Your existing `stage-1/tests/victory.txt` follows almost the same plan. It also includes invalid input (`abc`), which demonstrates §3.4 error handling, so keep that file for the submission. This plan is the clean version.

For Stage 2/3, the VIP (−5) and Critic (−12) loss rates and Stage 3's Host and combo meals change the numbers, so this file won't produce the same results there.

---



