/**
 * Shared kernel: error model, pagination, current-user access and cross-cutting configuration.
 *
 * <p>Open module: every other module may depend on any of its types.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Shared",
        type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package io.github.navysama.taskmanager.shared;
