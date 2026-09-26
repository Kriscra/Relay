package org.vrz.relay.api.service;

/**
 * Defines the priority of a registered service provider within the Relay service ecosystem.
 * <p>
 * When multiple providers register for the same service interface, the provider with the
 * highest numerical weight is selected as the primary active provider.
 * If the primary provider unregisters, the registry automatically falls back to the next
 * highest available provider.
 */
public enum ServicePriority {

    /**
     * Fallback provider used only when no other providers are available.
     * Ideal for default offline or dummy implementations.
     */
    FALLBACK(-1),

    /**
     * Lowest priority provider. Runs only if normal or high providers are absent.
     */
    LOWEST(0),

    /**
     * Low priority provider.
     */
    LOW(1),

    /**
     * Standard priority level. Default for most third-party service implementations.
     */
    NORMAL(2),

    /**
     * High priority provider. Overrides normal providers.
     */
    HIGH(3),

    /**
     * Highest priority provider. Takes precedence over all other registrations.
     * Reserved for primary server core systems or explicit server-level overrides.
     */
    HIGHEST(4);

    private final int weight;

    ServicePriority(int weight) {
        this.weight = weight;
    }

    /**
     * Returns the numerical weight of this priority.
     *
     * @return integer weight, where higher values indicate higher precedence
     */
    public int getWeight() {
        return weight;
    }
}
