package com.zjsu.pjt.product.config;

import com.zjsu.pjt.product.client.InventoryClient;
import com.zjsu.pjt.product.dto.InventoryCreateRequest;
import com.zjsu.pjt.product.model.Product;
import com.zjsu.pjt.product.repository.ProductRepository;
import com.zjsu.pjt.product.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryClient inventoryClient;

    @Autowired
    private ProductService productService;

    @Override
    public void run(String... args) throws Exception {
        seedAndPublishProducts();
    }

    private void seedAndPublishProducts() {
        if (productRepository.count() == 0) {
            log.info("数据库为空，开始初始化并上架15条商品数据...");

            List<Product> productsToSave = new ArrayList<>();
            productsToSave.add(createProduct("Giant", "公路车", "TCR Advanced 2", "Shimano 105", "M", "星空黑", 16800, "/images/giant-tcr.jpg"));
            productsToSave.add(createProduct("Trek", "山地车", "Marlin 7", "Shimano Deore", "L", "熔岩红", 5980, "/images/trek-marlin.jpg"));
            productsToSave.add(createProduct("Merida", "城市车", "Explorer 300", "Shimano Tourney", "S", "珍珠白", 2298, "/images/merida-explorer.jpg"));
            productsToSave.add(createProduct("Specialized", "公路车", "Allez Sprint", "SRAM Force", "M", "金属银", 25000, "/images/specialized-allez.jpg"));
            productsToSave.add(createProduct("Cannondale", "山地车", "Trail 5", "microSHIFT Advent X", "M", "石墨黑", 6800, "/images/cannondale-trail.jpg"));
            productsToSave.add(createProduct("Brompton", "折叠车", "C Line Explore", "Sturmey Archer 6-Speed", "One Size", "竞速绿", 13500, "/images/brompton-cline.jpg"));
            productsToSave.add(createProduct("Pinarello", "公路车", "Dogma F", "Shimano Dura-Ace Di2", "54", "冥王星闪", 98000, "/images/pinarello-dogma.jpg"));
            productsToSave.add(createProduct("Santa Cruz", "山地车", "Hightower Carbon", "SRAM GX Eagle", "L", "沙漠色", 45000, "/images/santacruz-hightower.jpg"));
            productsToSave.add(createProduct("Dahon", "折叠车", "Mariner D8", "Shimano Altus 8-Speed", "One Size", "拉丝银", 4500, "/images/dahon-mariner.jpg"));
            productsToSave.add(createProduct("Cervélo", "公路车", "S5", "SRAM Red eTap AXS", "56", "五黑", 89000, "/images/cervelo-s5.jpg"));
            productsToSave.add(createProduct("Yeti", "山地车", "SB150", "Shimano XTR", "M", "绿松石", 62000, "/images/yeti-sb150.jpg"));
            productsToSave.add(createProduct("Tern", "折叠车", "Verge X11", "SRAM Force 1 11-Speed", "One Size", "哑光黑", 22000, "/images/tern-verge.jpg"));
            productsToSave.add(createProduct("Look", "公路车", "795 Blade RS", "Shimano Ultegra Di2", "L", "变色龙", 55000, "/images/look-795.jpg"));
            productsToSave.add(createProduct("Kona", "山地车", "Honzo ESD", "Shimano XT/SLX", "M", "金属紫", 21000, "/images/kona-honzo.jpg"));
            productsToSave.add(createProduct("Giant", "城市车", "Escape 3", "Shimano Tourney", "M", "水泥灰", 2898, "/images/giant-escape.jpg"));

            List<Product> savedProducts = productRepository.saveAll(productsToSave);
            log.info("商品基础数据初始化完成，共 {} 条记录。", savedProducts.size());

            log.info("开始为初始化商品创建库存并执行上架操作...");
            for (Product product : savedProducts) {
                try {
                    // 随机生成一个库存数量，让数据更好看
                    int initialStock = 10 + (int)(Math.random() * 90); // 10到99的随机库存
                    InventoryCreateRequest request = new InventoryCreateRequest(product.getId(), initialStock);
                    inventoryClient.createInventoryRecord(request);
                    log.info("为商品 '{}' (ID: {}) 创建库存成功，数量: {}", product.getModel(), product.getId(), initialStock);

                    productService.updateOnShelfStatus(product.getId(), true);
                    log.info("为商品 '{}' (ID: {}) 执行自动上架成功。", product.getModel(), product.getId());
                } catch (Exception e) {
                    log.error("为商品 '{}' (ID: {}) 初始化库存或上架时失败: {}", product.getModel(), product.getId(), e.getMessage());
                }
            }
            log.info("商品库存创建及上架流程完成。");
        } else {
            log.info("数据库中已存在商品数据，无需初始化。");
        }
    }

    private Product createProduct(String brand, String category, String model, String gearSystem, String frameSize, String color, int price, String imageUrl) {
        Product p = new Product();
        p.setBrand(brand);
        p.setCategory(category);
        p.setModel(model);
        p.setGearSystem(gearSystem);
        p.setFrameSize(frameSize);
        p.setColor(color);
        p.setPrice(price);
        p.setImageUrl(imageUrl);
        return p;
    }
}