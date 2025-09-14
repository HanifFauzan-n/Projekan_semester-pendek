package com.example.kartu.services;

import com.example.kartu.models.TransactionHistory;
import com.example.kartu.models.Product;
import com.example.kartu.models.User;
import com.example.kartu.repositories.TransactionHistoryRepository;
import com.example.kartu.repositories.ProductRepository;
import com.example.kartu.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;

    @Transactional
    public void purchaseProduct(Integer productId, String username) throws Exception {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new Exception("User not found"));
        Product product = productRepository.findById(productId).orElseThrow(() -> new Exception("Product not found"));

        if (product.getStock() <= 0) {
            throw new Exception("Product is out of stock.");
        }
        if (user.getBalance() < product.getPrice()) {
            throw new Exception("Insufficient balance.");
        }

        user.setBalance(user.getBalance() - product.getPrice());
        product.setStock(product.getStock() - 1);
        
        userRepository.save(user);
        productRepository.save(product);

        TransactionHistory history = new TransactionHistory();
        history.setUser(user);
        history.setProduct(product);
        transactionHistoryRepository.save(history);
    }
}