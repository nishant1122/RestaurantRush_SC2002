public class ArrivalSchedule {

    public Customer customerArrivingOn(int turn) {
        if (turn > 16 || turn % 3 != 1) {
            return null;
        }
        int id = turn / 3 + 1; 
        return new Customer(id);
    }
}
