package com.ownwn.server.java.lang.replacement;

import com.ownwn.server.java.lang.replacement.function.Comparator;
import com.ownwn.server.java.lang.replacement.stream.Stream;

import java.util.Iterator;

@SuppressWarnings("unchecked")
public class ArrayList<T> implements List<T> {
    Object[] array;
    int currentSize = 0;

    public ArrayList() {
        this(32);
    }

    public ArrayList(int initialCapacity) {
        array = new Object[initialCapacity <= 0 ? 32 : initialCapacity];
    }

    /** shallow copy constructor */
    public ArrayList(Collection<T> list) {
        this();
        addAll(list);
    }

    /** shallow copy constructor */
    public ArrayList(java.util.List<T> list) {
        this();
        for (T t : list) {
            add(t);
        }
    }

    @Override
    public int size() {
        return currentSize;
    }

    @Override
    public boolean isEmpty() {
        return currentSize == 0;
    }

    @Override
    public boolean contains(Object o) {
        for (int i = 0; i < currentSize; i++) {
            if (Objects.equals(array[i], o)) return true;
        }
        return false;
    }

    private void ensureCapacity() {
        if (currentSize >= array.length-1) {
            array = Arrays.copyOf(array, Math.max(currentSize*2, 1));
        }
    }

    @Override
    public boolean add(Object o) {
        ensureCapacity();

        array[currentSize++] = o;

        return true;
    }

    @Override
    public boolean remove(Object o) {
        int index = -1;
        for (int i = 0; i < size(); i++) {
            if (Objects.equals(array[i], o)) {
                index = i;
                break;
            }
        }
        if (index == -1) return false;

        remove(index);


        return true;
    }

    @Override
    public boolean containsAll(List<?> c) {
        for (var item : c) {
            if (!contains(item)) return false;
        }
        return true;
    }

    public boolean retainAll(List<?> c) {
        List<T> toRemove = new ArrayList<>();

        for (int i = 0; i < size(); i++) {
            var item = get(i);
            if (!c.contains(item)) toRemove.add(item);
        }
        for (var item : toRemove) {
            remove(item);
        }
        return true;
    }

    @Override
    public void clear() {
        currentSize = 0;
    }

    @Override
    public T get(int index) {
        return (T) array[index];
    }

    @Override
    public T set(int index, T elem) {
        Object old = array[index];
        array[index] = elem;
        return (T) old;
    }

    @Override
    public T remove(int index) {
        T old = (T) array[index];
        for (int i = index; i < currentSize-1; i++) {
            swap(i, i+1);
        }
        currentSize--;
        return old;
    }

    @Override
    public int indexOf(Object o) {
        for (int i = 0; i < size(); i++) {
            var item = get(i);
            if (Objects.equals(item, o)) return i;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        for (int i = size()-1; i >= 0; i--) {
            var item = get(i);
            if (Objects.equals(item, o)) return i;
        }
        return -1;
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return null;
    }

    @Override
    public T removeFirst() {
        return remove(0);
    }

    @Override
    public T removeLast() {
        return remove(size() - 1);
    }

    @Override
    public T[] toArray() {
        T[] res = (T[]) new Object[size()];
        for (int i = 0; i < size(); i++) {
            res[i] = (T) array[i];
        }
        return res;
    }

    @Override
    public void sort(Comparator<? super T> comparator) {
        throw new Error("nyi sort"); // todo
    }

    @Override
    public T[] toArray(Object[] arr) {
        if (arr.length < currentSize) {
            return toArray(); // supplied arr is too small, just make a new one
        }

        for (int i = 0; i < size(); i++) {
            arr[i] = (T) array[i];
        }
        return (T[]) arr;
    }

    @Override
    public Stream<T> stream() {
        return new Stream<T>(this) {
        };
    }

    @Override
    public void add(int index, Object elem) {
        if (index > currentSize) {
            throw new ArrayIndexOutOfBoundsException("Index " + index + " with size " + currentSize);
        }
        if (index == currentSize) {
            add(elem);
            return;
        }

        ensureCapacity();

        currentSize++;
        for (int i = currentSize; i > index; i--) {
            swap(i, i-1);
        }

        set(index, (T) elem);
    }

    @Override
    public int hashCode() {
        int res = 0;
        for (int i = 0; i < currentSize; i++) {
            Object o = array[i];
            if (o == null) i+= 12340987; // todo bad probs
            else res+= o.hashCode();
        }
        return res;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof java.util.ArrayList<?> l2) {
            if (l2.size() != size()) return false;
            for (int i = 0; i < size(); i++) {
                if (!Objects.equals(l2.get(i), get(i))) return false;
            }
            return true;
        } else if (obj instanceof ArrayList<?> l2) {
            if (l2.size() != size()) return false;
            for (int i = 0; i < size(); i++) {
                if (!Objects.equals(l2.get(i), get(i))) return false;
            }
            return true;
        }
        return false;

    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder("[");
        for (int i = 0; i < size(); i++) {
            res.append( get(i) == null ? "null" : get(i).toString());
            res.append(", ");
        }
        if (size() > 0)res.delete(res.length()-2, res.length());
        res.append("]");
        return res.toString();
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private int i = 0;
            @Override
            public boolean hasNext() {
                return i < ArrayList.this.size();
            }

            @Override
            public T next() {
                return (T) ArrayList.this.array[i++];
            }
        };
    }

    private void swap(int firstIndex, int secondIndex) {
        Object temp = array[firstIndex];
        array[firstIndex] = array[secondIndex];
        array[secondIndex] = temp;
    }
}
