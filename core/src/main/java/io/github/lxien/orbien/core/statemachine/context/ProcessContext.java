package io.github.lxien.orbien.core.statemachine.context;

import java.util.Map;

public interface ProcessContext {
    Object getVariable(String name);

    void setVariable(String name, Object value);

    Map<String, Object> getVariables();

    void setVariables(Map<String, Object> variables);

    Object removeVariable(String name);

    boolean hasVariable(String name);

    <T> T getVariableAsSafe(String name, Class<T> clazz);
}