package com.ibm.drl.hbcp.util;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;

public interface ImmutableCollection<E> extends Collection<E> {

    @Override
    default boolean isEmpty() {
        return !iterator().hasNext();
    }

    @NotNull
    @Override
    default Object[] toArray() {
        Object[] res = new Object[size()];
        int i = 0;
        for (E item : this) {
            res[i++] = item;
        }
        return res;
    }

    @NotNull
    @Override
    // TODO: I don't know if this is implemented correctly
    default <T> T[] toArray(@NotNull T[] ts) {
        int length = size();
        T[] res = Arrays.copyOf(ts, length);
        System.arraycopy(toArray(), length, res, 0, length);
        return res;
    }

    @Override
    default boolean add(E e) {
        throw new CannotBeMutatedException();
    }

    @Override
    default boolean remove(Object o) {
        throw new CannotBeMutatedException();
    }

    @Override
    default boolean containsAll(@NotNull Collection<?> collection) {
        return collection.stream().allMatch(this::contains);
    }

    @Override
    default boolean addAll(@NotNull Collection<? extends E> collection) {
        throw new CannotBeMutatedException();
    }

    @Override
    default boolean removeAll(@NotNull Collection<?> collection) {
        throw new CannotBeMutatedException();
    }

    @Override
    default boolean retainAll(@NotNull Collection<?> collection) {
        throw new CannotBeMutatedException();
    }

    @Override
    default void clear() {
        throw new CannotBeMutatedException();
    }

    class CannotBeMutatedException extends RuntimeException {
        /**
		 * Auto-generated in Eclipse
		 */
		private static final long serialVersionUID = 2271798045398539669L;

		CannotBeMutatedException() {
            super("This collection cannot be mutated.");
        }
    }
}
