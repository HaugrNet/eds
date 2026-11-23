import { h } from '../dom';
import { t } from '../i18n';
import { dockPane, isRooms, isSplit, loadLayout, movePane, PANES, paneName, panesIn, saveLayout, swapPanes, toggleRooms, type LayoutNode, type PaneId, type Side, type Split } from '../layout';

/** The width of the dividers, in pixels: a thin line, the only border between two panes. */
const GAP = 1;
/** The width of the strip of the rooms pane put away. */
const STRIP = 34;
/** The smallest a pane gets by dragging a divider. */
const MIN_WIDTH = 220;
const MIN_HEIGHT = 140;
/** Phones show one pane at a time, chosen in the header. */
const PHONE = window.matchMedia('(max-width: 760px)');

interface Rect { x: number; y: number; w: number; h: number }
type Zone = Side | 'center';
/** Near the window's edge, a pane is put along the whole edge. */
const EDGE = 28;
type Drop = { target: PaneId | 'edge'; zone: Zone };

/**
 * Places the panes as the layout says. The panes stay where they are in the
 * page and are only positioned, so they keep their state (a moved custom
 * element would run its setup again). Dividers resize; dragging a pane's
 * bar onto another pane puts it beside it (near an edge) or swaps the two
 * (in the middle). Which panes show is chosen in the settings (Panes, in
 * the account menu). Fires `layout` when the arrangement changed.
 */
export class Workspace {
  readonly element = h('main', { class: 'panes' });
  private frames = new Map<PaneId, HTMLElement>();
  private splitters: HTMLElement[] = [];
  private hint = h('div', { class: 'drop-hint', hidden: true });
  private layout: LayoutNode = loadLayout();
  /** Puts the rooms pane away, to a strip at the left edge; the strip brings it back. */
  private minimize = h('button', {
    type: 'button', class: 'pane-minimize', title: t('Minimize'), 'aria-label': t('Minimize'),
    onclick: () => this.set(toggleRooms(this.layout)),
  }, h('span', { 'aria-hidden': 'true' }, '«'));
  private strip = h('button', {
    type: 'button', class: 'rooms-strip', hidden: true, title: t('Show rooms and users'), 'aria-label': t('Show rooms and users'),
    onclick: () => this.set(toggleRooms(this.layout)),
  }, h('span', { class: 'strip-open', 'aria-hidden': 'true' }, '»'), h('span', { class: 'strip-name' }, paneName('rooms')));

  constructor(panes: Record<PaneId, HTMLElement>) {
    for (const pane of PANES) {
      const bar = h('div', { class: 'pane-bar', title: t('Drag onto another pane to move it') },
        h('span', { class: 'grip', 'aria-hidden': 'true' }, '⠿'), h('span', { class: 'pane-name' }, paneName(pane)));
      if (pane !== 'rooms') {
        // Shows on the panes with the content of a chamber (styles.css).
        bar.append(h('span', { class: 'pane-mark', title: t('{name}: end-to-end encrypted in this chamber. Only its members can read the content, with the key of the chamber on their devices.', { name: paneName(pane) }) }, 'E2E'));
      }
      const frame = h('section', { class: `pane ${pane}-pane`, 'aria-label': paneName(pane) }, bar, panes[pane]);
      frame.dataset.pane = pane;
      this.dragPane(bar, pane);
      if (pane === 'rooms') {
        // Taking hold of the button doesn't move the pane.
        this.minimize.addEventListener('pointerdown', (event) => event.stopPropagation());
        bar.append(this.minimize);
      }
      this.frames.set(pane, frame);
      this.element.append(frame);
    }
    this.element.append(this.strip, this.hint);
    new ResizeObserver(() => this.place()).observe(this.element);
    PHONE.addEventListener('change', () => this.place());
  }

  current(): LayoutNode {
    return this.layout;
  }

  /** Is the pane on the screen: in the arrangement, and of something the room shown has. */
  isShown(pane: PaneId): boolean {
    return this.shownPanes().includes(pane);
  }

  /** The panes of features the room shown doesn't have: they keep their place in the arrangement, and aren't shown. */
  private missing = new Set<PaneId>();

  /** Tells which panes the room shown has nothing for, e.g. the chat of a room without one. */
  setMissing(panes: PaneId[]): void {
    if (panes.length === this.missing.size && panes.every((pane) => this.missing.has(pane))) {
      return;
    }
    this.missing = new Set(panes);
    this.place();
    this.element.dispatchEvent(new CustomEvent('layout', { bubbles: true }));
  }

  /** The panes on the screen; the files when there would be none. */
  private shownPanes(): PaneId[] {
    const shown = panesIn(this.layout).filter((pane) => !this.missing.has(pane));
    return shown.length > 0 ? shown : ['files'];
  }

  private has(node: LayoutNode): boolean {
    return panesIn(node).some((pane) => !this.missing.has(pane));
  }

  /** Uses a new arrangement and keeps it. */
  set(layout: LayoutNode): void {
    this.layout = layout;
    saveLayout(layout);
    this.place();
    this.element.dispatchEvent(new CustomEvent('layout', { bubbles: true }));
  }

  /** Counts the dividers while placing, when they are only moved. */
  private next = 0;

  /**
   * Positions frames and dividers; on a phone, leaves it to the style sheet.
   * While a divider is dragged, the dividers are only moved, not made anew,
   * so the dragged one keeps the pointer.
   */
  place(rebuild = true): void {
    if (rebuild) {
      this.splitters.forEach((s) => s.remove());
      this.splitters = [];
    }
    this.next = 0;
    const shown = new Set(this.shownPanes());
    for (const [pane, frame] of this.frames) {
      frame.classList.toggle('hidden-pane', !shown.has(pane));
    }
    this.element.classList.toggle('stacked', PHONE.matches);
    // Away, the rooms pane is a strip at the left edge; the only pane shown stays.
    const away = !shown.has('rooms');
    this.strip.hidden = away ? PHONE.matches : true;
    this.minimize.disabled = shown.size < 2;
    if (PHONE.matches) {
      this.frames.forEach((frame) => frame.removeAttribute('style'));
      this.splitters.forEach((s) => s.remove());
      this.splitters = [];
      return;
    }
    const { clientWidth: w, clientHeight: hgt } = this.element;
    const left = away ? STRIP : 0;
    // Nothing of the arrangement is on the screen: the files alone.
    this.placeNode(this.has(this.layout) ? this.layout : { pane: 'files' }, { x: left, y: 0, w: w - left, h: hgt }, rebuild);
  }

  private placeNode(node: LayoutNode, rect: Rect, rebuild: boolean): void {
    if (!isSplit(node)) {
      const frame = this.frames.get(node.pane)!;
      Object.assign(frame.style, { left: `${rect.x}px`, top: `${rect.y}px`, width: `${rect.w}px`, height: `${rect.h}px` });
      return;
    }
    const row = node.dir === 'row';
    // The children with something on the screen share the place. What a missing pane leaves
    // goes to the panes with content, in the proportion of their sizes; the rooms pane keeps its own.
    const seen = node.children.map((child, i) => i).filter((i) => this.has(node.children[i]));
    const total = node.sizes.reduce((sum, size) => sum + size, 0) || 1;
    const fixed = seen.filter((i) => isRooms(node.children[i])).reduce((sum, i) => sum + node.sizes[i], 0);
    const others = seen.reduce((sum, i) => sum + node.sizes[i], 0) - fixed;
    const stretch = others > 0 ? (total - fixed) / others : total / (fixed || 1);
    const factor = (i: number) => (others > 0 && isRooms(node.children[i]) ? 1 : stretch);
    const available = ((row ? rect.w : rect.h) - GAP * (seen.length - 1)) / total;
    let offset = row ? rect.x : rect.y;
    seen.forEach((i, at) => {
      const length = Math.max(0, Math.round(available * node.sizes[i] * factor(i)));
      this.placeNode(node.children[i], row
        ? { x: offset, y: rect.y, w: length, h: rect.h }
        : { x: rect.x, y: offset, w: rect.w, h: length }, rebuild);
      offset += length;
      if (at < seen.length - 1) {
        const place = row
          ? { x: offset, y: rect.y, w: GAP, h: rect.h }
          : { x: rect.x, y: offset, w: rect.w, h: GAP };
        if (rebuild) {
          // Between two panes with content a size counts stretched; beside the rooms pane as it is.
          this.addSplitter(node, i, seen[at + 1], available * Math.min(factor(i), factor(seen[at + 1])), place);
        } else {
          const divider = this.splitters[this.next];
          if (divider) {
            Object.assign(divider.style, { left: `${place.x}px`, top: `${place.y}px`, width: `${place.w}px`, height: `${place.h}px` });
          }
        }
        this.next++;
        offset += GAP;
      }
    });
  }

  /** A divider between the children i and j of a split, which are beside each other on the screen; `available` is the length of a size of 1. */
  private addSplitter(node: Split, i: number, j: number, available: number, rect: Rect): void {
    const row = node.dir === 'row';
    const divider = h('div', {
      class: `splitter ${row ? 'vertical' : 'horizontal'}`, role: 'separator', tabindex: 0,
      'aria-orientation': row ? 'vertical' : 'horizontal',
      'aria-label': t('Divider'), title: t('Drag to resize; double-click to share equally'),
    });
    Object.assign(divider.style, { left: `${rect.x}px`, top: `${rect.y}px`, width: `${rect.w}px`, height: `${rect.h}px` });
    const minimum = (row ? MIN_WIDTH : MIN_HEIGHT) / Math.max(available, 1);
    const resize = (first: number) => {
      const pair = node.sizes[i] + node.sizes[j];
      const low = Math.min(minimum, pair / 2);
      node.sizes[i] = Math.min(pair - low, Math.max(low, first));
      node.sizes[j] = pair - node.sizes[i];
    };
    divider.addEventListener('pointerdown', (event) => {
      event.preventDefault();
      divider.setPointerCapture(event.pointerId);
      divider.classList.add('dragging');
      const start = row ? event.clientX : event.clientY;
      const size = node.sizes[i];
      const move = (e: PointerEvent) => {
        resize(size + ((row ? e.clientX : e.clientY) - start) / available);
        this.place(false);
      };
      const up = () => {
        divider.classList.remove('dragging');
        divider.removeEventListener('pointermove', move);
        divider.removeEventListener('pointerup', up);
        divider.removeEventListener('pointercancel', up);
        this.set(this.layout);
      };
      divider.addEventListener('pointermove', move);
      divider.addEventListener('pointerup', up);
      divider.addEventListener('pointercancel', up);
    });
    divider.addEventListener('dblclick', () => {
      resize((node.sizes[i] + node.sizes[j]) / 2);
      this.set(this.layout);
    });
    divider.addEventListener('keydown', (event) => {
      const back = row ? 'ArrowLeft' : 'ArrowUp';
      const forward = row ? 'ArrowRight' : 'ArrowDown';
      if (event.key === back || event.key === forward) {
        event.preventDefault();
        resize(node.sizes[i] + (event.key === back ? -0.03 : 0.03));
        saveLayout(this.layout);
        this.place(false);
      }
    });
    this.splitters.push(divider);
    this.element.append(divider);
  }

  /** Dragging a pane by its bar onto another pane. */
  private dragPane(bar: HTMLElement, pane: PaneId): void {
    bar.addEventListener('pointerdown', (event) => {
      if (PHONE.matches || event.button !== 0) {
        return;
      }
      const startX = event.clientX;
      const startY = event.clientY;
      let dragging = false;
      let drop: Drop | null = null;
      bar.setPointerCapture(event.pointerId);
      const move = (e: PointerEvent) => {
        if (!dragging && Math.hypot(e.clientX - startX, e.clientY - startY) < 6) {
          return;
        }
        dragging = true;
        this.element.classList.add('moving-pane');
        drop = this.dropAt(e.clientX, e.clientY, pane);
      };
      const end = (apply: boolean) => () => {
        bar.removeEventListener('pointermove', move);
        bar.removeEventListener('pointerup', up);
        bar.removeEventListener('pointercancel', cancel);
        this.element.classList.remove('moving-pane');
        this.hint.hidden = true;
        if (apply && dragging && drop) {
          const { target, zone } = drop;
          this.set(target === 'edge'
            ? dockPane(this.layout, pane, zone as Side)
            : zone === 'center'
              ? swapPanes(this.layout, pane, target)
              : movePane(this.layout, pane, target, zone));
        }
      };
      const up = end(true);
      const cancel = end(false);
      bar.addEventListener('pointermove', move);
      bar.addEventListener('pointerup', up);
      bar.addEventListener('pointercancel', cancel);
    });
  }

  /** The pane under the pointer and where on it, shown by the hint. */
  private dropAt(x: number, y: number, moving: PaneId): Drop | null {
    const base = this.element.getBoundingClientRect();
    const show = (rect: Rect, label: string) => {
      Object.assign(this.hint.style, { left: `${rect.x}px`, top: `${rect.y}px`, width: `${rect.w}px`, height: `${rect.h}px` });
      this.hint.textContent = label;
      this.hint.hidden = false;
    };
    // The window's edges: the pane goes along the whole edge.
    const edges: [Side, number][] = [['left', x - base.left], ['right', base.right - x], ['top', y - base.top], ['bottom', base.bottom - y]];
    const [edge, distance] = edges.sort((a, b) => a[1] - b[1])[0];
    if (distance >= 0 && distance < EDGE && panesIn(this.layout).length > 1) {
      const strip = edge === 'left' || edge === 'right' ? base.width * 0.3 : base.height * 0.5;
      show({
        left: { x: 0, y: 0, w: strip, h: base.height },
        right: { x: base.width - strip, y: 0, w: strip, h: base.height },
        top: { x: 0, y: 0, w: base.width, h: strip },
        bottom: { x: 0, y: base.height - strip, w: base.width, h: strip },
      }[edge], '');
      return { target: 'edge', zone: edge };
    }
    for (const pane of panesIn(this.layout)) {
      const frame = this.frames.get(pane)!;
      const r = frame.getBoundingClientRect();
      if (x < r.left || x > r.right || y < r.top || y > r.bottom) {
        continue;
      }
      if (pane === moving) {
        break;
      }
      const rx = (x - r.left) / r.width;
      const ry = (y - r.top) / r.height;
      const edges: [Zone, number][] = [['left', rx], ['right', 1 - rx], ['top', ry], ['bottom', 1 - ry]];
      edges.sort((a, b) => a[1] - b[1]);
      const zone: Zone = edges[0][1] > 0.3 ? 'center' : edges[0][0];
      const left = r.left - base.left;
      const top = r.top - base.top;
      const hint = {
        left: { x: left, y: top, w: r.width / 2, h: r.height },
        right: { x: left + r.width / 2, y: top, w: r.width / 2, h: r.height },
        top: { x: left, y: top, w: r.width, h: r.height / 2 },
        bottom: { x: left, y: top + r.height / 2, w: r.width, h: r.height / 2 },
        center: { x: left, y: top, w: r.width, h: r.height },
      }[zone];
      show(hint, zone === 'center' ? t('Swap') : '');
      return { target: pane, zone };
    }
    this.hint.hidden = true;
    return null;
  }
}
