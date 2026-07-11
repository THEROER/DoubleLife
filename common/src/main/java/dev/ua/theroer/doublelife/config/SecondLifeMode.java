package dev.ua.theroer.doublelife.config;

/**
 * Determines what a player's second (admin) inventory looks like when they
 * enter DoubleLife and what happens to it when they leave.
 */
public enum SecondLifeMode {
    /** Second life always starts empty; changes are discarded on exit. Legacy behaviour. */
    EMPTY,
    /** Second life starts from a fixed kit defined on the profile; changes are discarded. */
    KIT,
    /** Second life is a persistent second character whose inventory carries over between sessions. */
    PERSONA
}
