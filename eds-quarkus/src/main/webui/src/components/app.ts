import type { Circle } from '../api';
import { session } from '../session';
import { chat, offerChat, type ChatMessage } from '../chat';
import { forgetKeys, KEYS_CHANGED, loadKeys } from '../e2e/identity';
import { forgetRooms, isChamberRoom } from '../e2e/rooms';
import { h } from '../dom';
import { accessIn, isSystemAdministrator, myAccount, tendChamber } from '../account';
import { forgetRoom, startNotifications } from '../notifications';
import { restoreDownloads } from '../downloads';
import { brandMark } from './brand';
import { openMembersDialog } from './members-dialog';
import { openFeaturesDialog, FEATURES_CHANGED } from './features-dialog';
import { EdsCalendar } from './calendar-pane';
import { EdsPlans } from './plans-pane';
import { EdsLocation } from './location-pane';
import { FEATURE_PANES, paneName, togglePane } from '../layout';
import { roomFeatures, usable } from '../features/features';
import { socket } from '../socket';
import { startReporting, stopReporting } from '../features/reporter';
import type { CircleFeature } from '../api';
import { MEMBERSHIP_CHANGED, membershipChanged } from './room-members';
import { forgetTokens } from '../media';
import { EdsCircles } from './circles';
import { EdsChat } from './chat-pane';
import { EdsFiles } from './files';
import { EdsServer } from './server-settings';
import { openServerWindow } from './server-window';
import { StatusLine } from './status-line';
import { AccountMenu } from './account-menu';
import { avatar } from '../avatars';
import { Workspace } from './workspace';
import { t } from '../i18n';
import type { PaneId } from '../layout';
import { calls } from '../calls';
import { startReminders, stopReminders } from '../reminders';
import { EdsCallPanel } from './call-panel';
import { clearNotices } from './notices';

type View = PaneId;

/**
 * The signed-in app: on a desktop the panes (rooms and users, chat, files)
 * as each person arranges them (see Workspace), on a phone one pane at a
 * time, switched in the header. With the rooms pane hidden, the header has
 * a choice of the room. The System Administrator also gets the settings of
 * EDS, in a window over the panes; a line at the bottom tells about the server.
 */
/** What a room has, each with a pane and a tab: the chat, the files and the further features. */
const ROOM_PANES: PaneId[] = ['chat', 'files', ...FEATURE_PANES];

/** Phones show one pane at a time (styles.css). */
const PHONE = window.matchMedia('(max-width: 760px)');

export class EdsApp extends HTMLElement {
  private circlesPane = new EdsCircles();
  private chatPane = new EdsChat();
  private filesPane = new EdsFiles();
  private serverSettings = new EdsServer();
  private statusLine = new StatusLine();
  /** The call this device is in, and the calls ringing here, over the panes. */
  private callPanel = new EdsCallPanel();
  private circleTitle = h('span', { class: 'circle-name' });
  private circle: Circle | null = null;
  private administrator = false;
  /** The look after the keys of the chamber shown, and the chamber this device waits for the key of. */
  private keyWatch: number | undefined;
  private keyWatchRun = 0;
  private waitsIn: string | undefined;
  private listeners = new AbortController();
  private members = h('button', { class: 'secondary', hidden: true, onclick: () => {
    if (this.circle) {
      openMembersDialog(this.circle, this.administrator);
    }
  } }, t('Members'));
  private features = h('button', { class: 'secondary', hidden: true, onclick: () => {
    if (this.circle) {
      openFeaturesDialog(this.circle, this.administrator);
    }
  } }, t('Features'));
  private calendarPane = new EdsCalendar();
  private plansPane = new EdsPlans();
  private locationPane = new EdsLocation();
  private workspace = new Workspace({
    rooms: this.circlesPane, chat: this.chatPane, files: this.filesPane,
    calendar: this.calendarPane, plans: this.plansPane, location: this.locationPane,
  });
  /** The room, when the rooms pane is hidden. */
  private roomPicker = h('select', { class: 'room-picker', 'aria-label': t('Room'), hidden: true });
  private nav = new Map<View, HTMLButtonElement>();

  connectedCallback(): void {
    this.chatPane.openFolder = () => this.filesPane.currentFolderId();
    // Paused downloads of this member, from before a reload.
    void restoreDownloads();
    startReporting(session()?.memberId ?? '');
    // New messages from all rooms: counts and notifications.
    startNotifications((circleId) => {
      this.circlesPane.open(circleId);
    });
    const tabs = h('nav', { class: 'tabs', 'aria-label': t('Panes') });
    const views: [View, string][] = [['rooms', t('Rooms')], ['chat', t('Chat')], ['files', t('Files')],
      ['calendar', t('Calendar')], ['plans', t('Plans')], ['location', t('Location')]];
    for (const [view, label] of views) {
      // What a room has: the chat, the files and its further features. Their tabs show for the
      // room shown, also on a desktop, where a tab shows its pane or puts it away.
      const feature = ROOM_PANES.includes(view);
      const button = h('button', { class: feature ? 'tab feature-tab' : 'tab', hidden: feature, onclick: () => this.openView(view) }, label);
      this.nav.set(view, button);
      tabs.append(button);
    }
    const header = h('header', { class: 'top' },
      brandMark('brand'),
      h('strong', {}, 'Rune'),
      this.roomPicker,
      this.circleTitle,
      this.members,
      this.features,
      tabs,
      new AccountMenu(this.workspace).element);
    this.replaceChildren(header, this.workspace.element, this.statusLine.element, this.callPanel);
    // Calls, and the calls set for a time in the calendars of the rooms.
    calls.start();
    startReminders();
    this.workspace.place();
    void this.checkAdministrator();
    // "Server settings" of the account menu.
    this.addEventListener('open-server', () => openServerWindow(this.serverSettings));

    // The room picker follows the rooms pane: its rooms, and whether it's shown.
    this.roomPicker.addEventListener('change', () => this.circlesPane.open(this.roomPicker.value));
    this.addEventListener('rooms', () => this.fillRoomPicker());
    this.addEventListener('layout', () => { this.fillRoomPicker(); this.markTabs(); });
    PHONE.addEventListener('change', () => this.markTabs(), { signal: this.listeners.signal });

    // A room was chosen (circles.ts). Not named "select": that is also the
    // browser's event for text selected in a field, which bubbles up here.
    this.addEventListener('room-select', (event) => {
      const circle = (event as CustomEvent<Circle>).detail;
      this.circle = circle;
      this.showTitle(circle);
      this.members.hidden = false;
      this.features.hidden = false;
      this.roomPicker.value = circle.circleId ?? '';
      void this.checkAccess();
      void this.chatPane.show(circle);
      this.filesPane.show(circle);
      this.showFeatures(circle);
      this.setView('chat');
    });
    // A feature was switched in the room shown: its tab follows.
    socket.addEventListener('feature', (event) => {
      if ((event as CustomEvent<{ circleId?: string }>).detail.circleId === this.circle?.circleId) {
        void this.showFeatureTabs();
      }
    }, { signal: this.listeners.signal });
    document.addEventListener(FEATURES_CHANGED, () => void this.showFeatureTabs(), { signal: this.listeners.signal });
    // A file shared in the chat shows up in the files at once.
    this.addEventListener('shared', (event) => {
      if ((event as CustomEvent<ChatMessage>).detail.dataId) {
        void this.filesPane.refresh();
      }
    });
    // Access changed in the open room, e.g. in the rooms pane.
    document.addEventListener(MEMBERSHIP_CHANGED, (event) => {
      if ((event as CustomEvent<string>).detail === this.circle?.circleId) {
        void this.checkAccess();
      }
    }, { signal: this.listeners.signal });
    // Any change of the room's files, also by others, reloads the files
    // pane; a batch of uploads reloads it once.
    let pending: number | undefined;
    chat.addEventListener('changed', (event) => {
      if ((event as CustomEvent<{ circleId?: string }>).detail.circleId === this.circle?.circleId) {
        window.clearTimeout(pending);
        pending = window.setTimeout(() => void this.filesPane.refresh(), 400);
      }
    }, { signal: this.listeners.signal });
    // After leaving a room, it's gone from the list and the panes.
    document.addEventListener('room-left', (event) => {
      if ((event as CustomEvent<string>).detail === this.circle?.circleId) {
        this.circle = null;
        this.administrator = false;
        this.circleTitle.replaceChildren();
        delete this.dataset.room;
        delete this.dataset.roomId;
        this.members.hidden = true;
        this.features.hidden = true;
        void this.chatPane.show(null);
        this.filesPane.show(null);
        this.showFeatures(null);
        this.setView('rooms');
      }
      forgetRoom((event as CustomEvent<string>).detail);
      stopReporting((event as CustomEvent<string>).detail);
      void this.circlesPane.load();
    }, { signal: this.listeners.signal });
    // With the member's keys on this device, chambers open: the room shown
    // is read again.
    document.addEventListener(KEYS_CHANGED, () => {
      forgetRooms();
      void this.circlesPane.load();
      if (this.circle) {
        void this.chatPane.show(this.circle);
        void this.filesPane.refresh();
        this.showFeatures(this.circle);
        this.watchKeys();
      }
    }, { signal: this.listeners.signal });
    this.setView('rooms');
    void chat.connect().catch(() => undefined);
    // The member's keys for chambers, as far as this device has them; and a
    // stone which doesn't offer the chat yet gets it from its administrator.
    void loadKeys().catch(() => undefined);
    void myAccount().then((account) => (isSystemAdministrator(account) ? offerChat() : undefined)).catch(() => undefined);
  }

  /**
   * Administrators of the room may invite and change access; members who
   * may write can chat, upload and change files; who can only read, reads.
   * EDS checks it anyway, this only hides what would be refused.
   */
  private async checkAccess(): Promise<void> {
    const circle = this.circle;
    this.administrator = false;
    this.filesPane.setWritable(false);
    this.chatPane.setWritable(null);
    if (circle?.circleId) {
      const access = await accessIn(circle.circleId);
      // The same room by its ID: the list of rooms may have been read anew
      // meanwhile, which replaces the object (its name or avatar may change).
      if (this.circle?.circleId === circle.circleId) {
        this.administrator = access.administrator;
        this.filesPane.setWritable(access.write);
        this.chatPane.setWritable(access.write);
      }
    }
    this.watchKeys();
  }

  /**
   * Tends the keys of the chamber shown, now and as long as someone waits:
   * an administrator's Rune gives the key to members who wait for it, and
   * the Rune of a member who waits opens the chamber when the key came.
   * Neither has to do anything, or to open the chamber anew. It looks often
   * at first, then seldom.
   */
  private watchKeys(since = Date.now()): void {
    window.clearTimeout(this.keyWatch);
    const run = ++this.keyWatchRun;
    const circle = this.circle;
    const circleId = circle?.circleId;
    if (!circle || !circleId) {
      this.waitsIn = undefined;
      return;
    }
    void (async () => {
      const tended = await tendChamber(circleId).catch(() => null);
      // The room shown is the same by its ID: its name or avatar may have been read anew meanwhile.
      const shown = this.circle;
      if (run !== this.keyWatchRun || !shown || shown.circleId !== circleId) {
        return;
      }
      const waited = this.waitsIn === circleId;
      this.waitsIn = tended && (tended.kind === 'waiting' || tended.kind === 'behind') ? circleId : undefined;
      if (waited && tended?.kind === 'ready') {
        // The key came: the chamber is shown anew, readable.
        void this.circlesPane.load();
        void this.chatPane.show(shown);
        void this.filesPane.refresh();
        this.showFeatures(shown);
      }
      if (tended && tended.given > 0) {
        // Who has the key is shown with the members; this looks again, too.
        membershipChanged(circleId);
      } else if (!tended || tended.again) {
        this.keyWatch = window.setTimeout(() => this.watchKeys(since), Date.now() - since < 120_000 ? 5_000 : 30_000);
      }
    })();
  }

  /** The System Administrator gets more in the status line, from the settings of EDS. */
  private async checkAdministrator(): Promise<void> {
    let administrator = false;
    try {
      administrator = isSystemAdministrator(await myAccount());
    } catch {
      // Without the account, there are no settings of EDS.
    }
    if (administrator && this.isConnected) {
      this.statusLine.setAdministrator();
      void this.serverSettings.load();
    }
  }

  /** The open room in the header, with its avatar. */
  private showTitle(circle: Circle): void {
    const mark = h('span', { class: 'chamber-mark', hidden: true, title: t('A chamber: end-to-end encrypted. Only its members can read the content, with the key of the chamber on their devices.') }, t('Chamber'));
    this.circleTitle.replaceChildren(avatar('room', circle.avatar, circle.circleName ?? '', 'small'), h('span', {}, circle.circleName ?? ''), mark);
    // The panes with the content of a chamber look different from those of a room (styles.css).
    const circleId = circle.circleId ?? '';
    if (this.dataset.roomId !== circleId) {
      delete this.dataset.room;
    }
    this.dataset.roomId = circleId;
    void isChamberRoom(circleId).then((chamber) => {
      if (this.circle?.circleId === circleId) {
        this.dataset.room = chamber ? 'chamber' : 'room';
        mark.hidden = !chamber;
      }
    }).catch(() => undefined);
  }

  /** Shows the room picker when the rooms pane is hidden (on a desktop). */
  private fillRoomPicker(): void {
    const rooms = this.circlesPane.rooms();
    const current = this.circle?.circleId ?? '';
    // The open room's name or avatar may have changed.
    const updated = rooms.find((room) => room.circleId === current);
    if (updated && this.circle) {
      this.circle = updated;
      this.showTitle(updated);
      this.filesPane.renamed(updated);
    }
    this.roomPicker.replaceChildren(
      h('option', { value: '', disabled: true, selected: !current }, t('Choose a room…')),
      ...rooms.map((room) => h('option', { value: room.circleId ?? '', selected: room.circleId === current }, room.circleName ?? '')));
    this.roomPicker.hidden = this.workspace.isShown('rooms');
    // Without the room's name in the picker, the header shows it.
    this.circleTitle.hidden = !this.roomPicker.hidden;
  }

  disconnectedCallback(): void {
    window.clearTimeout(this.keyWatch);
    this.keyWatchRun++;
    this.listeners.abort();
    this.statusLine.close();
    stopReminders();
    calls.stop();
    clearNotices();
    chat.close();
    forgetTokens();
    forgetRooms();
    forgetKeys();
    stopReporting();
  }

  /** The panes of the features turn to the room, and their tabs show for the features it has. */
  private showFeatures(circle: Circle | null): void {
    void this.calendarPane.show(circle);
    void this.plansPane.show(circle);
    void this.locationPane.show(circle);
    void this.showFeatureTabs();
  }

  private async showFeatureTabs(): Promise<void> {
    const circleId = this.circle?.circleId;
    let features: CircleFeature[] | null = [];
    try {
      features = circleId ? await roomFeatures(circleId) : [];
    } catch {
      // Without an answer: the chat and the files, as far as they go.
      features = null;
    }
    if (circleId !== this.circle?.circleId) {
      return;
    }
    // Every room has files; the chat and the rest are there when the room has them.
    const has = (pane: PaneId) => !!circleId && (pane === 'files' || (features ? usable(features, pane) : pane === 'chat'));
    for (const pane of ROOM_PANES) {
      const button = this.nav.get(pane);
      if (button) {
        button.hidden = !has(pane);
      }
    }
    // The pane of what the room lacks keeps its place in the arrangement and isn't shown; without a room all are.
    const missing = circleId ? ROOM_PANES.filter((pane) => !has(pane)) : [];
    this.workspace.setMissing(missing);
    if (missing.includes(this.dataset.view as PaneId)) {
      this.setView('files');
    }
    this.markTabs();
  }

  /**
   * A tab was chosen. On a phone its pane shows alone. On a desktop the tab
   * of the chat, the files or a feature shows its pane, or puts it away when
   * it shows: the tab is the switch of the pane. The last pane stays.
   */
  private openView(view: View): void {
    if (ROOM_PANES.includes(view) && !PHONE.matches) {
      // Away, it comes back where it was.
      const shown = this.workspace.isShown(view);
      this.workspace.set(togglePane(this.workspace.current(), view));
      if (shown) {
        return;
      }
    }
    this.setView(view);
  }

  private setView(view: View): void {
    this.dataset.view = view;
    this.markTabs();
  }

  /**
   * Marks the tabs: on a phone the one whose pane shows alone; on a desktop,
   * where only the tabs of features show, those whose pane is in the layout.
   */
  private markTabs(): void {
    const phone = PHONE.matches;
    for (const [name, button] of this.nav) {
      const shown = phone ? name === this.dataset.view : this.workspace.isShown(name);
      button.setAttribute('aria-pressed', String(shown));
      // On a desktop the tab says what a click does.
      button.title = phone ? '' : shown ? t('Hide the pane "{name}"', { name: paneName(name) }) : t('Show the pane "{name}"', { name: paneName(name) });
    }
  }
}

customElements.define('eds-app', EdsApp);
