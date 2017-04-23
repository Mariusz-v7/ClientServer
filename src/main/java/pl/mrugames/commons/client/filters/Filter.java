package pl.mrugames.commons.client.filters;

import javax.annotation.Nullable;

public interface Filter<Input, Output> {
    @Nullable
    Output filter(@Nullable Input input);
}
