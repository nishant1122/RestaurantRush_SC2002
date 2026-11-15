public class HappyHour {
    public static final int RECOVERY = 15;

    private boolean used = false;
    private int turnsLeft = 0;

    public boolean canActivate(){
        return !used;
    }

    public void activate(){
        used = true;
        turnsLeft = 2; 
    }

    public boolean isActive(){
        return turnsLeft > 0;
    }
    public int getTurnsLeft(){
        return turnsLeft;
    }

   
    public void endTurn() {
        if (turnsLeft > 0) {
            turnsLeft--;
        }
    }

    public int applyDiscount(int priceCents) {
        return isActive() ? Money.percent(priceCents, 80) : priceCents;
    }
}