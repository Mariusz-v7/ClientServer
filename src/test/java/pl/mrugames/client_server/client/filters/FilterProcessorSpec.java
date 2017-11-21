package pl.mrugames.client_server.client.filters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

class FilterProcessorSpec {
    private FilterProcessor filterProcessor;
    private List<Filter<?, ?>> filters;


    @BeforeEach
    void before() {
        filters = new LinkedList<>();
        filterProcessor = spy(new FilterProcessor());
    }

    @Test
    void givenEmptyFilters_thenReturnOptionalOfInput() {
        Optional<String> result = filterProcessor.filter("Hello", Collections.emptyList());
        assertThat(result).isPresent();

        result.ifPresent(s -> assertThat(s).isEqualTo("Hello"));
    }

    @Test
    void givenFilterReturnsNull_thenReturnOptionalEmpty() {
        filters.add(i -> null);
        assertThat(filterProcessor.filter("xxx", filters)).isEmpty();
    }

    @Test
    void givenFilterReturnsDifferentValue_thenReturnOptionalOfThatValue() {
        filters.add(i -> 123);

        Optional<Integer> result = filterProcessor.filter("xxx", filters);
        assertThat(result).isPresent();

        result.ifPresent(integer -> assertThat(integer).isEqualTo(123));
    }

    @Test
    void whenMultipleFilters_thenValueFromLastIsReturned() {
        filters.add(i -> 123);
        filters.add(i -> null);
        filters.add(i -> "Blah!");

        Optional<String> result = filterProcessor.filter(new Object(), filters);
        assertThat(result).isPresent();

        result.ifPresent(s -> assertThat(s).isEqualTo("Blah!"));
    }

    @Test
    void fullExampleOfFiltering() {
        filters.add(i -> (int) i + 10);
        filters.add(i -> (int) i * 2.1);
        filters.add(i -> (double) i + 1);
        filters.add(i -> String.valueOf((double) i));
        filters.add(s -> "Hello " + s);

        Optional<String> result = filterProcessor.filter(0, filters);
        assertThat(result).isPresent();

        result.ifPresent(s -> assertThat(s).isEqualTo("Hello " + Double.toString(10 * 2.1 + 1)));
    }
}
