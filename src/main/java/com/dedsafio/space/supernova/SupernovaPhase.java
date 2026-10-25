package com.dedsafio.space.supernova;

import com.dedsafio.space.config.ModConfig;

public enum SupernovaPhase {
    IDLE, P1_COUNTDOWN, P1_EXPLOSION, P1_AFTERMATH, BETWEEN, P2_COUNTDOWN, P2_EXPLOSION, P2_AFTERMATH;

    public int ticks(ModConfig c) {
        return switch (this) {
            case P1_COUNTDOWN -> c.supernova1CountdownSeconds * 20;
            case P1_EXPLOSION -> c.explosion1Ticks;
            case P1_AFTERMATH -> c.aftermath1Ticks;
            case BETWEEN -> c.betweenTicks;
            case P2_COUNTDOWN -> c.supernova2CountdownSeconds * 20;
            case P2_EXPLOSION -> c.explosion2Ticks;
            case P2_AFTERMATH -> c.aftermath2Ticks;
            default -> 0;
        };
    }

    public boolean isPart1() { return this == P1_COUNTDOWN || this == P1_EXPLOSION || this == P1_AFTERMATH; }
    public boolean isPart2() { return this == P2_COUNTDOWN || this == P2_EXPLOSION || this == P2_AFTERMATH; }
}
