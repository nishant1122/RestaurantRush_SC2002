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