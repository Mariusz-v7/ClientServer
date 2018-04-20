package pl.mrugames.nucleus.server.client.filters;

import javax.annotation.Nullable;

public interface Filter<Input, Output> {
    @Nullable
    Output filter(@Nullable Input input);
}
