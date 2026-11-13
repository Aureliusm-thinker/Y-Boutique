package com.yvette.api.notification;

import java.util.List;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publish one of these to send an email once the current transaction commits (so a rolled-back
 * change never emails anyone). Sent in the background.
 */
public record MailRequest(String to, String subject, List<Mailer.Block> blocks) {

    @Component
    static class Sender {

        private final Mailer mailer;

        Sender(Mailer mailer) {
            this.mailer = mailer;
        }

        @Async
        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
        public void send(MailRequest request) {
            mailer.send(request.to(), request.subject(), request.blocks());
        }
    }
}
