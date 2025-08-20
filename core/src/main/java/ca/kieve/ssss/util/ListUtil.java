package ca.kieve.ssss.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ListUtil {
    private ListUtil() {
        // Do not instantiate
    }

    public static Object[] flatten(Object ...objects) {
        return Arrays.stream(objects)
            .map((obj) -> {
                if (obj instanceof List) {
                    return flattenToNonList((List<?>) obj);
                } else {
                    return List.of(obj);
                }
            }).flatMap(Collection::stream)
            .toArray();
    }

    private static List<Object> flattenToNonList(List<?> in) {
        List<Object> result = new ArrayList<>();

        in.forEach((obj) -> {
            if (obj instanceof List<?> list) {
                result.addAll(flattenToNonList(list));
            } else {
                result.add(obj);
            }
        });
        return result;
    }
}
