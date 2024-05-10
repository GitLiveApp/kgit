package org.eclipse.jgit.lib

/**
 * Parsed information about a checkout.
 *
 * @since 3.0
 */
interface CheckoutEntry {
    /**
     * Get the name of the branch before checkout
     *
     * @return the name of the branch before checkout
     */
	val fromBranch: String?

    /**
     * Get the name of the branch after checkout
     *
     * @return the name of the branch after checkout
     */
	val toBranch: String?
}
