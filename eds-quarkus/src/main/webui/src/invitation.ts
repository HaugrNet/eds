import { invitationFragment, parseInvitationFragment, type Invitation } from './e2e/convention';

/**
 * The invitation link: the app's address with the invited name and the
 * one-time token in the fragment, and for a chamber the invite secret, under
 * which the room key waits for the invited person (E2E.md). The fragment
 * never leaves the browser, so neither token nor secret is sent to any
 * server or written to a log when the link is opened; the invite secret
 * never reaches the stone at all.
 */
export type { Invitation };

export function invitationLink(invitation: Invitation): string {
  // Rune's own address, wherever it is served (the stone's root, usually).
  return `${location.origin}${location.pathname.replace(/[^/]*$/, '')}${invitationFragment(invitation)}`;
}

/** The invitation in the address of the page, if it was opened from one. */
export function invitationFromLocation(): Invitation | null {
  return parseInvitationFragment(location.hash);
}

/** Removes the invitation from the address, so it isn't kept in the history. */
export function forgetInvitationLocation(): void {
  history.replaceState(null, '', location.pathname + location.search);
}
