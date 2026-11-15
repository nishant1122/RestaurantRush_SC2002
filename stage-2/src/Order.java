public class Order {
    private final MenuItem item;
    private final int priceCents;
    private int unitsDone = 0;
    private OrderStatus status = OrderStatus.PREPARING;

    public Order(MenuItem item, int priceCents) {
        this.item = item;
        this.priceCents = priceCents;
    }
    public void prepareOneUnit() {
        unitsDone++;
        if (unitsDone >= item.getPrepUnits()) {
            status = OrderStatus.READY;
        }
    }
    public void markServed() { status = OrderStatus.SERVED; }
    public void markPaid() { status = OrderStatus.PAID; }
    public void cancel() { status = OrderStatus.CANCELLED; }
    public MenuItem getItem() { return item; }
    public int getPriceCents() { return priceCents; }
    public OrderStatus getStatus() { return status; }

    @Override
    public String toString() {
        String text = item.getName() + " " + Money.format(priceCents) + " [" + status;
        if (status == OrderStatus.PREPARING) {
            text += " " + unitsDone + "/" + item.getPrepUnits();
        }
        return text + "]";
    }
}
