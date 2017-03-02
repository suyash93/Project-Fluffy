/**
 * Copyright 2016 Gash.
 * <p>
 * This file and intellectual content is protected under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package gash.router.global.edges;

import global.Global;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalEdgeList {
	public ConcurrentHashMap<String, GlobalEdgeInfo> map = new ConcurrentHashMap<String, GlobalEdgeInfo>();
	private GlobalEdgeListener listener;

	public GlobalEdgeList() {
	}

	public GlobalEdgeInfo createIfNew(int ref, String host, int port) {
		if (hasNode(host))
			return getNode(host);
		else
			return addNode(ref, host, port);
	}

	public GlobalEdgeInfo addNode(int ref, String host, int port) {
		if (!verify(ref, host, port)) {
			// TODO log error
			throw new RuntimeException("Invalid node info");
		}

		if (!hasNode(host)) {
			GlobalEdgeInfo ei = new GlobalEdgeInfo(ref, host, port);
			map.put(host, ei);
			if (listener != null)
				listener.onAdd(ei);
			return ei;
		} else
			return null;
	}

	private boolean verify(int ref, String host, int port) {
		if (ref < 0 || host == null || port < 1024)
			return false;
		else
			return true;
	}

	public boolean hasNode(String host) {
		return map.containsKey(host);

	}

	public GlobalEdgeInfo getNode(String host) {
		return map.get(host);
	}

	public void removeNode(String host) {
		map.remove(host);
		if (listener != null)
			listener.onRemove(getNode(host));
	}

	public void clear() {
		map.clear();
	}

	/* Returns the mapping of every node with other nodes in the cluster or network*/

	public ConcurrentHashMap<String, GlobalEdgeInfo> getEdgeListMap() {

		return this.map;
	}

	public boolean isEmpty() {
		if (map.size() > 0) {
			return false;
		}
		return true;
	}

	public void addListener(GlobalEdgeListener listener) {
		this.listener = listener;
	}

	public int size() {
		return map.size();
	}
}
