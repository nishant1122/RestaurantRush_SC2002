public class Main {
    public static void main(String[] args) {
        Menu menu = new Menu();
        menu.addItem(new MenuItem("Salad", 900, 1));
        menu.addItem(new MenuItem("Burger", 1500, 1));
        menu.addItem(new MenuItem("Pasta", 1800, 2));

        System.out.println("=== Restaurant Rush: Menu ===");
        for (MenuItem item : menu.getItems()){
            System.out.println(" "+item);
        }
    }
}