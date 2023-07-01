package com.flipkart.krystal.krystex.commands;

public sealed interface NodeInputCommand extends NodeRequestCommand
    permits ExecuteWithInputs, SkipNode {}
