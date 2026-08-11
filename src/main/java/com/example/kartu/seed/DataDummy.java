package com.example.kartu.seed;

import com.example.kartu.enums.TransactionStatus;
import com.example.kartu.models.Category;
import com.example.kartu.models.Product;
import com.example.kartu.models.Provider;
import com.example.kartu.models.TopUp;
import com.example.kartu.models.TransactionHistory;
import com.example.kartu.models.User;
import com.example.kartu.models.Voucher;
import com.example.kartu.repositories.CategoryRepository;
import com.example.kartu.repositories.ProductRepository;
import com.example.kartu.repositories.ProviderRepository;
import com.example.kartu.repositories.TopUpRepository;
import com.example.kartu.repositories.TransactionHistoryRepository;
import com.example.kartu.repositories.UserRepository;
import com.example.kartu.repositories.VoucherRepository;

import lombok.extern.slf4j.Slf4j;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DataDummy implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private TopUpRepository topUpRepository;

    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String DUMMY_PASSWORD = "password123";
    private static final String LOGO_PATH_PREFIX = "static/img/";

    private static final String ID_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final SecureRandom random = new SecureRandom();

    @Override
    public void run(String... args) throws Exception {
        seedCategories();
        seedProvidersAndProducts();
        seedUsers();
        seedVouchers();
        seedTopUps();
        seedTransactions();
        log.info("[DUMMY] Semua data dummy berhasil disiapkan.");
    }

    private void seedCategories() {
        if (categoryRepository.findByTypeContainingIgnoreCase("ACCESSORIES").isEmpty()) {
            categoryRepository.save(new Category("CD-003", "ACCESSORIES"));
            log.info("[DUMMY] Kategori ACCESSORIES dibuat.");
        }
    }

    private void seedProvidersAndProducts() {
        Category pulsa = categoryRepository.findByTypeContainingIgnoreCase("MOBILE CREDIT").get(0);
        Category data = categoryRepository.findByTypeContainingIgnoreCase("DATA PLAN").get(0);
        Category aksesoris = categoryRepository.findByTypeContainingIgnoreCase("ACCESSORIES").get(0);

        Provider telkomsel = createProvider("Telkomsel");
        Provider indosat = createProvider("Indosat");
        Provider xl = createProvider("XL");
        Provider tri = createProvider("Tri");
        Provider smartfren = createProvider("Smartfren");
        Provider axis = createProvider("Axis");

        if (productRepository.count() == 0) {
            List<Product> products = List.of(
                    createProduct(telkomsel, pulsa, "Telkomsel 5.000", 5500, 100, "Pulsa Telkomsel nominal 5.000"),
                    createProduct(telkomsel, pulsa, "Telkomsel 10.000", 10500, 100, "Pulsa Telkomsel nominal 10.000"),
                    createProduct(telkomsel, pulsa, "Telkomsel 50.000", 50500, 50, "Pulsa Telkomsel nominal 50.000"),
                    createProduct(indosat, pulsa, "Indosat 10.000", 10200, 100, "Pulsa Indosat nominal 10.000"),
                    createProduct(xl, pulsa, "XL 20.000", 20200, 100, "Pulsa XL nominal 20.000"),
                    createProduct(tri, pulsa, "Tri 10.000", 10050, 0, "Pulsa Tri nominal 10.000"),
                    createProduct(smartfren, pulsa, "Smartfren 25.000", 25200, 100, "Pulsa Smartfren nominal 25.000"),
                    createProduct(axis, pulsa, "Axis 5.000", 5200, 100, "Pulsa Axis nominal 5.000"),
                    createProduct(telkomsel, data, "Telkomsel Kuota 5GB", 30000, 50, "Kuota internet Telkomsel 5GB"),
                    createProduct(indosat, data, "Indosat Kuota 10GB", 35000, 50, "Kuota internet Indosat 10GB"),
                    createProduct(xl, data, "XL Kuota 15GB", 45000, 30, "Kuota internet XL 15GB"),
                    createProduct(smartfren, data, "Smartfren Kuota 20GB", 60000, 30, "Kuota internet Smartfren 20GB"),
                    createProduct(null, aksesoris, "Casing HP Silicon", 20000, 25, "Casing HP silikon semua merek"),
                    createProduct(null, aksesoris, "Kabel Data Type-C", 25000, 15, "Kabel data USB Type-C"));

            productRepository.saveAll(products);
            log.info("[DUMMY] {} produk dibuat.", products.size());
        }
    }

    private void seedUsers() {
        createUser("rizky21", "081211112222", "081233334444", 250000);
        createUser("salsa08", "081277778888", "081299990000", 150000);
        createUser("dinda99", "081222223333", "081244445555", 500000);
        createUser("bagas11", "081288889999", "081200001111", 100000);
        createUser("aldi77", "081233334455", "081255556677", 0);
        createBannedUser();
    }

    private void seedVouchers() {
        if (voucherRepository.count() > 0) {
            return;
        }

        Voucher v1 = new Voucher();
        v1.setCode("HEMAT10");
        v1.setDiscountAmount(10000.0);
        v1.setStock(10);
        v1.setActive(true);

        Voucher v2 = new Voucher();
        v2.setCode("ZELATAN50");
        v2.setDiscountAmount(50000.0);
        v2.setStock(50);
        v2.setActive(true);

        Voucher v3 = new Voucher();
        v3.setCode("OFF20");
        v3.setDiscountAmount(20000.0);
        v3.setStock(20);
        v3.setActive(false);

        voucherRepository.saveAll(List.of(v1, v2, v3));
        log.info("[DUMMY] 3 voucher dibuat.");
    }

    private void seedTopUps() {
        if (topUpRepository.count() > 0) {
            return;
        }

        User rizky = userRepository.findByUsername("rizky21").orElseThrow();
        User salsa = userRepository.findByUsername("salsa08").orElseThrow();
        User dinda = userRepository.findByUsername("dinda99").orElseThrow();
        User bagas = userRepository.findByUsername("bagas11").orElseThrow();
        User aldi = userRepository.findByUsername("aldi77").orElseThrow();

        TopUp t1 = createTopUp(rizky, 100000.0, TransactionStatus.SUCCESS, LocalDateTime.now().minusDays(5));
        TopUp t2 = createTopUp(salsa, 50000.0, TransactionStatus.SUCCESS, LocalDateTime.now().minusDays(3));
        TopUp t3 = createTopUp(dinda, 200000.0, TransactionStatus.SUCCESS, LocalDateTime.now().minusDays(2));
        TopUp t4 = createTopUp(bagas, 50000.0, TransactionStatus.SUCCESS, LocalDateTime.now().minusDays(1));
        TopUp t5 = createTopUp(aldi, 25000.0, TransactionStatus.FAILED, LocalDateTime.now().minusDays(4));
        TopUp t6 = createTopUp(rizky, 50000.0, TransactionStatus.PENDING, LocalDateTime.now());

        topUpRepository.saveAll(List.of(t1, t2, t3, t4, t5, t6));
        log.info("[DUMMY] 6 data top up dibuat.");
    }

    private void seedTransactions() {
        if (transactionHistoryRepository.count() > 0) {
            return;
        }

        User rizky = userRepository.findByUsername("rizky21").orElseThrow();
        User salsa = userRepository.findByUsername("salsa08").orElseThrow();
        User dinda = userRepository.findByUsername("dinda99").orElseThrow();
        User bagas = userRepository.findByUsername("bagas11").orElseThrow();

        List<TransactionHistory> histories = List.of(
                createTransaction(rizky, "Telkomsel 10.000", 10500.0, TransactionStatus.SUCCESS,
                        LocalDateTime.now().minusHours(2)),
                createTransaction(rizky, "Telkomsel Kuota 5GB", 30000.0, TransactionStatus.SUCCESS,
                        LocalDateTime.now().minusDays(1)),
                createTransaction(salsa, "Indosat 10.000", 10200.0, TransactionStatus.SUCCESS,
                        LocalDateTime.now().minusDays(2)),
                createTransaction(salsa, "Smartfren Kuota 20GB", 60000.0, TransactionStatus.SUCCESS,
                        LocalDateTime.now().minusDays(3)),
                createTransaction(dinda, "XL Kuota 15GB", 45000.0, TransactionStatus.SUCCESS,
                        LocalDateTime.now().minusDays(1)),
                createTransaction(dinda, "Telkomsel 50.000", 50500.0, TransactionStatus.SUCCESS,
                        LocalDateTime.now().minusHours(5)),
                createTransaction(bagas, "XL 20.000", 20200.0, TransactionStatus.FAILED,
                        LocalDateTime.now().minusDays(2)),
                createTransaction(bagas, "Tri 10.000", 10050.0, TransactionStatus.FAILED,
                        LocalDateTime.now().minusDays(1)),
                createTransaction(salsa, "Casing HP Silicon", 20000.0, TransactionStatus.SUCCESS,
                        LocalDateTime.now().minusHours(8)),
                createTransaction(dinda, "Telkomsel 10.000", 10500.0, TransactionStatus.SUCCESS,
                        LocalDateTime.now().minusHours(1)));

        transactionHistoryRepository.saveAll(histories);
        log.info("[DUMMY] {} riwayat transaksi dibuat.", histories.size());
    }

    private Provider createProvider(String name) {
        Optional<Provider> existing = providerRepository.findAll().stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst();
        byte[] logo = readLogo(name);
        if (existing.isPresent()) {
            Provider provider = existing.get();
            provider.setLogo(logo);
            return providerRepository.save(provider);
        }
        Provider provider = new Provider();
        provider.setName(name);
        provider.setLogo(logo);
        return providerRepository.save(provider);
    }

    private byte[] readLogo(String providerName) {
        try {
            ClassPathResource resource = new ClassPathResource(LOGO_PATH_PREFIX + providerName + ".png");
            BufferedImage source = ImageIO.read(resource.getInputStream());
            if (source == null) {
                return new byte[0];
            }

            int maxSize = 240;
            double scale = Math.min(1.0, Math.min((double) maxSize / source.getWidth(),
                    (double) maxSize / source.getHeight()));
            int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
            BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = resized.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source, 0, 0, width, height, null);
            graphics.dispose();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(resized, "png", output);
            return output.toByteArray();
        } catch (Exception e) {
            log.warn("[DUMMY] Logo {} tidak ditemukan, pakai logo kosong.", providerName);
            return new byte[0];
        }
    }

    private Product createProduct(Provider provider, Category category, String name, int price, int stock,
            String description) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(price);
        product.setStock(stock);
        product.setDescription(description);
        product.setProvider(provider);
        product.setCategory(category);
        return product;
    }

    private void createUser(String username, String phone, String emergency, int balance) {
        if (userRepository.findByUsername(username).isEmpty()) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(DUMMY_PASSWORD));
            user.setPhoneNumber(phone);
            user.setEmergencyNumber(emergency);
            user.setDanaNumber(phone);
            user.setBalance(balance);
            user.setStatus("ACTIVE");
            user.setRole("ROLE_USER");
            userRepository.save(user);
            log.info("[DUMMY] User '{}' dibuat.", username);
        }
    }

    private void createBannedUser() {
        if (userRepository.findByUsername("banned_user").isEmpty()) {
            User user = new User();
            user.setUsername("banned_user");
            user.setPassword(passwordEncoder.encode(DUMMY_PASSWORD));
            user.setPhoneNumber("081244445566");
            user.setEmergencyNumber("081266667788");
            user.setDanaNumber("081244445566");
            user.setBalance(10000);
            user.setStatus("BANNED");
            user.setRole("ROLE_USER");
            userRepository.save(user);
            log.info("[DUMMY] User 'banned_user' dibuat (status BANNED).");
        }
    }

    private TopUp createTopUp(User user, Double amount, TransactionStatus status, LocalDateTime date) {
        TopUp topUp = new TopUp(user, amount);
        topUp.setStatus(status);
        topUp.setDate(date);
        return topUp;
    }

    private TransactionHistory createTransaction(User user, String productName, Double amountPaid,
            TransactionStatus status, LocalDateTime timestamp) {
        Product product = productRepository.findAll().stream()
                .filter(p -> p.getName().equalsIgnoreCase(productName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Product not found for dummy: " + productName));

        TransactionHistory history = new TransactionHistory();
        history.setUser(user);
        history.setProduct(product);
        history.setTimestamp(timestamp);
        history.setStatus(status);
        history.setTransactionId(generateUniqueTransactionId());
        history.setSerialNumber(generateUniqueSerialNumber());
        history.setAmountPaid(amountPaid);
        return history;
    }

    private String generateUniqueTransactionId() {
        String newId;
        do {
            String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            newId = "ZLC-" + datePart + "-" + randomPart(4);
        } while (transactionHistoryRepository.existsByTransactionId(newId));
        return newId;
    }

    private String generateUniqueSerialNumber() {
        String sn;
        do {
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 16; i++) {
                sb.append(random.nextInt(10));
            }
            sn = sb.toString();
        } while (transactionHistoryRepository.existsBySerialNumber(sn));
        return sn;
    }

    private String randomPart(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ID_CHARS.charAt(random.nextInt(ID_CHARS.length())));
        }
        return sb.toString();
    }
}
