import { t } from './i18n';
import { read, write } from './settings';

/**
 * The arrangement of Rune's panes on a desktop, chosen by each person and
 * kept on the device: a tree of splits, side by side (row) or above each
 * other (column), with the panes as leaves. Panes that aren't in the tree
 * are hidden. Phones show one pane at a time instead.
 */
export type PaneId = 'rooms' | 'chat' | 'files' | 'calendar' | 'plans' | 'location';
export type Side = 'left' | 'right' | 'top' | 'bottom';
export type Split = { dir: 'row' | 'column'; children: LayoutNode[]; sizes: number[] };
export type LayoutNode = { pane: PaneId } | Split;

export const PANES: PaneId[] = ['rooms', 'chat', 'files', 'calendar', 'plans', 'location'];

/** The panes of features a room may have beside chat and files; they show when asked for. */
export const FEATURE_PANES: PaneId[] = ['calendar', 'plans', 'location'];

/** Panes Rune had once, which a kept arrangement may still name: the server settings are a window now. */
const FORMER_PANES = ['server'];

export function paneName(pane: PaneId): string {
  const names: Record<PaneId, string> = {
    rooms: t('Rooms and users'), chat: t('Chat'), files: t('Files'), calendar: t('Calendar'), plans: t('Plans'), location: t('Location'),
  };
  return names[pane];
}

// Also named in settings.ts (ACCOUNT_SETTINGS): the layout is stored with the account.
const KEY = 'rune.layout';

const leaf = (pane: PaneId): LayoutNode => ({ pane });

export interface Preset {
  name: string;
  layout: LayoutNode;
}

export function presets(): Preset[] {
  return [
    { name: t('Three columns'), layout: { dir: 'row', children: [leaf('rooms'), leaf('chat'), leaf('files')], sizes: [0.22, 0.43, 0.35] } },
    { name: t('Chat on top'), layout: { dir: 'column', children: [leaf('chat'), { dir: 'row', children: [leaf('rooms'), leaf('files')], sizes: [0.4, 0.6] }], sizes: [0.6, 0.4] } },
    { name: t('Rooms beside chat over files'), layout: { dir: 'row', children: [leaf('rooms'), { dir: 'column', children: [leaf('chat'), leaf('files')], sizes: [0.6, 0.4] }], sizes: [0.25, 0.75] } },
    { name: t('Chat and files'), layout: { dir: 'row', children: [leaf('chat'), leaf('files')], sizes: [0.55, 0.45] } },
    { name: t('Chat only'), layout: leaf('chat') },
  ];
}

export function defaultLayout(): LayoutNode {
  return presets()[0].layout;
}

export function isSplit(node: LayoutNode): node is Split {
  return 'dir' in node;
}

/** The panes shown, in reading order. */
export function panesIn(node: LayoutNode): PaneId[] {
  return isSplit(node) ? node.children.flatMap(panesIn) : [node.pane];
}

function valid(node: unknown, seen: Set<string>): node is LayoutNode {
  if (!node || typeof node !== 'object') {
    return false;
  }
  const n = node as Record<string, unknown>;
  if (typeof n.pane === 'string') {
    if (!(PANES as string[]).concat(FORMER_PANES).includes(n.pane) || seen.has(n.pane)) {
      return false;
    }
    seen.add(n.pane);
    return true;
  }
  return (n.dir === 'row' || n.dir === 'column')
    && Array.isArray(n.children) && Array.isArray(n.sizes)
    && n.children.length >= 2 && n.children.length === n.sizes.length
    && n.sizes.every((s) => typeof s === 'number' && s > 0)
    && n.children.every((c) => valid(c, seen));
}

export function loadLayout(): LayoutNode {
  try {
    const parsed: unknown = JSON.parse(read(KEY) ?? 'null');
    if (valid(parsed, new Set())) {
      let layout: LayoutNode | null = normalize(parsed);
      for (const former of FORMER_PANES) {
        layout = layout && removePane(layout, former as PaneId);
      }
      return layout ?? defaultLayout();
    }
  } catch {
    // A broken setting falls back to the default.
  }
  return defaultLayout();
}

export function saveLayout(node: LayoutNode): void {
  write(KEY, JSON.stringify(node));
}

export function resetLayout(): LayoutNode {
  write(KEY, null);
  return defaultLayout();
}

/**
 * Tidies a tree: a split with one child becomes the child, a split in a
 * split of the same direction is merged into it, and sizes add up to 1.
 */
export function normalize(node: LayoutNode): LayoutNode {
  if (!isSplit(node)) {
    return node;
  }
  const children: LayoutNode[] = [];
  const sizes: number[] = [];
  node.children.forEach((child, i) => {
    const tidy = normalize(child);
    if (isSplit(tidy) && tidy.dir === node.dir) {
      tidy.children.forEach((grandchild, j) => {
        children.push(grandchild);
        sizes.push(node.sizes[i] * tidy.sizes[j]);
      });
    } else {
      children.push(tidy);
      sizes.push(node.sizes[i]);
    }
  });
  if (children.length === 1) {
    return children[0];
  }
  const total = sizes.reduce((a, b) => a + b, 0);
  return { dir: node.dir, children, sizes: sizes.map((s) => s / total) };
}

/** The tree without the pane, or null if it was the only one. */
export function removePane(node: LayoutNode, pane: PaneId): LayoutNode | null {
  if (!isSplit(node)) {
    return node.pane === pane ? null : node;
  }
  const children: LayoutNode[] = [];
  const sizes: number[] = [];
  let freed = 0;
  node.children.forEach((child, i) => {
    const rest = removePane(child, pane);
    if (rest) {
      children.push(rest);
      sizes.push(node.sizes[i]);
    } else {
      freed += node.sizes[i];
    }
  });
  // The place which got free goes to the panes with content: the rooms pane keeps its size.
  const takers = children.map((child, i) => i).filter((i) => !isRooms(children[i]));
  const share = takers.reduce((sum, i) => sum + sizes[i], 0);
  if (freed > 0 && share > 0) {
    takers.forEach((i) => { sizes[i] += freed * sizes[i] / share; });
  }
  return children.length === 0 ? null : normalize({ dir: node.dir, children, sizes });
}

/** The rooms pane by itself: where one chooses, which keeps its size when other panes come and go. */
export function isRooms(node: LayoutNode): boolean {
  return !isSplit(node) && node.pane === 'rooms';
}

// The arrangement from before a pane was put away, on this device.
const WITH = 'rune.layout.';

/**
 * Puts a pane away, or brings it back to where it was and as large as it
 * was. When the other panes were arranged anew meanwhile, the rooms pane
 * comes back at the left edge, another pane at the right. The only pane
 * shown stays.
 */
export function togglePane(node: LayoutNode, pane: PaneId): LayoutNode {
  if (panesIn(node).includes(pane)) {
    const rest = removePane(node, pane);
    if (!rest) {
      return node;
    }
    write(WITH + pane, JSON.stringify(node));
    return rest;
  }
  try {
    const kept: unknown = JSON.parse(read(WITH + pane) ?? 'null');
    if (valid(kept, new Set()) && panesIn(kept).includes(pane)) {
      const rest = removePane(normalize(kept), pane);
      if (rest && JSON.stringify(rest) === JSON.stringify(node)) {
        return normalize(kept);
      }
    }
  } catch {
    // Then at the edge.
  }
  return pane === 'rooms' ? normalize({ dir: 'row', children: [leaf('rooms'), node], sizes: [0.22, 0.78] }) : addPane(node, pane);
}

/** Puts the rooms pane away, or brings it back. */
export function toggleRooms(node: LayoutNode): LayoutNode {
  return togglePane(node, 'rooms');
}

/** Puts the pane beside the target pane, on the given side. */
export function insertPane(node: LayoutNode, pane: PaneId, target: PaneId, side: Side): LayoutNode {
  const dir = side === 'left' || side === 'right' ? 'row' : 'column';
  const before = side === 'left' || side === 'top';
  const place = (n: LayoutNode): LayoutNode => {
    if (!isSplit(n)) {
      return n.pane === target
        ? { dir, children: before ? [leaf(pane), n] : [n, leaf(pane)], sizes: [0.5, 0.5] }
        : n;
    }
    const i = n.children.findIndex((c) => !isSplit(c) && c.pane === target);
    if (i >= 0 && n.dir === dir) {
      // Beside the target in the same direction: the target gives half its room.
      const children = [...n.children];
      const sizes = [...n.sizes];
      const half = sizes[i] / 2;
      sizes[i] = half;
      children.splice(before ? i : i + 1, 0, leaf(pane));
      sizes.splice(before ? i : i + 1, 0, half);
      return { dir: n.dir, children, sizes };
    }
    return { dir: n.dir, children: n.children.map(place), sizes: n.sizes };
  };
  return normalize(place(node));
}

/** Moves a pane next to another one. */
export function movePane(node: LayoutNode, pane: PaneId, target: PaneId, side: Side): LayoutNode {
  if (pane === target) {
    return node;
  }
  const rest = removePane(node, pane);
  return rest ? insertPane(rest, pane, target, side) : node;
}

/** Puts a pane along a whole edge of the window, e.g. the chat across the top. */
export function dockPane(node: LayoutNode, pane: PaneId, side: Side): LayoutNode {
  const rest = removePane(node, pane);
  if (!rest) {
    return node;
  }
  const dir = side === 'left' || side === 'right' ? 'row' : 'column';
  const before = side === 'left' || side === 'top';
  const share = dir === 'row' ? 0.3 : 0.5;
  return normalize({ dir, children: before ? [leaf(pane), rest] : [rest, leaf(pane)], sizes: before ? [share, 1 - share] : [1 - share, share] });
}

/** Swaps the places of two panes. */
export function swapPanes(node: LayoutNode, a: PaneId, b: PaneId): LayoutNode {
  if (!isSplit(node)) {
    return node.pane === a ? leaf(b) : node.pane === b ? leaf(a) : node;
  }
  return { dir: node.dir, children: node.children.map((c) => swapPanes(c, a, b)), sizes: node.sizes };
}

/** Shows a hidden pane again, at the right. */
export function addPane(node: LayoutNode, pane: PaneId): LayoutNode {
  if (panesIn(node).includes(pane)) {
    return node;
  }
  if (isSplit(node) && node.dir === 'row') {
    // The new pane takes its place from the panes with content, an equal share of theirs; the rooms pane keeps its size.
    const givers = node.children.map((child, i) => i).filter((i) => !isRooms(node.children[i]));
    const theirs = givers.reduce((sum, i) => sum + node.sizes[i], 0);
    if (givers.length > 0 && theirs > 0) {
      const share = theirs / (givers.length + 1);
      const sizes = node.sizes.map((size, i) => (givers.includes(i) ? size * (theirs - share) / theirs : size));
      return normalize({ dir: 'row', children: [...node.children, leaf(pane)], sizes: [...sizes, share] });
    }
    const n = node.children.length;
    return normalize({ dir: 'row', children: [...node.children, leaf(pane)], sizes: [...node.sizes.map((s) => (s * n) / (n + 1)), 1 / (n + 1)] });
  }
  return { dir: 'row', children: [node, leaf(pane)], sizes: isRooms(node) ? [0.22, 0.78] : [0.65, 0.35] };
}
