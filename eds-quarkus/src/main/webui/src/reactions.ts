/**
 * What a member gave a message, and the entry which says so. The stone
 * replaces that entry with every change, and what it announces of the old one
 * may arrive after the new one is known: the entry tells them apart.
 */
export interface Given {
  entryId: string;
  position: number;
  reactions: string[];
}

/** What is needed of an entry of the kind reaction. */
export interface ReactionEntry {
  entryId: string;
  position: number;
  memberId?: string;
  body: { reactions?: unknown[] } | null;
}

/**
 * Notes an entry of reactions, or its end, in what the members gave one
 * message. Kept apart from the chat so it can be checked without a stone
 * (scripts/test-features.ts).
 */
export function noteGiven(byMember: Map<string, Given>, entry: ReactionEntry, removed: boolean): void {
  if (!entry.memberId) {
    return;
  }
  const known = byMember.get(entry.memberId);
  if (removed) {
    // Only the entry which is known takes the reactions with it: an earlier
    // one was replaced, and its end says nothing of them.
    if (known?.entryId === entry.entryId) {
      byMember.delete(entry.memberId);
    }
  } else if (!known || entry.position >= known.position) {
    // Kept also when it has no reactions, so an earlier entry which arrives
    // late doesn't bring its own back.
    const reactions = (entry.body?.reactions ?? []).filter((reaction): reaction is string => typeof reaction === 'string');
    byMember.set(entry.memberId, { entryId: entry.entryId, position: entry.position, reactions });
  }
}
