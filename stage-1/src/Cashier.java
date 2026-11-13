public class Cashier {

    public String collectPayment(Restaurant restaurant) {
        for (Customer customer : restaurant.getCustomers()) { 
            if (customer.getState() == CustomerState.READY_TO_PAY) {
                int bill = customer.getOrder().getPriceCents();
                restaurant.addRevenue(bill);
                customer.pay();
                return customer + " pays " + Money.format(bill) + " (satisfaction " + customer.getSatisfaction() + "). Table freed.";
            }
        }
        return null;
    }
}
