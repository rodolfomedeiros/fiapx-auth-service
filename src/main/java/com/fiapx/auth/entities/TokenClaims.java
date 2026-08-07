package com.fiapx.auth.entities;

import java.util.UUID;

/** Identidade que um token válido carrega, devolvida pela introspecção. */
public record TokenClaims(UUID subject, String email, String name) {}
