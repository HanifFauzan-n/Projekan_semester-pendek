package com.example.kartu.seed;

import com.example.kartu.enums.TransactionStatus;
import com.example.kartu.models.*;
import com.example.kartu.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Order(2)
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
    private FlashSaleRepository flashSaleRepository;

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
        seedFlashSales();
        seedTopUps();
        seedTransactions();
        log.info("[DUMMY] All extensive dummy data prepared successfully.");
    }

    private Category getOrCreateCategory(String code, String type) {
        List<Category> found = categoryRepository.findByTypeContainingIgnoreCase(type);
        if (!found.isEmpty()) {
            return found.get(0);
        }
        return categoryRepository.save(new Category(code, type));
    }

    private void seedCategories() {
        getOrCreateCategory("CD-001", "PULSA");
        getOrCreateCategory("CD-002", "PAKET DATA");
        getOrCreateCategory("CD-003", "ACCESSORIES");
        getOrCreateCategory("CD-004", "TOKEN PLN");
        getOrCreateCategory("CD-005", "VOUCHER GAME");
    }

    private void seedProvidersAndProducts() {
        Category pulsa = getOrCreateCategory("CD-001", "PULSA");
        Category data = getOrCreateCategory("CD-002", "PAKET DATA");
        Category aksesoris = getOrCreateCategory("CD-003", "ACCESSORIES");
        Category pln = getOrCreateCategory("CD-004", "TOKEN PLN");
        Category game = getOrCreateCategory("CD-005", "VOUCHER GAME");

        Provider telkomsel = createProvider("Telkomsel");
        Provider indosat = createProvider("Indosat");
        Provider xl = createProvider("XL");
        Provider tri = createProvider("Tri");
        Provider smartfren = createProvider("Smartfren");
        Provider axis = createProvider("Axis");
        Provider plnProvider = createProvider("PLN");
        // Voucher game publishers (products: docs/sql/007). Logo = static/img/<name>.png, reloaded on every start.
        List.of("Mobile Legends", "Free Fire", "PUBG Mobile", "Genshin Impact", "Valorant").forEach(this::createProvider);

        if (productRepository.count() == 0) {
            List<Product> products = List.of(
                    // Pulsa Reguler
                    createProduct(telkomsel, pulsa, "Telkomsel 5.000", 5500, 100, "Pulsa reguler Telkomsel masa aktif 7 hari"),
                    createProduct(telkomsel, pulsa, "Telkomsel 10.000", 10500, 100, "Pulsa reguler Telkomsel masa aktif 15 hari"),
                    createProduct(telkomsel, pulsa, "Telkomsel 25.000", 25500, 80, "Pulsa reguler Telkomsel masa aktif 30 hari"),
                    createProduct(telkomsel, pulsa, "Telkomsel 50.000", 50500, 50, "Pulsa reguler Telkomsel masa aktif 45 hari"),
                    createProduct(telkomsel, pulsa, "Telkomsel 100.000", 100000, 40, "Pulsa reguler Telkomsel masa aktif 60 hari"),

                    createProduct(indosat, pulsa, "Indosat 10.000", 10200, 100, "Pulsa reguler Indosat IM3 masa aktif 15 hari"),
                    createProduct(indosat, pulsa, "Indosat 25.000", 25200, 80, "Pulsa reguler Indosat IM3 masa aktif 30 hari"),
                    createProduct(indosat, pulsa, "Indosat 50.000", 50200, 50, "Pulsa reguler Indosat IM3 masa aktif 45 hari"),

                    createProduct(xl, pulsa, "XL 10.000", 10300, 100, "Pulsa reguler XL masa aktif 15 hari"),
                    createProduct(xl, pulsa, "XL 20.000", 20200, 100, "Pulsa reguler XL masa aktif 30 hari"),
                    createProduct(xl, pulsa, "XL 50.000", 50300, 60, "Pulsa reguler XL masa aktif 45 hari"),

                    createProduct(tri, pulsa, "Tri 10.000", 10050, 80, "Pulsa reguler Tri 10.000 menambah masa aktif"),
                    createProduct(tri, pulsa, "Tri 25.000", 25100, 60, "Pulsa reguler Tri 25.000"),
                    createProduct(tri, pulsa, "Tri 50.000", 50100, 40, "Pulsa reguler Tri 50.000"),

                    createProduct(smartfren, pulsa, "Smartfren 25.000", 25200, 100, "Pulsa reguler Smartfren 25.000"),
                    createProduct(smartfren, pulsa, "Smartfren 50.000", 50200, 50, "Pulsa reguler Smartfren 50.000"),

                    createProduct(axis, pulsa, "Axis 5.000", 5200, 100, "Pulsa reguler Axis 5.000"),
                    createProduct(axis, pulsa, "Axis 10.000", 10200, 80, "Pulsa reguler Axis 10.000"),
                    createProduct(axis, pulsa, "Axis 25.000", 25200, 60, "Pulsa reguler Axis 25.000"),

                    // Paket Data
                    createProduct(telkomsel, data, "Telkomsel 5GB / 30 Hari", 30000, 50, "Paket data Telkomsel 5GB 30 hari semua jaringan"),
                    createProduct(telkomsel, data, "Telkomsel 14GB MAXstream", 55000, 40, "Kuota internet 14GB + langganan MAXstream"),
                    createProduct(telkomsel, data, "Telkomsel 28GB OMG!", 95000, 30, "Kuota internet 28GB 24 jam full"),

                    createProduct(indosat, data, "Indosat 10GB / 30 Hari", 35000, 50, "Indosat Freedom Internet 10GB 30 hari"),
                    createProduct(indosat, data, "Indosat 25GB Freedom", 65000, 40, "Indosat Freedom Internet 25GB 24 jam"),

                    createProduct(xl, data, "XL 15GB / 30 Hari", 45000, 30, "Paket data XL Xtra Combo Flex 15GB 30 hari"),
                    createProduct(xl, data, "XL 30GB Xtra Combo", 75000, 25, "Paket data XL Xtra Combo 30GB"),

                    createProduct(tri, data, "Tri Happy 12GB 30 Hari", 38000, 45, "Kuota Tri Happy 12GB 24 jam tanpa pembagian"),
                    createProduct(smartfren, data, "Smartfren 20GB / 30 Hari", 60000, 30, "Paket Smartfren Kuota 20GB 30 hari"),
                    createProduct(axis, data, "Axis Bronet 8GB 30 Hari", 28000, 50, "Paket Axis Bronet 8GB 24 jam"),

                    // Token PLN & Aksesori
                    createProduct(plnProvider, pln, "Token Listrik PLN 20.000", 21500, 100, "Token listrik prabayar PLN Rp 20.000"),
                    createProduct(plnProvider, pln, "Token Listrik PLN 50.000", 51500, 80, "Token listrik prabayar PLN Rp 50.000"),
                    createProduct(plnProvider, pln, "Token Listrik PLN 100.000", 101500, 50, "Token listrik prabayar PLN Rp 100.000"),

                    createProduct(null, aksesoris, "Silicone Phone Case", 20000, 25, "Casing silikon pelindung HP berbagai model"),
                    createProduct(null, aksesoris, "Type-C Fast Data Cable", 25000, 30, "Kabel data fast charging Type-C 3A"),
                    createProduct(null, aksesoris, "Headset Bass 3.5mm", 35000, 20, "Earphone stereo kabel 3.5mm"),
                    createProduct(null, aksesoris, "Tempered Glass 9D", 15000, 4, "Anti gores kaca full cover (stok menipis)")
            );

            productRepository.saveAll(products);
            log.info("[DUMMY] {} products created.", products.size());
        }
    }

    private void seedUsers() {
        createUser("rizky21", "081211112222", 250000);
        createUser("salsa08", "081277778888", 150000);
        createUser("dinda99", "081222223333", 500000);
        createUser("bagas11", "081288889999", 100000);
        createUser("aldi77", "081233334455", 80000);
        createUser("maya_safitri", "081255556666", 200000);
        createUser("budi_santoso", "081299990000", 350000);
        createBannedUser();
    }

    private void seedVouchers() {
        if (voucherRepository.count() > 0) return;

        LocalDateTime now = LocalDateTime.now();

        List<Voucher> list = List.of(
                createVoucher("DISKON50", "PERCENT", 50.0, 50000.0, 20000.0, 100, 35, now.minusDays(1), now.plusHours(12)),
                createVoucher("ZELATAN5K", "NOMINAL", 5000.0, 0.0, null, 50, 10, now.minusDays(2), now.plusHours(5)),
                createVoucher("SAVE10", "NOMINAL", 10000.0, 25000.0, null, 30, 12, now.minusHours(3), now.plusHours(24)),
                createVoucher("PROMOHEMAT", "NOMINAL", 3000.0, 0.0, null, 100, 20, now.minusHours(1), now.plusHours(48)),
                createVoucher("MERDEKA", "PERCENT", 25.0, 30000.0, 15000.0, 20, 5, now.minusDays(1), now.plusHours(3)),
                createVoucher("MAHASISWA", "NOMINAL", 4000.0, 20000.0, null, 40, 15, now.minusHours(2), now.plusHours(6))
        );
        voucherRepository.saveAll(list);
        log.info("[DUMMY] {} vouchers created.", list.size());
    }

    private Voucher createVoucher(String code, String discountType, Double discountValue,
                                  Double minPurchase, Double maxDiscount,
                                  Integer usageLimit, Integer usedCount,
                                  LocalDateTime startAt, LocalDateTime endAt) {
        Voucher v = new Voucher();
        v.setCode(code);
        v.setDiscountType(discountType);
        v.setDiscountValue(discountValue);
        v.setMinPurchase(minPurchase);
        v.setMaxDiscount(maxDiscount);
        v.setUsageLimit(usageLimit);
        v.setUsedCount(usedCount);
        v.setStartAt(startAt);
        v.setEndAt(endAt);
        v.setActive(true);
        return v;
    }

    private void seedFlashSales() {
        if (flashSaleRepository.count() > 0) return;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusHours(10);

        List<Product> prods = productRepository.findAll();
        if (prods.isEmpty()) return;

        List<FlashSale> sales = new ArrayList<>();

        prods.stream().filter(p -> p.getName().contains("5GB")).findFirst().ifPresent(p -> {
            FlashSale fs = new FlashSale();
            fs.setProduct(p);
            fs.setFlashPrice(24000);
            fs.setStartAt(now.minusHours(1));
            fs.setEndAt(end);
            fs.setQuota(50);
            fs.setSoldCount(14);
            fs.setActive(true);
            sales.add(fs);
        });

        prods.stream().filter(p -> p.getName().contains("10GB")).findFirst().ifPresent(p -> {
            FlashSale fs = new FlashSale();
            fs.setProduct(p);
            fs.setFlashPrice(28000);
            fs.setStartAt(now.minusHours(2));
            fs.setEndAt(end);
            fs.setQuota(40);
            fs.setSoldCount(20);
            fs.setActive(true);
            sales.add(fs);
        });

        prods.stream().filter(p -> p.getName().contains("50.000")).findFirst().ifPresent(p -> {
            FlashSale fs = new FlashSale();
            fs.setProduct(p);
            fs.setFlashPrice(47500);
            fs.setStartAt(now.minusMinutes(45));
            fs.setEndAt(end);
            fs.setQuota(30);
            fs.setSoldCount(9);
            fs.setActive(true);
            sales.add(fs);
        });

        prods.stream().filter(p -> p.getName().contains("Silicone")).findFirst().ifPresent(p -> {
            FlashSale fs = new FlashSale();
            fs.setProduct(p);
            fs.setFlashPrice(12500);
            fs.setStartAt(now.minusHours(3));
            fs.setEndAt(end);
            fs.setQuota(25);
            fs.setSoldCount(18);
            fs.setActive(true);
            sales.add(fs);
        });

        flashSaleRepository.saveAll(sales);
        log.info("[DUMMY] {} flash sales created.", sales.size());
    }

    private void seedTopUps() {
        if (topUpRepository.count() > 0) return;

        User rizky = userRepository.findByUsername("rizky21").orElseThrow();
        User salsa = userRepository.findByUsername("salsa08").orElseThrow();
        User dinda = userRepository.findByUsername("dinda99").orElseThrow();
        User bagas = userRepository.findByUsername("bagas11").orElseThrow();
        User aldi = userRepository.findByUsername("aldi77").orElseThrow();
        User maya = userRepository.findByUsername("maya_safitri").orElseThrow();
        User budi = userRepository.findByUsername("budi_santoso").orElseThrow();

        LocalDateTime now = LocalDateTime.now();

        List<TopUp> list = List.of(
                createTopUp(rizky, 100000.0, TransactionStatus.SUCCESS, now.minusDays(6), "QRIS", "BCA"),
                createTopUp(salsa, 50000.0, TransactionStatus.SUCCESS, now.minusDays(5), "VIRTUAL_ACCOUNT", "BNI"),
                createTopUp(dinda, 200000.0, TransactionStatus.SUCCESS, now.minusDays(4), "EWALLET", "OVO"),
                createTopUp(budi, 300000.0, TransactionStatus.SUCCESS, now.minusDays(3), "QRIS", "DANA"),
                createTopUp(maya, 150000.0, TransactionStatus.SUCCESS, now.minusDays(2), "VIRTUAL_ACCOUNT", "BRI"),
                createTopUp(bagas, 50000.0, TransactionStatus.SUCCESS, now.minusDays(1), "VIRTUAL_ACCOUNT", "Mandiri"),
                createTopUp(aldi, 25000.0, TransactionStatus.FAILED, now.minusDays(4), "QRIS", "ShopeePay"),
                createTopUp(rizky, 50000.0, TransactionStatus.PENDING, now.minusHours(2), null, null)
        );

        topUpRepository.saveAll(list);
        log.info("[DUMMY] {} top up records created.", list.size());
    }

    private void seedTransactions() {
        if (transactionHistoryRepository.count() > 0) return;

        User rizky = userRepository.findByUsername("rizky21").orElseThrow();
        User salsa = userRepository.findByUsername("salsa08").orElseThrow();
        User dinda = userRepository.findByUsername("dinda99").orElseThrow();
        User bagas = userRepository.findByUsername("bagas11").orElseThrow();
        User maya = userRepository.findByUsername("maya_safitri").orElseThrow();
        User budi = userRepository.findByUsername("budi_santoso").orElseThrow();

        LocalDateTime now = LocalDateTime.now();

        List<TransactionHistory> histories = List.of(
                // 6 Hari lalu
                createTransaction(rizky, "Telkomsel 10.000", 10500.0, TransactionStatus.SUCCESS, now.minusDays(6).plusHours(2)),
                createTransaction(salsa, "Indosat 10.000", 10200.0, TransactionStatus.SUCCESS, now.minusDays(6).plusHours(5)),
                createTransaction(budi, "XL 15GB / 30 Hari", 45000.0, TransactionStatus.SUCCESS, now.minusDays(6).plusHours(8)),

                // 5 Hari lalu
                createTransaction(dinda, "Telkomsel 5GB / 30 Hari", 30000.0, TransactionStatus.SUCCESS, now.minusDays(5).plusHours(3)),
                createTransaction(maya, "Token Listrik PLN 50.000", 51500.0, TransactionStatus.SUCCESS, now.minusDays(5).plusHours(7)),
                createTransaction(bagas, "Axis 5.000", 5200.0, TransactionStatus.SUCCESS, now.minusDays(5).plusHours(10)),

                // 4 Hari lalu
                createTransaction(rizky, "Tri 25.000", 25100.0, TransactionStatus.SUCCESS, now.minusDays(4).plusHours(1)),
                createTransaction(salsa, "Smartfren 20GB / 30 Hari", 60000.0, TransactionStatus.SUCCESS, now.minusDays(4).plusHours(4)),
                createTransaction(budi, "Token Listrik PLN 100.000", 101500.0, TransactionStatus.SUCCESS, now.minusDays(4).plusHours(9)),

                // 3 Hari lalu
                createTransaction(dinda, "Telkomsel 50.000", 50500.0, TransactionStatus.SUCCESS, now.minusDays(3).plusHours(2)),
                createTransaction(maya, "Indosat 25GB Freedom", 65000.0, TransactionStatus.SUCCESS, now.minusDays(3).plusHours(6)),
                createTransaction(bagas, "Silicone Phone Case", 20000.0, TransactionStatus.SUCCESS, now.minusDays(3).plusHours(8)),

                // 2 Hari lalu
                createTransaction(rizky, "XL 30GB Xtra Combo", 75000.0, TransactionStatus.SUCCESS, now.minusDays(2).plusHours(3)),
                createTransaction(salsa, "Telkomsel 14GB MAXstream", 55000.0, TransactionStatus.SUCCESS, now.minusDays(2).plusHours(5)),
                createTransaction(budi, "Type-C Fast Data Cable", 25000.0, TransactionStatus.SUCCESS, now.minusDays(2).plusHours(7)),
                createTransaction(dinda, "Telkomsel 10.000", 10500.0, TransactionStatus.SUCCESS, now.minusDays(2).plusHours(9)),

                // 1 Hari lalu
                createTransaction(maya, "Telkomsel 28GB OMG!", 95000.0, TransactionStatus.SUCCESS, now.minusDays(1).plusHours(2)),
                createTransaction(rizky, "Token Listrik PLN 20.000", 21500.0, TransactionStatus.SUCCESS, now.minusDays(1).plusHours(4)),
                createTransaction(salsa, "Axis Bronet 8GB 30 Hari", 28000.0, TransactionStatus.SUCCESS, now.minusDays(1).plusHours(7)),
                createTransaction(bagas, "Tri Happy 12GB 30 Hari", 38000.0, TransactionStatus.SUCCESS, now.minusDays(1).plusHours(11)),

                // Hari ini
                createTransaction(dinda, "Telkomsel 5GB / 30 Hari", 30000.0, TransactionStatus.SUCCESS, now.minusHours(5)),
                createTransaction(budi, "Indosat 10GB / 30 Hari", 35000.0, TransactionStatus.SUCCESS, now.minusHours(3)),
                createTransaction(maya, "Headset Bass 3.5mm", 35000.0, TransactionStatus.SUCCESS, now.minusHours(2)),
                createTransaction(rizky, "Telkomsel 100.000", 100000.0, TransactionStatus.SUCCESS, now.minusMinutes(45)),
                createTransaction(salsa, "Smartfren 25.000", 25200.0, TransactionStatus.SUCCESS, now.minusMinutes(15))
        );

        transactionHistoryRepository.saveAll(histories);
        log.info("[DUMMY] {} transaction history records created across 7 days.", histories.size());
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
            return new byte[0];
        }
    }

    private Product createProduct(Provider provider, Category category, String name, int price, int stock, String description) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(price);
        product.setCostPrice((int) (price * 0.88)); // Margin keuntungan ~12%
        product.setStock(stock);
        product.setDescription(description);
        product.setProvider(provider);
        product.setCategory(category);
        return product;
    }

    private void createUser(String username, String phone, int balance) {
        if (userRepository.findByUsername(username).isEmpty()) {
            User user = new User();
            user.setUsername(username);
            user.setEmail(username + "@zelatancell.test");
            user.setEmailVerified(true);
            user.setPassword(passwordEncoder.encode(DUMMY_PASSWORD));
            user.setPhoneNumber(phone);
            user.setBalance(balance);
            user.setStatus("ACTIVE");
            user.setRole("ROLE_USER");
            userRepository.save(user);
            log.info("[DUMMY] User '{}' created.", username);
        }
    }

    private void createBannedUser() {
        if (userRepository.findByUsername("banned_user").isEmpty()) {
            User user = new User();
            user.setUsername("banned_user");
            user.setEmail("banned_user@zelatancell.test");
            user.setEmailVerified(true);
            user.setPassword(passwordEncoder.encode(DUMMY_PASSWORD));
            user.setPhoneNumber("081244445566");
            user.setBalance(10000);
            user.setStatus("BANNED");
            user.setRole("ROLE_USER");
            userRepository.save(user);
            log.info("[DUMMY] User 'banned_user' created (BANNED status).");
        }
    }

    private TopUp createTopUp(User user, Double amount, TransactionStatus status, LocalDateTime date, String method, String channel) {
        TopUp topUp = new TopUp(user, amount);
        topUp.setStatus(status);
        topUp.setDate(date);
        topUp.setExternalId("topup-" + user.getId() + "-" + randomPart(6));
        topUp.setPaymentMethod(method);
        topUp.setPaymentChannel(channel);
        if (status == TransactionStatus.SUCCESS) {
            topUp.setPaidAt(date.plusMinutes(2));
        }
        return topUp;
    }

    private TransactionHistory createTransaction(User user, String productName, Double amountPaid,
            TransactionStatus status, LocalDateTime timestamp) {
        Product product = productRepository.findAll().stream()
                .filter(p -> p.getName().equalsIgnoreCase(productName))
                .findFirst()
                .orElse(null);

        TransactionHistory history = new TransactionHistory();
        history.setUser(user);
        history.setCustomer(user.getUsername());
        history.setCustomerNumber(user.getPhoneNumber());
        history.setProduct(product);
        history.setTimestamp(timestamp);
        history.setStatus(status);
        history.setTransactionId(generateUniqueTransactionId());
        history.setSerialNumber(generateUniqueSerialNumber());
        history.setAmountPaid(amountPaid);
        history.setCostPrice(product != null && product.getCostPrice() != null ? Double.valueOf(product.getCostPrice()) : amountPaid * 0.88);
        history.setDiscountAmount(0.0);
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
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(ID_CHARS.charAt(random.nextInt(ID_CHARS.length())));
        }
        return builder.toString();
    }
}
