package io.smallrye.stork.servicediscovery.composite.util;

import java.util.Iterator;
import java.util.List;

class CombiningIterator<T> implements Iterator<T> {

    // iterator of a list that contains the next element, or null:
    private Iterator<T> nextNonEmptyIterator;

    private final Iterator<List<T>> listOfListsIterator;

    CombiningIterator(List<List<T>> contents) {
        listOfListsIterator = contents.iterator();
        nextNonEmptyIterator = nextNonEmptyIterator(listOfListsIterator);
    }

    private Iterator<T> nextNonEmptyIterator(Iterator<List<T>> listIterator) {
        while (listIterator.hasNext()) {
            List<T> currentList = listIterator.next();
            if (!currentList.isEmpty()) {
                return currentList.iterator();
            }
        }
        return null;
    }

    @Override
    public boolean hasNext() {
        return nextNonEmptyIterator != null && nextNonEmptyIterator.hasNext();
    }

    @Override
    public T next() {
        T result = nextNonEmptyIterator.next();
        if (!nextNonEmptyIterator.hasNext()) { // the end of the current list
            nextNonEmptyIterator = nextNonEmptyIterator(listOfListsIterator);
        }
        return result;
    }
}
