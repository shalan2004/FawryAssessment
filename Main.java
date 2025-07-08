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
    public int getStock() { return stock; }

    public void takeFromStock(int qty) {
        stock -= qty;
    }

    public abstract boolean isExpired();
    public abstract boolean needsShipping();
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
    public void addMore(int extra) { qty += extra; }
}

class Customer {
    private String name;
    private double balance;

    public Customer(String name, double balance) {
        this.name = name;
        this.balance = balance;
    }

    public String getName() { return name; }
    public double getBalance() { return balance; }

    public void pay(double amt) {
        balance -= amt;
    }
}

class Cart {
    private Map<String, CartItem> items = new HashMap<>();

    public void add(Product p, int qty) {
        if (p.getStock() < qty) throw new RuntimeException("Oops: stock too low");
        if (p.isExpired()) throw new RuntimeException("Sorry, product expired");

        String key = p.getName();
        if (items.containsKey(key)) {
            CartItem existing = items.get(key);
            int combined = existing.getQty() + qty;
            if (combined > p.getStock()) throw new RuntimeException("Trying to add too much");
            existing.addMore(qty);
        } else {
            items.put(key, new CartItem(p, qty));
        }
    }

    public boolean isEmpty() { return items.isEmpty(); }
    public Collection<CartItem> getAll() { return items.values(); }
    public void reset() { items.clear(); }

    public double subtotal() {
        double total = 0;
        for (CartItem item : items.values()) total += item.getTotal();
        return total;
    }

    public List<Shippable> getShippables() {
        List<Shippable> result = new ArrayList<>();
        for (CartItem item : items.values()) {
            if (item.getProduct().needsShipping()) {
                for (int i = 0; i < item.getQty(); i++) {
                    result.add((Shippable) item.getProduct());
                }
            }
        }
        return result;
    }
}

class ShippingService {
    public double handle(List<Shippable> items) {
        if (items.isEmpty()) return 0;

        double totalWeight = 0;
        System.out.println("\n==Items to ship==");
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, Double> weights = new HashMap<>();

        for (Shippable item : items) {
            counts.put(item.getName(), counts.getOrDefault(item.getName(), 0) + 1);
            weights.put(item.getName(), item.getWeight());
        }

        for (String name : counts.keySet()) {
            int qty = counts.get(name);
            double w = weights.get(name);
            System.out.println(qty + "x " + name + " " + (int)(w * 1000) + "g");
            totalWeight += qty * w;
        }

        System.out.println("Total weight to ship: " + totalWeight + "kg");
        return Math.ceil(totalWeight * 12);
    }
}

class FawryApp {
    private ShippingService shipper = new ShippingService();

    public void checkout(Customer user, Cart cart) {
        if (cart.isEmpty()) throw new RuntimeException("Nothing in cart");

        for (CartItem item : cart.getAll()) {
            Product p = item.getProduct();
            if (p.isExpired()) throw new RuntimeException(p.getName() + " is expired");
            if (p.getStock() < item.getQty()) throw new RuntimeException("Out of stock: " + p.getName());
        }

        double subtotal = cart.subtotal();
        List<Shippable> shipItems = cart.getShippables();
        double shipCost = shipper.handle(shipItems);
        double finalTotal = subtotal + shipCost;

        if (user.getBalance() < finalTotal) throw new RuntimeException("Can't pay that much");

        for (CartItem item : cart.getAll()) item.getProduct().takeFromStock(item.getQty());
        user.pay(finalTotal);

        System.out.println("\n==Receipt==");
        for (CartItem item : cart.getAll()) {
            System.out.println(item.getQty() + "x " + item.getProduct().getName() + " " + (int)item.getTotal());
        }
        System.out.println("Subtotal: " + (int)subtotal);
        System.out.println("Shipping: " + (int)shipCost);
        System.out.println("Total Paid: " + (int)finalTotal);
        System.out.println("Wallet Left: " + (int)user.getBalance());
        System.out.println("----\n");

        cart.reset();
    }
}

public class Main {
    public static void main(String[] args) {
        Product cheese = new PerishableProduct("Cheese", 100, 5, LocalDate.now().plusDays(3), 0.2);
        Product biscuits = new PerishableProduct("Biscuits", 150, 2, LocalDate.now().plusDays(5), 0.7);
        Product tv = new PhysicalProduct("TV", 5000, 3, 10);
        Product card = new DigitalProduct("Mobile Card", 50, 20);

        Customer abdul = new Customer("Abdulrahman Shalan", 10000);
        Cart cart = new Cart();
        FawryApp app = new FawryApp();

        try {
            cart.add(cheese, 2);
            cart.add(biscuits, 1);
            cart.add(card, 1);
            app.checkout(abdul, cart);
        } catch (Exception e) {
            System.out.println("Err: " + e.getMessage());
        }

        try {
            cart.add(tv, 3);
            app.checkout(abdul, cart);
        } catch (Exception e) {
            System.out.println("Err: " + e.getMessage());
            cart.reset();
        }

        try {
            cart.add(cheese, 20);
            app.checkout(abdul, cart);
        } catch (Exception e) {
            System.out.println("Err: " + e.getMessage());
            cart.reset();
        }

        try {
            app.checkout(abdul, cart);
        } catch (Exception e) {
            System.out.println("Err: " + e.getMessage());
        }

        try {
            Product expired = new PerishableProduct("Old Yogurt", 30, 1, LocalDate.now().minusDays(2), 0.2);
            cart.add(expired, 1);
            app.checkout(abdul, cart);
        } catch (Exception e) {
            System.out.println("Err: " + e.getMessage());
        }
    }
}
