package io.github.lxien.orbien.core.statemachine.context;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProcessContextImpl implements ProcessContext {
    private final Map<String, Object> variables = new ConcurrentHashMap<>();

    @Override
    public Object getVariable(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name);
        }
        return null;
    }

    @Override
    public void setVariable(String name, Object value) {
        if (!variables.containsKey(name)) {
            variables.put(name, value);
        }
    }

    @Override
    public Map<String, Object> getVariables() {
        return new HashMap<>(variables);
    }

    @Override
    public void setVariables(final Map<String, Object> variables) {
        if (variables != null) {
            variables.forEach(this::setVariable);
        }
    }

    @Override
    public boolean hasVariable(String name) {
        return variables.containsKey(name);
    }

    public <T> T getAndRemoveAs(String name, Class<T> clazz) {
        Object value = variables.remove(name);
        return value != null ? clazz.cast(value) : null;
    }
    @Override
    public <T> T getVariableAsSafe(String name, Class<T> clazz) {
        Object value = getVariable(name);
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        return null;
    }
    @Override
    public Object removeVariable(String name) {
        if (variables.containsKey(name)) {
            return variables.remove(name);
        }
        return null;
    }
}