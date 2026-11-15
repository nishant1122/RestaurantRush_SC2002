import java.util.ArrayList;
import java.util.List;

public class Restaurant {
    private final Menu menu;
    private final List<Table> tables = new ArrayList<>();
    private final List<Customer> customers = new ArrayList<>(); // everyone who arrived, in arrival order
    private int revenueCents = 0;

    public Restaurant(Menu menu, int tableCount) {
        this.menu = menu;
        for (int i = 1; i <= tableCount; i++) {
            tables.add(new Table(i));
        }
    }

    public void addCustomer(Customer customer) {
        customers.add(customer);
    }

    public String seatCustomer(int customerId) {
        Customer customer = findCustomer(customerId);
        if (customer == null || customer.getState() != CustomerState.WAITING) {
            throw new IllegalArgumentException("Customer " + customerId + " is not in the waiting queue.");
        }
        Table table = findFreeTable();
        if (table == null) {
            throw new IllegalArgumentException("No free table. A table is freed only when its customer pays or leaves.");
        }
        customer.sitAt(table);
        return customer + " is seated at Table " + table.getNumber() + ".";
    }

    public Customer findCustomer(int id) {
        for (Customer c : customers) {
            if (c.getId() == id) {
                return c;
            }
        }
        return null;
    }

    public Table findFreeTable() {
        for (Table t : tables) {
            if (t.isFree()) {
                return t;
            }
        }
        return null;
    }

    public void addRevenue(int cents) { revenueCents += cents; }



    public List<Customer> getCustomersInState(CustomerState state) {
        List<Customer> result = new ArrayList<>();
        for (Customer c : customers) {
            if (c.getState() == state) {
                result.add(c);
            }
        }
        return result;
    }

    public List<Customer> getWaitingQueue() { return getCustomersInState(CustomerState.WAITING); }

    public List<Customer> getCustomersWithOrder(OrderStatus status) {
        List<Customer> result = new ArrayList<>();
        for (Customer c : customers) {
            if (c.getOrder() != null && c.getOrder().getStatus() == status) {
                result.add(c);
            }
        }
        return result;
    }


    public int getPaidCount() { return getCustomersInState(CustomerState.PAID).size(); }
    public int getLeftCount() { return getCustomersInState(CustomerState.LEFT).size(); }

    public int getServedCount() {
        return getCustomersInState(CustomerState.EATING).size()
                + getCustomersInState(CustomerState.READY_TO_PAY).size()
                + getPaidCount();
    }

    public double getAverageSatisfaction() {
        List<Customer> paid = getCustomersInState(CustomerState.PAID);
        if (paid.isEmpty()) {
            return -1;
        }
        int total = 0;
        for (Customer c : paid) {
            total += c.getSatisfaction();
        }
        return (double) total / paid.size();
    }

    public Menu getMenu() { return menu; }
    public List<Table> getTables() { return tables; }
    public List<Customer> getCustomers() { return customers; }
    public int getRevenueCents() { return revenueCents; }
}
