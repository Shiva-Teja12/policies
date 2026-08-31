package com.example.hrmspolicies2.notification;

public record EmailDeliveryResult(
        boolean successful,
        String failureReason
) {
    public static EmailDeliveryResult success() {
        return new EmailDeliveryResult(
                true,
                null
        );
    }

    public static EmailDeliveryResult failure(
            String reason
    ) {
        return new EmailDeliveryResult(
                false,
                reason
        );
    }
}