package nl.rutgerkok.chestsignprotect;

import org.bukkit.block.Sign;

import com.google.common.base.Optional;

/**
 * Parses a single sign. It essentially converts between {@link Sign} and
 * {@link ProtectionSign}.
 *
 */
public interface SignParser {

    /**
     * Gets the type of the sign.
     *
     * @param header
     *            First line of the sign, may contain extra spaces and color
     *            codes.
     * @return The type of the sign.
     */
    Optional<SignType> getSignType(Sign sign);

    /**
     * Gets whether the sign header is valid. Calling this method is the same as
     * calling {@code getSignType(sign).isPresent()}.
     *
     * @param sign
     *            The sign to check.
     * @return True if the header is valid, false otherwise.
     */
    boolean hasValidHeader(Sign sign);

    /**
     * Parses the given sign for all names on it.
     *
     * @param sign
     *            The sign to parse.
     * @return The parsed sign.
     */
    Optional<ProtectionSign> parseSign(Sign sign);

    /**
     * Saves the contents of the given sign to the world.
     *
     * @param sign
     *            The sign to save.
     * @param saveInformation
     *            Data on the sign to save.
     */
    void saveSign(ProtectionSign sign);

}
