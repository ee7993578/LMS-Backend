package com.learningJWT.LearningTemplate.Services;

import com.learningJWT.LearningTemplate.Enum.Status;
import com.learningJWT.LearningTemplate.Model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription,Long> {

    Subscription findByLibraryId(Long libraryId) ;
    List<Subscription> findByStatus(Status subscriptionStatus);
}
