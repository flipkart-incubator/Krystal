package com.flipkart.krystal.krystex;

import com.flipkart.krystal.krystex.node.NodeInputs;

public record InputsAndResult<T>(NodeInputs nodeInputs, SingleValue<T> result) {}
