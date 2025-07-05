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
        if (amount > stock) throw new RuntimeException("Not enough stock!");
        stock -= amount;
    }

    public abstract boolean isExpired();
    public abstract boolean needsShipping();

    @Override
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

    public boolean isExpired() { return LocalDate.now().isAfter(expiry); }
    public boolean needsShipping() { return true; }
    public double getWeight() { return weight; }
    public LocalDate getExpiryDate() { return expiry; }
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
    private Product product;
    private int quantity;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public double getTotalPrice() { return product.getPrice() * quantity; }
    public void increaseQuantity(int amount) { quantity += amount; }
}

class Customer {
    private String name;
    private double balance;

    public Customer(String name, double balance) {
        this.name = name;
        this.balance = balance;
    }

    public String getName() { return name; }
    public double getWalletBalance() { return balance; }

    public void deductFromWallet(double amount) {
        if (amount > balance) throw new RuntimeException("Insufficient balance");
        balance -= amount;
    }
}

class ShoppingCart {
    private Map<String, CartItem> items = new HashMap<>();

    public void addItem(Product product, int quantity) {
        if (product.getStockQuantity() < quantity) throw new RuntimeException("Not enough stock");
        if (product.isExpired()) throw new RuntimeException("Product expired");

        String name = product.getName();
        if (items.containsKey(name)) {
            CartItem item = items.get(name);
            int totalQty = item.getQuantity() + quantity;
            if (totalQty > product.getStockQuantity()) throw new RuntimeException("Exceeds stock");
            item.increaseQuantity(quantity);
        } else {
            items.put(name, new CartItem(product, quantity));
        }
    }

    public boolean isEmpty() { return items.isEmpty(); }
    public Collection<CartItem> getAllItems() { return items.values(); }
    public void clearCart() { items.clear(); }

    public double calculateSubtotal() {
        return items.values().stream().mapToDouble(CartItem::getTotalPrice).sum();
    }

    public List<Shippable> getItemsForShipping() {
        List<Shippable> shippables = new ArrayList<>();
        for (CartItem item : items.values()) {
            if (item.getProduct().needsShipping()) {
                Shippable s = (Shippable) item.getProduct();
                for (int i = 0; i < item.getQuantity(); i++) shippables.add(s);
            }
        }
        return shippables;
    }
}

class ShippingService {
    private static final double COST_PER_KG = 12.0;

    public double calculateShippingCost(List<Shippable> items) {
        double totalWeight = items.stream().mapToDouble(Shippable::getWeight).sum();
        return Math.ceil(totalWeight * COST_PER_KG);
    }

    public void processShipment(List<Shippable> items) {
        if (items.isEmpty()) return;
        System.out.println("SHIPPING DETAILS\n==================");
        Map<String, Integer> counts = new HashMap<>();
        Map<String, Double> weights = new HashMap<>();

        for (Shippable item : items) {
            counts.put(item.getName(), counts.getOrDefault(item.getName(), 0) + 1);
            weights.put(item.getName(), item.getWeight());
        }

        double totalWeight = 0;
        for (String name : counts.keySet()) {
            int qty = counts.get(name);
            double w = weights.get(name) * qty;
            totalWeight += w;
            System.out.println(qty + "x " + name + " (" + (w * 1000) + "g)");
        }
        System.out.println("Total package weight: " + totalWeight + "kg\n");
    }
}

class FawryECommerceSystem {
    private ShippingService shippingService = new ShippingService();

    public void processCheckout(Customer customer, ShoppingCart cart) {
        if (cart.isEmpty()) throw new RuntimeException("Empty cart");

        for (CartItem item : cart.getAllItems()) {
            Product p = item.getProduct();
            if (p.isExpired()) throw new RuntimeException(p.getName() + " is expired");
            if (p.getStockQuantity() < item.getQuantity()) throw new RuntimeException("Out of stock: " + p.getName());
        }

        double subtotal = cart.calculateSubtotal();
        List<Shippable> shipItems = cart.getItemsForShipping();
        double shippingCost = shippingService.calculateShippingCost(shipItems);
        double total = subtotal + shippingCost;

        if (customer.getWalletBalance() < total) throw new RuntimeException("Insufficient balance");

        if (!shipItems.isEmpty()) shippingService.processShipment(shipItems);
        for (CartItem item : cart.getAllItems()) item.getProduct().reduceStock(item.getQuantity());
        customer.deductFromWallet(total);

        printReceipt(customer, cart, subtotal, shippingCost, total);
        cart.clearCart();
    }

    private void printReceipt(Customer customer, ShoppingCart cart, double subtotal, double shipping, double total) {
        System.out.println("FAWRY RECEIPT\n================");
        System.out.println("Customer: " + customer.getName() + "\n");
        for (CartItem item : cart.getAllItems()) {
            System.out.println(item.getQuantity() + "x " + item.getProduct().getName() + " - EGP " + item.getTotalPrice());
        }
        System.out.println("----------------\nSubtotal: EGP " + subtotal);
        if (shipping > 0) System.out.println("Shipping: EGP " + shipping);
        System.out.println("TOTAL: EGP " + total);
        System.out.println("Remaining balance: EGP " + customer.getWalletBalance());
        System.out.println("================\n");
    }
}

public class Main {
    public static void main(String[] args) {
        Product cheese = new PerishableProduct("Egyptian Cheese", 85, 12, LocalDate.now().plusDays(10), 0.25);
        Product cookies = new PerishableProduct("Chocolate Cookies", 45, 8, LocalDate.now().plusDays(20), 0.3);
        Product tv = new PhysicalProduct("Samsung TV", 8500, 5, 12.5);
        Product iphone = new PhysicalProduct("iPhone 15", 25000, 10, 0.8);
        Product credit = new DigitalProduct("Orange Credit", 100, 50);

        Customer customer = new Customer("Abdulrahman Shalan", 30000);
        ShoppingCart cart = new ShoppingCart();
        FawryECommerceSystem system = new FawryECommerceSystem();

        List<String> errors = new ArrayList<>();

        try {
            cart.addItem(cheese, 3);
            cart.addItem(cookies, 2);
            cart.addItem(credit, 1);
            system.processCheckout(customer, cart);
        } catch (Exception e) {
            errors.add("Error: " + e.getMessage());
        }

        try {
            cart.addItem(tv, 4);
            system.processCheckout(customer, cart);
        } catch (Exception e) {
            errors.add("Error: " + e.getMessage());
            cart.clearCart();
        }

        try {
            cart.addItem(cheese, 20);
            system.processCheckout(customer, cart);
        } catch (Exception e) {
            errors.add("Error: " + e.getMessage());
            cart.clearCart();
        }

        try {
            system.processCheckout(customer, cart);
        } catch (Exception e) {
            errors.add("Error: " + e.getMessage());
        }

        try {
            Product expiredMilk = new PerishableProduct("Expired Milk", 30, 5, LocalDate.now().minusDays(3), 0.5);
            cart.addItem(expiredMilk, 1);
            system.processCheckout(customer, cart);
        } catch (Exception e) {
            errors.add("Error: " + e.getMessage());
        }

        errors.forEach(System.out::println);
    }
}