public class Table {
    private final int number;
    private Customer customer; 

    public Table(int number) {
        this.number = number;
    }

    public boolean isFree(){
        return customer == null;
    
    }
    public void seat(Customer customer){
        this.customer = customer;
    }

    public void free(){
        this.customer = null;
    }

    public int getNumber() {
        return number;
    }
    public Customer getCustomer() {
        return customer;
    }
}
