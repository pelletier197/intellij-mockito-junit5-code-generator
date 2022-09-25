package com.pelletier197.plugin;

import com.intellij.openapi.components.NamedComponent;
import org.jetbrains.annotations.NotNull;

/**
 * Created by przemek on 8/8/15.
 */
public class MockitoGenPlugin implements NamedComponent {
    @NotNull
    @Override
    public String getComponentName() {
        return "MockitoJunit5Gen";
    }
}
