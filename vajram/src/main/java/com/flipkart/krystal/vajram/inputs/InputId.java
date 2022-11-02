package com.flipkart.krystal.vajram.inputs;

import com.flipkart.krystal.vajram.das.DataAccessSpec;

public record InputId<T>(DataAccessSpec accessSpec, String inputName) {

}
