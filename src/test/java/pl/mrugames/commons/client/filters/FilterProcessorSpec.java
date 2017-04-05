package pl.mrugames.commons.client.filters;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

@RunWith(BlockJUnit4ClassRunner.class)
public class FilterProcessorSpec {
    private FilterProcessor filterProcessor;
    private List<Filter<Object, Object>> filters;


    @Before
    public void before() {
        filters = new LinkedList<>();
        filterProcessor = spy(new FilterProcessor());
    }

    @Test
    public void givenEmptyFilters_thenReturnOptionalOfInput() {
        Optional<String> result = filterProcessor.filter("Hello", Collections.emptyList());
        assertThat(result).isPresent();

        if (result.isPresent()) {
            assertThat(result.get()).isEqualTo("Hello");
        }
    }

    @Test
    public void givenFilterReturnsNull_thenReturnOptionalEmpty() {
        filters.add(i -> null);
        assertThat(filterProcessor.filter("xxx", filters)).isEmpty();
    }

    @Test
    public void givenFilterReturnsDifferentValue_thenReturnOptionalOfThatValue() {
        filters.add(i -> 123);

        Optional<Integer> result = filterProcessor.filter("xxx", filters);
        assertThat(result).isPresent();

        if (result.isPresent()) {
            assertThat(result.get()).isEqualTo(123);
        }
    }

    @Test
    public void whenMultipleFilters_thenValueFromSecondIsReturned() {
        filters.add(i -> 123);
        filters.add(i -> null);
        filters.add(i -> "Blah!");

        Optional<String> result = filterProcessor.filter(new Object(), filters);
        assertThat(result).isPresent();

        if (result.isPresent()) {
            assertThat(result.get()).isEqualTo("Blah!");
        }
    }
}
