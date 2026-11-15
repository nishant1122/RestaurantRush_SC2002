public class CriticCustomer extends Customer {

    public CriticCustomer(int id) {
        super(id);
    }

    @Override
    public String getType() {
        return "Critic";
    }

    @Override
    public int getSatisfactionLoss() {
        return 12;
    }

    @Override
    public MenuItem chooseItem(Menu menu) {
        return menu.getMostExpensive();
    }
}