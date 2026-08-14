package com.hogger.siliconbay.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.hibernate.Session;
import org.hibernate.Transaction;

import com.google.gson.JsonObject;
import com.hogger.siliconbay.dto.ProductDTO;
import com.hogger.siliconbay.dto.ProductUpsertDTO;
import com.hogger.siliconbay.dto.StockDTO;
import com.hogger.siliconbay.dto.ReviewDTO;
import com.hogger.siliconbay.dto.RatingDTO;
import com.hogger.siliconbay.entity.Architecture;
import com.hogger.siliconbay.entity.Category;
import com.hogger.siliconbay.entity.Manufacturer;
import com.hogger.siliconbay.entity.Model;
import com.hogger.siliconbay.entity.Product;
import com.hogger.siliconbay.entity.Seller;
import com.hogger.siliconbay.entity.Status;
import com.hogger.siliconbay.entity.Stock;
import com.hogger.siliconbay.entity.Review;
import com.hogger.siliconbay.entity.User;
import com.hogger.siliconbay.util.AppUtil;
import com.hogger.siliconbay.util.CurrentUserUtil;
import com.hogger.siliconbay.util.HibernateUtil;
import com.hogger.siliconbay.util.ProductImageStorage;

import jakarta.servlet.http.HttpServletRequest;

public class ProductService {
    public String getFeaturedProducts() {
        JsonObject responseObject = new JsonObject();

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            List<Product> productList = hibernateSession
                    .createQuery("select distinct p from Product p order by p.createdAt desc", Product.class)
                    .setMaxResults(8)
                    .getResultList();

            List<ProductDTO> productDTOList = new ArrayList<>();
            for (Product product : productList) {
                productDTOList.add(getProductDTO(product, hibernateSession));
            }

            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Featured products found successfully");
            responseObject.add("products", AppUtil.GSON.toJsonTree(productDTOList));
        } catch (Exception e) {
            responseObject.addProperty("status", false);
            responseObject.addProperty("message", "Unable to load featured products!");
        }

        return AppUtil.GSON.toJson(responseObject);
    }

    public String getBestSellingProducts() {
        JsonObject responseObject = new JsonObject();

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            List<Object[]> productIdRows = hibernateSession.createQuery(
                            "select s.product.id, sum(oi.qty) from OrderItem oi join oi.stock s group by s.product.id order by sum(oi.qty) desc",
                            Object[].class)
                    .setMaxResults(8)
                    .getResultList();

            List<ProductDTO> productDTOList = new ArrayList<>();
            for (Object[] row : productIdRows) {
                int productId = ((Number) row[0]).intValue();
                Product product = hibernateSession.get(Product.class, productId);
                if (product != null) {
                    productDTOList.add(getProductDTO(product, hibernateSession));
                }
            }

            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Best selling products found successfully");
            responseObject.add("products", AppUtil.GSON.toJsonTree(productDTOList));
        } catch (Exception e) {
            responseObject.addProperty("status", false);
            responseObject.addProperty("message", "Unable to load best selling products!");
        }

        return AppUtil.GSON.toJson(responseObject);
    }

    public String getProductById(int id) {
        JsonObject responseObject = new JsonObject();

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            Product product = hibernateSession.get(Product.class, id);
            if (product == null) {
                responseObject.addProperty("status", false);
                responseObject.addProperty("message", "Product not found!");
            } else {
                responseObject.addProperty("status", true);
                responseObject.addProperty("message", "Product found successfully");
                responseObject.add("product", AppUtil.GSON.toJsonTree(getProductDTO(product, hibernateSession)));
            }
        } catch (Exception e) {
            responseObject.addProperty("status", false);
            responseObject.addProperty("message", "Unable to load product!");
        }

        return AppUtil.GSON.toJson(responseObject);
    }

    public String getMyProducts(HttpServletRequest request) {
        Integer userId = CurrentUserUtil.getUserId(request);
        if (userId == null) {
            return error("User session not found");
        }

        String role = CurrentUserUtil.getRole(request);

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Product> productList;
            if (role != null && role.equalsIgnoreCase("ADMIN")) {
                productList = session.createQuery("select distinct p from Product p order by p.createdAt desc", Product.class)
                        .getResultList();
            } else {
                Seller seller = session.createQuery("FROM Seller s WHERE s.user.id=:userId", Seller.class)
                        .setParameter("userId", userId)
                        .getSingleResultOrNull();
                if (seller == null) {
                    return error("Seller profile not found");
                }

                productList = session.createQuery("select distinct p from Product p where p.seller.id=:sellerId order by p.createdAt desc", Product.class)
                        .setParameter("sellerId", seller.getId())
                        .getResultList();
            }

            List<ProductDTO> productDTOList = new ArrayList<>();
            for (Product product : productList) {
                productDTOList.add(getProductDTO(product, session));
            }

            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Products found successfully");
            responseObject.add("products", AppUtil.GSON.toJsonTree(productDTOList));
            return AppUtil.GSON.toJson(responseObject);
        } catch (Exception e) {
            return error("Unable to load products!");
        }
    }

    public String createProduct(FormDataMultiPart formData, HttpServletRequest request) {
        Integer userId = CurrentUserUtil.getUserId(request);
        String role = CurrentUserUtil.getRole(request);
        if (userId == null) {
            return error("User session not found");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            ProductUpsertDTO dto = readProductPayload(formData);
            if (dto == null) {
                tx.rollback();
                return error("Product data is required");
            }

            Product product = new Product();
            applyPayload(session, product, dto, userId, role, true);
            session.persist(product);

            upsertMainStock(session, product, dto);
            product.setImages(collectImages(formData, dto.getImages(), Collections.emptyList()));

            if (product.getImages() == null || product.getImages().isEmpty()) {
                tx.rollback();
                return error("At least one product image is required");
            }

            tx.commit();

            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Product created successfully");
            responseObject.add("product", AppUtil.GSON.toJsonTree(getProductDTO(product, session)));
            return AppUtil.GSON.toJson(responseObject);
        } catch (Exception e) {
            return error("Unable to create product: " + e.getMessage());
        }
    }

    public String updateProduct(int productId, FormDataMultiPart formData, HttpServletRequest request) {
        Integer userId = CurrentUserUtil.getUserId(request);
        String role = CurrentUserUtil.getRole(request);
        if (userId == null) {
            return error("User session not found");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            Product product = session.get(Product.class, productId);
            if (product == null) {
                tx.rollback();
                return error("Product not found");
            }

            if (!canManageProduct(product, userId, role)) {
                tx.rollback();
                return error("Product not found");
            }

            ProductUpsertDTO dto = readProductPayload(formData);
            if (dto == null) {
                tx.rollback();
                return error("Product data is required");
            }

            applyPayload(session, product, dto, userId, role, false);
            upsertMainStock(session, product, dto);

            List<String> images = collectImages(formData, dto.getImages(), product.getImages() == null ? Collections.emptyList() : product.getImages());
            if (images.isEmpty()) {
                tx.rollback();
                return error("At least one product image is required");
            }
            product.setImages(images);

            tx.commit();

            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Product updated successfully");
            responseObject.add("product", AppUtil.GSON.toJsonTree(getProductDTO(product, session)));
            return AppUtil.GSON.toJson(responseObject);
        } catch (Exception e) {
            return error("Unable to update product: " + e.getMessage());
        }
    }

    private ProductDTO getProductDTO(Product product, Session hibernateSession) {
        ProductDTO productDTO = new ProductDTO();

        productDTO.setId(product.getId());
        productDTO.setName(product.getName());
        productDTO.setDescription(product.getDescription());

        if (product.getModel() != null) {
            productDTO.setModelId(product.getModel().getId());
            productDTO.setModelName(product.getModel().getName());
        }
        if (product.getManufacturer() != null) {
            productDTO.setManufacturerId(product.getManufacturer().getId());
            productDTO.setManufacturerName(product.getManufacturer().getName());
        }
        if (product.getArchitecture() != null) {
            productDTO.setArchitectureId(product.getArchitecture().getId());
            productDTO.setArchitectureName(product.getArchitecture().getName());
        }
        if (product.getCategory() != null) {
            productDTO.setCategoryId(product.getCategory().getId());
            productDTO.setCategoryName(product.getCategory().getName());
        }
        if (product.getSeller() != null) {
            productDTO.setSellerId(product.getSeller().getId());
            productDTO.setSellerName(product.getSeller().getCompanyName());
        }

        List<Stock> stockList = hibernateSession.createQuery(
                        "FROM Stock s WHERE s.product.id=:productId ORDER BY s.price ASC",
                        Stock.class)
                .setParameter("productId", product.getId())
                .getResultList();

        List<StockDTO> stockDTOList = new ArrayList<>();
        for (Stock stock : stockList) {
            stockDTOList.add(getStockDTO(stock));
        }
        productDTO.setStockList(stockDTOList);

        if (!stockList.isEmpty()) {
            Stock mainStock = stockList.get(0);
            productDTO.setPrice(mainStock.getPrice());
            productDTO.setAvailableQty(mainStock.getQty());
        }

        if (product.getImages() != null) {
            productDTO.setImages(new ArrayList<>(product.getImages()));
        }

        List<Review> reviews = hibernateSession.createQuery(
                "FROM Review r WHERE r.product.id=:productId ORDER BY r.createdAt DESC",
                Review.class)
                .setParameter("productId", product.getId())
                .getResultList();

        List<ReviewDTO> reviewDTOList = new ArrayList<>();
        double totalStars = 0;
        for (Review r : reviews) {
            String userName = "Anonymous";
            if (r.getUser() != null) {
                userName = r.getUser().getFirstName() + " " + r.getUser().getLastName();
            }
            String formattedDate = r.getCreatedAt() != null ? r.getCreatedAt().toLocalDate().toString() : "";
            reviewDTOList.add(new ReviewDTO(r.getStars(), r.getMessage(), userName, formattedDate));
            totalStars += r.getStars();
        }
        productDTO.setReviews(reviewDTOList);
        
        double avg = reviews.isEmpty() ? 0.0 : totalStars / reviews.size();
        avg = Math.round(avg * 10.0) / 10.0;
        productDTO.setRating(new RatingDTO(avg, reviews.size()));

        return productDTO;
    }

    private StockDTO getStockDTO(Stock stock) {
        StockDTO stockDTO = new StockDTO();
        stockDTO.setId(stock.getId());
        stockDTO.setPrice(stock.getPrice());
        stockDTO.setQty(stock.getQty());

        if (stock.getDiscount() != null) {
            stockDTO.setDiscountName(stock.getDiscount().getCouponCode());
            Double discountValue = stock.getDiscount().getValue();
            stockDTO.setDiscountPercentage(discountValue == null ? 0 : discountValue);
        }
        if (stock.getStatus() != null) {
            stockDTO.setStatus(stock.getStatus().getValue());
        }

        return stockDTO;
    }

    private ProductUpsertDTO readProductPayload(FormDataMultiPart formData) {
        if (formData == null || formData.getField("product") == null) {
            return null;
        }

        String payload = formData.getField("product").getValue();
        return AppUtil.GSON.fromJson(payload, ProductUpsertDTO.class);
    }

    private void applyPayload(Session session, Product product, ProductUpsertDTO dto, Integer userId, String role, boolean isCreate) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (dto.getDescription() == null || dto.getDescription().isBlank()) {
            throw new IllegalArgumentException("Product description is required");
        }
        if (dto.getCategoryId() == null || dto.getCategoryId() <= 0) {
            throw new IllegalArgumentException("Category is required");
        }
        if (dto.getPrice() == null || dto.getPrice() <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero");
        }
        if (dto.getQty() == null || dto.getQty() < 0) {
            throw new IllegalArgumentException("Quantity must be zero or greater");
        }

        Category category = session.get(Category.class, dto.getCategoryId());
        if (category == null) {
            throw new IllegalArgumentException("Category not found");
        }

        product.setName(dto.getName().trim());
        product.setDescription(dto.getDescription().trim());
        product.setCategory(category);

        if (dto.getModelId() != null && dto.getModelId() > 0) {
            Model model = session.get(Model.class, dto.getModelId());
            if (model == null) {
                throw new IllegalArgumentException("Model not found");
            }
            product.setModel(model);
        }

        if (dto.getManufacturerId() != null && dto.getManufacturerId() > 0) {
            Manufacturer manufacturer = session.get(Manufacturer.class, dto.getManufacturerId());
            if (manufacturer == null) {
                throw new IllegalArgumentException("Manufacturer not found");
            }
            product.setManufacturer(manufacturer);
        }

        if (dto.getArchitectureId() != null && dto.getArchitectureId() > 0) {
            Architecture architecture = session.get(Architecture.class, dto.getArchitectureId());
            if (architecture == null) {
                throw new IllegalArgumentException("Architecture not found");
            }
            product.setArchitecture(architecture);
        }

        if (isCreate) {
            product.setSeller(resolveSeller(session, dto, userId, role));
        } else if (dto.getSellerId() != null && dto.getSellerId() > 0 && role != null && role.equalsIgnoreCase("ADMIN")) {
            Seller seller = session.get(Seller.class, dto.getSellerId());
            if (seller == null) {
                throw new IllegalArgumentException("Seller not found");
            }
            product.setSeller(seller);
        }
    }

    private Seller resolveSeller(Session session, ProductUpsertDTO dto, Integer userId, String role) {
        if (dto.getSellerId() != null && dto.getSellerId() > 0 && role != null && role.equalsIgnoreCase("ADMIN")) {
            Seller seller = session.get(Seller.class, dto.getSellerId());
            if (seller == null) {
                throw new IllegalArgumentException("Seller not found");
            }
            return seller;
        }

        Seller seller = session.createQuery("FROM Seller s WHERE s.user.id=:userId", Seller.class)
                .setParameter("userId", userId)
                .getSingleResultOrNull();
        if (seller == null) {
            throw new IllegalArgumentException("Seller profile not found");
        }

        return seller;
    }

    private void upsertMainStock(Session session, Product product, ProductUpsertDTO dto) {
        Stock stock = session.createQuery("FROM Stock s WHERE s.product.id=:productId ORDER BY s.price ASC", Stock.class)
                .setParameter("productId", product.getId())
                .setMaxResults(1)
                .getSingleResultOrNull();

        if (stock == null) {
            stock = new Stock();
            stock.setProduct(product);
            stock.setStatus(session.createNamedQuery("Status.findByValue", Status.class)
                    .setParameter("value", String.valueOf(Status.Type.ACTIVE))
                    .getSingleResult());
            session.persist(stock);
        }

        stock.setPrice(dto.getPrice());
        stock.setQty(dto.getQty());
    }

    private List<String> collectImages(FormDataMultiPart formData, List<String> requestedImages, List<String> fallbackImages) throws IOException {
        List<String> images = new ArrayList<>();
        if (requestedImages != null) {
            for (String image : requestedImages) {
                if (image != null && !image.isBlank()) {
                    images.add(image);
                }
            }
        }

        if (images.isEmpty() && fallbackImages != null) {
            images.addAll(fallbackImages);
        }

        if (formData == null) {
            return images;
        }

        List<FormDataBodyPart> imageParts = formData.getFields("images");
        for (FormDataBodyPart imagePart : imageParts) {
            String imageUrl = ProductImageStorage.save(imagePart);
            if (imageUrl != null && !imageUrl.isBlank()) {
                images.add(imageUrl);
            }
        }

        return images;
    }

    private boolean canManageProduct(Product product, Integer userId, String role) {
        if (role != null && role.equalsIgnoreCase("ADMIN")) {
            return true;
        }

        return product.getSeller() != null
                && product.getSeller().getUser() != null
                && product.getSeller().getUser().getId() == userId;
    }

    public String searchProducts(String query, Integer categoryId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            StringBuilder hql = new StringBuilder("select distinct p from Product p where 1=1");
            if (query != null && !query.trim().isBlank()) {
                hql.append(" and (lower(p.name) like :query or lower(p.description) like :query)");
            }
            if (categoryId != null) {
                hql.append(" and p.category.id = :categoryId");
            }
            hql.append(" order by p.createdAt desc");

            var q = session.createQuery(hql.toString(), Product.class);
            if (query != null && !query.trim().isBlank()) {
                q.setParameter("query", "%" + query.trim().toLowerCase() + "%");
            }
            if (categoryId != null) {
                q.setParameter("categoryId", categoryId);
            }

            List<Product> productList = q.getResultList();
            List<ProductDTO> productDTOList = new ArrayList<>();
            for (Product product : productList) {
                productDTOList.add(getProductDTO(product, session));
            }

            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Products found successfully");
            responseObject.add("products", AppUtil.GSON.toJsonTree(productDTOList));
            return AppUtil.GSON.toJson(responseObject);
        } catch (Exception e) {
            return error("Unable to search products: " + e.getMessage());
        }
    }

    public String addReview(int productId, ReviewDTO reviewDto, int userId) {
        if (reviewDto.getStars() < 1 || reviewDto.getStars() > 5) {
            return error("Rating must be between 1 and 5 stars");
        }
        if (reviewDto.getMessage() == null || reviewDto.getMessage().trim().isBlank()) {
            return error("Review message is required");
        }

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = hibernateSession.beginTransaction();
            try {
                Product product = hibernateSession.get(Product.class, productId);
                if (product == null) {
                    transaction.rollback();
                    return error("Product not found");
                }

                User user = hibernateSession.get(User.class, userId);
                if (user == null) {
                    transaction.rollback();
                    return error("User not found");
                }

                Review review = new Review();
                review.setProduct(product);
                review.setUser(user);
                review.setStars(reviewDto.getStars());
                review.setMessage(reviewDto.getMessage().trim());

                hibernateSession.persist(review);
                transaction.commit();

                JsonObject response = new JsonObject();
                response.addProperty("status", true);
                response.addProperty("message", "Review submitted successfully");
                return AppUtil.GSON.toJson(response);
            } catch (Exception e) {
                transaction.rollback();
                return error("Unable to save review: " + e.getMessage());
            }
        }
    }

    private String error(String message) {
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }
}