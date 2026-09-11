package com.example.demo.config;

import com.example.demo.model.Member;
import com.example.demo.model.Product;
import com.example.demo.model.Role;
import com.example.demo.repository.MemberRepository;
import com.example.demo.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    public DataInitializer(ProductRepository productRepository, MemberRepository memberRepository) {
        this.productRepository = productRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    public void run(String... args) {
        seedProducts();
        seedAdmin();
    }

    private void seedProducts() {
        if (productRepository.count() > 0) {
            return;
        }

        // 換頁功能需要夠多測試資料，這裡用一個小 record 裝種子資料，跑迴圈建立，
        // 不用像之前那樣手動宣告 p1、p2...p30 這麼多個變數。
        record SeedProduct(String name, String price, int stock) {}

        List<SeedProduct> seedList = List.of(
            new SeedProduct("無線滑鼠", "399", 50),
            new SeedProduct("機械式鍵盤", "1590", 20),
            new SeedProduct("藍牙耳機", "890", 35),
            new SeedProduct("27吋 4K 顯示器", "8990", 15),
            new SeedProduct("USB-C 多功能擴充座", "1290", 40),
            new SeedProduct("筆電支架", "690", 60),
            new SeedProduct("電競滑鼠墊", "350", 100),
            new SeedProduct("Webcam 網路攝影機", "1490", 25),
            new SeedProduct("行動電源 20000mAh", "890", 45),
            new SeedProduct("無線充電盤", "590", 55),
            new SeedProduct("藍牙喇叭", "1690", 30),
            new SeedProduct("桌上型麥克風", "2290", 18),
            new SeedProduct("電競耳機麥克風組", "2490", 22),
            new SeedProduct("人體工學滑鼠", "690", 48),
            new SeedProduct("靜音鍵盤", "990", 33),
            new SeedProduct("螢幕遮光罩", "450", 20),
            new SeedProduct("筆電包", "990", 40),
            new SeedProduct("大尺寸滑鼠墊", "590", 60),
            new SeedProduct("HDMI 傳輸線 2M", "190", 150),
            new SeedProduct("Type-C 傳輸線 1M", "150", 200),
            new SeedProduct("無線充電滑鼠", "790", 38),
            new SeedProduct("藍牙鍵盤", "1290", 25),
            new SeedProduct("平板保護殼", "650", 45),
            new SeedProduct("手機支架", "350", 80),
            new SeedProduct("車用手機架", "490", 70),
            new SeedProduct("藍牙接收器", "390", 60),
            new SeedProduct("USB Hub 4孔", "350", 90),
            new SeedProduct("螢幕清潔組", "250", 120),
            new SeedProduct("桌上型立燈", "1190", 28),
            new SeedProduct("電腦清潔噴罐", "199", 200)
        );

        for (SeedProduct s : seedList) {
            Product p = new Product();
            p.setName(s.name());
            p.setPrice(new BigDecimal(s.price()));
            p.setStock(s.stock());
            productRepository.save(p);
        }
    }

    // 測試用的管理員帳號：admin / admin123
    private void seedAdmin() {
        if (memberRepository.existsByUsername("admin")) {
            return;
        }

        Member admin = new Member();
        admin.setUsername("admin");
        admin.setPassword(new BCryptPasswordEncoder().encode("admin123"));
        admin.setEmail("admin@example.com");
        admin.setName("系統管理員");
        admin.setRole(Role.ADMIN);

        memberRepository.save(admin);
    }
}