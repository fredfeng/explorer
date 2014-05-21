/* Soot - a J*va Optimization Framework
 * Copyright (C) 2007 Manu Sridharan
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */
package edu.utexas.spark.ondemand.genericutil;

import java.util.Set;

import edu.utexas.spark.ondemand.genericutil.Stack;

public class Propagator<T> {

	private final Set<T> marked;
	
	private final Stack<T> worklist;

	public Propagator(Set<T> marked, Stack<T> worklist) {
		super();
		this.marked = marked;
		this.worklist = worklist;
	}
	
	public void prop(T val) {
		if (marked.add(val)) {
			worklist.push(val);
		}
	}
}