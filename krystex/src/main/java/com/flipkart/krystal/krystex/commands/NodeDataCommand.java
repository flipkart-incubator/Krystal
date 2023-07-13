package com.flipkart.krystal.krystex.commands;


public sealed interface NodeDataCommand extends NodeCommand permits BatchNodeCommand,
    GranularNodeCommand {

}
