package com.learningJWT.LearningTemplate.Paylod.Request;

import com.learningJWT.LearningTemplate.Enum.SubscriptionChangeMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionChangeRequest {
    private Long newPlanId;
    /** EXTEND (default) or REPLACE. Null/blank is treated as EXTEND. */
    private SubscriptionChangeMode mode;
}
