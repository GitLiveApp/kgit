/*
 * Copyright (C) 2011, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;

import org.junit.Test;

public class BlockListTest {
	@Test
	public void testEmptyList() {
		BlockList<String> empty;

		empty = new BlockList<>();
		assertEquals(0, empty.size());
		assertTrue(empty.isEmpty());
		assertFalse(empty.iterator().hasNext());

		empty = new BlockList<>(0);
		assertEquals(0, empty.size());
		assertTrue(empty.isEmpty());
		assertFalse(empty.iterator().hasNext());

		empty = new BlockList<>(1);
		assertEquals(0, empty.size());
		assertTrue(empty.isEmpty());
		assertFalse(empty.iterator().hasNext());

		empty = new BlockList<>(64);
		assertEquals(0, empty.size());
		assertTrue(empty.isEmpty());
		assertFalse(empty.iterator().hasNext());
	}

	@Test
	public void testGet() {
		BlockList<String> list = new BlockList<>(4);

		try {
			list.get(-1);
			fail("accepted out-of-bounds index");
		} catch (IndexOutOfBoundsException badIndex) {
			assertEquals(String.valueOf(-1), badIndex.getMessage());
		}

		try {
			list.get(0);
			fail("accepted out-of-bounds index");
		} catch (IndexOutOfBoundsException badIndex) {
			assertEquals(String.valueOf(0), badIndex.getMessage());
		}

		try {
			list.get(4);
			fail("accepted out-of-bounds index");
		} catch (IndexOutOfBoundsException badIndex) {
			assertEquals(String.valueOf(4), badIndex.getMessage());
		}

		String fooStr = "foo";
		String barStr = "bar";
		String foobarStr = "foobar";

		list.add(fooStr);
		list.add(barStr);
		list.add(foobarStr);

		assertSame(fooStr, list.get(0));
		assertSame(barStr, list.get(1));
		assertSame(foobarStr, list.get(2));

		try {
			list.get(3);
			fail("accepted out-of-bounds index");
		} catch (IndexOutOfBoundsException badIndex) {
			assertEquals(String.valueOf(3), badIndex.getMessage());
		}
	}

	@Test
	public void testSet() {
		BlockList<String> list = new BlockList<>(4);

		try {
			list.set(-1, "foo");
			fail("accepted out-of-bounds index");
		} catch (IndexOutOfBoundsException badIndex) {
			assertEquals(String.valueOf(-1), badIndex.getMessage());
		}

		try {
			list.set(0, "foo");
			fail("accepted out-of-bounds index");
		} catch (IndexOutOfBoundsException badIndex) {
			assertEquals(String.valueOf(0), badIndex.getMessage());
		}

		try {
			list.set(4, "foo");
			fail("accepted out-of-bounds index");
		} catch (IndexOutOfBoundsException badIndex) {
			assertEquals(String.valueOf(4), badIndex.getMessage());
		}

		String fooStr = "foo";
		String barStr = "bar";
		String foobarStr = "foobar";

		list.add(fooStr);
		list.add(barStr);
		list.add(foobarStr);

		assertSame(fooStr, list.get(0));
		assertSame(barStr, list.get(1));
		assertSame(foobarStr, list.get(2));

		assertSame(fooStr, list.set(0, barStr));
		assertSame(barStr, list.set(1, fooStr));

		assertSame(barStr, list.get(0));
		assertSame(fooStr, list.get(1));

		try {
			list.set(3, "bar");
			fail("accepted out-of-bounds index");
		} catch (IndexOutOfBoundsException badIndex) {
			assertEquals(String.valueOf(3), badIndex.getMessage());
		}
	}

	@Test
	public void testAddToEnd() {
		BlockList<Integer> list = new BlockList<>(4);
		int cnt = BlockList.BLOCK_SIZE * 3;

		for (int i = 0; i < cnt; i++)
			list.add(Integer.valueOf(42 + i));
		assertEquals(cnt, list.size());

		for (int i = 0; i < cnt; i++)
			assertEquals(Integer.valueOf(42 + i), list.get(i));

		list.clear();
		assertEquals(0, list.size());
		assertTrue(list.isEmpty());

		for (int i = 0; i < cnt; i++)
			list.add(i, Integer.valueOf(42 + i));
		assertEquals(cnt, list.size());

		for (int i = 0; i < cnt; i++)
			assertEquals(Integer.valueOf(42 + i), list.get(i));
	}

	@Test
	public void testAddSlowPath() {
		BlockList<String> list = new BlockList<>(4);

		String fooStr = "foo";
		String barStr = "bar";
		String foobarStr = "foobar";
		String firstStr = "first";
		String zeroStr = "zero";

		list.add(fooStr);
		list.add(barStr);
		list.add(foobarStr);
		assertEquals(3, list.size());

		list.add(1, firstStr);
		assertEquals(4, list.size());
		assertSame(fooStr, list.get(0));
		assertSame(firstStr, list.get(1));
		assertSame(barStr, list.get(2));
		assertSame(foobarStr, list.get(3));

		list.add(0, zeroStr);
		assertEquals(5, list.size());
		assertSame(zeroStr, list.get(0));
		assertSame(fooStr, list.get(1));
		assertSame(firstStr, list.get(2));
		assertSame(barStr, list.get(3));
		assertSame(foobarStr, list.get(4));
	}

	@Test
	public void testRemoveFromEnd() {
		BlockList<String> list = new BlockList<>(4);

		String fooStr = "foo";
		String barStr = "bar";
		String foobarStr = "foobar";

		list.add(fooStr);
		list.add(barStr);
		list.add(foobarStr);

		assertSame(foobarStr, list.remove(2));
		assertEquals(2, list.size());

		assertSame(barStr, list.remove(1));
		assertEquals(1, list.size());

		assertSame(fooStr, list.remove(0));
		assertEquals(0, list.size());
	}

	@Test
	public void testRemoveSlowPath() {
		BlockList<String> list = new BlockList<>(4);

		String fooStr = "foo";
		String barStr = "bar";
		String foobarStr = "foobar";

		list.add(fooStr);
		list.add(barStr);
		list.add(foobarStr);

		assertSame(barStr, list.remove(1));
		assertEquals(2, list.size());
		assertSame(fooStr, list.get(0));
		assertSame(foobarStr, list.get(1));

		assertSame(fooStr, list.remove(0));
		assertEquals(1, list.size());
		assertSame(foobarStr, list.get(0));

		assertSame(foobarStr, list.remove(0));
		assertEquals(0, list.size());
	}

	@Test
	public void testAddRemoveAdd() {
		BlockList<Integer> list = new BlockList<>();
		for (int i = 0; i < BlockList.BLOCK_SIZE + 1; i++)
			list.add(Integer.valueOf(i));
		assertEquals(Integer.valueOf(BlockList.BLOCK_SIZE),
				list.remove(list.size() - 1));
		assertEquals(Integer.valueOf(BlockList.BLOCK_SIZE - 1),
				list.remove(list.size() - 1));
		assertTrue(list.add(Integer.valueOf(1)));
		assertEquals(Integer.valueOf(1), list.get(list.size() - 1));
	}

	@Test
	public void testAddAllFromOtherList() {
		BlockList<Integer> src = new BlockList<>(4);
		int cnt = BlockList.BLOCK_SIZE * 2;

		for (int i = 0; i < cnt; i++)
			src.add(Integer.valueOf(42 + i));
		src.add(Integer.valueOf(1));

		BlockList<Integer> dst = new BlockList<>(4);
		dst.add(Integer.valueOf(255));
		dst.addAll(src);
		assertEquals(cnt + 2, dst.size());
		for (int i = 0; i < cnt; i++)
			assertEquals(Integer.valueOf(42 + i), dst.get(i + 1));
		assertEquals(Integer.valueOf(1), dst.get(dst.size() - 1));
	}

	@Test
	public void testFastIterator() {
		BlockList<Integer> list = new BlockList<>(4);
		int cnt = BlockList.BLOCK_SIZE * 3;

		for (int i = 0; i < cnt; i++)
			list.add(Integer.valueOf(42 + i));
		assertEquals(cnt, list.size());

		Iterator<Integer> itr = list.iterator();
		for (int i = 0; i < cnt; i++) {
			assertTrue(itr.hasNext());
			assertEquals(Integer.valueOf(42 + i), itr.next());
		}
		assertFalse(itr.hasNext());
	}

	@Test
	public void testAddRejectsBadIndexes() {
		BlockList<Integer> list = new BlockList<>(4);
		list.add(Integer.valueOf(41));
		assertEquals(Integer.valueOf(41), list.get(0));

		try {
			list.add(-1, Integer.valueOf(42));
			fail("accepted out-of-bounds index");
		} catch (IndexOutOfBoundsException badIndex) {
			assertEquals(String.valueOf(-1), badIndex.getMessage());
		}

		try {
			list.add(4, Integer.valueOf(42));
			fail("accepted out-of-bounds index");
		} catch (IndexOutOfBoundsException badIndex) {
			assertEquals(String.valueOf(4), badIndex.getMessage());
		}
	}

	@Test
	public void testRemoveRejectsBadIndexes() {
		BlockList<Integer> list = new BlockList<>(4);
		list.add(Integer.valueOf(41));
		assertEquals(Integer.valueOf(41), list.get(0));

		try {
			list.remove(-1);
			fail("accepted out-of-bounds index");
		} catch (IndexOutOfBoundsException badIndex) {
			assertEquals(String.valueOf(-1), badIndex.getMessage());
		}

		try {
			list.remove(4);
			fail("accepted out-of-bounds index");
		} catch (IndexOutOfBoundsException badIndex) {
			assertEquals(String.valueOf(4), badIndex.getMessage());
		}
	}
}
