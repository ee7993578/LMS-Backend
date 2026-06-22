package com.learningJWT.LearningTemplate.Mapper;

import com.learningJWT.LearningTemplate.Model.Subscription;
import com.learningJWT.LearningTemplate.Paylod.DTO.SubscriptionDTO;

public class SubscriptionMapper {

    public static SubscriptionDTO todto(Subscription subscription) {

        return SubscriptionDTO.builder()
                .library(subscription.getLibrary())
                .libraryPlan(subscription.getLibraryPlan())
                .libraryPlanId(subscription.getLibraryPlan().getPlanId())
                .subscriptionId(subscription.getSubscriptionId())
                .billingEnd(subscription.getBillingEnd())
                .billingStart(subscription.getBillingStart())
                .bufferAllowed(subscription.getBufferAllowed())
                .bufferExpiryDays(subscription.getBufferExpiryDays())
                .planName(subscription.getPlanName())
                .pricePerMonth(subscription.getPricePerMonth())
                .studentLimit(subscription.getStudentLimit())
                .GraceEndDate(subscription.getGraceEndDate())
                .build();
    }
}
