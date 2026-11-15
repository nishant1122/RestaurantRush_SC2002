public class Money{
    public static String format(int cents){
        return String.format("$%d.%02d", cents/100, cents%100);
    }

    public static int percent(int cents, int percent){
        return (cents * percent + 50) / 100;
    }
}