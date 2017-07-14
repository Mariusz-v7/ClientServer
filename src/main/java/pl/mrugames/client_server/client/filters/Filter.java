package pl.mrugames.client_server.client.filters;

import javax.annotation.Nullable;

public interface Filter<Input, Output> {
    @Nullable
    Output filter(@Nullable Input input);
}
