package com.flipkart.krystal.lattice.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.flipkart.krystal.annos.ApplicableToElements;
import com.flipkart.krystal.core.KrystalElement.Vajram;
import com.flipkart.krystal.serial.SerdeProtocol;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
@ApplicableToElements(Vajram.class)
public @interface RemotelyInvocable {}
