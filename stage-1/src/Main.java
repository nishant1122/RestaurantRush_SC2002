public class Main {
    public static void main(String[] args) {
        Menu menu = new Menu();
        menu.addItem(new MenuItem("Salad", 900, 1));
        menu.addItem(new MenuItem("Burger", 1500, 1));
        menu.addItem(new MenuItem("Pasta", 1800, 2));
        Restaurant restaurant = new Restaurant(menu, 2);
        ArrivalSchedule schedule = new ArrivalSchedule();
        Waiter waiter = new Waiter();
        Chef chef = new Chef();
        Cashier cashier = new Cashier();

        for (int turn = 1; turn <= 6; turn++) {
            System.out.println("--- Turn " + turn + " ---");
            Customer arriving = schedule.customerArrivingOn(turn);
            if (arriving != null) restaurant.addCustomer(arriving);
            switch (turn) {
                case 1 -> System.out.println(waiter.seat(restaurant, 1));
                case 2 -> System.out.println(waiter.takeOrder(restaurant, 1));
                case 3 -> System.out.println(chef.prepare(restaurant, 1));
                case 4 -> System.out.println(waiter.serve(restaurant, 1, turn));
                case 5 -> restaurant.findCustomer(1).finishEating();
                case 6 -> System.out.println(cashier.collectPayment(restaurant));
            }
            for (Customer c : restaurant.getCustomers())
                System.out.println("  " + c + " " + c.getState() + ", satisfaction " + c.getSatisfaction());
        }
        System.out.println("Revenue: " + Money.format(restaurant.getRevenueCents()));
    }
}