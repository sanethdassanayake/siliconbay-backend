package com.hogger.siliconbay.util;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import com.hogger.siliconbay.entity.Architecture;
import com.hogger.siliconbay.entity.Brand;
import com.hogger.siliconbay.entity.Category;
import com.hogger.siliconbay.entity.DeliveryType;
import com.hogger.siliconbay.entity.Manufacturer;
import com.hogger.siliconbay.entity.Model;
import com.hogger.siliconbay.entity.Order;
import com.hogger.siliconbay.entity.OrderItem;
import com.hogger.siliconbay.entity.Product;
import com.hogger.siliconbay.entity.Seller;
import com.hogger.siliconbay.entity.Status;
import com.hogger.siliconbay.entity.Stock;
import com.hogger.siliconbay.entity.User;

public final class DataSeeder {
    private DataSeeder() {
    }

    public static void seed() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();

            Status activeStatus = getOrCreateStatus(session, Status.Type.ACTIVE.name());
            Status verifiedStatus = getOrCreateStatus(session, Status.Type.VERIFIED.name());
            Status approvedStatus = getOrCreateStatus(session, Status.Type.APPROVED.name());
            Status packingStatus = getOrCreateStatus(session, Status.Type.PACKING.name());

            Brand stm32Brand = getOrCreateByName(session, Brand.class, "name", "STM32");
            Brand microchipBrand = getOrCreateByName(session, Brand.class, "name", "PIC");
            Brand intelCoreBrand = getOrCreateByName(session, Brand.class, "name", "Xeon");
            Brand xilinxBrand = getOrCreateByName(session, Brand.class, "name", "Zynq UltraScale+");
            Brand tiBrand = getOrCreateByName(session, Brand.class, "name", "Texas Instruments");
            Brand nvidiaBrand = getOrCreateByName(session, Brand.class, "name", "Jetson Orin");

            Manufacturer stMicroelectronics = getOrCreateByName(session, Manufacturer.class, "name", "STMicroelectronics");
            Manufacturer microchipTech = getOrCreateByName(session, Manufacturer.class, "name", "Microchip Technology Inc.");
            Manufacturer intelCorp = getOrCreateByName(session, Manufacturer.class, "name", "Intel Corporation");
            Manufacturer amdXilinx = getOrCreateByName(session, Manufacturer.class, "name", "AMD Xilinx");
            Manufacturer texasInstr = getOrCreateByName(session, Manufacturer.class, "name", "Texas Instruments");
            Manufacturer nvidiaCorp = getOrCreateByName(session, Manufacturer.class, "name", "NVIDIA Corporation");

            Architecture armArchitecture = getOrCreateByName(session, Architecture.class, "name", "ARM Cortex");
            Architecture riscVArchitecture = getOrCreateByName(session, Architecture.class, "name", "RISC-V Open");
            Architecture x86Architecture = getOrCreateByName(session, Architecture.class, "name", "x86_64");
            Architecture fpgaArchitecture = getOrCreateByName(session, Architecture.class, "name", "FPGA Fabric");
            Architecture mipsArchitecture = getOrCreateByName(session, Architecture.class, "name", "MIPS32");

            Category processorsCategory = getOrCreateByName(session, Category.class, "name", "Microcontrollers & MCUs");
            Category storageCategory = getOrCreateByName(session, Category.class, "name", "EEPROM & Flash Memory");
            Category componentsCategory = getOrCreateByName(session, Category.class, "name", "Discrete Semiconductors");
            Category sensorsCategory = getOrCreateByName(session, Category.class, "name", "Analog & Sensor ICs");

            // Fixed: Executed directly without variable assignment to clear "never used" warnings
            getOrCreateByName(session, Category.class, "name", "RF & Wireless Modules");
            getOrCreateByName(session, Category.class, "name", "Power Management ICs (PMIC)");
            getOrCreateByName(session, Category.class, "name", "SRAM & DRAM Cache");
            getOrCreateByName(session, Category.class, "name", "Optoelectronics & Drivers");
            getOrCreateByName(session, Category.class, "name", "Audio ICs & Codecs");

            DeliveryType standardDelivery = getOrCreateDeliveryType(session, "Standard Delivery", 4.99);

            User sellerUser = getOrCreateUser(
                    session,
                    "demo.seller@siliconbay.com",
                    "Demo",
                    "Seller",
                    "seed-password",
                    "SELLER-001",
                    verifiedStatus
            );

            User customerUser = getOrCreateUser(
                    session,
                    "demo.buyer@siliconbay.com",
                    "Demo",
                    "Buyer",
                    "seed-password",
                    "BUYER-001",
                    verifiedStatus
            );

            Seller seller = getOrCreateSeller(
                    session,
                    sellerUser,
                    "SiliconBay Industrial Authorized Distribution",
                    "+1 555 0199",
                    "store@siliconbay.com",
                    approvedStatus
            );

            List<ProductSeed> productSeeds = List.of(
                    new ProductSeed("STM32F407VGT6 Microcontroller", "ARM Cortex-M4 MCU with 1MB Flash, 168 MHz CPU, hardware crypto accelerator, and extensive rich peripherals.", stm32Brand, stMicroelectronics, armArchitecture, processorsCategory, "/products/stm32f407.jpg", 14.25, 450),
                    new ProductSeed("PIC16F877A-I/P 8-Bit MCU", "Legacy 40-pin Flash-based 8-bit microcontroller featuring 256 bytes of EEPROM data memory and up to 20MHz operating speeds.", microchipBrand, microchipTech, mipsArchitecture, processorsCategory, "/products/pic16f877a.jpg", 5.49, 1200),
                    new ProductSeed("Intel Xeon Silver 4314", "Scalable x86 server processor with 16 cores, 32 threads, 24MB cache, optimized for advanced data enterprise networks.", intelCoreBrand, intelCorp, x86Architecture, processorsCategory, "/products/xeon4314.jpg", 699.99, 14),
                    new ProductSeed("Zynq UltraScale+ MPSoC", "High-performance Quad-core ARM Cortex-A53 system on chip with dense programmable logic FPGA architecture cells.", xilinxBrand, amdXilinx, fpgaArchitecture, processorsCategory, "/products/zynq_ultrascale.jpg", 285.50, 45),
                    new ProductSeed("TI LM317T Linear Regulator", "Industry standard positive adjustable voltage regulator capable of supplying in excess of 1.5A over an output range of 1.2V to 37V.", tiBrand, texasInstr, armArchitecture, componentsCategory, "/products/lm317t.jpg", 1.15, 5000),
                    new ProductSeed("NVIDIA Jetson Orin Nano 8GB", "Compact Edge AI computing module providing up to 40 TOPS of AI performance, targeting autonomous machinery and computer vision.", nvidiaBrand, nvidiaCorp, armArchitecture, processorsCategory, "/products/orin_nano.jpg", 499.00, 32),
                    new ProductSeed("TI ADS1115 Ultra-Compact ADC", "Precision 16-bit analog-to-digital converter featuring an internal low-drift reference voltage and programmable gain amplifier.", tiBrand, texasInstr, riscVArchitecture, sensorsCategory, "/products/ads1115.jpg", 6.80, 850),
                    new ProductSeed("STM32MP157 Open-Source MPU", "Dual ARM Cortex-A7 microprocessor core operating alongside a dedicated Cortex-M4 coprocessor for complex industrial processing.", stm32Brand, stMicroelectronics, armArchitecture, processorsCategory, "/products/stm32mp1.jpg", 22.40, 180),
                    new ProductSeed("Microchip 24LC256 EEPROM", "I2C compatible 256-Kilobit electrically erasable programmable read-only memory module designed for low-power non-volatile configuration data storage.", microchipBrand, microchipTech, mipsArchitecture, storageCategory, "/products/24lc256.jpg", 0.95, 3500)
            );

            // Consolidated Loop: Clears code duplication warnings by combining Product and Stock creation
            List<Stock> stocks = new ArrayList<>();
            for (ProductSeed seed : productSeeds) {
                Product product = getOrCreateProduct(
                        session,
                        seed.name(),
                        seed.description(),
                        seed.brand(),
                        seed.manufacturer(),
                        seed.architecture(),
                        seed.category(),
                        seller,
                        List.of(seed.imagePath())
                );

                stocks.add(getOrCreateStock(session, product, seed.price(), seed.qty(), activeStatus));
            }

            session.flush();

            Order sampleOrder = getOrCreateOrder(session, customerUser, packingStatus, standardDelivery);

            getOrCreateOrderItem(session, sampleOrder, stocks.get(0), 10);
            getOrCreateOrderItem(session, sampleOrder, stocks.get(4), 100);
            getOrCreateOrderItem(session, sampleOrder, stocks.get(6), 5);

            session.getTransaction().commit();
            System.out.println("Seeded demo catalog data successfully.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to seed demo catalog data: " + e.getMessage(), e);
        }
    }

    private static Status getOrCreateStatus(Session session, String value) {
        Status status = session.createNamedQuery("Status.findByValue", Status.class)
                .setParameter("value", value)
                .uniqueResult();

        if (status != null) {
            return status;
        }

        status = new Status();
        status.setValue(value);
        session.persist(status);
        return status;
    }

    private static DeliveryType getOrCreateDeliveryType(Session session, String name, double price) {
        DeliveryType deliveryType = session.createQuery(
                        "from DeliveryType d where d.name = :name",
                        DeliveryType.class)
                .setParameter("name", name)
                .uniqueResult();

        if (deliveryType != null) {
            return deliveryType;
        }

        deliveryType = new DeliveryType();
        deliveryType.setName(name);
        deliveryType.setPrice(price);
        session.persist(deliveryType);
        return deliveryType;
    }

    private static User getOrCreateUser(
            Session session,
            String email,
            String firstName,
            String lastName,
            String password,
            String verificationCode,
            Status status
    ) {
        User user = session.createQuery("from User u where u.email = :email", User.class)
                .setParameter("email", email)
                .uniqueResult();

        if (user != null) {
            if (user.getVerificationCode() == null || user.getVerificationCode().isBlank()) {
                user.setVerificationCode(verificationCode);
            }
            return user;
        }

        user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPassword(password);
        user.setVerificationCode(verificationCode);
        user.setRole("USER");
        user.setStatus(status);
        session.persist(user);
        return user;
    }

    private static Seller getOrCreateSeller(
            Session session,
            User user,
            String companyName,
            String companyMobile,
            String companyEmail,
            Status status
    ) {
        Seller seller = session.createQuery("from Seller s where s.companyEmail = :companyEmail", Seller.class)
                .setParameter("companyEmail", companyEmail)
                .uniqueResult();

        if (seller != null) {
            return seller;
        }

        seller = new Seller();
        seller.setUser(user);
        seller.setCompanyName(companyName);
        seller.setCompanyMobile(companyMobile);
        seller.setCompanyEmail(companyEmail);
        seller.setStatus(status);
        session.persist(seller);
        return seller;
    }

    private static Product getOrCreateProduct(
            Session session,
            String name,
            String description,
            Brand brand,
            Manufacturer manufacturer,
            Architecture architecture,
            Category category,
            Seller seller,
            List<String> images
    ) {
        Product existingProduct = session.createQuery("from Product p where p.name = :name", Product.class)
                .setParameter("name", name)
                .uniqueResult();

        if (existingProduct != null) {
            return existingProduct;
        }

        Product product = new Product();
        product.setName(name);
        product.setDescription(description);

        Model model = getOrCreateModel(session, brand, name);
        product.setModel(model);
        product.setManufacturer(manufacturer);
        product.setArchitecture(architecture);
        product.setCategory(category);
        product.setSeller(seller);
        product.setImages(new ArrayList<>(images));

        session.persist(product);
        return product;
    }

    private static Model getOrCreateModel(Session session, Brand brand, String name) {
        Model model = session.createQuery(
                        "from Model m where m.brand = :brand and m.name = :name",
                        Model.class)
                .setParameter("brand", brand)
                .setParameter("name", name)
                .uniqueResult();

        if (model != null) {
            return model;
        }

        model = new Model();
        model.setBrand(brand);
        model.setName(name);
        session.persist(model);
        return model;
    }

    private static <T> T getOrCreateByName(Session session, Class<T> entityClass, String propertyName, String value) {
        T entity = session.createQuery(
                        "from " + entityClass.getSimpleName() + " e where e." + propertyName + " = :value",
                        entityClass)
                .setParameter("value", value)
                .uniqueResult();

        if (entity != null) {
            return entity;
        }

        try {
            T newEntity = entityClass.getDeclaredConstructor().newInstance();
            entityClass.getMethod("set" + capitalize(propertyName), String.class).invoke(newEntity, value);
            session.persist(newEntity);
            return newEntity;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unable to seed " + entityClass.getSimpleName() + " with value " + value, e);
        }
    }

    private static Stock getOrCreateStock(Session session, Product product, double price, int qty, Status status) {
        Stock existingStock = session.createQuery("from Stock s where s.product = :product", Stock.class)
                .setParameter("product", product)
                .uniqueResult();

        if (existingStock != null) {
            return existingStock;
        }

        Stock stock = new Stock();
        stock.setProduct(product);
        stock.setPrice(price);
        stock.setQty(qty);
        stock.setStatus(status);
        session.persist(stock);
        return stock;
    }

    private static Order getOrCreateOrder(Session session, User user, Status status, DeliveryType deliveryType) {
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

    private static void getOrCreateOrderItem(Session session, Order order, Stock stock, int qty) {
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

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    // Fixed: Replaced the old class structure with a clean, modern Java Record
    private record ProductSeed(
            String name,
            String description,
            Brand brand,
            Manufacturer manufacturer,
            Architecture architecture,
            Category category,
            String imagePath,
            double price,
            int qty
    ) {}
}