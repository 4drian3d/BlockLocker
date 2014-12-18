package nl.rutgerkok.chestsignprotect;

import java.util.List;

import nl.rutgerkok.chestsignprotect.profile.Profile;

import org.bukkit.Location;

/**
 * Represents the information on a protection sign in the world. Instances of
 * this interface must be immutable.
 *
 */
public interface ProtectionSign {

    /**
     * Gets the type of this sign. The type of the sign depends on the header of
     * the sign.
     *
     * @return The type.
     */
    SignType getType();

    /**
     * Gets all profiles currently on the sign. The list will have one, two or
     * three profiles. In other words: it is always save to call
     * {@code getProfiles().get(0)}.
     *
     * @return All profiles.
     */
    List<Profile> getProfiles();

    /**
     * Creates a new protection sign object with the given profiles. Existing
     * profiles are erased. If this sign is not of the type
     * {@link SignType#MORE_USERS} the first entry of the list will become the
     * owner.
     *
     * <p>
     * This object is immutable and will not be modified. The actual sign in the
     * world will not be modified too. You must save the returned
     * {@link ProtectionSign} using {@link SignParser#saveSign(ProtectionSign)}.
     *
     * 
     * @param profiles
     *            The profiles for the protection sign.
     * @return The new object. * @throws NullPointerException If any entry in
     *         the list is 0.
     * @throws IllegalArgumentException
     *             If the list is empty, or if the list has a size larger than
     *             3.
     */
    ProtectionSign withProfiles(List<Profile> profiles);

    /**
     * Gets the location of this sign.
     *
     * @return The location.
     */
    Location getLocation();

}
