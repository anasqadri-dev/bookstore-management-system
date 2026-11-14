package com.power_puff.architecturefixtures.controller;

import com.power_puff.architecturefixtures.repository.InvalidRepository;

/** Deliberately invalid test fixture used only by the architecture rule's negative test. */
public class InvalidController {
    private final InvalidRepository repository = new InvalidRepository();

    public InvalidRepository repository() {
        return repository;
    }
}
