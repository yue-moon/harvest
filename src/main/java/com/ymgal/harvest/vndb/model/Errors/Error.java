package com.ymgal.harvest.vndb.model.Errors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ymgal.harvest.vndb.Interfaces.IVndbError;

/// <summary>
///		Represents an Error, with the most basic Id and Message
/// </summary>
public abstract class Error implements IVndbError {
    /// <inheritdoc cref="IVndbError.Type"/>
    @JsonProperty("id")
    public ErrorType Type;
    /// <inheritdoc cref="IVndbError.Message"/>
    @JsonProperty("msg")
    public String Message;
}

