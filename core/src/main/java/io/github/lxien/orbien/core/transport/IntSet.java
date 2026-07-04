/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.lxien.orbien.core.transport;

import java.util.BitSet;
import java.util.stream.IntStream;

public class IntSet {
    private final BitSet bitSet;
    
    public IntSet() {
        this.bitSet = new BitSet();
    }
    
    public void add(int value) {
        bitSet.set(value);
    }
    
    public void remove(int value) {
        bitSet.clear(value);
    }
    
    public boolean contains(int value) {
        return bitSet.get(value);
    }
    
    public int size() {
        return bitSet.cardinality();
    }
    
    public IntStream stream() {
        return IntStream.range(0, bitSet.length()).filter(bitSet::get);
    }
    
    public boolean isEmpty() {
        return bitSet.isEmpty();
    }
}