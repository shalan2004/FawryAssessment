import java.time.LocalDate;
import java.util.*;

abstract class Product {
    private String name;
    private double price;
    private int stock;

    public Product(String name, double price, int stock) {
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getStockQuantity() { return stock; }

    public void reduceStock(int amount) {
        stock -= amount;
    }

    public abstract boolean isExpired();
    public abstract boolean needsShipping();

    public String toString() {
        return name + " - EGP " + price + " (Stock: " + stock + ")";
    }
}

interface Shippable {
    String getName();
    double getWeight();
}

class PerishableProduct extends Product implements Shippable {
    private LocalDate expiry;
    private double weight;

    public PerishableProduct(String name, double price, int stock, LocalDate expiry, double weight) {
        super(name, price, stock);
        this.expiry = expiry;
        this.weight = weight;
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(expiry);
    }

    public boolean needsShipping() { return true; }
    public double getWeight() { return weight; }
}

class PhysicalProduct extends Product implements Shippable {
    private double weight;

    public PhysicalProduct(String name, double price, int stock, double weight) {
        super(name, price, stock);
        this.weight = weight;
    }

    public boolean isExpired() { return false; }
    public boolean needsShipping() { return true; }
    public double getWeight() { return weight; }
}

class DigitalProduct extends Product {
    public DigitalProduct(String name, double price, int stock) {
        super(name, price, stock);
    }
    public boolean isExpired() { return false; }
    public boolean needsShipping() { return false; }
}

class CartItem {
    private Product p;
    private int qty;

    public CartItem(Product p, int qty) {
        this.p = p;
        this.qty = qty;
    }

    public Product getProduct() { return p; }
    public int getQty() { return qty; }
    public double getTotal() { return p.getPrice() * qty; }
    public void addQty(int more) { qty += more; }
}

class Customer {
    private String fullName;
    private double wallet;

    public Customer(String fullName, double wallet) {
        this.fullName = fullName;
        this.wallet = wallet;
    }

    public String getName() { return fullName; }
    public double getBalance() { return wallet; }
    public void pay(double amount) {
        if (amount > wallet) throw new RuntimeException("Not enough cash");
        wallet -= amount;
    }
}

class Cart {
    private Map<String, CartItem> items = new HashMap<>();

    public void add(Product p, int q) {
        if (p.getStockQuantity() < q) throw new RuntimeException("Stock too low");
        if (p.isExpired()) throw new RuntimeException("Oops! Product expired");

        String key = p.getName();
        if (items.containsKey(key)) {
            CartItem ci = items.get(key);
            if (ci.getQty() + q > p.getStockQuantity()) throw new RuntimeException("Too much of that product");
            ci.addQty(q);
        } else {
            items.put(key, new CartItem(p, q));
        }
    }

    public boolean empty() { return items.isEmpty(); }
    public Collection<CartItem> all() { return items.values(); }
    public void clear() { items.clear(); }

    public double totalBeforeShipping() {
        return items.values().stream().mapToDouble(CartItem::getTotal).sum();
    }

    public List<Shippable> getShippables() {
        List<Shippable> list = new ArrayList<>();
        for (CartItem ci : items.values()) {
            if (ci.getProduct().needsShipping()) {
                Shippable s = (Shippable) ci.getProduct();
                for (int i = 0; i < ci.getQty(); i++) list.add(s);
            }
        }
        return list;
    }
}

class ShippingService {
    double ship(List<Shippable> items) {
        double totalWeight = 0;
        Map<String, Integer> count = new LinkedHashMap<>();
        Map<String, Double> unitWeight = new HashMap<>();

        for (Shippable s : items) {
            count.put(s.getName(), count.getOrDefault(s.getName(), 0) + 1);
            unitWeight.put(s.getName(), s.getWeight());
        }

        System.out.println("==Shipment notice==");
        for (String name : count.keySet()) {
            int qty = count.get(name);
            double weight = unitWeight.get(name);
            System.out.println(qty + "x " + name + " " + (int)(weight * 1000) + "g");
            totalWeight += qty * weight;
        }
        System.out.println("Total package weight " + totalWeight + "kg\n");
        return Math.ceil(totalWeight * 12);
    }
}

class StoreSystem {
    ShippingService shipper = new ShippingService();

    void checkout(Customer c, Cart cart) {
        if (cart.empty()) throw new RuntimeException("Cart's empty");

        for (CartItem i : cart.all()) {
            Product p = i.getProduct();
            if (p.isExpired()) throw new RuntimeException(p.getName() + " expired");
            if (p.getStockQuantity() < i.getQty()) throw new RuntimeException("No enough: " + p.getName());
        }

        double subtotal = cart.totalBeforeShipping();
        List<Shippable> toShip = cart.getShippables();
        double shipping = toShip.isEmpty() ? 0 : shipper.ship(toShip);
        double total = subtotal + shipping;

        if (c.getBalance() < total) throw new RuntimeException("Not enough balance");

        // reduce stock
        for (CartItem i : cart.all()) i.getProduct().reduceStock(i.getQty());
        c.pay(total);

        // print receipt
        System.out.println("==Checkout receipt==");
        for (CartItem i : cart.all()) {
            System.out.println(i.getQty() + "x " + i.getProduct().getName() + " " + (int) i.getTotal());
        }
        System.out.println("----------------------");
        System.out.println("Subtotal " + (int)subtotal);
        System.out.println("Shipping " + (int)shipping);
        System.out.println("Amount " + (int)total);
        System.out.println("Left in wallet: " + (int)c.getBalance());
        System.out.println("END.\n");

        cart.clear();
    }
}

public class Main {
    public static void main(String[] args) {
        Product cheese = new PerishableProduct("Cheese", 100, 5, LocalDate.now().plusDays(3), 0.2);
        Product biscuits = new PerishableProduct("Biscuits", 150, 2, LocalDate.now().plusDays(5), 0.7);
        Product tv = new PhysicalProduct("TV", 5000, 3, 10);
        Product mobileCard = new DigitalProduct("Mobile Card", 50, 20);

        Customer user = new Customer("Abdulrahman Shalan", 10000);
        Cart cart = new Cart();
        StoreSystem store = new StoreSystem();

        try {
            cart.add(cheese, 2);
            cart.add(biscuits, 1);
            cart.add(mobileCard, 1);
            store.checkout(user, cart);
        } catch (Exception e) {
            System.out.println("Oops: " + e.getMessage());
        }

        try {
            cart.add(tv, 3);
            store.checkout(user, cart);
        } catch (Exception e) {
            System.out.println("Oops: " + e.getMessage());
            cart.clear();
        }

        try {
            cart.add(cheese, 20);
            store.checkout(user, cart);
        } catch (Exception e) {
            System.out.println("Oops: " + e.getMessage());
            cart.clear();
        }

        try {
            store.checkout(user, cart);
        } catch (Exception e) {
            System.out.println("Oops: " + e.getMessage());
        }

        try {
            Product expired = new PerishableProduct("Old Yogurt", 30, 1, LocalDate.now().minusDays(2), 0.2);
            cart.add(expired, 1);
            store.checkout(user, cart);
        } catch (Exception e) {
            System.out.println("Oops: " + e.getMessage());
        }
    }
}
