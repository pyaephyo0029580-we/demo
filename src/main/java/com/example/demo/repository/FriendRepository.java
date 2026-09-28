package com.example.demo.repository;

import com.example.demo.entity.Friend;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRepository
        extends CrudRepository<Friend, Long> {

    List<Friend> findByRequesterIdAndStatus(
            Long requesterId,
            String status
    );

    List<Friend> findByReceiverIdAndStatus(
            Long receiverId,
            String status
    );

    Optional<Friend> findByRequesterIdAndReceiverId(
            Long requesterId,
            Long receiverId
    );
}