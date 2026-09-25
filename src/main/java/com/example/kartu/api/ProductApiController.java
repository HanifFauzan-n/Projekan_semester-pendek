package com.example.kartu.api;

import com.example.kartu.enums.PurchaseTarget;
import com.example.kartu.models.Category;
import com.example.kartu.models.FlashSale;
import com.example.kartu.models.Product;
import com.example.kartu.models.Provider;
import com.example.kartu.repositories.CategoryRepository;
import com.example.kartu.repositories.FlashSaleRepository;
import com.example.kartu.repositories.ProductRepository;
import com.example.kartu.repositories.ProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductApiController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProviderRepository providerRepository;
    private final FlashSaleRepository flashSaleRepository;

    @GetMapping("/products")
    public ResponseEntity<List<Map<String, Object>>> getProducts(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) Integer providerId,
            @RequestParam(required = false) String search) {

        List<Product> products = productRepository.findAll(Sort.by("price"));
        List<FlashSale> activeFlashSales = flashSaleRepository.findActiveFlashSales(LocalDateTime.now());
        // Same rule as checkout: the cheapest flash sale that still has quota.
        Map<Integer, FlashSale> flashSaleMap = activeFlashSales.stream()
                .filter(FlashSale::isRunning)
                .collect(Collectors.toMap(f -> f.getProduct().getId(), f -> f,
                        (a, b) -> a.getFlashPrice() <= b.getFlashPrice() ? a : b));

        List<Map<String, Object>> result = products.stream()
                .filter(p -> PurchaseTarget.of(p) != null) // physical goods are sold at the counter only
                .filter(p -> categoryId == null || (p.getCategory() != null && categoryId.equalsIgnoreCase(p.getCategory().getId())))
                .filter(p -> providerId == null || (p.getProvider() != null && providerId.equals(p.getProvider().getId())))
                .filter(p -> search == null || search.isBlank() || p.getName().toLowerCase().contains(search.toLowerCase()))
                .map(p -> mapProduct(p, flashSaleMap.get(p.getId())))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Integer id) {
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Product p = productOpt.get();
        FlashSale activeFs = flashSaleRepository.findRunningForProduct(p.getId(), LocalDateTime.now())
                .stream().findFirst().orElse(null);

        return ResponseEntity.ok(mapProduct(p, activeFs));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getCategories() {
        return ResponseEntity.ok(categoryRepository.findAll(Sort.by("code")));
    }

    /** Provider logo as a cacheable PNG, so product cards don't carry base64 images. */
    @GetMapping("/providers/{id}/logo")
    public ResponseEntity<byte[]> getProviderLogo(@PathVariable Integer id) {
        return providerRepository.findById(id)
                .map(Provider::getLogo)
                .filter(logo -> logo.length > 0)
                .map(logo -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                        .body(logo))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/providers")
    public ResponseEntity<List<Map<String, Object>>> getProviders() {
        List<Provider> providers = providerRepository.findAll();
        List<Map<String, Object>> result = providers.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("name", p.getName());
            map.put("logoBase64", p.getLogoBase64());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> mapProduct(Product p, FlashSale flashSale) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", p.getId());
        map.put("name", p.getName());
        map.put("price", p.getPrice());
        map.put("costPrice", p.getCostPrice());
        map.put("stock", p.getStock());
        map.put("description", p.getDescription());
        map.put("target", PurchaseTarget.of(p));
        if (p.getCategory() != null) {
            map.put("category", Map.of("id", p.getCategory().getId(), "type", p.getCategory().getType()));
        }
        if (p.getProvider() != null) {
            byte[] logo = p.getProvider().getLogo();
            map.put("provider", Map.of("id", p.getProvider().getId(), "name", p.getProvider().getName(),
                    "hasLogo", logo != null && logo.length > 0));
        }

        if (flashSale != null && flashSale.isRunning()) {
            map.put("isFlashSale", true);
            map.put("flashPrice", flashSale.getFlashPrice());
            map.put("flashSaleEndAt", flashSale.getEndAt());
            map.put("flashSaleQuota", flashSale.getQuota());
            map.put("flashSaleSoldCount", flashSale.getSoldCount());
        } else {
            map.put("isFlashSale", false);
        }

        return map;
    }
}
