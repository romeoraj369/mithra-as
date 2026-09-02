package com.gamor.mithrax.domain.memory;

/**
 * Coarse local memory kinds. Stored as the enum name so new types can be added
 * without a schema change.
 */
public enum MemoryType {
    FACT,
    DECISION,
    ACTION,
    DEADLINE,
    PERSON,
    PREFERENCE,
    CONTEXT
}
