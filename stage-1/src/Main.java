public class Main {
    public static void main(String[] args) {
        Menu menu = new Menu();
        menu.addItem(new MenuItem("Salad", 900, 1));
        menu.addItem(new MenuItem("Burger", 1500, 1));
        menu.addItem(new MenuItem("Pasta", 1800, 2));
        Restaurant restaurant = new Restaurant(menu, 2);

        Customer c1 = new Customer(1);
        restaurant.addCustomer(c1);
        System.out.println(c1 + " arrives: " + c1.getState());
        System.out.println(restaurant.seatCustomer(1));

        MenuItem dish = c1.chooseItem(menu);
        c1.placeOrder(new Order(dish, c1.calculateBill(dish.getPriceCents())));
        System.out.println(c1 + " ordered " + c1.getOrder());

        while (c1.getOrder().getStatus() == OrderStatus.PREPARING) {
            c1.getOrder().prepareOneUnit();
            System.out.println("  cooking... " + c1.getOrder());
        }
        c1.receiveFood(1);
        c1.finishEating();
        restaurant.addRevenue(c1.getOrder().getPriceCents());
        c1.pay();
        System.out.println(c1 + " is " + c1.getState() + ", revenue " + Money.format(restaurant.getRevenueCents()));
    }
}