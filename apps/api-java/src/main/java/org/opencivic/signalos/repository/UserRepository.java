package org.opencivic.signalos.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    /**
     * Just enough to render a byline: two columns, no managed entity.
     *
     * <p>Room messages each carry an author, and a page holds up to 200 of them. Resolving those names
     * with {@code findById} inside the render loop cost one query and one fully managed {@code User}
     * - password hash, roles and all - per message. Two columns projected avoids both, and leaves the
     * entity load count flat as the page grows, which is what makes that visible in a test.
     */
    List<DisplayName> findDisplayNamesByIdIn(Collection<UUID> ids);

    interface DisplayName {
        UUID getId();

        String getDisplayName();

        String getUsername();
    }
}