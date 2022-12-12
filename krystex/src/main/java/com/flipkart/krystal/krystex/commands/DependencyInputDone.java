package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.Node;

/**
 * @param node the node whose input is DONE
 * @param inputName The input name of the input which is DONE
 */
public record DependencyInputDone(Node<?> node, String inputName) implements NodeCommand {}
