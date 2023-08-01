package com.valuephone.image.helper;

/**
 * @author tcigler
 * @since 1.0
 */
public class ValuePair<F, S> {

    private final F firstValue;
    private final S secondValue;

    /**
     * @param firstValue
     * @param secondValue
     */
    public ValuePair(F firstValue, S secondValue) {
        this.firstValue = firstValue;
        this.secondValue = secondValue;
    }

    public F getFirstValue() {
        return firstValue;
    }

    public S getSecondValue() {
        return secondValue;
    }
}
