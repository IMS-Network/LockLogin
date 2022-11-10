package eu.locklogin.plugin.bungee.command.util;

/*
 * Private GSA code
 *
 * The use of this code
 * without GSA team authorization
 * will be a violation of
 * terms of use determined
 * in <a href="http://karmaconfigs.cf/license/"> here </a>
 * or (fallback domain) <a href="https://karmaconfigs.github.io/page/license"> here </a>
 */

import eu.locklogin.plugin.bungee.command.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SystemCommand {

    String command() default "";

    String[] aliases() default {""};

    /**
     * Get the plugin command manager
     */
    class manager {

        /**
         * Get a list of recognized system account commands
         *
         * @return a list of system commands
         */
        public static Class<?>[] recognizedClasses() {
            return new Class[]{
                    AccountCommand.class,
                    AliasCommand.class,
                    GoogleAuthCommand.class,
                    LockLoginCommand.class,
                    LoginCommand.class,
                    PanicCommand.class,
                    PinCommand.class,
                    PlayerInfoCommand.class,
                    RegisterCommand.class};
        }

        /**
         * Get the declared aliases of the class
         *
         * @param clazz the command class
         * @return the aliases of the class
         */
        public static List<String> getDeclaredAliases(final Class<?> clazz) {
            if (clazz.isAnnotationPresent(SystemCommand.class)) {
                SystemCommand cmd = clazz.getAnnotation(SystemCommand.class);

                try {
                    return Arrays.asList(cmd.aliases());
                } catch (Throwable ignored) {
                }
            }

            return Collections.emptyList();
        }

        /**
         * Get the declared command of the
         * class
         *
         * @param clazz the command class
         * @return the command of the class
         */
        public static String getDeclaredCommand(final Class<?> clazz) {
            if (clazz.isAnnotationPresent(SystemCommand.class)) {
                SystemCommand cmd = clazz.getAnnotation(SystemCommand.class);

                try {
                    return cmd.command();
                } catch (Throwable ignored) {
                }
            }

            return "";
        }
    }
}
