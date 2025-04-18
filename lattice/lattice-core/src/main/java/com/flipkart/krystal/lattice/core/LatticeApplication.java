package com.flipkart.krystal.lattice.core;

/**
 * A lattice application hosts a Krystal graph as a process. As part of the application setup, it
 * allows application owners to add functionality to the graph and control how the graph executes
 * (via {@link Dopant dopants})
 *
 * <p>For example, an application owner might choose to "dope" their application with a "Server
 * Dopant" which exposes some of the hosted vajrams for invocation from outside the application
 * process - thus converting the krystal graph into a web server.
 */
public interface LatticeApplication {}
