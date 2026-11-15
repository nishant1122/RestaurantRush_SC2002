### **Tool: Claude Sonnet 5.5**

## **Q1**

Run me through the Satisfaction Loss logic for the Restaurant Rush game.

## **A1**

## **Satisfaction Loss logic**

### **The core rule (Step 5, end of every turn)**

Every customer who is **waiting in the queue or seated but not yet served** loses satisfaction. The loss is clamped at a minimum of 0\.

| State at turn end | Loses satisfaction? |
| ----- | ----- |
| In the waiting queue | Yes |
| Seated, no order taken yet | Yes |
| Seated, order taken, cooking or READY (not yet served) | Yes |
| SERVED, eating, or READY\_TO\_PAY | No, satisfaction is frozen |
| PAID or left | N/A |

“Served” is the cutoff. A dish that is READY but not yet SERVED still costs the customer points, which is why the brief says a dish finished this turn can only be served next turn.

### **Loss per turn by customer type**

| Type | Loss per waiting turn | Introduced |
| ----- | ----- | ----- |
| Regular | 8 | Stage 1 |
| VIP | 5 | Stage 2 |
| Critic | 12 | Stage 2 |

All types follow the same flow. Only the loss amount (and bill and special behaviour) differs. The brief says this must be dispatched through the customer objects, not through type checks in the turn controller. The controller should just call something like `customer.applyWaitingLoss()` on each unserved customer, and each subclass supplies its own rate.

### **Abandonment**

If satisfaction hits 0 after the loss is applied:

1. The customer leaves without paying.  
2. Any unfinished order is cancelled.  
3. The table is freed.  
4. It counts as an unhappy departure, adds nothing to revenue, and is not included in the paid-customer average.

### **Stage 3 modifier: Happy Hour**

This is the only thing that raises satisfaction. For each of its two active turns, at the **start** of the turn, customers still waiting for a table or food recover 15 points, capped at 100\. Customers who are already served or eating are unaffected, matching the loss rule. The end-of-turn loss still applies afterwards, so a Regular nets \+7 on those turns (+15 then \-8).

### **Worked example**

This follows the brief’s Regular who orders Pasta, with the loss applied at the end of each turn.

| Turn | Event | End-of-turn satisfaction |
| ----- | ----- | ----- |
| 1 | Arrives at 100, seated | 92 (loss) |
| 2 | Order taken, Chef does unit 1 | 84 (loss) |
| 3 | Pasta READY, can’t be served yet | 76 (loss) |
| 4 | Served | 76 (no loss from here) |
| 5-6 | Eating, then paid | 76 is recorded for the average |

Note that the customer who arrives on a turn also loses points at the end of that same turn.

### **How fast each type reaches 0**

Starting from 100 with no service:

* **Critic (12):** hits 0 on the 9th unserved turn.  
* **Regular (8):** hits 0 on the 13th unserved turn.  
* **VIP (5):** would need 20 turns, so a VIP never abandons within an 18-turn game.

### **What this means for the win condition**

The 60 target is an average of satisfaction recorded at payment, so you can estimate how many waiting turns each type can afford:

* **Regular:** 5 loss turns lands exactly on 60\.  
* **VIP:** 8 loss turns lands exactly on 60\.  
* **Critic:** 3 loss turns gives 64, and 4 gives 52\. They are expensive to keep waiting, but they pay full price.

Since the target is an average, a VIP served late can offset a Critic served slowly.

### **Implementation notes**

* Apply the loss in one place, at the end of each turn, after the Kitchen/Serving and Eating/Payment steps. That way the customer served this turn is correctly exempt.  
* Check for abandonment right after applying the loss, in the same step.  
* Happy Hour recovery happens at turn start, before the manager makes choices. The brief doesn’t say whether it falls before or after that turn’s arrivals. Either works since new arrivals are at 100 anyway, but document your choice.

If you want, I can sketch how the `Customer` hierarchy and the turn controller should split this logic for your Stage 3 repo.

## **Q2**

For Stage 2, how should the satisfaction loss for a VIP customer be represented in a sequence diagram? Should it be represented after every turn satisfaction is lost?

## **A2**

No, you shouldn’t draw the loss after every turn. A sequence diagram shows one scenario with its control structure, so repeated behaviour is shown once, inside a loop fragment. The brief asks for “relevant guards and loops” and “one complete scenario,” not a turn-by-turn trace.

### **Recommended structure**

Put the loss inside the per-turn loop, and nest a per-customer loop inside it. The message name stays generic, so the polymorphism is visible:

```
loop [turn <= 18]
    ...steps 1-4...
    loop [for each customer in waiting/seated, not served]
        GameController -> Customer : applyWaitingLoss()
        Customer -> Customer : reduce satisfaction by lossRate (min 0)
        opt [satisfaction == 0]
            GameController -> Customer : abandon()
            GameController -> Table : free()
            GameController -> Order : cancel()
        end
    end
end
```

### **Showing the VIP specifically**

* **Lifeline:** use an object like `vip : VIPCustomer`. The controller’s call is still `applyWaitingLoss()`, and the self-message on the VIP lifeline can be labelled something like `satisfaction -= 5`.  
* **Polymorphism:** add a UML note on that call, such as “dispatched polymorphically: Regular 8, VIP 5, Critic 12”. This directly supports the brief’s rule that behaviour is dispatched through customer objects rather than type checks in the controller.  
* **Don’t expand every turn:** if you want to show the number changing, annotate one iteration (for example, “turn 2: 100 → 95”) instead of drawing each turn.

### **Keep the scenario focused**

The brief requires the scenario to include “one Stage 2 or 3 behaviour (e.g. VIP pricing or Happy Hour)”. The VIP’s 10% discount at payment is the more natural choice, because it sits on the main flow: arrival, order, READY, SERVED, eating, payment, result check. Satisfaction loss is background logic, so one compact loop fragment is enough. If you also want the 5-point loss visible, it fits on the same VIP lifeline without a separate diagram.

### **Also required by the brief**

Since the scenario must show a manager choice changing an outcome, you could have the VIP wait a couple of turns (loop iterations with loss), then have the Waiter prioritise them. The recorded satisfaction at payment (e.g. 90 instead of 100\) then feeds the average in the result check.

I can write this out as PlantUML if you’d like.

## **Q3**

Explain the boundary-control-entity separation for the different classes for the game.

## **A3**

## **Boundary-Control-Entity (BCE) for Restaurant Rush**

BCE splits classes by role. **Boundary** classes talk to the outside world, **entity** classes hold the domain state and its rules, and **control** classes coordinate the flow. The brief’s rule “the UI only collects the manager’s decisions and displays the state; game rules belong in domain and control objects” is exactly this separation.

The class names below are suggestions, so map them to whatever your repo uses.

### **Boundary: the CLI**

| Class | Responsibility |
| ----- | ----- |
| `GameCLI` / `ConsoleView` | Prints the start-of-turn display, end-of-turn results and the final Victory/Defeat |
| `InputHandler` | Reads and parses the manager’s choices, re-prompting on malformed input |

The boundary does not decide anything. It asks “which task for the Waiter?” and passes the answer to a controller. It should never apply satisfaction loss, compute a price, or check whether a table is free. Syntactic validation (is this a number in range?) can live here, but rule validation (“no free table”) belongs in the control or entity layer, which then returns a reason for the CLI to display.

### **Control: the turn and decision logic**

| Class | Responsibility |
| ----- | ----- |
| `GameController` | Runs the 18-turn loop and the six steps in order, and triggers the result check |
| `ArrivalScheduler` | Knows which turns have arrivals, and which customer type each is |
| `KitchenController` / `ServiceController` | Waiter-before-Chef sequencing, the READY-then-SERVED rule, and payment handling |
| `ResultEvaluator` | Checks the three targets (paid count, revenue, average satisfaction) |
| `HappyHourPolicy` (Stage 3\) | Once-per-game activation, two-turn duration, and the 15-point recovery |

Control classes orchestrate. They call methods on entities rather than reaching in to change fields. For example, the controller calls `customer.applyWaitingLoss()` instead of doing `customer.satisfaction -= 8`.

### **Entity: the domain objects**

| Class | Responsibility |
| ----- | ----- |
| `Customer` (abstract) with `RegularCustomer`, `VIPCustomer`, `CriticCustomer` | Own satisfaction, status, and their own loss rate, billing and special behaviour |
| `Order`, `MenuItem`, `ComboMeal` | Preparation progress, status (READY, SERVED, PAID, cancelled), and price |
| `Table` | Occupied or free, and the seated customer |
| `Waiter`, `Chef`, `Cashier`, `Host` | Staff capacity per turn (one task, one unit, one payment) |
| Enums such as `OrderStatus`, `CustomerStatus` | State vocabulary |

Entities enforce their own invariants. Satisfaction is clamped at 0, an order can’t be served unless it is READY, and a customer can’t pay before READY\_TO\_PAY. This is where your encapsulation examples for the class diagram come from.

### **Dependency rules**

```
Boundary  -->  Control  -->  Entity
```

* Boundary talks to control, and control talks to entities.  
* Entities never call control or boundary classes. A `Customer` must not print anything or know about the controller.  
* The boundary should not manipulate entities directly. It can read state for display, ideally through the controller or read-only getters.

### **How it plays out in a turn**

1. `GameController` asks `ArrivalScheduler` for this turn’s customer and adds them to the queue (control to entity).  
2. `GameController` asks `GameCLI` for the Waiter and Chef choices (control to boundary), then validates and applies them to the staff entities.  
3. For satisfaction loss, the controller loops over unserved customers and calls `applyWaitingLoss()`, so polymorphism is resolved inside the entity.  
4. `GameCLI` is given the updated state to display (control to boundary).

### **Where the Stage 2 and 3 features land**

* **VIP and Critic:** new entity subclasses only. The controller doesn’t change, which supports your OCP argument.  
* **Happy Hour:** a control-level policy that the pricing logic consults when an order is taken. It should not leak into the CLI.  
* **Host:** a new entity role plus one extra seating call in the controller’s step 2\.  
* **ComboMeal:** an entity, with the customer’s choice rule documented.

### **For your class diagram**

Label each class with a stereotype (`<<boundary>>`, `<<control>>`, `<<entity>>`) or colour-code them, and use the dependency arrows above. The brief asks for “principal entity classes, necessary control and boundary classes,” so this labelling answers that directly. For the sequence diagram, use the matching lifeline stereotypes, with the CLI on the left, the controller in the middle and entities on the right.

## **Q4**

What are some possible exceptions during the run of the game? How can they be prevented?

## **A4**

## **Possible exceptions in Restaurant Rush**

The brief’s key rule is in 3.4: **an invalid choice must explain why it failed and let the manager choose again, without using up the staff task.** So most of the “exceptions” here are expected rule violations, and they should be handled as validation, not as crashes.

### **1\. Input errors (boundary layer)**

| Problem | Prevention |
| ----- | ----- |
| Non-numeric input (`abc`) causing `NumberFormatException` or `InputMismatchException` | Read a whole line with `nextLine()`, parse in try/catch, and re-prompt |
| Number out of range (menu option 7 of 4\) | Check the range against the list size before using it |
| Empty line or EOF (`NoSuchElementException`) | Check `hasNextLine()`, and treat EOF as a clean exit |
| Leftover newline after `nextInt()` | Avoid `nextInt()` entirely and always use `nextLine()` plus parsing |

Put this in one `InputHandler` helper such as `readIntInRange(prompt, min, max)` so every prompt behaves the same.

### **2\. Rule violations (control and entity layers)**

These come from the manager’s choices and should return a reason rather than throwing past the game loop.

| Invalid choice | Prevention |
| ----- | ----- |
| Seat a customer with no free table | Check `hasFreeTable()` first, and show “No free table” |
| Seat when the queue is empty | Check queue size |
| Take an order from a customer who is not seated or already ordered | Validate the customer state before acting |
| Serve an order that is not READY, or was finished this turn | Check status, and record which turn it became READY |
| Chef works on an order that is already READY, cancelled or PAID | Filter the selectable orders to those still needing units |
| Activate Happy Hour twice, or in the wrong turn | Keep a `used` flag, and reject with a message |
| Host or Waiter exceeds the one-action limit | Track an `actedThisTurn` flag, and reject the second action |

The best approach is to **list only legal options** to the manager (for example, only READY orders under “serve”). Then most invalid choices can’t be made at all. Keep validation in the controller as a second line of defence.

### **3\. State and logic errors (entity layer)**

| Problem | Prevention |
| ----- | ----- |
| Satisfaction going below 0 | Clamp with `Math.max(0, ...)` in the entity |
| Satisfaction above 100 from Happy Hour | Clamp with `Math.min(100, ...)` |
| Paying before READY\_TO\_PAY | Guard in `Customer`/`Order`, throwing `IllegalStateException` for programmer errors |
| Abandoned customer still referenced by a table or order | Cancel the order and free the table in one method, so the state can’t be half-updated |
| Cashier handling a customer who has left | Remove abandoned customers from the payment list |
| Two staff acting on the same order inconsistently | Fix the order of operations (Waiter before Chef) in one place |

Use `IllegalStateException` or `IllegalArgumentException` for bugs that should never happen, and use return values or a custom checked exception (say `InvalidActionException` with a message) for expected invalid moves.

### **4\. Arithmetic and data errors**

| Problem | Prevention |
| ----- | ----- |
| Rounding errors in money (VIP 10%, Happy Hour 20%, Combo 90%) | Store cents as `int` or `long`, never `double`, as the brief says |
| Applying discounts in the wrong order | VIP first, then Happy Hour, as in the brief’s \$10.80 example |
| Division by zero when computing average satisfaction with no paid customers | Return N/A when the paid count is 0 |
| Null customer or order references | Avoid returning null, use `Optional`, or check before use |

### **5\. Environmental problems**

| Problem | Prevention |
| ----- | ----- |
| Crash on an unexpected error | Wrap the main loop in a top-level try/catch that prints a message and exits cleanly |
| Fixed output depending on random input | Use a seeded `Random`, so runs are reproducible for the TA’s victory and defeat runs |

### **Design suggestions**

* Have action methods return a result object, for example `ActionResult(success, message)`, so the CLI just prints the message and re-prompts.  
* Keep validation next to the rule it protects. For instance, `Table.seat()` should refuse an occupied table even if the controller already checked.  
* Don’t use exceptions for normal flow. A manager choosing something illegal is routine, so handle it with a check and a message.  
* Test the edge cases in your notes: all tables full, a customer abandoning with an order in progress, Happy Hour activated on turn 18, and the turn 16 customer who can’t finish.

I can sketch the `ActionResult` pattern or the `InputHandler` helper in Java if that would be useful.

