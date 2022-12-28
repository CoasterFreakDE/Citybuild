package dev.lupluv.cb.annotations;

import org.bukkit.permissions.PermissionDefault;

public @interface RegisterCommand {
    String name();
    String description() default "";

    String[] aliases() default {};

    String permission() default "";
    PermissionDefault permissionDefault() default PermissionDefault.TRUE;
}
