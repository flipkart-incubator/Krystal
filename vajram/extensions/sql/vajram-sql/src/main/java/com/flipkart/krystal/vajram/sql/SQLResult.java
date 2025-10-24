package com.flipkart.krystal.vajram.sql;

import java.util.List;

public record SQLResult(List<?> rows, long rowsUpdated) {}
