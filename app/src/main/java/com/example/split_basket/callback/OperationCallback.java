package com.example.split_basket.callback;

/**
 * Callback interface for async operations.
 * Compatible with API 22+ (replaces BiConsumer which requires API 24+)
 */
public interface OperationCallback {
    /**
     * Called when an operation completes
     *
     * @param success true if operation succeeded, false otherwise
     * @param message status or error message
     */
    void onComplete(boolean success, String message);
}
