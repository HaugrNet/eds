import { CredentialType, MemberRole, FeatureState, TrustLevel, type Circle, type Member, type Trustee } from './api';
import type { Invitation } from './invitation';
import { friendly } from './messages';
import { api, base64, login, session } from './session';
import { t } from './i18n';
import { createKeys, loadKeys } from './e2e/identity';
import { adoptInvitation, changeKey, ensureKeys, forgetRooms, inviteSecret, makeChamber, memberKeyState, repair, roomState, secretFor, type RoomKind } from './e2e/rooms';
import { switchFeature } from './features/features';

/**
 * Creates a room, or a chamber: a room whose content Rune encrypts before
 * the stone gets it (E2E.md). The creator administers it. The choice is
 * made here and stays. A member without keys gets them with their first
 * chamber; then the recovery key is returned, to be shown once. The features
 * named are switched on in it: the chat, unless told otherwise.
 */
export async function createRoom(name: string, chamber: boolean, features: string[] = ['chat']): Promise<{ circleId: string; recoveryKey: string | null }> {
  // The keys first: without them there is no chamber, and no room is left behind.
  const recoveryKey = chamber ? await ensureKeys() : null;
  const response = await api.circles.postCircles({ processCircleRequest: { circleName: name } });
  const circleId = response.circleId ?? '';
  if (chamber) {
    await makeChamber(circleId);
  }
  // The features chosen for it; one the stone doesn't offer is left out, and can be switched on in the room later.
  for (const feature of features) {
    await switchFeature(circleId, feature, FeatureState.On).catch(() => undefined);
  }
  return { circleId, recoveryKey };
}

/** What an invitation into a room gave: a token for a new person, or nothing if the name already had an account. */
export interface InviteResult {
  token?: string;
  /** For a chamber: the invite secret, which goes into the link and never to the stone. */
  secret?: Uint8Array;
  message: string;
}

/**
 * Invites a name into a room; only administrators of the room may. Into a
 * chamber, the room key goes along: for a new person under an invite
 * secret, which only the link carries; for someone who has keys already,
 * wrapped to their public key.
 */
export async function inviteToRoom(circleId: string, name: string, trustLevel: TrustLevel): Promise<InviteResult> {
  const chamber = (await roomState(circleId)).kind !== 'room';
  let secret: string | undefined;
  let invite: Uint8Array | undefined;
  if (chamber) {
    const existing = (await listUsers()).find((user) => (user.accountName ?? '').toLowerCase() === name.toLowerCase());
    if (existing?.memberId) {
      // Without keys they get the room key later, by a repair.
      secret = (await secretFor(circleId, existing.memberId, existing.publicKey)) ?? undefined;
    } else {
      const made = await inviteSecret(circleId, name);
      secret = made.secret;
      invite = made.inviteSecret;
    }
  }
  const response = await api.members.inviteMember({ processMemberRequest: {
    credentialType: CredentialType.Session,
    credential: session()?.token,
    newAccountName: name,
    circleId,
    trustLevel,
    secret,
  } });
  if (response.returnCode !== 200) {
    throw new Error(friendly(response.returnMessage));
  }
  if (response.invitation) {
    return { token: response.invitation, secret: invite, message: t('Invitation for {name} created.', { name }) };
  }
  if (invite && response.memberId) {
    // The name had an account after all, which the list didn't show: the
    // secret under the invite secret is of no use to it. An administrator's
    // repair gives the key.
    await api.circles.putCircleTrusteeById({ circleId, memberId: response.memberId, processTrusteeRequest: { secret: '' } }).catch(() => undefined);
  }
  return { message: t('{name} already uses Rune, and was added to the room.', { name }) };
}

/**
 * Accepts an invitation with the chosen passphrase, and logs in. An
 * invitation into a chamber carries the invite secret: then the new member
 * gets keys, the room key is taken over with the invite secret and wrapped
 * to them, and the recovery key is returned, to be shown once. Nobody else
 * has to be online for any of it.
 */
export async function acceptInvitation(invitation: Invitation, passphrase: string): Promise<string | null> {
  const response = await api.members.updateMember({ processMemberRequest: {
    accountName: invitation.name,
    credentialType: CredentialType.Invitation,
    credential: invitation.token,
    newCredential: base64(passphrase),
  } });
  if (response.returnCode !== 200) {
    const message = response.returnMessage ?? '';
    throw new Error(/invitation|Account does not/i.test(message)
      ? t('This invitation is no longer valid. Please ask for a new one.')
      : friendly(message));
  }
  await login(invitation.name, passphrase);
  if (!invitation.secret) {
    return null;
  }
  await loadKeys();
  const recoveryKey = await createKeys();
  await adoptInvitation(invitation.name, invitation.secret);
  return recoveryKey;
}

/**
 * Changes the passphrase. EDS only allows that with the current passphrase,
 * not with the session, and the old session is replaced by a new login.
 */
export async function changePassphrase(current: string, next: string): Promise<void> {
  const accountName = session()?.accountName ?? '';
  const response = await api.members.updateMember({ processMemberRequest: {
    accountName,
    credentialType: CredentialType.Passphrase,
    credential: base64(current),
    newCredential: base64(next),
  } });
  if (response.returnCode !== 200) {
    throw new Error(response.returnCode === 403 ? t('The current passphrase is wrong.') : friendly(response.returnMessage));
  }
  await login(accountName, next);
}

/** What each trust level means in Rune. */
export const ACCESS: Record<string, string> = {
  [TrustLevel.Read]: t('Can read'),
  [TrustLevel.Write]: t('Can read and write'),
  [TrustLevel.Admin]: t('Administrator'),
  [TrustLevel.Sysop]: t('Administrator'),
};

export function isAdministrator(trustee: Trustee | undefined): boolean {
  return trustee?.trustLevel === TrustLevel.Admin || trustee?.trustLevel === TrustLevel.Sysop;
}

/** The members of a room: administrators first, then by name. */
export async function listMembers(circleId: string): Promise<Trustee[]> {
  const response = await api.circles.getCircleTrustees({ circleId });
  return (response.trustees ?? []).sort((a, b) =>
    Number(isAdministrator(b)) - Number(isAdministrator(a)) || (a.accountName ?? '').localeCompare(b.accountName ?? ''));
}

/** Changes the access of a member; only administrators of the room may. */
export async function setAccess(circleId: string, memberId: string, trustLevel: TrustLevel): Promise<void> {
  await api.circles.putCircleTrusteeById({ circleId, memberId, processTrusteeRequest: { trustLevel } });
}

/**
 * Removes a member from a room; with the own ID, leaves the room. Removing
 * others needs an administrator, leaving doesn't.
 */
export async function removeMember(circleId: string, memberId: string): Promise<void> {
  const leaving = memberId === session()?.memberId;
  const chamber = !leaving && (await roomState(circleId)).kind === 'ready';
  await api.circles.deleteCircleTrusteeById({ circleId, memberId });
  if (leaving) {
    forgetRooms(circleId);
  } else if (chamber) {
    // The one who was removed had the key of the chamber: it gets a new one,
    // for those who are left.
    await changeKey(circleId, await listMembers(circleId));
  }
}

/**
 * Repair on sight (E2E.md): an administrator of a chamber who sees its
 * members gives the room key to those who have keys and not the current
 * room key. Returns how many got it.
 */
export async function repairRoom(circleId: string, members: Trustee[]): Promise<number> {
  const me = members.find((member) => member.memberId === session()?.memberId);
  if (!isAdministrator(me) || (await roomState(circleId)).kind !== 'ready') {
    return 0;
  }
  return repair(circleId, members);
}

/** What tending a chamber found: how this device stands to it, how many members got its key, and whether to look again. */
export interface Tended {
  kind: RoomKind;
  given: number;
  again: boolean;
}

/**
 * Tends the keys of a chamber which is open (E2E.md, "Repair"), so nobody
 * has to look for a place to hand a key over. An administrator gives the
 * key to the members who have keys and wait for it; `again` says that
 * someone is still without it. A member who waits is told so, to look again
 * until an administrator's app gave them the key. A room is left alone.
 */
export async function tendChamber(circleId: string): Promise<Tended> {
  const state = await roomState(circleId);
  if (state.kind === 'waiting' || state.kind === 'behind') {
    return { kind: state.kind, given: 0, again: true };
  }
  if (state.kind !== 'ready' || !state.current) {
    return { kind: state.kind, given: 0, again: false };
  }
  const me = session()?.memberId;
  let members = await listMembers(circleId);
  if (!isAdministrator(members.find((member) => member.memberId === me))) {
    return { kind: 'ready', given: 0, again: false };
  }
  const given = await repair(circleId, members);
  if (given > 0) {
    members = await listMembers(circleId);
  }
  const version = state.current.version;
  const again = members.some((member) => member.memberId !== me && ['none', 'behind'].includes(memberKeyState(member, version)));
  return { kind: 'ready', given, again };
}

/**
 * The memberships of the logged-in member, with their access. Not GET
 * /circles: with the default setting eds.show.all.circles, that lists every
 * Circle in EDS.
 */
export async function myTrustees(): Promise<Trustee[]> {
  const response = await api.trustees.fetchTrustees({ fetchTrusteeRequest: {
    credentialType: CredentialType.Session,
    credential: session()?.token,
  } });
  if (response.returnCode !== 200) {
    // A member without rooms has no Trustee records, which EDS reports as a
    // warning; anything else is an error.
    if (response.returnCode === 404 || response.returnCode === 401) {
      return [];
    }
    throw new Error(friendly(response.returnMessage));
  }
  return response.trustees ?? [];
}

/** The rooms the logged-in member is in. */
export async function myRooms(): Promise<Circle[]> {
  return (await myTrustees()).map((trustee) => ({ circleId: trustee.circleId, circleName: trustee.circleName, avatar: trustee.circleAvatar }));
}

/** The logged-in member's own account, e.g. for the avatar in the header. */
export async function myAccount(): Promise<Member | undefined> {
  const memberId = session()?.memberId;
  return memberId ? (await api.members.getMemberById({ memberId })).members?.[0] : undefined;
}

/** Sets the own avatar (an ID of avatars.ts), or removes it with ''. */
export async function setOwnAvatar(avatar: string): Promise<void> {
  const response = await api.members.updateMember({ processMemberRequest: {
    credentialType: CredentialType.Session,
    credential: session()?.token,
    avatar,
  } });
  if (response.returnCode !== 200) {
    throw new Error(friendly(response.returnMessage));
  }
  document.dispatchEvent(new CustomEvent('avatars-changed'));
}

/** Sets a room's avatar, or removes it with ''; administrators of the room may. */
export async function setRoomAvatar(circleId: string, avatar: string): Promise<void> {
  await api.circles.putCircleById({ circleId, processCircleRequest: { avatar } });
  document.dispatchEvent(new CustomEvent('avatars-changed'));
}

/** Renames a room; administrators of the room may. EDS has no two rooms of the same name. */
export async function renameRoom(circleId: string, name: string): Promise<void> {
  await api.circles.putCircleById({ circleId, processCircleRequest: { circleName: name } });
  document.dispatchEvent(new CustomEvent('room-renamed'));
}

/** Every room in EDS; only for the System Administrator. */
export async function allRooms(): Promise<Circle[]> {
  return (await api.circles.getCircles()).circles ?? [];
}

/** All users (accounts) of EDS, by name. */
export async function listUsers(): Promise<Member[]> {
  const response = await api.members.getMembers();
  return (response.members ?? []).sort((a, b) => (a.accountName ?? '').localeCompare(b.accountName ?? ''));
}

export function isSystemAdministrator(member: Member | undefined): boolean {
  return member?.memberRole === MemberRole.Admin;
}

/**
 * The first System Administrator: the account which claimed the stone. A
 * stone may have several administrators; only this one makes, unmakes or
 * deletes them, and it can't lose its role or be deleted.
 */
export function isFirstAdministrator(member: Member | undefined): boolean {
  return member?.firstAdministrator === true;
}

/** Makes a user an administrator or an administrator a user; only the first System Administrator may. */
export async function setRole(memberId: string, role: MemberRole): Promise<void> {
  await api.members.putMemberById({ memberId, processMemberRequest: { memberRole: role } });
}

/**
 * The rooms of a user. EDS shows all of them, or with the setting
 * eds.show.trustees off only the rooms shared with the logged-in member.
 */
export async function roomsOf(memberId: string): Promise<Circle[]> {
  const response = await api.members.getMemberById({ memberId });
  return (response.circles ?? []).sort((a, b) => (a.circleName ?? '').localeCompare(b.circleName ?? ''));
}

/**
 * Creates a user with a passphrase, without invitation; only a System
 * Administrator may, and only the first may make it an administrator. (An
 * invitation always makes a user.)
 */
export async function createUser(name: string, passphrase: string, role: MemberRole = MemberRole.Standard): Promise<string | undefined> {
  const response = await api.members.postMembers({ processMemberRequest: {
    newAccountName: name,
    newCredential: base64(passphrase),
    memberRole: role,
  } });
  return response.memberId;
}

/** Deletes a user; only the System Administrator may. */
export async function deleteUser(memberId: string): Promise<void> {
  await api.members.deleteMemberById({ memberId });
}

/**
 * Adds an existing user to a room. Only administrators of the room may, as
 * EDS needs their access to the room's key to give it to the user.
 */
export async function addMember(circleId: string, memberId: string, trustLevel: TrustLevel): Promise<void> {
  let secret: string | undefined;
  if ((await roomState(circleId)).kind !== 'room') {
    // Into a chamber the room key goes along, if the user has keys.
    const user = (await api.members.getMemberById({ memberId })).members?.[0];
    secret = (await secretFor(circleId, memberId, user?.publicKey)) ?? undefined;
  }
  await api.circles.postCircleTrustees({ circleId, processTrusteeRequest: { memberId, trustLevel, secret } });
}

/** What the logged-in member may do in a room. */
export interface RoomAccess {
  write: boolean;
  administrator: boolean;
}

export async function accessIn(circleId: string): Promise<RoomAccess> {
  try {
    const me = (await listMembers(circleId)).find((trustee) => trustee.memberId === session()?.memberId);
    return { write: !!me && me.trustLevel !== TrustLevel.Read, administrator: isAdministrator(me) };
  } catch {
    return { write: false, administrator: false };
  }
}
