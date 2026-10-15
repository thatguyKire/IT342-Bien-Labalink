package edu.cit.bien.labalink.service;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Defers side effects (emails, WebSocket pushes) until the database
 * transaction has committed, so clients never hear about changes that
 * were rolled back.
 */
final class AfterCommit {

    private AfterCommit() {
    }

    static void run(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        action.run();
                    }
                });
        } else {
            action.run();
        }
    }
}
