package pl.mrugames.client_server.client.filters;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class FilterProcessor {
    public final static FilterProcessor EMPTY_FILTER_PROCESSOR = new FilterProcessor(Collections.emptyList());

    public static FilterProcessor oneFilterFactory(Filter<?, ?> filter) {
        return new FilterProcessor(Collections.singletonList(filter));
    }

    private final List<Filter<?, ?>> filters;

    public FilterProcessor(List<Filter<?, ?>> filters) {
        this.filters = filters;
    }

    @SuppressWarnings("unchecked")
    public <Input, Output> Optional<Output> filter(Input input) {
        if (filters.isEmpty()) {
            return Optional.of((Output) input);
        }

        Object output = input;
        for (Filter filter : filters) {
            output = filter.filter(output);
        }

        return Optional.ofNullable((Output) output);
    }
}
