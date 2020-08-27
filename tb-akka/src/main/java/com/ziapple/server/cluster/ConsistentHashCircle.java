/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ziapple.server.cluster;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * 一致性Hash环，保证同样的Hash得到同样的ServerInstance
 *  * n 台服务器从0到n-1编号，将值的关键字哈希后对n取余便得到了服务器的编号。
 *  * 在一致性哈希算法中，服务器也和关键字一样进行哈希。哈希空间足够大（一般取[0,2^32）)，并且被当作一个收尾相接的环（哈希环的由来），
 *  * 对服务器进行哈希就相当于将服务器放置在这个哈希环上。当我们需要查找一个关键字时，将它哈希（就是把它也定位到环上），
 *  * 然后沿着哈希环顺时针移动直到找到下一台服务器，当到达哈希环尾端后仍然找不到服务器时，使用第一台服务器。
 *  * 理论上这样就搞定上面说到的问题啦，但是在实践中，经过哈希后的服务器经常在环上聚集起来，这就会使得第一台服务器的压力大于其它服务器。
 *  * 这可以通过让服务器在环上分布得更均匀来改善。具体通过以下做法来实现：引入虚拟节点的概念，通过replica count（副本数）
 *  * 来控制每台物理服务器对应的虚节点数，当我们要添加一台服务器时，从0 到 replica count - 1 循环，
 *  * 将哈希关键字改为服务器关键字加上虚节点编号（hash(ser_str#1),hash(ser_str#2)...）生成虚节点的位置，将虚节点放置到哈希环上。
 *  * 这样做能有效地将服务器均匀分配到环上。注意到这里所谓的服务器副本是虚拟的节点，
 *  * 所以完全不涉及服务器之间的数据同步问题（简单地讲，就是现在变成了二段映射，先找到虚节点，然后再根据虚节点找到对应的物理机）
 */
@Slf4j
public class ConsistentHashCircle {
    private final ConcurrentNavigableMap<Long, ServerInstance> circle =
            new ConcurrentSkipListMap<>();

    public void put(long hash, ServerInstance instance) {
        circle.put(hash, instance);
    }

    public void remove(long hash) {
        circle.remove(hash);
    }

    public boolean isEmpty() {
        return circle.isEmpty();
    }

    public boolean containsKey(Long hash) {
        return circle.containsKey(hash);
    }

    public ConcurrentNavigableMap<Long, ServerInstance> tailMap(Long hash) {
        return circle.tailMap(hash);
    }

    public Long firstKey() {
        return circle.firstKey();
    }

    public ServerInstance get(Long hash) {
        return circle.get(hash);
    }

    public void log() {
        circle.entrySet().forEach((e) -> log.debug("{} -> {}", e.getKey(), e.getValue().getServerAddress()));
    }
}
