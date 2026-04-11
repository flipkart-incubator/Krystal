package com.flipkart.krystal.model;

import java.util.List;

public interface UnmodifiableModelList<M extends Model, I extends ImmutableModel> extends List<M> {}
