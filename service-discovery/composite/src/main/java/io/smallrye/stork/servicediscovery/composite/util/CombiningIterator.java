package io.smallrye.stork.servicediscovery.composite.util;

import java.util.Iterator;
import java.util.List;

public class CombiningIterator<T> implements Iterator<T> {

    private final List<List<T>> contents;

    // iterator of a list that contains the next element, or null:
    private Iterator<T> nextNonEmptyIterator;

    private Iterator<List<T>> listOfListsIteator;

    public CombiningIterator(List<List<T>> contents) {
        this.contents = contents;
        listOfListsIteator = contents.iterator();
        nextNonEmptyIterator = nextNonEmptyIterartor(listOfListsIteator);
    }

    private Iterator<T> nextNonEmptyIterartor(Iterator<List<T>> listIterator) {
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
            nextNonEmptyIterator = nextNonEmptyIterartor(listOfListsIteator);
        }
        return result;
    }
}
