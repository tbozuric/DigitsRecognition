package hr.fer.zemris.projekt.filters;

import java.util.ArrayList;
import java.util.List;

public class FilterAggregator<T> {
    private List<Filter<T>> filters;


    public FilterAggregator() {
        this.filters = new ArrayList<>();
    }

    public void addFilter(Filter<T> filter) {
        filters.add(filter);
    }

    public boolean removeFilter(Filter<T> filter) {
        return filters.remove(filter);
    }

    public List<Filter<T>> getFilters() {
        return filters;
    }

    public void setFilters(List<Filter<T>> filters) {
        this.filters = filters;
    }
}
