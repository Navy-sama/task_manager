/**
 * Task module: CRUD of the authenticated user's tasks, with status filter, text search and pagination.
 *
 * <p>Depends only on {@code shared}: the owner is identified by the id carried in the access token, never by a JPA
 * relation to the authentication module's {@code User} entity.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Tasks", allowedDependencies = "shared")
package io.github.navysama.taskmanager.task;
