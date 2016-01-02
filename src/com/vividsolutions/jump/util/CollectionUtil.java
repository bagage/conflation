/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */
package com.vividsolutions.jump.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CollectionUtil {
    public CollectionUtil() {}

    /**
     * Returns a List of Lists: all combinations of the elements of the given List.
     * @param maxCombinationSize combinations larger than this value are discarded
     * @param mandatoryItem an item that all returned combinations must contain,
     * or null to leave unspecified
     */
    public static <E> List<List<E>> combinations(
        List<E> original,
        int maxCombinationSize,
        E mandatoryItem) {
        List<List<E>> combinations = new ArrayList<>();

        //Combinations are given by the bits of each binary number from 1 to 2^N
        for (int i = 1; i <= ((int) Math.pow(2, original.size()) - 1); i++) {
            List<E> combination = new ArrayList<>();

            for (int j = 0; j < original.size(); j++) {
                if ((i & (int) Math.pow(2, j)) > 0) {
                    combination.add(original.get(j));
                }
            }

            if (combination.size() > maxCombinationSize) {
                continue;
            }

            if ((mandatoryItem != null) && !combination.contains(mandatoryItem)) {
                continue;
            }

            combinations.add(combination);
        }

        return combinations;
    }

    public static void removeKeys(Collection<?> keys, Map<?,?> map) {
        for (Object key : keys) {
            map.remove(key);
        }
    }
}
