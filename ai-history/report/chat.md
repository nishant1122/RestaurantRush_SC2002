Tool: Claude (Sonnet 5.5)

---

## Q1

What are some key design principles used in this project, and where do they show up in the code?

## A1

The report shows five main principles at work, plus one deliberate gap.

**1. Control / boundary / entity separation**
- ConsoleUI (boundary) only reads and prints, with no game rules.
- GameController (control) only sequences the six turn steps.
- Restaurant, Customer, Order, Waiter and the other entity classes hold the rules about their own state.

**2. Single Responsibility Principle (SRP)**
- Order owns its status changes, Money owns formatting and rounding, and Menu owns the item list.
- A change to the screen layout doesn't touch any game rule.

**3. Open-Closed Principle (OCP)**
- Stage 2 added VIPCustomer and CriticCustomer by extending Customer.
- GameController, Waiter, Chef, Cashier and ConsoleUI were not changed.
- Stage 3's ComboMeal likewise extends MenuItem and only needs registration in Main.

**4. Liskov Substitution Principle (LSP)**
- VIP and Critic customers override only the satisfaction loss, the bill and the food choice.
- They keep the same service flow, so they can replace Customer without breaking the turn sequence.

**5. Encapsulation, polymorphism and composition**
- Order status changes only through prepareOneUnit(), markServed(), markPaid() and cancel().
- The controller calls loseSatisfaction(), and the object decides the behaviour, so there are no instanceof checks.
- Restaurant owns its Menu, 2 Tables and HappyHour, and a ComboMeal is made of two MenuItems.

---

## Q2

How is the game structured, how can I organise it in my report?

## A2

The game is a turn-based Java command-line restaurant simulation. You play the manager, and the challenge is that staff time is limited, so you decide who gets served first.

### How the game works

**Base setting (fixed so a TA can reproduce runs)**
- 18 turns, 2 tables, and one Waiter, one Chef and one Cashier, each limited to one action per turn.
- Six customers arrive, at the start of turns 1, 4, 7, 10, 13 and 16.
- Menu: Salad ($9, 1 prep unit), Burger ($15, 1 unit), Pasta ($18, 2 units).
- Win condition: at least 4 paid customers, at least $45 revenue, and average satisfaction of paid customers at least 60.

**Each turn runs six steps in order**
1. Arrival: the scheduled customer joins the queue at satisfaction 100.
2. Manager choices: the Waiter seats, takes an order or serves, and the Chef advances one order.
3. Kitchen and serving: the Waiter acts before the Chef, and a dish that finishes this turn can only be served next turn.
4. Eating and payment: eating takes a full turn, and the Cashier handles one customer per turn, oldest first.
5. Waiting and abandonment: unserved customers lose satisfaction, and anyone reaching 0 leaves and frees the table.
6. Result: after turn 18 the game is Victory or Defeat. There is no early ending.

**Three stages, each a tagged runnable commit**

| Stage | Adds |
|---|---|
| 1 | Regular customers, menu, Waiter/Chef/Cashier, full turn loop, both endings |
| 2 | VIP (loses 5 per turn, 10% off) and Critic (loses 12 per turn, plus a behaviour you define) |
| 3 | Host (seats one extra customer per turn), ComboMeal (two items, 90% price), Happy Hour (once per game, 2 turns, 20% off, +15 satisfaction) |

Two design constraints drive the marking:
- Customer-specific behaviour must be dispatched through the customer objects, not through type checks in the turn controller.
- The UI only collects decisions and displays state. All rules live in domain and control classes.

### A class structure to aim for

This is a suggested shape, so check it against your actual code, because the diagrams must match it.
- **Boundary:** the CLI class.
- **Control:** a game or turn controller that runs the six steps.
- **Entities:** a Customer hierarchy (Regular, VIP, Critic), Order, Table, MenuItem (and Combo), and the staff roles.
- **Enums:** order status (e.g. READY, SERVED, PAID) and customer state.

### How to organise the report

1. **Introduction and game overview:** setting, rules, objective, and the three stages.
2. **Design and evolution:** one section per stage, each with its class diagram. For each transition (1→2, 2→3) include:
   - what you added (new classes and their roles);
   - what you modified, with reasons;
   - what still works, with checks that earlier behaviour is intact;
   - the effect on the design, since line count alone is insufficient.
3. **Class diagram discussion:** attributes, visibility, multiplicities, and annotated examples of encapsulation, polymorphism and composition (25 marks).
4. **Sequence diagram:** one scenario where a manager choice changes the outcome. Cover arrival, order, READY then SERVED, eating, payment and the result check, plus one Stage 2 or 3 behaviour such as VIP pricing or Happy Hour. State the stage, starting conditions and outcome, with guards and loops (20 marks).
5. **Design discussion:**
   - a table of key abstractions and responsibilities;
   - SOLID principles only where your code genuinely reflects them;
   - trade-offs and one alternative design you considered;
   - how player choices change results (15 marks).
6. **Functional evidence:** one reproducible victory run and one defeat run with test notes, plus your Critic behaviour and combo-choice rules documented.
7. **Individual reflections:** one section per member under their own name, covering what you learned, how you applied course concepts, what challenged you, and how you solved it, with examples from your own work.
8. **Appendices:** GitHub links, AI history links, and declarations.

The README should link to each stage tag, each transition's compare link, every member's `ai-history/` folder, and the video timestamps. Also remember each member's AI chats go on their own branch, in order and not summarised.

---

## Q3

What are some patterns and key features in the design?

## A3

### Patterns used

- **Simple factory:** `ArrivalSchedule.customerArrivingOn(turn)` decides which `Customer` subtype to create (Regular, VIP or Critic), so `GameController` never names a concrete customer class.
- **Composite-like composition:** `ComboMeal` is a `MenuItem` built from two `MenuItem`s (price 90% of the sum, preparation units added). It is not a full Composite pattern, because a combo has exactly two components and no nested tree.
- **Polymorphism over type checks:** `VIPCustomer` and `CriticCustomer` override `getSatisfactionLoss()`, `calculateBill()` and `chooseItem()`. The controller and staff call these on `Customer`, so there are no `instanceof` checks.
- **Enums for state:** `CustomerState` and `OrderStatus` define the life cycles. Order status changes only through `prepareOneUnit()`, `markServed()`, `markPaid()` and `cancel()`.

### Patterns considered and not used

- **`PricingPolicy` interface:** the design uses a plain `HappyHour` class instead, because there is only one such policy. The cost is that `Waiter.takeOrder()` had to change in Stage 3.
- **Customer rules inside `GameController`:** rejected because every new customer type would edit the controller and break the Open-Closed Principle.

### Key features of the design

- **Control / boundary / entity split:** `ConsoleUI` handles all input and output, `GameController` runs the turn sequence, and the entity classes hold the game rules.
- **Turn sequence:** arrivals, manager choices, kitchen and serving, eating and payment, waiting loss, then the result. The Waiter acts before the Chef, and each state change takes effect one turn later.
- **Capacity as the main constraint:** the Waiter does one task and the Chef advances one order per turn, so the player's choices decide the outcome.
- **Staged extension:** Stage 2 added customer types with no change to the controller, staff or UI. Stage 3 added `Host`, `ComboMeal` and `HappyHour`, which are new steps in the turn.
- **Money as integer cents:** `Money` formats and rounds, which avoids floating-point errors. The price is locked in the `Order` when it is taken.
- **Error handling:** an invalid choice prints the reason, and the manager chooses again without using up the staff task.
- **Reproducible runs:** each stage is a full copy in its own folder with `tests/victory.txt` and `tests/defeat.txt`, so it can be built and run on its own.

---

## Q4

What is the key difference from stage 2 to stage 3?

## A4

The key difference is what kind of change each stage makes to the design.

Stage 2 adds new kinds of customer, with no change to the flow. VIP and Critic follow the same order and service flow as Regular customers. They differ only in satisfaction loss (5, 12, 8), the bill (VIP 10% off), and the Critic's special behaviour. The point is polymorphism: the differences must be dispatched through the customer objects, not through type checks in the turn controller. If your design is good, this is mostly adding subclasses.

Stage 3 changes the rules of the game itself, along three separate axes.

- A new role (Host): a second seating action per turn, in addition to the Waiter's, so the staff and turn-step logic must handle it.
- A new menu concept (ComboMeal): a composite of two existing items, where prep units are the sum of its parts and price is 90% of the combined price, rounded to the cent. This tests composition and your integer-cents handling.
- A new time-based pricing policy (Happy Hour): it can be switched on once per game and lasts for 2 turns. It gives 20% off bills and restores 15 satisfaction at turn start. It also has to interact with the VIP discount (VIP first, then Happy Hour), and the price is locked when the order is taken.

In short: Stage 2 varies existing behaviour by adding subclasses, while Stage 3 introduces new responsibilities and policies that cut across the turn loop, pricing, and state over time. Stage 3 is therefore where you are most likely to modify earlier classes, such as the turn controller (Host step, turn-start recovery) and order pricing. Each modification needs a reason in your transition evidence.

For the report's 2→3 section, I'd highlight the Happy Hour and VIP pricing interaction as your strongest example of design impact, since it shows whether pricing is cleanly encapsulated or scattered across classes.
