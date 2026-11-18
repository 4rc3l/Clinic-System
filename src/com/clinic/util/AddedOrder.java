package com.clinic.util;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Default "time added" order for the table panels (newest added on top).
 *
 * ABSTRACTION   - panels only know "arrange(list)"; how the order is worked out is hidden.
 * ENCAPSULATION - arrange() is final and always works on a COPY, so the original list
 *                 (e.g. the clinic's patient list) is never changed by a panel.
 * INHERITANCE   - every ordering extends this class and only supplies order(list).
 * POLYMORPHISM  - Patients, Appointments and Payments all call the same arrange(); each
 *                 one gets its own behaviour from the subclass it was created with.
 */
public abstract class AddedOrder<T> {

    /** Returns a new list, newest added first. The given collection is left untouched. */
    public final List<T> arrange(Collection<? extends T> items) {
        List<T> copy = new ArrayList<>(items);
        order(copy);
        return copy;
    }

    /** Puts the (already copied) list in order. Each kind of ordering implements this. */
    protected abstract void order(List<T> list);

    // ------------------------------------------------------------------
    // Ready-made orderings
    // ------------------------------------------------------------------

    /**
     * For lists that have no timestamp (patients, appointments): the clinic adds new
     * entries to the END of its list and saves them in that same order, so the
     * newest-added is simply the last one. Reversing it puts the newest on top.
     */
    public static <T> AddedOrder<T> byInsertion() {
        return new AddedOrder<T>() {
            @Override
            protected void order(List<T> list) {
                Collections.reverse(list);
            }
        };
    }

    /**
     * For items that carry their own "created at" time (bills). Newest on top; if two have
     * the same time, the one added later in the list goes first.
     */
    public static <T> AddedOrder<T> byTimestamp(Function<? super T, LocalDateTime> createdAt) {
        return new AddedOrder<T>() {
            @Override
            protected void order(List<T> list) {
                list.sort(Comparator.comparing(createdAt));   // stable: ties keep insertion order
                Collections.reverse(list);
            }
        };
    }
}