package org.example;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.AggregateIterable;

import org.bson.Document;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class OrderCollection {

    public static MongoDatabase connectToDB() {
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = mongoClient.getDatabase("eShop");
        return database;
    }

    private MongoCollection<Document> orderCollection;

    public OrderCollection(MongoDatabase database) {
        this.orderCollection = database.getCollection("OrderCollection");
    }

    public void insertOrder(Document order) {
        orderCollection.insertOne(order);
    }

    public void updateDeliveryAddress(int orderId, String newAddress) {
        orderCollection.updateOne(Filters.eq("orderid", orderId), Updates.set("delivery_address", newAddress));
    }

    public void deleteOrder(int orderId) {
        orderCollection.deleteOne(Filters.eq("orderid", orderId));
    }

    public void displayOrders() {
        MongoCursor<Document> cursor = orderCollection.find().iterator();

        while (cursor.hasNext()) {
            Document order = cursor.next();
            int orderId = order.getInteger("orderid");
            List<Document> products = (List<Document>) order.get("products");

            for (Document product : products) {
                String productName = product.getString("product_name");
                Number priceNumber = product.get("price", Number.class);
                double price = priceNumber.doubleValue(); // Convert to double
                int quantity = product.getInteger("quantity");
                double total = price * quantity;

                System.out.printf("%d\t%s\t%.2f\t%d\t%.2f\n", orderId, productName, price, quantity, total);
            }
        }
    }


    public void calculateTotalAmount() {
        AggregateIterable<Document> result = orderCollection.aggregate(
                Arrays.asList(
                        Aggregates.group(null, Accumulators.sum("total", "$total_amount"))
                )
        );

        Document total = result.first();
        if (total != null) {
            Object totalAmount = total.get("total");
            double totalValue;
            if (totalAmount instanceof Number) {
                totalValue = ((Number) totalAmount).doubleValue();
            } else {
                System.out.println("Unexpected data type for total amount.");
                return;
            }
            System.out.println("Total amount: " + totalValue);
        }
    }


    public void countProductById(String productId) {
        AggregateIterable<Document> result = orderCollection.aggregate(
                Arrays.asList(
                        Aggregates.unwind("$products"),
                        Aggregates.match(Filters.eq("products.product_id", productId)),
                        Aggregates.group("$products.product_id", Accumulators.sum("totalQuantity", "$products.quantity"))
                )
        );

        Document total = result.first();
        if (total != null) {
            System.out.println("Total quantity for product_id '" + productId + "': " + total.getInteger("totalQuantity"));
        }
    }


    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        MongoDatabase database = connectToDB();
        OrderCollection orderService = new OrderCollection(database);

        while (true) {
            System.out.println("\nOrder Management System");
            System.out.println("1. Insert Order");
            System.out.println("2. Update Delivery Address");
            System.out.println("3. Delete Order");
            System.out.println("4. Display All Orders");
            System.out.println("5. Calculate Total Amount");
            System.out.println("6. Count Product By ID");
            System.out.println("7. Exit");
            System.out.print("Select an option: ");
            int choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    // Insert order example
                    System.out.print("Enter order ID: ");
                    int orderId = scanner.nextInt();

                    System.out.print("Enter delivery address: ");
                    scanner.nextLine();  // Consume newline left-over
                    String deliveryAddress = scanner.nextLine();

                    // Collecting product details
                    Document product1 = new Document();
                    System.out.print("Enter product ID for first product: ");
                    product1.append("product_id", scanner.nextLine());

                    System.out.print("Enter product name for first product: ");
                    product1.append("product_name", scanner.nextLine());

                    System.out.print("Enter size for first product: ");
                    product1.append("size", scanner.nextLine());

                    System.out.print("Enter price for first product: ");
                    product1.append("price", scanner.nextDouble());

                    System.out.print("Enter quantity for first product: ");
                    product1.append("quantity", scanner.nextInt());

                    scanner.nextLine();  // Consume newline left-over

                    Document product2 = new Document();
                    System.out.print("Enter product ID for second product: ");
                    product2.append("product_id", scanner.nextLine());

                    System.out.print("Enter product name for second product: ");
                    product2.append("product_name", scanner.nextLine());

                    System.out.print("Enter size for second product: ");
                    product2.append("size", scanner.nextLine());

                    System.out.print("Enter price for second product: ");
                    product2.append("price", scanner.nextDouble());

                    System.out.print("Enter quantity for second product: ");
                    product2.append("quantity", scanner.nextInt());

                    // Calculating total amount
                    double totalAmount = (product1.getDouble("price") * product1.getInteger("quantity")) +
                            (product2.getDouble("price") * product2.getInteger("quantity"));

                    // Creating order document
                    Document order = new Document("orderid", orderId)
                            .append("products", Arrays.asList(product1, product2))
                            .append("total_amount", totalAmount)
                            .append("delivery_address", deliveryAddress);

                    // Insert the order into the database (assuming orderService is set up)
                    orderService.insertOrder(order);

                    System.out.println("Order inserted successfully!");
                    break;
                case 2:
                    // Update delivery address
                    System.out.print("Enter Order ID: ");
                    int orderIdToUpdate = scanner.nextInt();
                    scanner.nextLine();  // Consume newline
                    System.out.print("Enter new delivery address: ");
                    String newAddress = scanner.nextLine();
                    orderService.updateDeliveryAddress(orderIdToUpdate, newAddress);
                    System.out.println("Delivery address updated successfully!");
                    break;
                case 3:
                    // Delete order
                    System.out.print("Enter Order ID to delete: ");
                    int orderIdToDelete = scanner.nextInt();
                    orderService.deleteOrder(orderIdToDelete);
                    System.out.println("Order deleted successfully!");
                    break;
                case 4:
                    // Display all orders
                    orderService.displayOrders();
                    break;
                case 5:
                    // Calculate total amount
                    orderService.calculateTotalAmount();
                    break;
                case 6:
                    // Count product by ID
                    System.out.print("Enter Product ID: ");
                    String productId = scanner.next();
                    orderService.countProductById(productId);
                    break;
                case 7:
                    // Exit
                    System.out.println("Exiting...");
                    scanner.close();
                    System.exit(0);
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }
}