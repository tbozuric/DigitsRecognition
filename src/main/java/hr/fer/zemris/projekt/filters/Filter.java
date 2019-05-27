package hr.fer.zemris.projekt.filters;

import java.util.function.Predicate;

public class Filter<T> {
    private Predicate<T> filter;

    public Filter(Predicate<T> filter) {
        this.filter = filter;
    }

    public Predicate<T> getFilter() {
        return filter;
    }

    public void setFilter(Predicate<T> filter) {
        this.filter = filter;
    }
}
