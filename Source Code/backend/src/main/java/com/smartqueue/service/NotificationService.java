package com.smartqueue.service;

import com.smartqueue.entity.QueueEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Simulates the notification subsystem.
 *
 * In production this would integrate with:
 *  - Spring Mail (JavaMail) for email
 *  - Twilio / AWS SNS for SMS
 *  - Firebase FCM for push notifications
 *
 * All methods are preserved from the project spec.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    /**
     * Notifies the customer linked to a QueueEntry.
     * Spec: NotificationService.notifyCustomer(QueueEntry entry, String message)
     */
    public void notifyCustomer(QueueEntry entry, String message) {
        log.info("[NOTIFY] → {} (Queue #{}): {}",
            entry.getCustomer().getName(), entry.getQueueNumber(), message);
        // TODO: integrate real SMS/push channel
    }

    /**
     * Notifies a customer by name and contact (used for appointment events).
     * Spec: NotificationService.notifyCustomer(customerName, contactInfo, message)
     */
    public void notifyCustomer(String customerName, String contactInfo, String message) {
        log.info("[NOTIFY] → {} ({}): {}", customerName, contactInfo, message);
        // TODO: integrate real email/SMS channel
    }
}
