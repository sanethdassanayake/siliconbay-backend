package com.hogger.siliconbay.service;

import com.google.gson.JsonObject;
import com.hogger.siliconbay.dto.ProductDTO;
import com.hogger.siliconbay.dto.StockDTO;
import com.hogger.siliconbay.entity.Product;
import com.hogger.siliconbay.entity.Stock;
import com.hogger.siliconbay.util.AppUtil;
import com.hogger.siliconbay.util.HibernateUtil;
import org.hibernate.Session;

import java.util.ArrayList;
import java.util.List;

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

        return productDTO;
    }

    private StockDTO getStockDTO(Stock stock) {
        StockDTO stockDTO = new StockDTO();
        stockDTO.setId(stock.getId());
        stockDTO.setPrice(stock.getPrice());
        stockDTO.setQty(stock.getQty());

        if (stock.getDiscount() != null) {
            stockDTO.setDiscountName(stock.getDiscount().getCouponCode());
            stockDTO.setDiscountPercentage(stock.getDiscount().getValue() == null ? 0 : stock.getDiscount().getValue());
        }
        if (stock.getStatus() != null) {
            stockDTO.setStatus(stock.getStatus().getValue());
        }

        return stockDTO;
    }
}
