package com.flipkart.krystal.vajram.sql;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


public record SQLResult(List<?> rows, long rowsUpdated) {}
