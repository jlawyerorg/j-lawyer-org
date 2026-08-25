/**
 * Applies a user defined order, persisted as a list of ids, to a list of items.
 *
 * Mirrors the desktop client's `StoredOrderUtils`: the stored order is a hint only, so
 * ids that no longer resolve to an item are dropped and items the stored order does not
 * know about (e.g. a mailbox added after the order was saved) are appended at the end.
 * That keeps a stale order harmless instead of hiding entries.
 */
export function applyStoredOrder<T>(
  items: readonly T[] | null | undefined,
  orderedIds: readonly string[] | null | undefined,
  idFn: (item: T) => string,
): T[] {
  if (!items?.length) return [];
  if (!orderedIds?.length) return [...items];

  // keeps lookup cheap and preserves the original relative order of the remainder
  const remaining = new Map<string, T>();
  for (const item of items) {
    const id = idFn(item);
    if (id) remaining.set(id, item);
  }

  const result: T[] = [];
  for (const id of orderedIds) {
    if (!id) continue;
    const item = remaining.get(id);
    if (item !== undefined) {
      remaining.delete(id);
      result.push(item);
    }
  }

  result.push(...remaining.values());
  return result;
}

/**
 * Moves the entry at `from` to position `to`, returning a new array. Used by the
 * drag-and-drop reordering of the mailbox / postbox rows in the sidebars.
 */
export function moveItem<T>(items: readonly T[], from: number, to: number): T[] {
  const result = [...items];
  if (from === to || from < 0 || to < 0 || from >= result.length || to >= result.length) {
    return result;
  }
  const [moved] = result.splice(from, 1);
  result.splice(to, 0, moved);
  return result;
}
