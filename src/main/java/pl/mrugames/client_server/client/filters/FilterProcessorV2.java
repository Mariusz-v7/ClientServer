package pl.mrugames.client_server.client.filters;

import java.util.List;
import java.util.Optional;

public class FilterProcessorV2 {
    private final List<Filter<?, ?>> filters;

    public FilterProcessorV2(List<Filter<?, ?>> filters) {
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
