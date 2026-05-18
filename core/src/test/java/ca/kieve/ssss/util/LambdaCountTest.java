package ca.kieve.ssss.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LambdaCountTest {
    @Test
    void defaultsToZero() {
        LambdaCount count = new LambdaCount();

        assertEquals(0, count.get());
    }

    @Test
    void initialValueRespected() {
        LambdaCount count = new LambdaCount(5);

        assertEquals(5, count.get());
    }

    @Test
    void incrementAdvancesByOne() {
        LambdaCount count = new LambdaCount();

        count.increment();
        count.increment();

        assertEquals(2, count.get());
    }

    @Test
    void incrementAndGetReturnsTheNewValue() {
        LambdaCount count = new LambdaCount(10);

        assertEquals(11, count.incrementAndGet());
        assertEquals(11, count.get());
    }

    @Test
    void getAndIncrementReturnsTheOldValue() {
        LambdaCount count = new LambdaCount(10);

        assertEquals(10, count.getAndIncrement());
        assertEquals(11, count.get());
    }

    @Test
    void addAppliesDelta() {
        LambdaCount count = new LambdaCount(3);

        count.add(7);
        count.add(-2);

        assertEquals(8, count.get());
    }

    @Test
    void worksInsideALambda() {
        // The whole reason this class exists.
        LambdaCount count = new LambdaCount();
        List<Integer> values = List.of(1, 2, 3, 4, 5);

        values.forEach(v -> {
            if (v % 2 == 0) {
                count.increment();
            }
        });

        assertEquals(2, count.get());
    }
}
