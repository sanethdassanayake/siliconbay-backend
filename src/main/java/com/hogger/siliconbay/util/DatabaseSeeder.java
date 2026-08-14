package com.hogger.siliconbay.util;

import org.hibernate.Session;
import org.hibernate.Transaction;

import com.hogger.siliconbay.entity.Architecture;
import com.hogger.siliconbay.entity.Brand;
import com.hogger.siliconbay.entity.Category;
import com.hogger.siliconbay.entity.DeliveryType;
import com.hogger.siliconbay.entity.Manufacturer;
import com.hogger.siliconbay.entity.Model;
import com.hogger.siliconbay.entity.Order;
import com.hogger.siliconbay.entity.OrderItem;
import com.hogger.siliconbay.entity.PaymentMethod;
import com.hogger.siliconbay.entity.PaymentType;
import com.hogger.siliconbay.entity.Product;
import com.hogger.siliconbay.entity.Seller;
import com.hogger.siliconbay.entity.Status;
import com.hogger.siliconbay.entity.Stock;
import com.hogger.siliconbay.entity.User;
import com.hogger.siliconbay.entity.UserPaymentInstrument;

public class DatabaseSeeder {
    public void seed() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                seedStatuses(session);
                seedPaymentData(session);
                seedDeliveryTypes(session);
                seedUsersAndSeller(session);
                seedCatalog(session);
                seedPaymentInstruments(session);
                seedOrders(session);
                tx.commit();
                System.out.println("Database seed completed...");
            } catch (Exception e) {
                tx.rollback();
                throw e;
            }
        }
    }

    private void seedStatuses(Session session) {
        for (Status.Type type : Status.Type.values()) {
            Status status = session.createNamedQuery("Status.findByValue", Status.class)
                    .setParameter("value", type.name())
                    .getSingleResultOrNull();
            if (status == null) {
                status = new Status();
                status.setValue(type.name());
                session.persist(status);
            }
        }
    }

    private void seedPaymentData(Session session) {
        PaymentType cardType = session.createQuery("FROM PaymentType pt WHERE pt.name=:name", PaymentType.class)
                .setParameter("name", "CARD")
                .getSingleResultOrNull();
        if (cardType == null) {
            cardType = new PaymentType();
            cardType.setName("CARD");
            session.persist(cardType);
        }

        PaymentType walletType = session.createQuery("FROM PaymentType pt WHERE pt.name=:name", PaymentType.class)
                .setParameter("name", "WALLET")
                .getSingleResultOrNull();
        if (walletType == null) {
            walletType = new PaymentType();
            walletType.setName("WALLET");
            session.persist(walletType);
        }

        createPaymentMethodIfMissing(session, "VISA", cardType);
        createPaymentMethodIfMissing(session, "MASTER", cardType);
        createPaymentMethodIfMissing(session, "PAYPAL", walletType);
    }

    private void createPaymentMethodIfMissing(Session session, String code, PaymentType type) {
        PaymentMethod method = session.createQuery("FROM PaymentMethod pm WHERE pm.code=:code", PaymentMethod.class)
                .setParameter("code", code)
                .getSingleResultOrNull();
        if (method == null) {
            method = new PaymentMethod();
            method.setCode(code);
            method.setType(type);
            session.persist(method);
        }
    }

    private void seedDeliveryTypes(Session session) {
        String currency = Env.get("app.currency") == null ? "LKR" : Env.get("app.currency");
        if ("USD".equalsIgnoreCase(currency)) {
            createDeliveryTypeIfMissing(session, "Standard", 5);
            createDeliveryTypeIfMissing(session, "Express", 12);
        } else {
            createDeliveryTypeIfMissing(session, "Standard", 500);
            createDeliveryTypeIfMissing(session, "Express", 1200);
        }
    }

    private void createDeliveryTypeIfMissing(Session session, String name, double price) {
        DeliveryType type = session.createQuery("FROM DeliveryType dt WHERE dt.name=:name", DeliveryType.class)
                .setParameter("name", name)
                .getSingleResultOrNull();
        if (type == null) {
            type = new DeliveryType();
            type.setName(name);
            type.setPrice(price);
            session.persist(type);
        }
    }

    private void seedUsersAndSeller(Session session) {
        Status verified = getStatus(session, Status.Type.VERIFIED);
        Status active = getStatus(session, Status.Type.ACTIVE);

        createUserIfMissing(session, "Admin", "User", "admin@siliconbay.com", "Admin@123", verified, "ADMIN");
        createUserIfMissing(session, "Demo", "Buyer", "buyer@siliconbay.com", "Buyer@123", verified, "USER");
        User sellerUser = createUserIfMissing(session, "Demo", "Seller", "seller@siliconbay.com", "Seller@123", verified, "SELLER");

        Seller seller = session.createQuery("FROM Seller s WHERE s.user.id=:userId", Seller.class)
                .setParameter("userId", sellerUser.getId())
                .getSingleResultOrNull();
        if (seller == null) {
            seller = new Seller();
            seller.setUser(sellerUser);
            seller.setCompanyName("Demo Seller Pvt Ltd");
            seller.setCompanyMobile("0770000000");
            seller.setCompanyEmail("seller@siliconbay.com");
            seller.setStatus(active);
            session.persist(seller);
        }
    }

    private User createUserIfMissing(Session session, String firstName, String lastName, String email, String password, Status status, String role) {
        User user = session.createNamedQuery("User.getByEmail", User.class)
                .setParameter("email", email)
                .getSingleResultOrNull();
        if (user == null) {
            user = new User();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setPassword(password);
            user.setVerificationCode("000000");
            user.setStatus(status);
            user.setRole(role);
            session.persist(user);
        } else {
            if (user.getRole() == null || !user.getRole().equalsIgnoreCase(role)) {
                user.setRole(role);
            }
            if (user.getStatus() == null || !status.getValue().equalsIgnoreCase(user.getStatus().getValue())) {
                user.setStatus(status);
            }
        }
        return user;
    }

    private void seedCatalog(Session session) {
        Brand brand = session.createQuery("FROM Brand b WHERE b.name=:name", Brand.class)
                .setParameter("name", "NVIDIA")
                .getSingleResultOrNull();
        if (brand == null) {
            brand = new Brand();
            brand.setName("NVIDIA");
            session.persist(brand);
        }

        Model model = session.createQuery("FROM Model m WHERE m.name=:name", Model.class)
                .setParameter("name", "RTX 4060")
                .getSingleResultOrNull();
        if (model == null) {
            model = new Model();
            model.setName("RTX 4060");
            model.setBrand(brand);
            session.persist(model);
        }

        Manufacturer manufacturer = session.createQuery("FROM Manufacturer m WHERE m.name=:name", Manufacturer.class)
                .setParameter("name", "ASUS")
                .getSingleResultOrNull();
        if (manufacturer == null) {
            manufacturer = new Manufacturer();
            manufacturer.setName("ASUS");
            session.persist(manufacturer);
        }

        Architecture architecture = session.createQuery("FROM Architecture a WHERE a.name=:name", Architecture.class)
                .setParameter("name", "Ada Lovelace")
                .getSingleResultOrNull();
        if (architecture == null) {
            architecture = new Architecture();
            architecture.setName("Ada Lovelace");
            session.persist(architecture);
        }

        Category category = session.createQuery("FROM Category c WHERE c.name=:name", Category.class)
                .setParameter("name", "Graphics Card")
                .getSingleResultOrNull();
        if (category == null) {
            category = new Category();
            category.setName("Graphics Card");
            session.persist(category);
        }

        User sellerUser = session.createNamedQuery("User.getByEmail", User.class)
                .setParameter("email", "seller@siliconbay.com")
                .getSingleResult();
        Seller seller = session.createQuery("FROM Seller s WHERE s.user.id=:userId", Seller.class)
                .setParameter("userId", sellerUser.getId())
                .getSingleResult();

        Product product = session.createQuery("FROM Product p WHERE p.name=:name", Product.class)
                .setParameter("name", "ASUS RTX 4060 8GB")
                .getSingleResultOrNull();
        if (product == null) {
            product = new Product();
            product.setName("ASUS RTX 4060 8GB");
            product.setDescription("Demo graphics card for backend showcase");
            product.setModel(model);
            product.setManufacturer(manufacturer);
            product.setArchitecture(architecture);
            product.setCategory(category);
            product.setSeller(seller);
            session.persist(product);
        }

        Stock stock = session.createQuery("FROM Stock s WHERE s.product.id=:productId", Stock.class)
                .setParameter("productId", product.getId())
                .setMaxResults(1)
                .getSingleResultOrNull();
        if (stock == null) {
            stock = new Stock();
            stock.setProduct(product);
            String currency = Env.get("app.currency") == null ? "LKR" : Env.get("app.currency");
            if ("USD".equalsIgnoreCase(currency)) {
                stock.setPrice(1250);
            } else {
                stock.setPrice(125000);
            }
            stock.setQty(20);
            stock.setStatus(getStatus(session, Status.Type.ACTIVE));
            session.persist(stock);
        }
    }

    private void seedPaymentInstruments(Session session) {
        User buyer = session.createNamedQuery("User.getByEmail", User.class)
                .setParameter("email", "buyer@siliconbay.com")
                .getSingleResultOrNull();
        if (buyer == null) {
            return;
        }

        UserPaymentInstrument instrument = session.createQuery("FROM UserPaymentInstrument p WHERE p.user.id=:userId", UserPaymentInstrument.class)
                .setParameter("userId", buyer.getId())
                .setMaxResults(1)
                .getSingleResultOrNull();
        if (instrument != null) {
            return;
        }

        PaymentMethod method = session.createQuery("FROM PaymentMethod pm WHERE pm.code=:code", PaymentMethod.class)
                .setParameter("code", "VISA")
                .getSingleResultOrNull();
        if (method == null) {
            return;
        }

        instrument = new UserPaymentInstrument();
        instrument.setUser(buyer);
        instrument.setPaymentMethod(method);
        instrument.setGatewayToken("demo-gateway-token");
        instrument.setLast4(1111);
        instrument.setBrand("VISA");
        instrument.setExpMonth(12);
        instrument.setExpYear(2030);
        instrument.setDefault(true);
        session.persist(instrument);
    }

    private void seedOrders(Session session) {
        User buyer = session.createNamedQuery("User.getByEmail", User.class)
                .setParameter("email", "buyer@siliconbay.com")
                .getSingleResultOrNull();
        if (buyer == null) {
            return;
        }

        DeliveryType standardDelivery = session.createQuery("FROM DeliveryType dt WHERE dt.name=:name", DeliveryType.class)
                .setParameter("name", "Standard")
                .getSingleResultOrNull();
        DeliveryType expressDelivery = session.createQuery("FROM DeliveryType dt WHERE dt.name=:name", DeliveryType.class)
                .setParameter("name", "Express")
                .getSingleResultOrNull();
        if (standardDelivery == null || expressDelivery == null) {
            return;
        }

        Product demoProduct = session.createQuery("FROM Product p WHERE p.name=:name", Product.class)
                .setParameter("name", "ASUS RTX 4060 8GB")
                .getSingleResultOrNull();
        if (demoProduct == null) {
            return;
        }

        Stock stock = session.createQuery("FROM Stock s WHERE s.product.id=:productId", Stock.class)
                .setParameter("productId", demoProduct.getId())
                .setMaxResults(1)
                .getSingleResultOrNull();
        if (stock == null) {
            return;
        }

        UserPaymentInstrument instrument = session.createQuery("FROM UserPaymentInstrument p WHERE p.user.id=:userId", UserPaymentInstrument.class)
                .setParameter("userId", buyer.getId())
                .setMaxResults(1)
                .getSingleResultOrNull();

        Status packing = getStatus(session, Status.Type.PACKING);
        Status delivered = getStatus(session, Status.Type.DELIVERED);
        Status pending = getStatus(session, Status.Type.PENDING);
        Status completed = getStatus(session, Status.Type.COMPLETED);

        Order packingOrder = getOrCreateOrder(session, buyer, packing, standardDelivery);
        getOrCreateOrderItem(session, packingOrder, stock, 1);
        getOrCreateTransaction(session, packingOrder, instrument, pending, "PayHere", "SEED-ORDER-001", stock.getPrice() + standardDelivery.getPrice());

        Order deliveredOrder = getOrCreateOrder(session, buyer, delivered, expressDelivery);
        getOrCreateOrderItem(session, deliveredOrder, stock, 2);
        getOrCreateTransaction(session, deliveredOrder, instrument, completed, "PayHere", "SEED-ORDER-002", (stock.getPrice() * 2) + expressDelivery.getPrice());
    }

    private Order getOrCreateOrder(Session session, User user, Status status, DeliveryType deliveryType) {
        Order existingOrder = session.createQuery(
                        "from Order o where o.user = :user and o.status = :status and o.deliveryType = :deliveryType",
                        Order.class)
                .setParameter("user", user)
                .setParameter("status", status)
                .setParameter("deliveryType", deliveryType)
                .uniqueResult();

        if (existingOrder != null) {
            return existingOrder;
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(status);
        order.setDeliveryType(deliveryType);
        session.persist(order);
        return order;
    }

    private void getOrCreateOrderItem(Session session, Order order, Stock stock, int qty) {
        OrderItem existingOrderItem = session.createQuery(
                        "from OrderItem oi where oi.order = :order and oi.stock = :stock",
                        OrderItem.class)
                .setParameter("order", order)
                .setParameter("stock", stock)
                .uniqueResult();

        if (existingOrderItem != null) {
            return;
        }

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setStock(stock);
        orderItem.setQty(qty);
        session.persist(orderItem);
    }

    private void getOrCreateTransaction(Session session, Order order, UserPaymentInstrument instrument, Status status, String paymentMethod, String paymentId, double amount) {
        com.hogger.siliconbay.entity.Transaction existingTransaction = session.createQuery("FROM Transaction t WHERE t.order.id=:orderId", com.hogger.siliconbay.entity.Transaction.class)
                .setParameter("orderId", order.getId())
                .getSingleResultOrNull();

        if (existingTransaction != null) {
            return;
        }

        com.hogger.siliconbay.entity.Transaction transaction = new com.hogger.siliconbay.entity.Transaction();
        transaction.setOrder(order);
        transaction.setAmount(amount);
        transaction.setUserPaymentInstrument(instrument);
        transaction.setStatus(status);
        transaction.setPaymentMethod(paymentMethod);
        transaction.setPaymentId(paymentId);
        session.persist(transaction);
    }

    private Status getStatus(Session session, Status.Type type) {
        return session.createNamedQuery("Status.findByValue", Status.class)
                .setParameter("value", type.name())
                .getSingleResult();
    }
}
