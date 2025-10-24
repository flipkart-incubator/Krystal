package com.flipkart.krystal.vajram.sql.r2dbc;

import io.r2dbc.spi.Result;
import java.util.List;
import reactor.core.publisher.Mono;

public interface RowMapper {
  <T> Mono<List<T>> map(Result result, Class<T> type);
}
