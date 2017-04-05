package pl.mrugames.commons.client.filters;

import com.sun.istack.internal.Nullable;

public interface Filter<Input, Output> {
    @Nullable
    Output filter(@Nullable Input input);
}
