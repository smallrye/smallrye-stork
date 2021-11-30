package io.smallrye.stork.servicediscovery.composite.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * An immutable list that is backed by multiple.
 *
 * The list assumes that the underlying lists are immutable
 *
 * @param <T>
 */
public class CombiningList<T> implements List<T> {
    private final List<List<T>> contents;
    private final int size;

    public CombiningList(List<List<T>> listOfLists) {
        this.contents = listOfLists;
        int size = 0;
        for (List<T> list : listOfLists) {
            size += list.size();
        }
        this.size = size;

    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) > -1;
    }

    @Override
    public Iterator<T> iterator() {
        return new CombiningIterator<>(contents);
    }

    @Override
    public Object[] toArray() {
        Object[] result = new Object[size];
        copyToArray(result);
        return result;
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        T1[] result = a;
        if (a.length != size) {
            result = Arrays.copyOf(a, size);
        }
        copyToArray(result);
        return result;
    }

    private void copyToArray(Object[] result) {
        Iterator<T> it = iterator();
        int position = 0;
        while (it.hasNext()) {
            result[position++] = it.next();
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        if (c == null) {
            throw new NullPointerException("Cannot check if a list contains all elements of a null list");
        }
        boolean result = true;
        for (Object o : c) {
            result &= contains(o);
        }
        return result;
    }

    @Override
    public int indexOf(Object o) {
        int sizeOfAllPreviousLists = 0;
        for (List<T> list : contents) {
            int indexInThisList = list.indexOf(o);
            if (indexInThisList > -1) {
                return sizeOfAllPreviousLists + indexInThisList;
            }
            sizeOfAllPreviousLists += list.size();
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        int sizeOfAllPreviousLists = 0;
        int lastIndexOf = -1;
        for (List<T> list : contents) {
            int lastIndexInThisList = list.lastIndexOf(o);
            if (lastIndexInThisList > -1) {
                lastIndexOf = sizeOfAllPreviousLists + lastIndexInThisList;
            }
            sizeOfAllPreviousLists += list.size();
        }
        return lastIndexOf;
    }

    @Override
    public T get(int index) {
        for (List<T> list : contents) {
            if (index < list.size()) {
                return list.get(index);
            } else {
                index -= list.size();
            }
        }

        throw new IndexOutOfBoundsException("Not enough elements in the list to get an element on the index " + index);
    }

    @Override
    public boolean add(T t) {
        throw new UnsupportedOperationException("CombiningList is an immutable collection");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("CombiningList is an immutable collection");
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException("CombiningList is an immutable collection");
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        throw new UnsupportedOperationException("CombiningList is an immutable collection");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("CombiningList is an immutable collection");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("CombiningList is an immutable collection");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("CombiningList is an immutable collection");
    }

    @Override
    public T set(int index, T element) {
        throw new UnsupportedOperationException("CombiningList is an immutable collection");
    }

    @Override
    public void add(int index, T element) {
        throw new UnsupportedOperationException("CombiningList is an immutable collection");
    }

    @Override
    public T remove(int index) {
        throw new UnsupportedOperationException("CombiningList is an immutable collection");
    }

    @Override
    public ListIterator<T> listIterator() {
        throw new UnsupportedOperationException("This operation is not supported yet");
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        throw new UnsupportedOperationException("This operation is not supported yet");
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("This operation is not supported yet");
    }
}
