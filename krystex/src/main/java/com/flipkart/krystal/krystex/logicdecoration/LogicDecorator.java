package com.flipkart.krystal.krystex.logicdecoration;

import com.flipkart.krystal.config.ConfigListener;
import com.flipkart.krystal.krystex.decoration.Decorator;

public sealed interface LogicDecorator extends Decorator, ConfigListener
    permits OutputLogicDecorator {}
