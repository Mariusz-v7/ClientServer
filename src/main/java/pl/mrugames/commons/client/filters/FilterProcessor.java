package pl.mrugames.commons.client.filters;

import java.util.List;
import java.util.Optional;

public class FilterProcessor {
    private static FilterProcessor instance;

    public synchronized static FilterProcessor getInstance() {
        if (instance == null) {
            instance = new FilterProcessor();
        }

        return instance;
    }

    FilterProcessor() {
    }

    @SuppressWarnings("unchecked")
    public <Input, Output> Optional<Output> filter(Input input, List<Filter<?, ?>> filters) {
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
