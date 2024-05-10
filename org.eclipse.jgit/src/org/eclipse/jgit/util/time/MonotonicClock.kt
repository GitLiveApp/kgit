/*
 * Copyright (C) 2016, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.util.time

/**
 * A provider of time.
 *
 *
 * Clocks should provide wall clock time, obtained from a reasonable clock
 * source, such as the local system clock.
 *
 *
 * MonotonicClocks provide the following behavior, with the assertion always
 * being true if
 * [org.eclipse.jgit.util.time.ProposedTimestamp.blockUntil] is
 * used:
 *
 * <pre>
 * MonotonicClock clk = ...;
 * long r1;
 * try (ProposedTimestamp t1 = clk.propose()) {
 * r1 = t1.millis();
 * t1.blockUntil(...);
 * }
 *
 * try (ProposedTimestamp t2 = clk.propose()) {
 * assert t2.millis() &gt; r1;
 * }
</pre> *
 *
 * @since 4.6
 */
interface MonotonicClock {
    /**
     * Obtain a timestamp close to "now".
     *
     *
     * Proposed times are close to "now", but may not yet be certainly in the
     * past. This allows the calling thread to interleave other useful work
     * while waiting for the clock instance to create an assurance it will never
     * in the future propose a time earlier than the returned time.
     *
     *
     * A hypothetical implementation could read the local system clock (managed
     * by NTP) and return that proposal, concurrently sending network messages
     * to closely collaborating peers in the same cluster to also ensure their
     * system clocks are ahead of this time. In such an implementation the
     * [org.eclipse.jgit.util.time.ProposedTimestamp.blockUntil]
     * method would wait for replies from the peers indicating their own system
     * clocks have moved past the proposed time.
     *
     * @return a [org.eclipse.jgit.util.time.ProposedTimestamp] object.
     */
    fun propose(): ProposedTimestamp?
}
