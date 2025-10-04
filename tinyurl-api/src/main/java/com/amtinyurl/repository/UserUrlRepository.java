package com.amtinyurl.repository;

import com.amtinyurl.entity.UserUrl;
import com.amtinyurl.entity.UserUrlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserUrlRepository extends JpaRepository<UserUrl, UserUrlId> {

    @Query("SELECT uu FROM UserUrl uu JOIN FETCH uu.url WHERE uu.userIdLower = :userIdLower ORDER BY uu.createdAt DESC")
    List<UserUrl> findByUserIdLowerOrderByCreatedAtDesc(@Param("userIdLower") String userIdLower);

    boolean existsByUserIdLowerAndCode(String userIdLower, String code);

    void deleteByUserIdLowerAndCode(String userIdLower, String code);
}