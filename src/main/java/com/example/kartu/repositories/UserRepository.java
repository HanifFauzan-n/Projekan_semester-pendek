package com.example.kartu.repositories;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.kartu.models.User;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);
    Optional<User> findByUsernameOrEmail(String username, String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumberAndUsernameNot(String phoneNumber, String username);

    /**
     * Atomic debit: the balance check and the subtraction happen in one statement, so two
     * concurrent purchases can never both pass the check. Returns 0 when funds are short.
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE User u SET u.balance = u.balance - :amount WHERE u.id = :id AND u.balance >= :amount")
    int debitBalance(@Param("id") Integer id, @Param("amount") int amount);

    /** Atomic credit, used by every top up path (Xendit webhook and manual approval). */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE User u SET u.balance = COALESCE(u.balance, 0) + :amount WHERE u.id = :id")
    int creditBalance(@Param("id") Integer id, @Param("amount") int amount);

    /** SKPL-F19: admin user table search; :q is a lower-case LIKE pattern, "%" for all. */
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE :q OR LOWER(COALESCE(u.email, '')) LIKE :q "
            + "OR COALESCE(u.phoneNumber, '') LIKE :q ORDER BY u.id")
    org.springframework.data.domain.Page<User> search(@Param("q") String q, org.springframework.data.domain.Pageable pageable);

    /** Reads the balance straight from the database, bypassing any stale managed entity. */
    @Query("SELECT COALESCE(u.balance, 0) FROM User u WHERE u.id = :id")
    Integer findBalanceById(@Param("id") Integer id);
}
